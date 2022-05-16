package com.phaseshifter.canora.plugin.soundcloud.api.data;

public class SCUserMutable extends SCUser {
    private static final long serialVersionUID = 1;

    public void setId(String id) {
        this.id = id;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setPermalink_url(String permalink_url) {
        this.permalink_url = permalink_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDiscogs_name(String USESLASHdiscogs_name) {
        this.discogs_name = USESLASHdiscogs_name;
    }

    public void setMyspace_name(String USESLASHmyspace_name) {
        this.myspace_name = USESLASHmyspace_name;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setWebsite_title(String USESLASHwebsite_title) {
        this.website_title = USESLASHwebsite_title;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public void setTrack_count(String track_count) {
        this.track_count = track_count;
    }

    public void setPlaylist_count(String playlist_count) {
        this.playlist_count = playlist_count;
    }

    public void setFollowers_count(String followers_count) {
        this.followers_count = followers_count;
    }

    public void setFollowings_count(String followings_count) {
        this.followings_count = followings_count;
    }

    public void setPublic_favorites_count(String public_favorites_count) {
        this.public_favorites_count = public_favorites_count;
    }
}
