package com.phaseshifter.canora.data.media.playlist.metadata;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PlaylistMetadataSimple implements PlaylistMetadata, Serializable {
    private static final long serialVersionUID = 1;

    private UUID id;
    private String title;
    private ImageData artwork;

    public PlaylistMetadataSimple(UUID id, String title, ImageData artwork) {
        this.id = id;
        this.title = title;
        this.artwork = artwork;
    }

    public PlaylistMetadataSimple(PlaylistMetadataSimple copy) {
        this(copy.id, copy.title, copy.artwork);
    }

    public PlaylistMetadataSimple(PlaylistMetadata copy) {
        this(copy.getId(), copy.getTitle(), copy.getArtwork());
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public ImageData getArtwork() {
        return artwork;
    }

    public void setArtwork(ImageData artwork) {
        this.artwork = artwork;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaylistMetadataSimple metadata = (PlaylistMetadataSimple) o;
        return Objects.equals(id, metadata.id) &&
                Objects.equals(title, metadata.title) &&
                Objects.equals(artwork, metadata.artwork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, artwork);
    }

    @Override
    public String toString() {
        return "PlaylistMetadataSimple{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artwork=" + artwork +
                '}';
    }
}