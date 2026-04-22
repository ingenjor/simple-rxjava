package com.example.rx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class ObservableFlatMap<T, R> extends Observable<R> {
    private final Observable<T> source;
    private final Function<? super T, ? extends Observable<? extends R>> mapper;

    public ObservableFlatMap(Observable<T> source, Function<? super T, ? extends Observable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new FlatMapObserver<>(observer, mapper));
    }

    static final class FlatMapObserver<T, R> implements Observer<T>, Disposable {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends Observable<? extends R>> mapper;
        private Disposable upstream;
        private final AtomicInteger active = new AtomicInteger(1);
        private volatile boolean disposed;

        FlatMapObserver(Observer<? super R> downstream, Function<? super T, ? extends Observable<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            if (disposed) return;
            try {
                Observable<? extends R> innerObservable = mapper.apply(t);
                active.incrementAndGet();
                InnerObserver inner = new InnerObserver();
                innerObservable.subscribe(inner);
            } catch (Throwable e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposed) {
                disposed = true;
                downstream.onError(t);
                upstream.dispose();
            }
        }

        @Override
        public void onComplete() {
            if (active.decrementAndGet() == 0 && !disposed) {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        private class InnerObserver implements Observer<R> {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(R r) {
                if (!disposed) {
                    downstream.onNext(r);
                }
            }

            @Override
            public void onError(Throwable t) {
                FlatMapObserver.this.onError(t);
            }

            @Override
            public void onComplete() {
                if (active.decrementAndGet() == 0 && !disposed) {
                    downstream.onComplete();
                }
            }
        }
    }
}
