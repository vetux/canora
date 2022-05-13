package com.phaseshifter.canora.ui.redux.core;

public interface Dispatcher {
    void dispatch(Action action);

    static <S> Dispatcher chain(Middleware<S> middleware, Dispatcher next, Store<S> store) {
        return action -> middleware.apply(store, action, next);
    }
}