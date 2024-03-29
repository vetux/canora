package com.phaseshifter.canora.service.player;

import android.view.SurfaceView;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.service.player.state.PlayerState;
import com.phaseshifter.canora.utils.Observable;

import java.util.List;
import java.util.UUID;

public interface MediaPlayerService {
    /**
     * Initiates a shutdown of the service.
     * The shutdown does not commence until all bindings to the service are destroyed.
     */
    void shutdown();

    void setContent(List<PlayerData> pl);

    void play(UUID id);

    void next();

    void previous();

    void pauseResume();

    void pause();

    void resume();

    void seek(float percentage);

    void seek(long ms);

    void stop();

    void switchShuffle();

    void setShuffle(boolean state);

    void switchRepeat();

    void setRepeat(boolean state);

    void setVolume(float vol);

    /**
     * @param preset If negative equalizer is disabled otherwise the index of the equalizer preset in android.media.audiofx.Equalizer
     */
    void setEqualizerPreset(int preset);

    void setVideoSurfaceView(SurfaceView view);

    /**
     * Configure wheter or not inputs such as previous, next coming from media sesssion (For example bluetooth headphones) will be respected.
     * Useful for badly designed headphones which cause these controls to be triggered without user intention.
     *
     * @param enable When true the media session controls will be affecting the service.
     */
    void setEnableMediaSessionControls(boolean enable);

    Observable<PlayerState> getState();
}