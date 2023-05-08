package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.executor.ScheduledExecutorConfig;
import com.github.mimsic.pcp.executor.ScheduledExecutorConfigurer;
import com.github.mimsic.pcp.executor.StandardExecutorConfig;
import com.github.mimsic.pcp.executor.StandardExecutorConfigurer;
import com.github.mimsic.pcp.util.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HandlerTest implements Handler<Item> {

    private ScheduledExecutorService scheduledExecutor;
    private Processor<Item> processor;
    private CountDownLatch processorLatch;

    private int batchDelayFactor;
    private int batchNumber;
    private int itemNumber;

    public HandlerTest() {
    }

    @BeforeEach
    public void setUp() throws Exception {

        ExecutorService executor = new StandardExecutorConfigurer(
                StandardExecutorConfig.builder()
                        .corePoolSize(30)
                        .maxPoolSize(50)
                        .queueCapacity(1000)
                        .awaitTermination(5000)
                        .keepAliveTime(300000)
                        .resubmissionDelay(500)
                        .prestart(false)
                        .timeOut(true)
                        .timeUnit(TimeUnit.MILLISECONDS)
                        .build())
                .executor();

        this.scheduledExecutor = new ScheduledExecutorConfigurer(
                ScheduledExecutorConfig.builder()
                        .corePoolSize(30)
                        .awaitTermination(5000)
                        .cancelPolicy(true)
                        .prestart(false)
                        .timeUnit(TimeUnit.MILLISECONDS)
                        .build())
                .executor();


        int maxThreads = 15;
        int maxRate = 400;
        long maxLatency = 100;

        this.processor = new Processor<>(
                executor,
                scheduledExecutor,
                this,
                maxThreads,
                maxRate,
                maxLatency,
                TimeUnit.MILLISECONDS);

        this.batchDelayFactor = 5000;
        this.batchNumber = 5;
        this.itemNumber = 2000;
    }

    @Test
    public void testQueue() throws Exception {

        processorLatch = new CountDownLatch(batchNumber * itemNumber);
        long timeStamp1 = System.nanoTime();
        for (int i = 0; i < batchNumber; i++) {
            List<Item> items = new LinkedList<>();
            for (int j = 0; j < itemNumber; j++) {
                items.add(new Item(i, j));
            }
            int batchNumberId = i + 1;
            int batchDelay = batchDelayFactor * i;
            scheduledExecutor.schedule(() -> {
                processor.queue(items);
                Log.info(
                        this.getClass(),
                        "Batch {} pushed to the queue with a delay of {} milliseconds",
                        batchNumberId, batchDelay);
            }, batchDelay, TimeUnit.MILLISECONDS);
        }
        if (!processorLatch.await(300, TimeUnit.SECONDS)) {
            Log.error(this.getClass(), "Item processor failed");
            throw new TimeoutException();
        }
        long timeStamp2 = System.nanoTime();
        Log.info(
                this.getClass(),
                "Completed {} total items in {} milliseconds",
                batchNumber * itemNumber, (timeStamp2 - timeStamp1) / 1000000);
    }

    @Override
    public void process(Item item) throws Exception {

        // A delay between 10 and 15 milliseconds to simulate processing time
        Random random = new Random();
        Thread.sleep(random.ints(10, (50 + 1)).limit(1).findFirst().orElse(0));
        processorLatch.countDown();
    }

    @Override
    public Processor<Item> processor() {
        return processor;
    }
}