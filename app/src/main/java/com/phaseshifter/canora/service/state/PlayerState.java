package com.phaseshifter.canora.service.state;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.service.playback.PlaybackController;
import com.google.android.exoplayer2.ExoPlayer;

import java.io.Serializable;
import java.util.Objects;

public class PlayerState implements Serializable {
    private AudioData currentTrack;
    private PlaybackState playbackState;
    private boolean playing;
    private boolean repeating;
    private boolean shuffling;
    private long playerPosition; //Position of Player in milliseconds
    private float volume;
    private int equalizerPreset;

    public PlayerState(AudioData currentTrack, PlaybackState playbackState, boolean isPlaying, boolean isRepeating, boolean isShuffling, long playerPosition, float volume, int equalizerPreset) {
        this.currentTrack = currentTrack;
        this.playbackState = playbackState;
        this.playing = isPlaying;
        this.repeating = isRepeating;
        this.shuffling = isShuffling;
        this.playerPosition = playerPosition;
        this.volume = volume;
        this.equalizerPreset = equalizerPreset;
    }

    public PlayerState(PlaybackController playbackController, ExoPlayer player, float volume, boolean loadingTrack, int equalizerPreset) {
        this(
                playbackController.getCurrentTrack(),
                loadingTrack ? PlaybackState.STATE_BUFFERING : PlaybackState.fromInt(player.getPlaybackState()),
                player.isPlaying(),
                playbackController.getRepeat(),
                playbackController.getShuffle(),
                player.getCurrentPosition(),
                volume,
                equalizerPreset
        );
    }

    public PlayerState(PlayerState copy) {
        this(copy.currentTrack, copy.playbackState, copy.playing, copy.repeating, copy.shuffling, copy.playerPosition, copy.volume, copy.equalizerPreset);
    }

    public PlayerState() {
        this(null, null, false, false, false, 0, 0, 0);
    }

    public AudioData getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(AudioData currentTrack) {
        this.currentTrack = currentTrack;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public void setPlaybackState(PlaybackState playbackState) {
        this.playbackState = playbackState;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public boolean isShuffling() {
        return shuffling;
    }

    public void setShuffling(boolean shuffling) {
        this.shuffling = shuffling;
    }

    public long getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(long playerPosition) {
        this.playerPosition = playerPosition;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public int getEqualizerPreset() {
        return equalizerPreset;
    }

    public void setEqualizerPreset(int value) {
        equalizerPreset = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerState that = (PlayerState) o;
        return playing == that.playing &&
                repeating == that.repeating &&
                shuffling == that.shuffling &&
                playerPosition == that.playerPosition &&
                Float.compare(that.volume, volume) == 0 &&
                Objects.equals(currentTrack, that.currentTrack) &&
                playbackState == that.playbackState
                && equalizerPreset == that.equalizerPreset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentTrack, playbackState, playing, repeating, shuffling, playerPosition, volume, equalizerPreset);
    }

    @Override
    public String toString() {
        return "PlayerState{" +
                "currentTrack=" + currentTrack +
                ", playbackState=" + playbackState +
                ", playing=" + playing +
                ", repeating=" + repeating +
                ", shuffling=" + shuffling +
                ", playerPosition=" + playerPosition +
                ", volume=" + volume +
                ", equalizerPreset=" + equalizerPreset +
                '}';
    }
}