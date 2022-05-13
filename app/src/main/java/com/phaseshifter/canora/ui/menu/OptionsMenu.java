package com.phaseshifter.canora.ui.menu;

import java.util.HashSet;

public class OptionsMenu {
    public enum Action {
        OPEN_SETTINGS,
        OPEN_SORTOPTIONS,
        OPEN_FILTEROPTIONS,
        ADD_SELECTION,
        SELECT_START,
        SELECT_STOP,
        SELECT_ALL,
        DESELECT_ALL,
        EDIT_PLAYLIST,
        DELETE
    }

    public interface OptionsMenuListener {
        void onAction(Action action);
    }

    private final HashSet<Action> actions;

    public OptionsMenu(HashSet<Action> actions) {
        this.actions = actions;
    }

    public HashSet<Action> getActions() {
        return new HashSet<>(actions);
    }
}