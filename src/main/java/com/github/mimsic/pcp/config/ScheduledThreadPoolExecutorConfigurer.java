package com.github.mimsic.pcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ScheduledThreadPoolExecutorConfigurer implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledThreadPoolExecutorConfigurer.class);
    private static final String THREAD_NAME = "ScheduledThreadPoolExecutor-";

    private final AtomicLong threadNumber = new AtomicLong();
    private final ScheduledThreadPoolExecutorConfig scheduledThreadPoolExecutorConfig;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    public ScheduledThreadPoolExecutorConfigurer(ScheduledThreadPoolExecutorConfig scheduledThreadPoolExecutorConfig) {
        this.scheduledThreadPoolExecutorConfig = scheduledThreadPoolExecutorConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.notNull(scheduledThreadPoolExecutorConfig, "scheduledThreadPoolExecutorConfig is null");

        int corePoolSize = scheduledThreadPoolExecutorConfig.getCorePoolSize();
        long awaitTermination = scheduledThreadPoolExecutorConfig.getAwaitTermination();
        boolean prestartAllCoreThreads = scheduledThreadPoolExecutorConfig.isPrestart();
        boolean removeOnCancelPolicy = scheduledThreadPoolExecutorConfig.isCancelPolicy();
        TimeUnit timeUnit = scheduledThreadPoolExecutorConfig.getTimeUnit();

        Assert.isTrue(corePoolSize >= 0, "illegal value for corePoolSize: " + corePoolSize);
        Assert.isTrue(awaitTermination >= 0, "illegal value for awaitTermination: " + awaitTermination);
        Assert.isTrue(timeUnit != null, "timeUnit is null");

        ThreadFactory threadFactory = runnable -> {

            Thread thread = new Thread(runnable, THREAD_NAME + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                corePoolSize,
                threadFactory);

        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(removeOnCancelPolicy);

        int prestartedCoreThreadCount = 0;
        if (prestartAllCoreThreads) {
            prestartedCoreThreadCount = scheduledThreadPoolExecutor.prestartAllCoreThreads();
        }
        LOGGER.info("{} scheduled core thread(s) have been started.", prestartedCoreThreadCount);
    }

    @Override
    public void destroy() throws Exception {
        if (scheduledThreadPoolExecutor != null) {

            LOGGER.info(
                    "about to purge and shutdown scheduled thread pool executor, active thread count(s): {}",
                    scheduledThreadPoolExecutor.getActiveCount());

            scheduledThreadPoolExecutor.purge();
            scheduledThreadPoolExecutor.shutdownNow();

            LOGGER.info("scheduled thread pool executor terminated successfully: {}",
                    scheduledThreadPoolExecutor.awaitTermination(
                            scheduledThreadPoolExecutorConfig.getAwaitTermination(),
                            scheduledThreadPoolExecutorConfig.getTimeUnit()));
        }
    }

    @Bean(name = "ScheduledExecutor")
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        return scheduledThreadPoolExecutor;
    }
}
