package com.phaseshifter.canora.plugin.soundcloud.api_v2.data;

import com.phaseshifter.canora.plugin.soundcloud.api.data.SCTrack;

import java.io.Serializable;
import java.util.List;

public abstract class SCV2Track extends SCTrack {
    private static final long serialVersionUID = 1;

    public static class MediaTranscoding implements Serializable {
        public String url;
        public String preset;
        public String duration;
        public String protocol;
        public String mimeType;
        public String quality;

        public MediaTranscoding(String url, String preset, String duration, String protocol, String mimeType, String quality) {
            this.url = url;
            this.preset = preset;
            this.duration = duration;
            this.protocol = protocol;
            this.mimeType = mimeType;
            this.quality = quality;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPreset() {
            return preset;
        }

        public void setPreset(String preset) {
            this.preset = preset;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }
    }

    public static class PublisherMetadata implements Serializable {
        private final String artist;
        private final String publisher;
        private final String album_title;
        private final String release_title;

        public PublisherMetadata(String artist, String publisher, String album_title, String release_title) {
            this.artist = artist;
            this.publisher = publisher;
            this.album_title = album_title;
            this.release_title = release_title;
        }

        public String getArtist() {
            return artist;
        }

        public String getPublisher() {
            return publisher;
        }

        public String getAlbum_title() {
            return album_title;
        }

        public String getRelease_title() {
            return release_title;
        }
    }

    protected List<MediaTranscoding> codings;

    protected PublisherMetadata publisher_metadata;

    public List<MediaTranscoding> getCodings() {
        return codings;
    }

    public PublisherMetadata getPublisher_metadata() {
        return publisher_metadata;
    }
}
