package com.github.mimsic.pcp.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ItemHandlerTest implements ItemHandler<Item> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemHandlerTest.class);

    private final ItemProcessor<Item> itemProcessor;
    private final int itemNumber;

    private CountDownLatch processorLatch;
    private int threadNumber;

    @Autowired
    @Qualifier("StandardExecutor")
    private ThreadPoolExecutor standardThreadPoolExecutor;

    public ItemHandlerTest() {
        this.itemNumber = 100000;
        this.itemProcessor = new ItemProcessor<>(this, null);
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testQueue() throws Exception {

        long timeStamp1 = System.nanoTime();
        processorLatch = new CountDownLatch(itemNumber);
        for (int i = 0; i < itemNumber; i++) {
            itemProcessor.queue(new Item(i));
        }
        if (!processorLatch.await(10, TimeUnit.SECONDS)) {
            LOGGER.error("Item processor failed");
            throw new TimeoutException();
        }
        long timeStamp2 = System.nanoTime();
        LOGGER.info("Completed itemNumber {} with threadNumber {}, in {} micro seconds",
                itemNumber, threadNumber, (timeStamp2 - timeStamp1) / 1000);
    }

    @Override
    public void execute(Runnable runnable) {
        threadNumber++;
        standardThreadPoolExecutor.execute(runnable);
    }

    @Override
    public void logger(Exception ex) {
        LOGGER.info("", ex);
    }

    @Override
    public void process(Item item) throws Exception {
        processorLatch.countDown();
    }

    @Override
    public ItemProcessor<Item> processor() {
        return itemProcessor;
    }
}