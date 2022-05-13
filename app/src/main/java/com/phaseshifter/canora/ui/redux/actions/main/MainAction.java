package com.phaseshifter.canora.ui.redux.actions.main;

import com.phaseshifter.canora.ui.redux.core.Action;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;

public class MainAction implements Action {
    private final String type;
    private final MainStateImmutable payload;
    private final boolean error;

    public MainAction(String type, MainStateImmutable payload, boolean error) {
        this.type = type;
        this.payload = payload;
        this.error = error;
    }

    public MainAction(String type, MainStateImmutable payload) {
        this(type, payload, false);
    }

    public MainAction(String type, boolean error) {
        this(type, null, error);
    }

    public MainAction(String type) {
        this(type, null, false);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public MainStateImmutable getPayload() {
        return payload;
    }

    @Override
    public boolean isError() {
        return error;
    }
}