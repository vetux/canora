package com.phaseshifter.canora.model.repo;

import android.util.Log;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.model.comparison.AudioDataComparsion;
import com.phaseshifter.canora.model.comparison.AudioPlaylistComparison;
import com.phaseshifter.canora.model.provider.IContentProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local device data ( IContentProvider )
 */
public class DeviceAudioRepository {
    private final String LOG_TAG = "AudioDataRepository";
    private final IContentProvider contentProvider;

    private List<AudioData> tracks;
    private List<AudioPlaylist> artists;
    private List<AudioPlaylist> albums;
    private List<AudioPlaylist> genres;

    public DeviceAudioRepository(IContentProvider contentProvider,
                                 List<AudioData> tracks,
                                 List<AudioPlaylist> artists,
                                 List<AudioPlaylist> albums,
                                 List<AudioPlaylist> genres) {
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
        List<AudioData> tracks = contentProvider.getTracks();
        List<AudioPlaylist> artists = contentProvider.getArtists(tracks);
        List<AudioPlaylist> albums = contentProvider.getAlbums(tracks);
        List<AudioPlaylist> genres = contentProvider.getGenres();
        this.tracks = updateAudioData(this.tracks, tracks);
        this.artists = updateAudioPlaylists(this.artists, artists);
        this.albums = updateAudioPlaylists(this.albums, albums);
        this.genres = updateAudioPlaylists(this.genres, genres);
        Log.v(LOG_TAG, "refresh complete");
    }

    public List<AudioData> getTracks() {
        Log.v(LOG_TAG, "getTracks");
        return tracks;
    }

    public List<AudioPlaylist> getArtists() {
        Log.v(LOG_TAG, "getArtists");
        return artists;
    }

    public AudioPlaylist getArtist(UUID id) {
        Log.v(LOG_TAG, "getArtist " + id);
        for (AudioPlaylist pl : artists) {
            if (pl.getMetadata().getId().equals(id)) {
                return pl;
            }
        }
        return null;
    }

    public List<AudioPlaylist> getAlbums() {
        Log.v(LOG_TAG, "getAlbums");
        return albums;
    }

    public AudioPlaylist getAlbum(UUID id) {
        Log.v(LOG_TAG, "getAlbum " + id);
        for (AudioPlaylist pl : albums) {
            if (pl.getMetadata().getId().equals(id)) {
                return pl;
            }
        }
        return null;
    }

    public List<AudioPlaylist> getGenres() {
        Log.v(LOG_TAG, "getGenres");
        return genres;
    }

    public AudioPlaylist getGenre(UUID id) {
        Log.v(LOG_TAG, "getGenre " + id);
        for (AudioPlaylist pl : genres) {
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
    private List<AudioData> updateAudioData(List<AudioData> oldData, List<AudioData> newData) {
        List<AudioData> ret = new ArrayList<>();
        for (AudioData track : newData) {
            boolean foundOld = false;
            for (AudioData oldTrack : oldData) {
                if (AudioDataComparsion.isEqual_exclude_UUID(track, oldTrack)) {
                    AudioMetadataMemory patchedMetadata = new AudioMetadataMemory(track.getMetadata());
                    patchedMetadata.setId(oldTrack.getMetadata().getId());
                    ret.add(new AudioData(patchedMetadata, track.getDataSource()));
                    foundOld = true;
                    break;
                }
            }
            if (!foundOld)
                ret.add(track);
        }
        return ret;
    }

    private List<AudioPlaylist> updateAudioPlaylists(List<AudioPlaylist> oldData, List<AudioPlaylist> newData) {
        List<AudioPlaylist> ret = new ArrayList<>();
        for (AudioPlaylist playlist : newData) {
            boolean foundOld = false;
            for (AudioPlaylist oldPlaylist : oldData) {
                if (AudioPlaylistComparison.isEqualPermissive(playlist, oldPlaylist)) {
                    PlaylistMetadataMemory patchedMetadata = new PlaylistMetadataMemory(playlist.getMetadata());
                    patchedMetadata.setId(oldPlaylist.getMetadata().getId());
                    ret.add(new AudioPlaylist(patchedMetadata, updateAudioData(oldPlaylist.getData(), playlist.getData())));
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