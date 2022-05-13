package com.phaseshifter.canora.model.comparison;

import com.phaseshifter.canora.data.media.audio.AudioData;

import java.util.Arrays;
import java.util.Objects;

public class AudioDataComparsion {
    public static boolean isEqual_exclude_UUID(AudioData a0, AudioData a1) {
        if (!a0.getDataSource().equals(a1.getDataSource()))
            return false;
        if (a0.getMetadata().getArtwork() == null) {
            if (a1.getMetadata().getArtwork() != null)
                return false;
        } else {
            if (a1.getMetadata().getArtwork() == null)
                return false;
            if (!Objects.equals(a0.getMetadata().getArtwork().getDataSource(), a1.getMetadata().getArtwork().getDataSource()))
                return false;
        }
        return a0.getMetadata().getLength() == a1.getMetadata().getLength()
                && Objects.equals(a0.getMetadata().getTitle(), a1.getMetadata().getTitle())
                && Objects.equals(a0.getMetadata().getArtist(), a1.getMetadata().getArtist())
                && Objects.equals(a0.getMetadata().getAlbum(), a1.getMetadata().getAlbum())
                && Arrays.equals(a0.getMetadata().getGenres(), a1.getMetadata().getGenres());
    }
}