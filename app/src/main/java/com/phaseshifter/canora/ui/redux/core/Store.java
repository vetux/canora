package com.phaseshifter.canora.ui.redux.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Not Threadsafe.
 *
 * @param <T> State class.
 */
public class Store<T> {
    private final Reducer<T> reducer;
    private final List<StateListener<T>> stateListeners;

    private Dispatcher dispatcher = new Dispatcher() {
        @Override
        public void dispatch(Action action) {
            state = reducer.reduce(state, action);
        }
    };

    private T state;

    /**
     * @param reducer        The reducer implementation to use.
     * @param preloadedState The preloaded state.
     * @param middlewares    A list of optional middlewares, may be null. The order of execution matches the supplied list. Eg. the middleware at index 0 is executed first in the chain.
     */
    public Store(Reducer<T> reducer, T preloadedState, List<Middleware<T>> middlewares) {
        this.stateListeners = new ArrayList<>();
        this.reducer = reducer;
        this.state = preloadedState;
        this.state = this.reducer.reduce(this.state, null);
        if (middlewares != null) {
            for (int i = middlewares.size() - 1; i >= 0; i--) {
                dispatcher = Dispatcher.chain(middlewares.get(i), dispatcher, this);
            }
        }
    }

    public Store(Reducer<T> reducer, T preloadedState) {
        this(reducer, preloadedState, null);
    }

    public Store(Reducer<T> reducer, List<Middleware<T>> middlewares) {
        this(reducer, null, middlewares);
    }

    public Store(Reducer<T> reducer) {
        this(reducer, null, null);
    }

    /**
     * Dispatches the supplied action.
     * <p>
     * All subscribed StateListeners are notified of the new State when the reducer finishes.
     *
     * @param action The action to be reduced.
     * @throws RuntimeException When the message post to the handler fails.
     */
    public T dispatch(Action action) {
        dispatcher.dispatch(action);
        for (StateListener<T> stateListener : stateListeners) {
            stateListener.update(state);
        }
        return state;
    }

    /**
     * Subscribes the supplied StateListener to be notified of state updates.
     *
     * @param stateListener The listener instance to use.
     * @return A runnable which can be executed to unsubscribe the supplied listener.
     */
    public Runnable subscribe(StateListener<T> stateListener) {
        stateListeners.add(stateListener);
        return () -> {
            stateListeners.remove(stateListener);
        };
    }

    /**
     * @return Reference to the current state object.
     */
    public T getState() {
        return state;
    }
}