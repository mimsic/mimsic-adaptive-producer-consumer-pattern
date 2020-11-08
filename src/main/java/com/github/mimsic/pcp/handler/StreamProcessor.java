package com.github.mimsic.pcp.handler;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamProcessor<T> {

    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    private final Lock lock;
    private final StreamHandler<T> handler;
    private final Runnable runnable;
    private final long size;


    public StreamProcessor(StreamHandler<T> handler, Lock lock, long size) {

        this.lock = Optional.ofNullable(lock).orElseGet(ReentrantLock::new);
        this.handler = handler;
        this.runnable = this::runnable;
        this.size = size;
    }

    public void queue(T item) {

        try {
            queue.offer(item);
            if (inProgress.compareAndSet(false, true)) {
                handler.execute(runnable);
            }
        } catch (Exception ex) {
            inProgress.set(false);
            handler.logger(ex);
        }
    }

    private void runnable() {

        lock.lock();
        try {
            while (!Thread.currentThread().isInterrupted() &&
                    !inProgress.compareAndSet(queue.isEmpty(), false)) {

                try {
                    handler.process(queue.stream().limit(size));
                } catch (Exception exception) {
                    handler.logger(exception);
                }
            }
        } catch (Exception ex) {
            inProgress.set(false);
            handler.logger(ex);
        } finally {
            lock.unlock();
        }
    }
}
