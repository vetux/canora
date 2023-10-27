package com.phaseshifter.canora.data.media.playlist;

import com.phaseshifter.canora.data.media.player.PlayerData;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Playlist implements Serializable {
    private static final long serialVersionUID = 1;

    protected PlaylistMetadata metadata;
    protected List<PlayerData> tracks;

    public Playlist() {
    }

    public Playlist(PlaylistMetadata metadata, List<PlayerData> tracks) {
        this.metadata = metadata;
        this.tracks = tracks;
    }

    public PlaylistMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(PlaylistMetadata metadata) {
        this.metadata = metadata;
    }

    public List<PlayerData> getTracks() {
        return tracks;
    }

    public void setTracks(List<PlayerData> tracks) {
        this.tracks = tracks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist that = (Playlist) o;
        return Objects.equals(metadata, that.metadata) &&
                Objects.equals(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, tracks);
    }
}