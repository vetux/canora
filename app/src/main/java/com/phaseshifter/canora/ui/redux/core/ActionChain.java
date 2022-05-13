package com.phaseshifter.canora.ui.redux.core;

import java.util.ArrayList;
import java.util.List;

public class ActionChain implements Action {
    private final List<Action> chain;

    public ActionChain(List<Action> chain) {
        this.chain = chain;
    }

    public ActionChain() {
        this(new ArrayList<>());
    }

    public void chain(Action action) {
        chain.add(action);
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Object getPayload() {
        return chain;
    }

    @Override
    public boolean isError() {
        return true;
    }
}