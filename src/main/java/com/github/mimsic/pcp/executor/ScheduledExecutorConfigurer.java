package com.github.mimsic.pcp.executor;

import com.github.mimsic.pcp.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledExecutorConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledExecutorConfigurer.class);
    private static final String THREAD_NAME = "ScheduledExecutor-";
    private final AtomicLong threadNumber = new AtomicLong();
    private final ScheduledExecutorConfig executorConfig;
    private final ScheduledThreadPoolExecutor executor;

    public ScheduledExecutorConfigurer(ScheduledExecutorConfig executorConfig) {

        Assert.notNull(executorConfig, "ScheduledExecutorConfig is null");
        this.executorConfig = executorConfig;

        int corePoolSize = executorConfig.getCorePoolSize();
        long awaitTermination = executorConfig.getAwaitTermination();
        boolean prestartAllCoreThreads = executorConfig.isPrestart();
        boolean removeOnCancelPolicy = executorConfig.isCancelPolicy();
        TimeUnit timeUnit = executorConfig.getTimeUnit();

        Assert.isTrue(corePoolSize >= 0, "illegal value for corePoolSize: " + corePoolSize);
        Assert.isTrue(awaitTermination >= 0, "illegal value for awaitTermination: " + awaitTermination);
        Assert.isTrue(timeUnit != null, "timeUnit is null");

        ThreadFactory threadFactory = runnable -> {

            String treadName = THREAD_NAME + String.format("%03d", threadNumber.incrementAndGet());
            Thread thread = new Thread(runnable, treadName);
            thread.setDaemon(true);
            return thread;
        };
        executor = new ScheduledThreadPoolExecutor(
                corePoolSize,
                threadFactory);

        executor.setRemoveOnCancelPolicy(removeOnCancelPolicy);

        int prestartedCoreThreadCount = 0;
        if (prestartAllCoreThreads) {
            prestartedCoreThreadCount = executor.prestartAllCoreThreads();
        }

        Thread shutdownHook = new Thread(this::shutdown, THREAD_NAME + "shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOGGER.info("{} scheduled core thread(s) have been started.", prestartedCoreThreadCount);
    }

    private void shutdown() {
        if (executor != null) {

            LOGGER.info(
                    "About to purge and shutdown scheduled thread pool executor, active thread count(s): {}",
                    executor.getActiveCount());

            executor.purge();
            executor.shutdownNow();

            try {
                LOGGER.info(
                        "Scheduled thread pool executor terminated successfully: {}",
                        executor.awaitTermination(
                                executorConfig.getAwaitTermination(),
                                executorConfig.getTimeUnit()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ScheduledThreadPoolExecutor executor() {
        return executor;
    }
}
