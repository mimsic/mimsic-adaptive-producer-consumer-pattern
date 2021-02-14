package com.github.mimsic.pcp.handler;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface Handler<T> {

    void process(T data) throws Exception;

    Future<?> schedule(Runnable runnable, long initialDelay, long period, TimeUnit unit);

    Future<?> submit(Runnable runnable);

    Processor<T> processor();
}
