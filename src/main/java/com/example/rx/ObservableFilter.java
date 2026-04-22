package com.example.rx;

import java.util.function.Predicate;

public final class ObservableFilter<T> extends Observable<T> {
    private final Observable<T> source;
    private final Predicate<? super T> predicate;

    public ObservableFilter(Observable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new FilterObserver<>(observer, predicate));
    }

    static final class FilterObserver<T> implements Observer<T>, Disposable {
        private final Observer<? super T> downstream;
        private final Predicate<? super T> predicate;
        private Disposable upstream;

        FilterObserver(Observer<? super T> downstream, Predicate<? super T> predicate) {
            this.downstream = downstream;
            this.predicate = predicate;
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
                    if (predicate.test(t)) {
                        downstream.onNext(t);
                    }
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
