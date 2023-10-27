package com.phaseshifter.canora.data.media.player;

import com.phaseshifter.canora.data.media.image.ImageData;

import java.util.UUID;

public class PlayerMetadata {
    public UUID id;
    public String title;
    public String artist;
    public String album;
    public String[] genres;
    public long duration;
    public ImageData artwork;

    public PlayerMetadata() {
    }

    public PlayerMetadata(UUID id, String title, String artist, String album, String[] genres, long length, ImageData artwork) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genres = genres;
        this.duration = length;
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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String[] getGenres() {
        return genres;
    }

    public void setGenres(String[] genres) {
        this.genres = genres;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public ImageData getArtwork() {
        return artwork;
    }

    public void setArtwork(ImageData artwork) {
        this.artwork = artwork;
    }
}