package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.util.LoggerUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HandlerTest implements Handler<Item> {

    private Processor<Item> processor;
    private CountDownLatch processorLatch;
    private int itemNumber;

    @Autowired
    @Qualifier("ScheduledExecutor")
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Autowired
    @Qualifier("StandardExecutor")
    private ThreadPoolExecutor standardThreadPoolExecutor;

    public HandlerTest() {
    }

    @Before
    public void setUp() throws Exception {
        this.itemNumber = 10000;
        this.processor = new Processor<>(this, 12, 6, 300);
    }

    @Test
    public void testQueue() throws Exception {

        long timeStamp1 = System.nanoTime();
        processorLatch = new CountDownLatch(itemNumber);
        for (int i = 0; i < itemNumber; i++) {
            processor.queue(new Item(i));
        }
        if (!processorLatch.await(120, TimeUnit.SECONDS)) {
            LoggerUtil.error(this.getClass(), "Item processor failed");
            throw new TimeoutException();
        }
        long timeStamp2 = System.nanoTime();
        LoggerUtil.info(
                this.getClass(),
                "Completed itemNumber {} in {} milliseconds",
                itemNumber, (timeStamp2 - timeStamp1) / 1000000);
    }

    @Override
    public void process(Item item) throws Exception {

        // A delay between 10 and 15 milliseconds to simulate processing time
        Random random = new Random();
        Thread.sleep(random.ints(10, (15 + 1)).limit(1).findFirst().orElse(0));
        processorLatch.countDown();
    }

    @Override
    public Future<?> schedule(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return standardThreadPoolExecutor.submit(runnable);
    }

    @Override
    public Processor<Item> processor() {
        return processor;
    }
}