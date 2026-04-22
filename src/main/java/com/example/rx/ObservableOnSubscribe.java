package com.example.rx;

@FunctionalInterface
public interface ObservableOnSubscribe<T> {
    void subscribe(Observer<? super T> emitter) throws Exception;
}
