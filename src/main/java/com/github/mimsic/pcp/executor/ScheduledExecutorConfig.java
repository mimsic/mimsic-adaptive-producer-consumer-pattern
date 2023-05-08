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
public class ScheduledExecutorConfig {

    private int corePoolSize;
    private long awaitTermination;
    private boolean cancelPolicy;
    private boolean prestart;
    private TimeUnit timeUnit;
}
