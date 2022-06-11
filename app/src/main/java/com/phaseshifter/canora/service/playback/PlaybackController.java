package com.phaseshifter.canora.service.playback;

import com.phaseshifter.canora.data.media.audio.AudioData;

import java.util.List;
import java.util.UUID;

/**
 * Responsible for the order of playback.
 */
public interface PlaybackController {
    /**
     * @param c The updated data to act upon
     * @return True if the currently playing track was found in the new data set.
     */
    boolean setContent(List<AudioData> c);

    List<AudioData> getContent();

    AudioData setNext(UUID id);

    AudioData getNext();

    AudioData peekNext();

    AudioData getPrev();

    AudioData getCurrentTrack();

    void setRepeat(Boolean repeat);

    Boolean getRepeat();

    void setShuffle(Boolean shuffle);

    Boolean getShuffle();
}