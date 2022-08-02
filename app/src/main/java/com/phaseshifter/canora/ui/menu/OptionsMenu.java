package com.phaseshifter.canora.ui.menu;

import java.util.HashSet;

public abstract class OptionsMenu {
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
}