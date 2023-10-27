package com.phaseshifter.canora.data.media.playlist;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.util.UUID;

public class PlaylistMetadata {
    public UUID id;
    public String title;
    public ImageData artwork;

    public PlaylistMetadata() {
    }

    public PlaylistMetadata(UUID id, String title, ImageData artwork) {
        this.id = id;
        this.title = title;
        this.artwork = artwork;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ImageData getArtwork() {
        return artwork;
    }

    public void setArtwork(ImageData artwork) {
        this.artwork = artwork;
    }
}