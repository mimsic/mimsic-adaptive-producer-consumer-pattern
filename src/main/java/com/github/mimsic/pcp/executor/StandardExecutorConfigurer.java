package com.github.mimsic.pcp.executor;

import com.github.mimsic.pcp.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StandardExecutorConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardExecutorConfigurer.class);
    private static final String THREAD_NAME = "StandardExecutor-";
    private final AtomicLong threadNumber = new AtomicLong();
    private final StandardExecutorConfig executorConfig;
    private final ThreadPoolExecutor executor;

    public StandardExecutorConfigurer(StandardExecutorConfig executorConfig) {

        Assert.notNull(executorConfig, "StandardExecutorConfig is null");
        this.executorConfig = executorConfig;

        int corePoolSize = executorConfig.getCorePoolSize();
        int maximumPoolSize = executorConfig.getMaxPoolSize();
        int queueCapacity = executorConfig.getQueueCapacity();
        long awaitTermination = executorConfig.getAwaitTermination();
        long keepAliveTime = executorConfig.getKeepAliveTime();
        long resubmissionDelay = executorConfig.getResubmissionDelay();
        boolean allowCoreThreadTimeOut = executorConfig.isTimeOut();
        boolean prestartAllCoreThreads = executorConfig.isPrestart();
        TimeUnit timeUnit = executorConfig.getTimeUnit();

        Assert.isTrue(corePoolSize >= 0, "illegal value for corePoolSize: " + corePoolSize);
        Assert.isTrue(maximumPoolSize >= 0, "illegal value for maximumPoolSize: " + maximumPoolSize);
        Assert.isTrue(corePoolSize <= maximumPoolSize, "corePoolSize greater than maximumPoolSize");
        Assert.isTrue(queueCapacity > 0, "illegal value for queueCapacity: " + queueCapacity);
        Assert.isTrue(awaitTermination >= 0, "illegal value for awaitTermination: " + awaitTermination);
        Assert.isTrue(keepAliveTime >= 0, "illegal value for keepAliveTime: " + keepAliveTime);
        Assert.isTrue(resubmissionDelay >= 0, "illegal value for resubmissionDelay: " + resubmissionDelay);
        Assert.isTrue(timeUnit != null, "timeUnit is null");

        ThreadFactory threadFactory = runnable -> {

            String treadName = THREAD_NAME + String.format("%03d", threadNumber.incrementAndGet());
            Thread thread = new Thread(runnable, treadName);
            thread.setDaemon(true);
            return thread;
        };

        executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                timeUnit,
                new LinkedBlockingQueue<>(queueCapacity),
                threadFactory);

        executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);

        int prestartedCoreThreadCount = 0;
        if (prestartAllCoreThreads) {
            prestartedCoreThreadCount = executor.prestartAllCoreThreads();
        }

        Thread shutdownHook = new Thread(this::shutdown, THREAD_NAME + "shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOGGER.info("{} standard core thread(s) have been started.", prestartedCoreThreadCount);
    }

    private void shutdown() {
        if (executor != null) {

            LOGGER.info(
                    "About to purge and shutdown standard thread pool executor, active thread count(s): {}",
                    executor.getActiveCount());

            executor.purge();
            executor.shutdownNow();

            try {
                LOGGER.info(
                        "Standard thread pool executor terminated successfully: {}",
                        executor.awaitTermination(
                                executorConfig.getAwaitTermination(),
                                executorConfig.getTimeUnit()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ThreadPoolExecutor executor() {
        return executor;
    }
}
