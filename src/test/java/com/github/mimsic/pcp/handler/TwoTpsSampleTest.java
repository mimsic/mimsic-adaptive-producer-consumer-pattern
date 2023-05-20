package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.executor.ScheduledExecutorConfig;
import com.github.mimsic.pcp.executor.ScheduledExecutorConfigurer;
import com.github.mimsic.pcp.util.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TwoTpsSampleTest {

    private ScheduledExecutorService scheduledExecutor;

    @BeforeEach
    public void setUp() throws Exception {

        this.scheduledExecutor = new ScheduledExecutorConfigurer(
                ScheduledExecutorConfig.builder()
                        .corePoolSize(30)
                        .awaitTermination(5000)
                        .cancelPolicy(true)
                        .prestart(false)
                        .timeUnit(TimeUnit.MILLISECONDS)
                        .build())
                .executor();
    }

    @Test
    public void testTwoTpsSample() throws InterruptedException {

        // Total 2 TPS
        ScheduledFuture<?> scheduledFuture1 = scheduledExecutor.scheduleAtFixedRate(this::task, 5000, 1000, TimeUnit.MILLISECONDS); // 1TPS
        ScheduledFuture<?> scheduledFuture2 = scheduledExecutor.scheduleAtFixedRate(this::task, 5000, 1000, TimeUnit.MILLISECONDS); // 1TPS

        Log.info(
                this.getClass(),
                "ScheduledFuture {}, cancelled: {}, done: {}",
                1, scheduledFuture1.isCancelled(), scheduledFuture1.isDone());
        Log.info(
                this.getClass(),
                "ScheduledFuture {}, cancelled: {}, done: {}",
                2, scheduledFuture2.isCancelled(), scheduledFuture2.isDone());

        Thread.sleep(10000);

        scheduledFuture1.cancel(true);
        scheduledFuture2.cancel(true);

        Thread.sleep(5000);

        Log.info(
                this.getClass(),
                "ScheduledFuture {}, cancelled: {}, done: {}",
                1, scheduledFuture1.isCancelled(), scheduledFuture1.isDone());
        Log.info(
                this.getClass(),
                "ScheduledFuture {}, cancelled: {}, done: {}",
                2, scheduledFuture2.isCancelled(), scheduledFuture2.isDone());
    }

    private void task() {
        Log.info(
                this.getClass(),
                "Time: {} Thread name: {}",
                System.currentTimeMillis() / 1000, Thread.currentThread().getName());
    }
}
