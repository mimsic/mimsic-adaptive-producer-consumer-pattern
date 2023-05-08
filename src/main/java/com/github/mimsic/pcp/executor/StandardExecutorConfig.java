package com.github.mimsic.pcp.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class StandardExecutorConfig {

    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
    private long awaitTermination;
    private long keepAliveTime;
    private long resubmissionDelay;
    private boolean prestart;
    private boolean timeOut;
    private TimeUnit timeUnit;
}

