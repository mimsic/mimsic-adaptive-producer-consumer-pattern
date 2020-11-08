package com.github.mimsic.pcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class StandardThreadPoolExecutorConfigurer implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardThreadPoolExecutorConfigurer.class);
    private static final String THREAD_NAME = "StandardThreadPoolExecutor-";

    private final AtomicLong threadNumber= new AtomicLong();
    private final StandardThreadPoolExecutorConfig standardThreadPoolExecutorConfig;

    private ThreadPoolExecutor standardThreadPoolExecutor;

    @Autowired
    public StandardThreadPoolExecutorConfigurer(StandardThreadPoolExecutorConfig standardThreadPoolExecutorConfig) {
        this.standardThreadPoolExecutorConfig = standardThreadPoolExecutorConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.notNull(standardThreadPoolExecutorConfig, "standardThreadPoolExecutorConfig is null");

        int corePoolSize = standardThreadPoolExecutorConfig.getCorePoolSize();
        int maximumPoolSize = standardThreadPoolExecutorConfig.getMaxPoolSize();
        int queueCapacity = standardThreadPoolExecutorConfig.getQueueCapacity();
        long awaitTermination = standardThreadPoolExecutorConfig.getAwaitTermination();
        long keepAliveTime = standardThreadPoolExecutorConfig.getKeepAliveTime();
        long resubmissionDelay = standardThreadPoolExecutorConfig.getResubmissionDelay();
        boolean allowCoreThreadTimeOut = standardThreadPoolExecutorConfig.isTimeOut();
        boolean prestartAllCoreThreads = standardThreadPoolExecutorConfig.isPrestart();
        TimeUnit timeUnit = standardThreadPoolExecutorConfig.getTimeUnit();

        Assert.isTrue(corePoolSize >= 0, "Illegal value for corePoolSize: " + corePoolSize);
        Assert.isTrue(maximumPoolSize >= 0, "Illegal value for maximumPoolSize: " + maximumPoolSize);
        Assert.isTrue(corePoolSize <= maximumPoolSize, "corePoolSize greater than maximumPoolSize");
        Assert.isTrue(queueCapacity > 0, "Illegal value for queueCapacity: " + queueCapacity);
        Assert.isTrue(awaitTermination >= 0, "Illegal value for awaitTermination: " + awaitTermination);
        Assert.isTrue(keepAliveTime >= 0, "Illegal value for keepAliveTime: " + keepAliveTime);
        Assert.isTrue(resubmissionDelay >= 0, "Illegal value for resubmissionDelay: " + resubmissionDelay);
        Assert.isTrue(timeUnit != null, "timeUnit is null");

        ThreadFactory threadFactory = runnable -> {

            Thread thread = new Thread(runnable, THREAD_NAME + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };

        standardThreadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveTime, timeUnit,
                new LinkedBlockingQueue<>(queueCapacity), threadFactory);

        standardThreadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);

        int prestartedCoreThreadCount = 0;
        if (prestartAllCoreThreads) {
            prestartedCoreThreadCount = standardThreadPoolExecutor.prestartAllCoreThreads();
        }
        LOGGER.info("{} standard core thread(s) have been started.", prestartedCoreThreadCount);
    }

    @Override
    public void destroy() throws Exception {
        if (standardThreadPoolExecutor != null) {

            LOGGER.info(
                    "About to purge and shutdown standard thread pool executor, active thread count(s): {}",
                    standardThreadPoolExecutor.getActiveCount());
            standardThreadPoolExecutor.purge();
            standardThreadPoolExecutor.shutdownNow();

            LOGGER.info("standard thread pool executor terminated successfully: {}",
                    standardThreadPoolExecutor.awaitTermination(
                            standardThreadPoolExecutorConfig.getAwaitTermination(),
                            standardThreadPoolExecutorConfig.getTimeUnit()));
        }
    }

    @Bean(name = "StandardExecutor")
    public ThreadPoolExecutor threadPoolExecutor() {
        return standardThreadPoolExecutor;
    }
}
