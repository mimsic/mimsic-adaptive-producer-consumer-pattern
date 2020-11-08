package com.github.mimsic.pcp.handler;

import java.util.stream.Stream;

public interface StreamHandler<T> {

    void execute(Runnable runnable);

    void logger(Exception ex);

    void process(Stream<T> stream) throws Exception;

    StreamProcessor<T> processor();
}
