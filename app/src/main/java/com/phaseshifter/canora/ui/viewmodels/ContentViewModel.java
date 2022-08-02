package com.phaseshifter.canora.ui.viewmodels;

import android.content.Context;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.utils.Observable;

import java.util.HashSet;
import java.util.List;

public class ContentViewModel {
    public final Observable<String> contentName = new Observable<>();

    public final Observable<List<AudioData>> visibleTracks = new Observable<>();
    public final Observable<Integer> visibleTracksHighlightedIndex = new Observable<>(0);
    public final Observable<HashSet<Integer>> contentTracksSelection = new Observable<>();

    public final Observable<List<AudioPlaylist>> visiblePlaylists = new Observable<>();
    public final Observable<Integer> contentPlaylistHighlight = new Observable<>(0);
    public final Observable<HashSet<Integer>> contentPlaylistsSelection = new Observable<>();
}