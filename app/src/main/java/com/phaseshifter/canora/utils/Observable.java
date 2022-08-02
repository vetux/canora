package com.phaseshifter.canora.utils;

import java.util.HashSet;

public class Observable<T> {
    private final HashSet<Observer<T>> observers = new HashSet<>();

    private T value;

    public Observable(T value) {
        this.value = value;
    }

    public Observable() {
        this(null);
    }

    public void addObserver(Observer<T> observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer<T> observer) {
        observers.remove(observer);
    }

    public void removeAllObservers() {
        observers.clear();
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        value = newValue;
        notifyObservers();
    }

    public void setIfNotEqual(T newValue) {
        if (!value.equals(newValue)) {
            value = newValue;
            notifyObservers();
        }
    }

    public void notifyObservers() {
        for (Observer<T> observer : observers) {
            observer.update(this, value);
        }
    }
}