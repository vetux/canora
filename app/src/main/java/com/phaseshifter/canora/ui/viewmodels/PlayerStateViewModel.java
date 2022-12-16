package com.phaseshifter.canora.ui.viewmodels;

import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.service.player.state.PlaybackState;
import com.phaseshifter.canora.service.player.state.PlayerState;
import com.phaseshifter.canora.utils.Observable;

public class PlayerStateViewModel {
    public final Observable<Boolean> buffering = new Observable<>(false);

    public final Observable<String> trackTitle = new Observable<>("");
    public final Observable<String> trackArtist = new Observable<>("");
    public final Observable<ImageData> trackArtwork = new Observable<>(null);

    public final Observable<Long> trackLength = new Observable<>(0L);
    public final Observable<Long> trackPosition = new Observable<>(0L);

    public final Observable<Boolean> isPlaying = new Observable<>(false);
    public final Observable<Boolean> isShuffling = new Observable<>(false);
    public final Observable<Boolean> isRepeating = new Observable<>(false);

    public final Observable<Float> volume = new Observable<>(0f);

    public final Observable<Integer> equalizerPreset = new Observable<>(-1);

    public void applyPlayerState(PlayerState state, AudioPlaylist playingPlaylist) {
        if (state.isPlaying()
                && state.getPlaybackState() != PlaybackState.STATE_READY) {
            buffering.setIfNotEqual(true);
        } else {
            buffering.setIfNotEqual(state.getPlaybackState() == PlaybackState.STATE_BUFFERING);
        }

        if (state.getCurrentTrack() == null) {
            trackTitle.setIfNotEqual("");
            trackArtist.setIfNotEqual("");
            trackArtwork.setIfNotEqual(null);
            trackLength.setIfNotEqual(0L);
        } else {
            trackTitle.setIfNotEqual(state.getCurrentTrack().getMetadata().getTitle());
            trackArtist.setIfNotEqual(state.getCurrentTrack().getMetadata().getArtist());
            if (state.getCurrentTrack().getMetadata().getArtwork() == null
                    && playingPlaylist != null) {
                trackArtwork.setIfNotEqual(playingPlaylist.getMetadata().getArtwork());
            } else {
                trackArtwork.setIfNotEqual(state.getCurrentTrack().getMetadata().getArtwork());
            }
            trackLength.setIfNotEqual(state.getCurrentTrack().getMetadata().getLength());
        }

        trackPosition.setIfNotEqual(state.getPlayerPosition());

        isPlaying.setIfNotEqual(state.isPlaying());
        isShuffling.setIfNotEqual(state.isShuffling());
        isRepeating.setIfNotEqual(state.isRepeating());

        volume.setIfNotEqual(state.getVolume());

        equalizerPreset.setIfNotEqual(state.getEqualizerPreset());
    }

    public void notifyObservers() {
        buffering.notifyObservers();

        trackTitle.notifyObservers();
        trackArtist.notifyObservers();
        trackArtwork.notifyObservers();

        trackLength.notifyObservers();
        trackPosition.notifyObservers();

        isPlaying.notifyObservers();
        isShuffling.notifyObservers();
        isRepeating.notifyObservers();

        volume.notifyObservers();
    }
}