package com.phaseshifter.canora.plugin.soundcloud.api.data;

import java.io.Serializable;

public abstract class SCUser implements Serializable {
    private static final long serialVersionUID = 1;

    protected String id;
    protected String permalink;
    protected String username;
    protected String uri;
    protected String permalink_url;
    protected String avatar_url;
    protected String country;
    protected String full_name;
    protected String city;
    protected String description;
    protected String discogs_name;
    protected String myspace_name;
    protected String website;
    protected String website_title;
    protected String online;
    protected String track_count;
    protected String playlist_count;
    protected String followers_count;
    protected String followings_count;
    protected String public_favorites_count;

    public String getId() {
        return id;
    }

    public String getPermalink() {
        return permalink;
    }

    public String getUsername() {
        return username;
    }

    public String getUri() {
        return uri;
    }

    public String getPermalink_url() {
        return permalink_url;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public String getCountry() {
        return country;
    }

    public String getFull_name() {
        return full_name;
    }

    public String getCity() {
        return city;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscogs_name() {
        return discogs_name;
    }

    public String getMyspace_name() {
        return myspace_name;
    }

    public String getWebsite() {
        return website;
    }

    public String getWebsite_title() {
        return website_title;
    }

    public String getOnline() {
        return online;
    }

    public String getTrack_count() {
        return track_count;
    }

    public String getPlaylist_count() {
        return playlist_count;
    }

    public String getFollowers_count() {
        return followers_count;
    }

    public String getFollowings_count() {
        return followings_count;
    }

    public String getPublic_favorites_count() {
        return public_favorites_count;
    }
}
