package com.example.rx;

public final class ObservableSubscribeOn<T> extends Observable<T> {
    private final Observable<T> source;
    private final Scheduler scheduler;

    public ObservableSubscribeOn(Observable<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        scheduler.execute(() -> source.subscribe(new SubscribeOnObserver<>(observer)));
    }

    private static final class SubscribeOnObserver<T> implements Observer<T>, Disposable {
        private final Observer<? super T> downstream;
        private Disposable upstream;

        SubscribeOnObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
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
