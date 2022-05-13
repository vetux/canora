package com.phaseshifter.canora.model.provider;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;

public interface IContentProvider {
    List<AudioData> getTracks();

    List<AudioPlaylist> getAlbums(List<AudioData> cache);

    List<AudioPlaylist> getArtists(List<AudioData> cache);

    List<AudioPlaylist> getGenres();
}