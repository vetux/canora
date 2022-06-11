package com.phaseshifter.canora.ui.redux.state;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;
import com.phaseshifter.canora.ui.data.formatting.SortDef;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public interface MainStateImmutable {
    int getContentLoadSemaphore();

    List<AudioData> getContentTracks();

    List<AudioPlaylist> getContentPlaylists();

    List<AudioData> getSortedTracks();

    List<AudioPlaylist> getSortedPlaylists();

    List<AudioData> getVisibleTracks();

    List<AudioPlaylist> getVisiblePlaylists();

    SelectionIndicator getUiIndicator();

    SelectionIndicator getContentIndicator();

    PlayerState getPlayerState();

    boolean isFiltering();

    boolean isControlsMaximized();

    boolean isSelecting();

    HashSet<UUID> getSelection();

    SortDef getSortingDefinition();

    FilterDef getFilterDefinition();

    AppTheme getTheme();

    float getVolume();

    boolean isShuffling();

    boolean isRepeating();

    boolean isDevMode();

    int getSearchPage();

    boolean isScrollLoading();
}