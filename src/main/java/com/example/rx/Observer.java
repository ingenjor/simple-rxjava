package com.example.rx;

public interface Observer<T> {
    void onSubscribe(Disposable d);
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}
