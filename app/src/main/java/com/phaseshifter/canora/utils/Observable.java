package com.phaseshifter.canora.utils;

import java.util.HashSet;

public class Observable<T> {
    public interface Observer<Y> {
        void update(Observable<Y> observable, Y value);
    }

    private final HashSet<Observer<T>> observers = new HashSet<>();

    private T value;

    public Observable(T value) {
        this.value = value;
    }

    public Observable() {
        this(null);
    }

    public synchronized void addObserver(Observer<T> observer) {
        observers.add(observer);
    }

    public synchronized void removeObserver(Observer<T> observer) {
        observers.remove(observer);
    }

    public synchronized void removeAllObservers() {
        observers.clear();
    }

    public synchronized T get() {
        return value;
    }

    public synchronized void set(T newValue) {
        value = newValue;
        for (Observer<T> observer : observers) {
            observer.update(this, value);
        }
    }
}