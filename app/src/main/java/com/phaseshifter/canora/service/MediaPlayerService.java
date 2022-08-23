package com.phaseshifter.canora.service;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.utils.Observable;

import java.util.List;
import java.util.UUID;

public interface MediaPlayerService {
    /**
     * Initiates a shutdown of the service.
     * The shutdown does not commence until all bindings to the service are destroyed.
     */
    void shutdown();

    void setContent(List<AudioData> pl);

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

    void setEqualizerEnabled(boolean enabled);

    void setEqualizerPreset(int preset);

    Observable<PlayerState> getState();
}