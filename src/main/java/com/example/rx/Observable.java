package com.example.rx;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Observable<T> {

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new ObservableCreate<>(source);
    }

    public static <T> Observable<T> just(T item) {
        return create(emitter -> {
            emitter.onNext(item);
            emitter.onComplete();
        });
    }

    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create(emitter -> {
            for (T item : items) {
                if (emitter instanceof Disposable && ((Disposable) emitter).isDisposed()) {
                    break;
                }
                emitter.onNext(item);
            }
            emitter.onComplete();
        });
    }

    public static <T> Observable<T> fromCallable(Callable<T> callable) {
        return create(emitter -> {
            try {
                T result = callable.call();
                emitter.onNext(result);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    protected abstract void subscribeActual(Observer<? super T> observer);

    public final void subscribe(Observer<? super T> observer) {
        try {
            subscribeActual(observer);
        } catch (Throwable t) {
            observer.onError(t);
        }
    }

    public final Disposable subscribe() {
        return subscribe(item -> {}, Throwable::printStackTrace, () -> {});
    }

    public final Disposable subscribe(java.util.function.Consumer<? super T> onNext) {
        return subscribe(onNext, Throwable::printStackTrace, () -> {});
    }

    public final Disposable subscribe(java.util.function.Consumer<? super T> onNext,
                                      java.util.function.Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, () -> {});
    }

    public final Disposable subscribe(java.util.function.Consumer<? super T> onNext,
                                      java.util.function.Consumer<? super Throwable> onError,
                                      Runnable onComplete) {
        LambdaObserver<T> obs = new LambdaObserver<>(onNext, onError, onComplete);
        subscribe(obs);
        return obs;
    }

    public final <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new ObservableMap<>(this, mapper);
    }

    public final Observable<T> filter(Predicate<? super T> predicate) {
        return new ObservableFilter<>(this, predicate);
    }

    public final <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return new ObservableFlatMap<>(this, mapper);
    }

    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return new ObservableSubscribeOn<>(this, scheduler);
    }

    public final Observable<T> observeOn(Scheduler scheduler) {
        return new ObservableObserveOn<>(this, scheduler);
    }

    private static final class LambdaObserver<T> implements Observer<T>, Disposable {
        private final java.util.function.Consumer<? super T> onNext;
        private final java.util.function.Consumer<? super Throwable> onError;
        private final Runnable onComplete;
        private volatile Disposable upstream;
        private volatile boolean disposed;

        LambdaObserver(java.util.function.Consumer<? super T> onNext,
                       java.util.function.Consumer<? super Throwable> onError,
                       Runnable onComplete) {
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
        }

        @Override
        public void onNext(T item) {
            if (!disposed) {
                try {
                    onNext.accept(item);
                } catch (Throwable t) {
                    onError(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposed) {
                disposed = true;
                onError.accept(t);
            }
        }

        @Override
        public void onComplete() {
            if (!disposed) {
                disposed = true;
                onComplete.run();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            if (upstream != null) {
                upstream.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
