package me.jacob.proj.util;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntCounter implements IDCounter {

    private final AtomicInteger counter;

    public AtomicIntCounter() {
        counter = new AtomicInteger(0);
    }

    @Override
    public int nextUniqueId() {
        return counter.getAndIncrement();
    }
}
