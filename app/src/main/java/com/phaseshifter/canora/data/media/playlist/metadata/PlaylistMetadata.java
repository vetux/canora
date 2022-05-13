package com.phaseshifter.canora.data.media.playlist.metadata;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.util.UUID;

public interface PlaylistMetadata {
    UUID getId();

    String getTitle();

    ImageData getArtwork();
}