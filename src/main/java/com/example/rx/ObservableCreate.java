package com.example.rx;

public final class ObservableCreate<T> extends Observable<T> {
    private final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        CreateEmitter<T> emitter = new CreateEmitter<>(observer);
        observer.onSubscribe(emitter);
        try {
            source.subscribe(emitter);
        } catch (Throwable t) {
            emitter.onError(t);
        }
    }

    private static final class CreateEmitter<T> implements Observer<T>, Disposable {
        private final Observer<? super T> downstream;
        private volatile boolean disposed;

        CreateEmitter(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {}

        @Override
        public void onNext(T item) {
            if (!disposed) {
                downstream.onNext(item);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposed) {
                disposed = true;
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
