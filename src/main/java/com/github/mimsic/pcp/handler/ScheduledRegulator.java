package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.util.LoggerUtil;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

public class ScheduledRegulator<T> extends Regulator {

    private final Semaphore interrupt = new Semaphore(0);
    private final LongAdder rate = new LongAdder();
    private final LongAdder thread = new LongAdder();

    private final Processor<T> processor;
    private final int maxThreads;
    private final int minThreads;
    private final long maxRate;

    public ScheduledRegulator(
            Processor<T> processor,
            int maxThreads,
            int minThreads,
            long maxRate) {

        super(minThreads);

        this.processor = processor;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.maxRate = maxRate;
    }

    @Override
    public void add() {
        thread.increment();
    }

    @Override
    public void check() {
        if (interrupt.tryAcquire()) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void done() {
        rate.increment();
    }

    @Override
    public void regulate() {

        long totalRate = rate.sumThenReset();
        LoggerUtil.info(processor.handler().getClass(), "totalRate: {}", totalRate);

        long totalThreads = thread.sum();
        LoggerUtil.info(processor.handler().getClass(), "totalThreads: {}", totalThreads);

        if (totalThreads > 0) {

            long estimatedRate = (totalRate / totalThreads) * (totalThreads + 1);
            LoggerUtil.info(processor.handler().getClass(), "estimatedRate: {}", estimatedRate);

            if (estimatedRate < maxRate && totalThreads < maxThreads) {
                this.release();
                this.processor.execute();
            } else if (totalRate > maxRate && totalThreads > minThreads) {
                interrupt.release();
            }
        }
    }

    @Override
    public void remove() {
        thread.decrement();
    }
}
