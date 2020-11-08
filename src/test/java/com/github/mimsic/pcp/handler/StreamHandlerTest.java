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
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StreamHandlerTest implements StreamHandler<Item> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamHandlerTest.class);

    private final StreamProcessor<Item> streamProcessor;
    private final long batchSize;
    private final int itemNumber;

    private CountDownLatch processorLatch;
    private int threadNumber;

    @Autowired
    @Qualifier("StandardExecutor")
    private ThreadPoolExecutor standardThreadPoolExecutor;

    public StreamHandlerTest() {
        this.batchSize = 10;
        this.itemNumber = 100000;
        this.streamProcessor = new StreamProcessor<>(this, null, batchSize);
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testQueue() throws Exception {

        long timeStamp1 = System.nanoTime();
        processorLatch = new CountDownLatch(itemNumber);
        for (int i = 0; i < itemNumber; i++) {
            streamProcessor.queue(new Item(i));
        }
        if (!processorLatch.await(10, TimeUnit.SECONDS)) {
            LOGGER.error("Stream processor failed");
            throw new TimeoutException();
        }
        long timeStamp2 = System.nanoTime();
        LOGGER.info("Completed itemNumber {} with threadNumber {} and batchSize {}, in {} micro seconds",
                itemNumber, threadNumber, batchSize, (timeStamp2 - timeStamp1) / 1000);
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
    public void process(Stream<Item> stream) throws Exception {
        stream.forEach(item -> processorLatch.countDown());
    }

    @Override
    public StreamProcessor<Item> processor() {
        return streamProcessor;
    }
}