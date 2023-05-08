package com.github.mimsic.pcp.handler;

public interface Handler<T> {

    void process(T data) throws Exception;

    Processor<T> processor();
}
