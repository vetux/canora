package com.phaseshifter.canora.model.repo;

import android.util.Log;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.model.comparison.AudioDataComparsion;
import com.phaseshifter.canora.model.comparison.AudioPlaylistComparison;
import com.phaseshifter.canora.model.provider.IContentProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//TODO: Refactor/Redesign device audio repository data refresh

/**
 * Local device data ( IContentProvider )
 */
public class DeviceAudioRepository {
    private final String LOG_TAG = "AudioDataRepository";
    private final IContentProvider contentProvider;

    private List<PlayerData> tracks;
    private List<Playlist> artists;
    private List<Playlist> albums;
    private List<Playlist> genres;

    public DeviceAudioRepository(IContentProvider contentProvider,
                                 List<PlayerData> tracks,
                                 List<Playlist> artists,
                                 List<Playlist> albums,
                                 List<Playlist> genres) {
        this.contentProvider = contentProvider;
        this.tracks = tracks;
        this.artists = artists;
        this.albums = albums;
        this.genres = genres;
    }

    public DeviceAudioRepository(IContentProvider contentProvider) {
        this(contentProvider,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public void refresh() {
        Log.v(LOG_TAG, "refresh");
        List<PlayerData> tracks = contentProvider.getTracks();
        List<Playlist> artists = contentProvider.getArtists(tracks);
        List<Playlist> albums = contentProvider.getAlbums(tracks);
        List<Playlist> genres = contentProvider.getGenres();
        this.tracks = updateAudioData(this.tracks, tracks);
        this.artists = updatePlaylists(this.artists, artists);
        this.albums = updatePlaylists(this.albums, albums);
        this.genres = updatePlaylists(this.genres, genres);
        Log.v(LOG_TAG, "refresh complete");
    }

    public List<PlayerData> getTracks() {
        Log.v(LOG_TAG, "getTracks");
        return tracks;
    }

    public List<Playlist> getArtists() {
        Log.v(LOG_TAG, "getArtists");
        return artists;
    }

    public Playlist getArtist(UUID id) {
        Log.v(LOG_TAG, "getArtist " + id);
        for (Playlist pl : artists) {
            if (pl.getMetadata().getId().equals(id)) {
                return pl;
            }
        }
        return null;
    }

    public List<Playlist> getAlbums() {
        Log.v(LOG_TAG, "getAlbums");
        return albums;
    }

    public Playlist getAlbum(UUID id) {
        Log.v(LOG_TAG, "getAlbum " + id);
        for (Playlist pl : albums) {
            if (pl.getMetadata().getId().equals(id)) {
                return pl;
            }
        }
        return null;
    }

    public List<Playlist> getGenres() {
        Log.v(LOG_TAG, "getGenres");
        return genres;
    }

    public Playlist getGenre(UUID id) {
        Log.v(LOG_TAG, "getGenre " + id);
        for (Playlist pl : genres) {
            if (pl.getMetadata().getId().equals(id)) {
                return pl;
            }
        }
        return null;
    }

    /**
     * As most of the time when we do a refresh the actual data doesnt change and we only assign new UUIDs we use this
     * function to patch the newData with old UUIDS when only the UUIDS or the ordering of the tracks changes.
     *
     * @param oldData The old data
     * @param newData The updated data
     * @return The patched data, non null
     */
    private List<PlayerData> updateAudioData(List<PlayerData> oldData, List<PlayerData> newData) {
        List<PlayerData> ret = new ArrayList<>();
        for (PlayerData track : newData) {
            boolean foundOld = false;
            for (PlayerData oldTrack : oldData) {
                if (AudioDataComparsion.isEqual_exclude_UUID(track, oldTrack)) {
                    PlayerMetadata patchedMetadata = track.getMetadata();
                    patchedMetadata.setId(oldTrack.getMetadata().getId());
                    ret.add(new PlayerData(patchedMetadata, track.getDataSource()));
                    foundOld = true;
                    break;
                }
            }
            if (!foundOld)
                ret.add(track);
        }
        return ret;
    }

    private List<Playlist> updatePlaylists(List<Playlist> oldData, List<Playlist> newData) {
        List<Playlist> ret = new ArrayList<>();
        for (Playlist playlist : newData) {
            boolean foundOld = false;
            for (Playlist oldPlaylist : oldData) {
                if (AudioPlaylistComparison.isEqualPermissive(playlist, oldPlaylist)) {
                    PlaylistMetadata patchedMetadata = playlist.getMetadata();
                    patchedMetadata.setId(oldPlaylist.getMetadata().getId());
                    ret.add(new Playlist(patchedMetadata, updateAudioData(oldPlaylist.getTracks(), playlist.getTracks())));
                    foundOld = true;
                    break;
                }
            }
            if (!foundOld)
                ret.add(playlist);
        }
        return ret;
    }
}