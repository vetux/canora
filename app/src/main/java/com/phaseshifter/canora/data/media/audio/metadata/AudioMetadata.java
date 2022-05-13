package com.phaseshifter.canora.data.media.audio.metadata;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.util.UUID;

public interface AudioMetadata {
    UUID getId();

    String getTitle();

    String getArtist();

    String getAlbum();

    String[] getGenres();

    ImageData getArtwork();

    long getLength();
}