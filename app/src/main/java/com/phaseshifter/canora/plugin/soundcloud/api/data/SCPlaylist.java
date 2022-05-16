package com.phaseshifter.canora.plugin.soundcloud.api.data;

import java.io.Serializable;
import java.util.List;

public abstract class SCPlaylist implements Serializable {
    private static final long serialVersionUID = 1;

    protected int id;
    protected String created_at;
    protected String user_id;
    protected String user;
    protected String title;
    protected String permalink;
    protected String permalink_url;
    protected String uri;
    protected String sharing;
    protected String embeddable_by;
    protected String purchase_url;
    protected String artwork_url;
    protected String description;
    protected String label;
    protected String duration;
    protected String genre;
    protected String tag_list;
    protected String label_id;
    protected String label_name;
    protected String release;
    protected String release_day;
    protected String streamable;
    protected String downloadable;
    protected String ean;
    protected String playlist_type;
    protected List<SCTrack> tracks;

    public int getId() {
        return id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getPermalink() {
        return permalink;
    }

    public String getPermalink_url() {
        return permalink_url;
    }

    public String getUri() {
        return uri;
    }

    public String getSharing() {
        return sharing;
    }

    public String getEmbeddable_by() {
        return embeddable_by;
    }

    public String getPurchase_url() {
        return purchase_url;
    }

    public String getArtwork_url() {
        return artwork_url;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public String getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    public String getTag_list() {
        return tag_list;
    }

    public String getLabel_id() {
        return label_id;
    }

    public String getLabel_name() {
        return label_name;
    }

    public String getRelease() {
        return release;
    }

    public String getRelease_day() {
        return release_day;
    }

    public String getStreamable() {
        return streamable;
    }

    public String getDownloadable() {
        return downloadable;
    }

    public String getEan() {
        return ean;
    }

    public String getPlaylist_type() {
        return playlist_type;
    }

    public List<SCTrack> getTracks() {
        return tracks;
    }
}
