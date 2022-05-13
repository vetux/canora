package com.phaseshifter.canora.soundcloud.api_v2.data;

import com.phaseshifter.canora.soundcloud.api.data.SCTrack;

import java.io.Serializable;
import java.util.List;

public abstract class SCV2Track extends SCTrack {
    private static final long serialVersionUID = 1;

    public static class MediaTranscoding implements Serializable {
        private final String url;
        private final String preset;
        private final String duration;
        private final String protocol;
        private final String mimeType;
        private final String quality;

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

        public String getPreset() {
            return preset;
        }

        public String getDuration() {
            return duration;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getQuality() {
            return quality;
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
