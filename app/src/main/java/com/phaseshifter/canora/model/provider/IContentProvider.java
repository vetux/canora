package com.phaseshifter.canora.model.provider;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.Playlist;

import java.util.List;

public interface IContentProvider {
    List<PlayerData> getTracks();

    List<Playlist> getAlbums(List<PlayerData> cache);

    List<Playlist> getArtists(List<PlayerData> cache);

    List<Playlist> getGenres();
}