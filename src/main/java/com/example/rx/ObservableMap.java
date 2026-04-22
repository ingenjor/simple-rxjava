package com.example.rx;

import java.util.function.Function;

public final class ObservableMap<T, R> extends Observable<R> {
    private final Observable<T> source;
    private final Function<? super T, ? extends R> mapper;

    public ObservableMap(Observable<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new MapObserver<>(observer, mapper));
    }

    static final class MapObserver<T, R> implements Observer<T>, Disposable {
        private final Observer<? super R> downstream;
        private final Function<? super T, ? extends R> mapper;
        private Disposable upstream;

        MapObserver(Observer<? super R> downstream, Function<? super T, ? extends R> mapper) {
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
            if (!isDisposed()) {
                try {
                    R result = mapper.apply(t);
                    downstream.onNext(result);
                } catch (Throwable e) {
                    onError(e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!isDisposed()) {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
