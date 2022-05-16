package com.phaseshifter.canora.data.media.audio.metadata;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class AudioMetadataMemory implements AudioMetadata, Serializable {
    private static final long serialVersionUID = 1;

    protected UUID id;
    protected String title;
    protected String artist;
    protected String album;
    protected String[] genres;
    protected long length;
    protected ImageData artwork;

    public AudioMetadataMemory(UUID id, String title, String artist, String album, String[] genres, long length, ImageData artwork) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genres = genres;
        this.length = length;
        this.artwork = artwork;
    }

    public AudioMetadataMemory(AudioMetadataMemory copy) {
        this(copy.id, copy.title, copy.artist, copy.album, copy.genres, copy.length, copy.artwork);
    }

    public AudioMetadataMemory(AudioMetadata copy) {
        this(copy.getId(), copy.getTitle(), copy.getArtist(), copy.getAlbum(), copy.getGenres(), copy.getLength(), copy.getArtwork());
    }

    public AudioMetadataMemory() {
        this(null, null, null, null, null, 0, null);
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
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Override
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Override
    public String[] getGenres() {
        return genres;
    }

    public void setGenres(String[] genres) {
        this.genres = genres;
    }

    @Override
    public ImageData getArtwork() {
        return artwork;
    }

    public void setArtwork(ImageData artwork) {
        this.artwork = artwork;
    }

    @Override
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioMetadataMemory that = (AudioMetadataMemory) o;
        return length == that.length &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(album, that.album) &&
                Arrays.equals(genres, that.genres) &&
                Objects.equals(artwork, that.artwork);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, title, artist, album, length, artwork);
        result = 31 * result + Arrays.hashCode(genres);
        return result;
    }

    @Override
    public String toString() {
        return "AudioMetadataSimple{" +
                "uuid=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", genres=" + Arrays.toString(genres) +
                ", length=" + length +
                ", artwork=" + artwork +
                '}';
    }
}