package com.phaseshifter.canora.ui.redux.core;

public interface Middleware<T> {
    void apply(Store<T> store, Action action, Dispatcher next);
}