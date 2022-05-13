package com.phaseshifter.canora.ui.redux.core;

public interface Action {
    String getType();

    Object getPayload();

    boolean isError();
}