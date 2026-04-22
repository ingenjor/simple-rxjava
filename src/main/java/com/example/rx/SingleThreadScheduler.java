package com.example.rx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SingleThreadScheduler implements Scheduler {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "RxSingleThread");
        t.setDaemon(true);
        return t;
    });

    @Override
    public Disposable execute(Runnable task) {
        Future<?> future = executor.submit(task);
        return new Disposable() {
            @Override
            public void dispose() {
                future.cancel(true);
            }

            @Override
            public boolean isDisposed() {
                return future.isCancelled();
            }
        };
    }
}
