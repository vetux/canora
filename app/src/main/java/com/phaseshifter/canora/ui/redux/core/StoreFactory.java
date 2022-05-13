package com.phaseshifter.canora.ui.redux.core;

import java.util.ArrayList;
import java.util.List;

public class StoreFactory<T> {
    private Reducer<T> reducer = null;
    private T preloadedState = null;
    private List<Middleware<T>> middlewares = null;

    public StoreFactory() {
    }

    public StoreFactory<T> setReducer(Reducer<T> reducer) {
        this.reducer = reducer;
        return this;
    }

    public StoreFactory<T> setPreloadedState(T preloadedState) {
        this.preloadedState = preloadedState;
        return this;
    }

    public StoreFactory<T> addMiddleware(Middleware<T> middleware) {
        if (middlewares == null)
            middlewares = new ArrayList<>();
        middlewares.add(middleware);
        return this;
    }

    public Store<T> build() {
        return new Store<>(reducer, preloadedState, middlewares);
    }
}