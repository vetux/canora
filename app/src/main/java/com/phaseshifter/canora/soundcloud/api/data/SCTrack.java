package com.phaseshifter.canora.soundcloud.api.data;

import java.io.Serializable;

public abstract class SCTrack implements Serializable {
    private static final long serialVersionUID = 1;

    protected int id;
    protected String created_at;
    protected String user_id;
    protected SCUser user;
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
    protected String tag_list;
    protected String label_id;
    protected String label_name;
    protected String release;
    protected String release_day;
    protected String release_month;
    protected String release_year;
    protected String streamable;
    protected String downloadable;
    protected String state;
    protected String license;
    protected String track_type;
    protected String waveform_url;
    protected String download_url;
    protected String stream_url; //Throws 401 atm
    protected String video_url;
    protected String bpm;
    protected String commentable;
    protected String isrc;
    protected String key_signature;
    protected String comment_count;
    protected String download_count;
    protected String playback_count;
    protected String favoritings_count;
    protected String original_format;
    protected String original_content_size;
    protected String asset_data;
    protected String artwork_data;
    protected String user_favorite;

    public int getId() {
        return id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getUser_id() {
        return user_id;
    }

    public SCUser getUser() {
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

    public String getRelease_month() {
        return release_month;
    }

    public String getRelease_year() {
        return release_year;
    }

    public String getStreamable() {
        return streamable;
    }

    public String getDownloadable() {
        return downloadable;
    }

    public String getState() {
        return state;
    }

    public String getLicense() {
        return license;
    }

    public String getTrack_type() {
        return track_type;
    }

    public String getWaveform_url() {
        return waveform_url;
    }

    public String getDownload_url() {
        return download_url;
    }

    public String getStream_url() {
        return stream_url;
    }

    public String getVideo_url() {
        return video_url;
    }

    public String getBpm() {
        return bpm;
    }

    public String getCommentable() {
        return commentable;
    }

    public String getIsrc() {
        return isrc;
    }

    public String getKey_signature() {
        return key_signature;
    }

    public String getComment_count() {
        return comment_count;
    }

    public String getDownload_count() {
        return download_count;
    }

    public String getPlayback_count() {
        return playback_count;
    }

    public String getFavoritings_count() {
        return favoritings_count;
    }

    public String getOriginal_format() {
        return original_format;
    }

    public String getOriginal_content_size() {
        return original_content_size;
    }

    public String getAsset_data() {
        return asset_data;
    }

    public String getArtwork_data() {
        return artwork_data;
    }

    public String getUser_favorite() {
        return user_favorite;
    }
}
