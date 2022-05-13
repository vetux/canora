package com.phaseshifter.canora.ui.menu;

import java.util.HashSet;

public class ContextMenu {
    public enum Action {
        SELECT,
        INFO,
        EDIT,
        DELETE
    }

    public interface ContextMenuListener {
        void onAction(Action action);
    }

    private final HashSet<Action> actions;

    public ContextMenu(HashSet<Action> actions) {
        this.actions = actions;
    }

    public HashSet<Action> getActions() {
        return new HashSet<>(actions);
    }
}