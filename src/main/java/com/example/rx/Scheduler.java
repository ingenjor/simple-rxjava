package com.example.rx;

public interface Scheduler {
    Disposable execute(Runnable task);
}
