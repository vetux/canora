package com.phaseshifter.canora.ui.redux.reducers;

import com.phaseshifter.canora.ui.redux.actions.main.MainAction;
import com.phaseshifter.canora.ui.redux.core.Action;
import com.phaseshifter.canora.ui.redux.core.Reducer;
import com.phaseshifter.canora.ui.redux.state.MainState;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;

import static com.phaseshifter.canora.ui.redux.actions.main.MainActionType.*;

public class MainReducer extends Reducer<MainStateImmutable> {
    public MainReducer(MainStateImmutable initalState) {
        super(initalState);
    }

    public MainReducer() {
        this(new MainState());
    }

    @Override
    protected MainStateImmutable combineReducers(MainStateImmutable state, Action action) {
        MainAction castedAction = (MainAction) action;
        MainState ret = new MainState(state);
        switch (castedAction.getType()) {
            case SET_INDICATORS:
                ret.setContentIndicator(castedAction.getPayload().getContentIndicator());
                ret.setUiIndicator(castedAction.getPayload().getUiIndicator());
                break;
            case SET_CONTENT:
                ret.setContentTracks(castedAction.getPayload().getContentTracks());
                ret.setContentPlaylists(castedAction.getPayload().getContentPlaylists());
                ret.setSortedTracks(castedAction.getPayload().getSortedTracks());
                ret.setSortedPlaylists(castedAction.getPayload().getSortedPlaylists());
                ret.setVisibleTracks(castedAction.getPayload().getVisibleTracks());
                ret.setVisiblePlaylists(castedAction.getPayload().getVisiblePlaylists());
                break;
            case SET_PLAYERSTATE:
                ret.setPlayerState(castedAction.getPayload().getPlayerState());
                break;
            case SET_THEME:
                ret.setTheme(castedAction.getPayload().getTheme());
                break;
            case CONTENT_LOAD_START:
                ret.setContentLoadSemaphore(ret.getContentLoadSemaphore() + 1);
                break;
            case CONTENT_LOAD_STOP:
                ret.setContentLoadSemaphore(ret.getContentLoadSemaphore() - 1);
                break;
            case CONTROL_SET:
                ret.setControlsMaximized(castedAction.getPayload().isControlsMaximized());
                break;
            case CONTROL_MAX:
                ret.setControlsMaximized(true);
                break;
            case CONTROL_MIN:
                ret.setControlsMaximized(false);
                break;
            case FILTER_ENABLE:
                ret.setFiltering(true);
                break;
            case FILTER_DISABLE:
                ret.setFiltering(false);
                break;
            case FILTER_SETDEF:
                ret.setFilterDefinition(castedAction.getPayload().getFilterDefinition());
                break;
            case SET_FILTERSTATE:
                ret.setFiltering(castedAction.getPayload().isFiltering());
                ret.setFilterDefinition(castedAction.getPayload().getFilterDefinition());
                break;
            case SET_SELECTION:
                ret.setSelection(castedAction.getPayload().getSelection());
                break;
            case SELECTION_ENABLE:
                ret.setSelecting(true);
                break;
            case SELECTION_DISABLE:
                ret.setSelecting(false);
                break;
            case SET_SORTDEF:
                ret.setSortingDefinition(castedAction.getPayload().getSortingDefinition());
                break;
            case SET_VOLUME:
                ret.setVolume(castedAction.getPayload().getVolume());
                break;
            case SET_SHUFFLE:
                ret.setShuffling(castedAction.getPayload().isShuffling());
                break;
            case SET_REPEAT:
                ret.setRepeating(castedAction.getPayload().isRepeating());
                break;
            case SET_DEVMODE:
                ret.setDevMode(castedAction.getPayload().isDevMode());
                break;
            case SEARCH_LOAD_START:
                ret.setScrollLoading(true);
                break;
            case SEARCH_LOAD_STOP:
                ret.setScrollLoading(false);
                break;
            case SEARCH_INCREMENT_PAGE:
                ret.setSearchPage(state.getSearchPage() + 1);
                break;
            case SEARCH_RESET_PAGE:
                ret.setSearchPage(0);
                break;
        }
        return ret;
    }
}