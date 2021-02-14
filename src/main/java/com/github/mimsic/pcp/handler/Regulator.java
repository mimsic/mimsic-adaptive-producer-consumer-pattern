package com.github.mimsic.pcp.handler;

import java.util.concurrent.Semaphore;

public abstract class Regulator extends Semaphore {

    public Regulator(int minThreads) {
        super(minThreads);
    }

    abstract public void add();

    abstract public void check();

    abstract public void done();

    abstract public void regulate();

    abstract public void remove();
}
