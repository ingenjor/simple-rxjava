package com.example.rx;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class ObservableTest {

    @Test
    void testJustAndSubscribe() {
        List<Integer> result = new ArrayList<>();
        Observable.just(1, 2, 3).subscribe(result::add);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void testMap() {
        List<String> result = new ArrayList<>();
        Observable.just(1, 2, 3).map(x -> "Value: " + x).subscribe(result::add);
        assertEquals(List.of("Value: 1", "Value: 2", "Value: 3"), result);
    }

    @Test
    void testFilter() {
        List<Integer> result = new ArrayList<>();
        Observable.just(1, 2, 3, 4, 5).filter(x -> x % 2 == 0).subscribe(result::add);
        assertEquals(List.of(2, 4), result);
    }

    @Test
    void testFlatMap() {
        List<String> result = new ArrayList<>();
        Observable.just("A", "B")
                .flatMap(s -> Observable.just(s + "1", s + "2"))
                .subscribe(result::add);
        assertEquals(List.of("A1", "A2", "B1", "B2"), result);
    }

    @Test
    void testErrorHandling() throws InterruptedException {
        List<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Observable.<String>create(emitter -> {
            emitter.onNext("OK");
            throw new RuntimeException("Test error");
        }).subscribe(result::add, t -> {
            result.add("Error: " + t.getMessage());
            latch.countDown();
        });
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(List.of("OK", "Error: Test error"), result);
    }

    @Test
    void testDispose() throws InterruptedException {
        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Disposable d = Observable.<Integer>create(emitter -> {
                    for (int i = 0; i < 10; i++) {
                        if (emitter instanceof Disposable && ((Disposable) emitter).isDisposed()) {
                            System.out.println("Disposed, breaking");
                            break;
                        }
                        System.out.println("Emitting: " + i);
                        emitter.onNext(i);
                        Thread.sleep(50);
                    }
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())   // запускаем генерацию в фоновом потоке
                .subscribe(
                        item -> result.add(item),
                        Throwable::printStackTrace,
                        latch::countDown
                );

        // Даём время на старт генерации
        Thread.sleep(120);
        d.dispose();

        // onComplete не должен быть вызван после dispose
        boolean completed = latch.await(300, TimeUnit.MILLISECONDS);
        assertFalse(completed, "Should not complete after dispose");
        assertTrue(result.size() < 10, "Should have stopped early, got " + result.size());
    }
}
