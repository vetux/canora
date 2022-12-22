package com.phaseshifter.canora.service.player.playback;

import com.phaseshifter.canora.data.media.player.PlayerData;

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
    boolean setContent(List<PlayerData> c);

    List<PlayerData> getContent();

    PlayerData setNext(UUID id);

    PlayerData getNext();

    PlayerData peekNext();

    PlayerData getPrev();

    PlayerData getCurrentTrack();

    void setRepeat(Boolean repeat);

    Boolean getRepeat();

    void setShuffle(Boolean shuffle);

    Boolean getShuffle();
}