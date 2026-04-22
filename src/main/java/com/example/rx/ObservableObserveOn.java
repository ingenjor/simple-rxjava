package com.example.rx;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class ObservableObserveOn<T> extends Observable<T> {
    private final Observable<T> source;
    private final Scheduler scheduler;

    public ObservableObserveOn(Observable<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new ObserveOnObserver<>(observer, scheduler));
    }

    private static final class ObserveOnObserver<T> implements Observer<T>, Disposable, Runnable {
        private final Observer<? super T> downstream;
        private final Scheduler scheduler;
        private Disposable upstream;
        private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger wip = new AtomicInteger(0);
        private volatile boolean disposed;
        private volatile boolean done;
        private Throwable error;

        ObserveOnObserver(Observer<? super T> downstream, Scheduler scheduler) {
            this.downstream = downstream;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            if (!disposed) {
                queue.offer(t);
                schedule();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!disposed) {
                error = t;
                done = true;
                schedule();
            }
        }

        @Override
        public void onComplete() {
            if (!disposed) {
                done = true;
                schedule();
            }
        }

        private void schedule() {
            if (wip.getAndIncrement() == 0) {
                scheduler.execute(this);
            }
        }

        @Override
        public void run() {
            int missed = 1;
            for (;;) {
                if (disposed) {
                    queue.clear();
                    return;
                }
                boolean d = done;
                T v = queue.poll();
                boolean empty = v == null;

                if (d && empty) {
                    Throwable ex = error;
                    if (ex != null) {
                        downstream.onError(ex);
                    } else {
                        downstream.onComplete();
                    }
                    return;
                }

                if (empty) {
                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    downstream.onNext(v);
                }
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
    }
}
