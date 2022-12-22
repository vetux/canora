package com.phaseshifter.canora.data.media.player.metadata;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.util.UUID;

public interface PlayerMetadata {
    UUID getId();

    String getTitle();

    String getArtist();

    String getAlbum();

    String[] getGenres();

    ImageData getArtwork();

    long getDuration();
}