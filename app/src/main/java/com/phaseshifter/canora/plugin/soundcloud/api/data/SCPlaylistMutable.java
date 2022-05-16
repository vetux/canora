package com.phaseshifter.canora.plugin.soundcloud.api.data;

import java.util.List;

public class SCPlaylistMutable extends SCPlaylist {
    private static final long serialVersionUID = 1;

    public void setId(int id) {
        this.id = id;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public void setPermalink_url(String permalink_url) {
        this.permalink_url = permalink_url;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setSharing(String sharing) {
        this.sharing = sharing;
    }

    public void setEmbeddable_by(String embeddable_by) {
        this.embeddable_by = embeddable_by;
    }

    public void setPurchase_url(String purchase_url) {
        this.purchase_url = purchase_url;
    }

    public void setArtwork_url(String artwork_url) {
        this.artwork_url = artwork_url;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setTag_list(String tag_list) {
        this.tag_list = tag_list;
    }

    public void setLabel_id(String label_id) {
        this.label_id = label_id;
    }

    public void setLabel_name(String label_name) {
        this.label_name = label_name;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setRelease_day(String release_day) {
        this.release_day = release_day;
    }

    public void setStreamable(String streamable) {
        this.streamable = streamable;
    }

    public void setDownloadable(String downloadable) {
        this.downloadable = downloadable;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public void setPlaylist_type(String playlist_type) {
        this.playlist_type = playlist_type;
    }

    public void setTracks(List<SCTrack> tracks) {
        this.tracks = tracks;
    }
}
