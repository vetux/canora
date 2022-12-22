package com.phaseshifter.canora.model.provider;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;

public interface IContentProvider {
    List<PlayerData> getTracks();

    List<AudioPlaylist> getAlbums(List<PlayerData> cache);

    List<AudioPlaylist> getArtists(List<PlayerData> cache);

    List<AudioPlaylist> getGenres();
}