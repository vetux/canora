package com.phaseshifter.canora.model.repo;


import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;
import java.util.UUID;

public interface AudioPlaylistRepository {
    List<AudioPlaylist> getAll();

    AudioPlaylist get(UUID key);

    AudioPlaylist set(UUID key, AudioPlaylist playlist);

    AudioPlaylist add(AudioPlaylist playlist);

    void replace(UUID key, AudioPlaylist value);

    void remove(UUID key);

    void remove(List<UUID> keys);

    long getSize();
}