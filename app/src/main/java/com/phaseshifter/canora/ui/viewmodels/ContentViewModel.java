package com.phaseshifter.canora.ui.viewmodels;

import android.content.Context;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.ui.redux.core.StateListener;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;
import com.phaseshifter.canora.utils.Observable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.phaseshifter.canora.ui.utils.selectors.MainSelector.getPlaylistTitle;

public class ContentViewModel implements StateListener<MainStateImmutable> {
    public final Observable<String> contentName = new Observable<>();
    public final Observable<Boolean> isContentLoading = new Observable<>(false);

    public final Observable<Boolean> isSelecting = new Observable<>(false);

    public final Observable<List<AudioData>> visibleTracks = new Observable<>();
    public final Observable<Integer> contentTracksHighlight = new Observable<>(0);
    public final Observable<HashSet<Integer>> contentTracksSelection = new Observable<>();

    public final Observable<List<AudioPlaylist>> visiblePlaylists = new Observable<>();
    public final Observable<Integer> contentPlaylistHighlight = new Observable<>(0);
    public final Observable<HashSet<Integer>> contentPlaylistsSelection = new Observable<>();

    //TEMP
    public final Observable<AudioContentSelector> navigationHighlightPosition = new Observable<>(AudioContentSelector.TRACKS);
    public final Observable<String> searchText = new Observable<>();

    private final Context context;
    private final AudioDataRepository audioDataRepository;
    private final AudioPlaylistRepository audioPlaylistRepository;

    private MainStateImmutable previousState;

    public ContentViewModel(Context context, AudioDataRepository audioDataRepository, AudioPlaylistRepository audioPlaylistRepository) {
        this.context = context;
        this.audioDataRepository = audioDataRepository;
        this.audioPlaylistRepository = audioPlaylistRepository;
    }

    @Override
    public void update(MainStateImmutable updatedState) {
        if (previousState != null
                && !Objects.equals(updatedState.getTheme(), previousState.getTheme()))
            previousState = null;

        contentName.set(getContentName(updatedState));

        isContentLoading.set(updatedState.getContentLoadSemaphore() > 0);

        if (previousState == null
                || !Objects.equals(updatedState.isSelecting(), previousState.isSelecting()))
            isSelecting.set(updatedState.isSelecting());

        navigationHighlightPosition.set(updatedState.getUiIndicator().getSelector());

        if (previousState == null
                || !Objects.equals(updatedState.getFilterDefinition().filterFor, previousState.getFilterDefinition().filterFor)) {
            searchText.set(updatedState.getFilterDefinition().filterFor);
        }

        //Sort, filter and set content
        List<AudioData> processedTracks = updatedState.getVisibleTracks();
        List<AudioPlaylist> processedPlaylists = updatedState.getVisiblePlaylists();

        if (previousState == null
                || !Objects.equals(updatedState.getVisibleTracks(), previousState.getVisibleTracks())) {
            if (processedTracks == null) {
                visibleTracks.set(new ArrayList<>());
            } else {
                visibleTracks.set(processedTracks);
            }
        }
        if (previousState == null
                || !Objects.equals(updatedState.getVisiblePlaylists(), previousState.getVisiblePlaylists())) {
            if (processedPlaylists == null) {
                visiblePlaylists.set(new ArrayList<>());
            } else {
                visiblePlaylists.set(processedPlaylists);
            }
        }

        SelectionIndicator uiIndicator = updatedState.getUiIndicator();
        SelectionIndicator contentIndicator = updatedState.getContentIndicator();

        //Calculate and set selection
        if (updatedState.isSelecting()) {
            if (uiIndicator.isSubMenu()) {
                contentTracksSelection.set(null);
                HashSet<Integer> indices = new HashSet<>();
                for (int i = 0; i < processedPlaylists.size(); i++) {
                    if (updatedState.getSelection().contains(processedPlaylists.get(i).getMetadata().getId()))
                        indices.add(i);
                }
                contentPlaylistsSelection.set(indices);
            } else {
                contentPlaylistsSelection.set(null);
                HashSet<Integer> indices = new HashSet<>();
                for (int i = 0; i < processedTracks.size(); i++) {
                    if (updatedState.getSelection().contains(processedTracks.get(i).getMetadata().getId()))
                        indices.add(i);
                }
                contentTracksSelection.set(indices);
            }
        } else {
            contentTracksSelection.set(null);
            contentPlaylistsSelection.set(null);
        }

        //Calculate and set highlight
        if (uiIndicator.isSubMenu()) {
            if (contentIndicator != null
                    && uiIndicator.getSelector() == contentIndicator.getSelector()) {
                boolean found = false;
                List<AudioPlaylist> visiblePlaylists = updatedState.getVisiblePlaylists();
                if (visiblePlaylists != null) {
                    for (int i = 0; i < visiblePlaylists.size(); i++) {
                        if (visiblePlaylists.get(i).getMetadata().getId() == contentIndicator.getUuid()) {
                            contentPlaylistHighlight.set(i);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found)
                    contentPlaylistHighlight.set(null);
            } else {
                contentPlaylistHighlight.set(null);
            }
        } else {
            if (uiIndicator.equals(contentIndicator)
                    && updatedState.getPlayerState() != null
                    && updatedState.getPlayerState().getCurrentTrack() != null
                    && processedTracks != null) {
                int index = processedTracks.indexOf(updatedState.getPlayerState().getCurrentTrack());
                if (index != -1)
                    contentTracksHighlight.set(index);
                else
                    contentTracksHighlight.set(null);
            } else {
                contentTracksHighlight.set(null);
            }
        }
        previousState = updatedState;
    }

    private String getContentName(MainStateImmutable state) {
        SelectionIndicator uiIndicator = state.getUiIndicator();
        if (uiIndicator.isSubMenu()) {
            return getSelectorName(uiIndicator.getSelector());
        } else {
            return getIndicatorName(uiIndicator);
        }
    }

    private String getSelectorName(AudioContentSelector selector) {
        switch (selector) {
            case TRACKS:
                return context.getString(R.string.main_toolbar_title0tracks);
            case PLAYLISTS:
                return context.getString(R.string.main_toolbar_title0playlists);
            case ARTISTS:
                return context.getString(R.string.main_toolbar_title0artists);
            case ALBUMS:
                return context.getString(R.string.main_toolbar_title0albums);
            case GENRES:
                return context.getString(R.string.main_toolbar_title0genres);
        }
        return "Error";
    }

    private String getIndicatorName(SelectionIndicator indicator) {
        String text = getPlaylistTitle(indicator, audioDataRepository, audioPlaylistRepository);
        switch (indicator.getSelector()) {
            case TRACKS:
                return context.getString(R.string.main_toolbar_title0tracks);
            case PLAYLISTS:
                return context.getString(R.string.main_toolbar_title0subplaylist, text);
            case ARTISTS:
                return context.getString(R.string.main_toolbar_title0subartist, text);
            case ALBUMS:
                return context.getString(R.string.main_toolbar_title0subalbum, text);
            case GENRES:
                return context.getString(R.string.main_toolbar_title0subgenre, text);
        }
        return "Error";
    }
}