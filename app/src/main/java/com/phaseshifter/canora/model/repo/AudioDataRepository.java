package com.phaseshifter.canora.model.repo;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;
import java.util.UUID;

public interface AudioDataRepository {
    void refresh();

    List<AudioData> getTracks();

    List<AudioPlaylist> getArtists();

    AudioPlaylist getArtist(UUID id);

    List<AudioPlaylist> getAlbums();

    AudioPlaylist getAlbum(UUID id);

    List<AudioPlaylist> getGenres();

    AudioPlaylist getGenre(UUID id);
}