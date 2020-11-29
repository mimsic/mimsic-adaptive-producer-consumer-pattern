package com.github.mimsic.pcp.handler;

public interface ItemHandler<T> {

    void execute(Runnable runnable);

    void logger(Exception e);

    void process(T item) throws Exception;

    ItemProcessor<T> processor();
}
