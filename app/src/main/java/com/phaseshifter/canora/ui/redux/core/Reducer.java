package com.phaseshifter.canora.ui.redux.core;

import java.util.List;

public abstract class Reducer<T> {
    private final T initalState;

    public Reducer(T initalState) {
        this.initalState = initalState;
    }

    public T reduce(T previousState, Action action) {
        if (previousState == null)
            return initalState;
        else if (action == null)
            return previousState;
        T ret = previousState;
        if (action instanceof ActionChain) {
            List<Action> chain = (List<Action>) action.getPayload();
            for (Action chainAction : chain) {
                ret = combineReducers(ret, chainAction);
            }
        } else {
            ret = combineReducers(ret, action);
        }
        return ret;
    }

    protected abstract T combineReducers(T state, Action action);
}