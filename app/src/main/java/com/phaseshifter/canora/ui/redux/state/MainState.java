package com.phaseshifter.canora.ui.redux.state;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;
import com.phaseshifter.canora.ui.data.formatting.SortDef;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Only values which CAN be null WILL be initialized to null, values which get non null initialized HAVE TO be non null throughout execution.
 */
public class MainState implements MainStateImmutable, Serializable {
    protected transient int contentLoadSemaphore = 0; //Indicates how many loading processes are ongoing. The ViewModel transforms this into a boolean for the view.

    protected transient List<AudioData> contentTracks = null; //Raw content unfiltered / sorted
    protected transient List<AudioPlaylist> contentPlaylists = null;

    protected transient List<AudioData> sortedTracks = null;
    protected transient List<AudioPlaylist> sortedPlaylists = null;

    protected transient List<AudioData> visibleTracks = null; //The sorted /filtered contentTracks objects.
    protected transient List<AudioPlaylist> visiblePlaylists = null; //The sorted / filtered contentPlaylists objects

    protected SelectionIndicator uiIndicator = new SelectionIndicator(AudioContentSelector.TRACKS, null);
    protected SelectionIndicator contentIndicator = null;

    protected PlayerState playerState = null;

    protected boolean controlsMaximized = false;

    protected transient boolean filtering = false;
    protected transient FilterDef filterDefinition = new FilterDef();

    protected transient boolean selecting = false;
    protected transient HashSet<UUID> selection = new HashSet<>(); //UUIDs of the selected content. May point to tracks or playlists depending on state.

    protected SortDef sortingDefinition = new SortDef();

    protected transient AppTheme theme = null;

    protected float volume = 0;

    protected boolean shuffling = false;

    protected boolean repeating = false;

    protected boolean devMode = false;

    protected int searchPage = 0;
    protected boolean scrollLoading = false;

    public MainState() {
    }

    public MainState(MainState copy) {
        contentLoadSemaphore = copy.contentLoadSemaphore;
        if (copy.contentTracks == null) {
            contentTracks = null;
        } else {
            contentTracks = new ArrayList<>(copy.contentTracks);
        }
        if (copy.contentPlaylists == null) {
            contentPlaylists = null;
        } else {
            contentPlaylists = new ArrayList<>(copy.contentPlaylists);
        }
        if (copy.sortedTracks == null) {
            sortedTracks = null;
        } else {
            sortedTracks = new ArrayList<>(copy.sortedTracks);
        }
        if (copy.sortedPlaylists == null) {
            sortedPlaylists = null;
        } else {
            sortedPlaylists = new ArrayList<>(copy.sortedPlaylists);
        }
        if (copy.visibleTracks == null) {
            visibleTracks = null;
        } else {
            visibleTracks = new ArrayList<>(copy.visibleTracks);
        }
        if (copy.visiblePlaylists == null) {
            visiblePlaylists = null;
        } else {
            visiblePlaylists = new ArrayList<>(copy.visiblePlaylists);
        }
        uiIndicator = copy.uiIndicator;
        if (copy.contentIndicator == null) {
            contentIndicator = null;
        } else {
            contentIndicator = new SelectionIndicator(copy.contentIndicator);
        }
        if (copy.playerState == null) {
            playerState = null;
        } else {
            playerState = new PlayerState(copy.playerState);
        }
        controlsMaximized = copy.controlsMaximized;
        filtering = copy.filtering;
        if (copy.filterDefinition == null) {
            filterDefinition = new FilterDef();
        } else {
            filterDefinition = new FilterDef(copy.filterDefinition);
        }
        selecting = copy.selecting;
        if (copy.selection == null) {
            selection = new HashSet<>();
        } else {
            selection = new HashSet<>(copy.selection);
        }
        sortingDefinition = new SortDef(copy.sortingDefinition);
        if (copy.theme == null) {
            theme = null;
        } else {
            theme = new AppTheme(copy.theme);
        }
        volume = copy.volume;
        shuffling = copy.shuffling;
        repeating = copy.repeating;
        devMode = copy.devMode;
        searchPage = copy.searchPage;
        scrollLoading = copy.scrollLoading;
    }

    public MainState(MainStateImmutable copy) {
        this((MainState) copy);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        selection = new HashSet<>();
        filterDefinition = new FilterDef();
    }

    @Override
    public int getContentLoadSemaphore() {
        return contentLoadSemaphore;
    }

    public void setContentLoadSemaphore(int contentLoadSemaphore) {
        this.contentLoadSemaphore = contentLoadSemaphore;
    }

    @Override
    public List<AudioData> getContentTracks() {
        return contentTracks;
    }

    public void setContentTracks(List<AudioData> contentTracks) {
        this.contentTracks = contentTracks;
    }

    @Override
    public List<AudioPlaylist> getContentPlaylists() {
        return contentPlaylists;
    }

    public void setContentPlaylists(List<AudioPlaylist> contentPlaylists) {
        this.contentPlaylists = contentPlaylists;
    }

    @Override
    public List<AudioData> getSortedTracks() {
        return sortedTracks;
    }

    public void setSortedTracks(List<AudioData> sortedTracks) {
        this.sortedTracks = sortedTracks;
    }

    @Override
    public List<AudioPlaylist> getSortedPlaylists() {
        return sortedPlaylists;
    }

    public void setSortedPlaylists(List<AudioPlaylist> sortedPlaylists) {
        this.sortedPlaylists = sortedPlaylists;
    }

    @Override
    public List<AudioData> getVisibleTracks() {
        return visibleTracks;
    }

    public void setVisibleTracks(List<AudioData> visibleTracks) {
        this.visibleTracks = visibleTracks;
    }

    @Override
    public List<AudioPlaylist> getVisiblePlaylists() {
        return visiblePlaylists;
    }

    public void setVisiblePlaylists(List<AudioPlaylist> visiblePlaylists) {
        this.visiblePlaylists = visiblePlaylists;
    }

    @Override
    public SelectionIndicator getUiIndicator() {
        return uiIndicator;
    }

    public void setUiIndicator(SelectionIndicator uiIndicator) {
        this.uiIndicator = uiIndicator;
    }

    @Override
    public SelectionIndicator getContentIndicator() {
        return contentIndicator;
    }

    public void setContentIndicator(SelectionIndicator contentIndicator) {
        this.contentIndicator = contentIndicator;
    }

    @Override
    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    @Override
    public boolean isFiltering() {
        return filtering;
    }

    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
    }

    @Override
    public boolean isControlsMaximized() {
        return controlsMaximized;
    }

    public void setControlsMaximized(boolean controlsMaximized) {
        this.controlsMaximized = controlsMaximized;
    }

    @Override
    public boolean isSelecting() {
        return selecting;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    @Override
    public HashSet<UUID> getSelection() {
        return selection;
    }

    public void setSelection(HashSet<UUID> selection) {
        this.selection = selection;
    }

    @Override
    public SortDef getSortingDefinition() {
        return sortingDefinition;
    }

    public void setSortingDefinition(SortDef sortingDefinition) {
        this.sortingDefinition = sortingDefinition;
    }

    @Override
    public FilterDef getFilterDefinition() {
        return filterDefinition;
    }

    public void setFilterDefinition(FilterDef filterDefinition) {
        this.filterDefinition = filterDefinition;
    }

    @Override
    public AppTheme getTheme() {
        return theme;
    }

    public void setTheme(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    @Override
    public boolean isShuffling() {
        return shuffling;
    }

    public void setShuffling(boolean shuffling) {
        this.shuffling = shuffling;
    }

    @Override
    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    @Override
    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    @Override
    public int getSearchPage() {
        return searchPage;
    }

    public void setSearchPage(int page) {
        this.searchPage = page;
    }

    @Override
    public boolean isScrollLoading() {
        return scrollLoading;
    }

    public void setScrollLoading(boolean loading) {
        this.scrollLoading = loading;
    }
}