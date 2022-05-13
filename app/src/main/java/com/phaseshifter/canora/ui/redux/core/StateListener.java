package com.phaseshifter.canora.ui.redux.core;

public interface StateListener<T> {
    void update(final T updatedState);
}