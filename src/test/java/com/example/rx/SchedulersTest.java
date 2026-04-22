package com.example.rx;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class SchedulersTest {

    @Test
    void testSubscribeOn() throws InterruptedException {
        List<String> threadNames = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just("Hello")
                .map(s -> {
                    threadNames.add(Thread.currentThread().getName());
                    return s;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(s -> {
                    threadNames.add(Thread.currentThread().getName());
                    latch.countDown();
                });

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(2, threadNames.size());
        assertTrue(threadNames.get(0).startsWith("RxIOThread"));
        assertTrue(threadNames.get(1).startsWith("RxIOThread"));
    }

    @Test
    void testObserveOn() throws InterruptedException {
        List<String> threadNames = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just("World")
                .observeOn(Schedulers.computation())
                .map(s -> {
                    threadNames.add(Thread.currentThread().getName());
                    return s;
                })
                .observeOn(Schedulers.single())
                .subscribe(s -> {
                    threadNames.add(Thread.currentThread().getName());
                    latch.countDown();
                });

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(2, threadNames.size());
        assertTrue(threadNames.get(0).startsWith("RxComputationThread"));
        assertTrue(threadNames.get(1).startsWith("RxSingleThread"));
    }
}
