package com.phaseshifter.canora.soundcloud.api_v2.data;

import com.phaseshifter.canora.soundcloud.api.data.SCTrack;
import com.phaseshifter.canora.soundcloud.api.data.SCUser;

import java.util.List;

public class SCV2TrackMutable extends SCV2Track {
    private static final long serialVersionUID = 1;

    public SCV2TrackMutable() {
    }

    public SCV2TrackMutable(SCTrack track) {
        this.id = track.getId();
        this.created_at = track.getCreated_at();
        this.user_id = track.getUser_id();
        this.user = track.getUser();
        this.title = track.getTitle();
        this.permalink = track.getPermalink();
        this.uri = track.getUri();
        this.sharing = track.getSharing();
        this.embeddable_by = track.getEmbeddable_by();
        this.purchase_url = track.getPurchase_url();
        this.artwork_url = track.getArtwork_url();
        this.description = track.getDescription();
        this.label = track.getLabel();
        this.duration = track.getDuration();
        this.tag_list = track.getTag_list();
        this.label_id = track.getLabel_id();
        this.label_name = track.getLabel_name();
        this.release = track.getRelease();
        this.release_day = track.getRelease_day();
        this.release_month = track.getRelease_month();
        this.release_year = track.getRelease_year();
        this.streamable = track.getStreamable();
        this.downloadable = track.getDownloadable();
        this.state = track.getState();
        this.license = track.getLicense();
        this.track_type = track.getTrack_type();
        this.waveform_url = track.getWaveform_url();
        this.download_url = track.getDownload_url();
        this.stream_url = track.getStream_url();
        this.video_url = track.getVideo_url();
        this.bpm = track.getBpm();
        this.commentable = track.getCommentable();
        this.isrc = track.getIsrc();
        this.key_signature = track.getKey_signature();
        this.comment_count = track.getComment_count();
        this.download_count = track.getDownload_count();
        this.playback_count = track.getPlayback_count();
        this.favoritings_count = track.getFavoritings_count();
        this.original_format = track.getOriginal_format();
        this.original_content_size = track.getOriginal_content_size();
        this.asset_data = track.getAsset_data();
        this.artwork_data = track.getArtwork_data();
        this.user_favorite = track.getUser_favorite();
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setUser(SCUser user) {
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

    public void setRelease_month(String release_month) {
        this.release_month = release_month;
    }

    public void setRelease_year(String release_year) {
        this.release_year = release_year;
    }

    public void setStreamable(String streamable) {
        this.streamable = streamable;
    }

    public void setDownloadable(String downloadable) {
        this.downloadable = downloadable;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setTrack_type(String track_type) {
        this.track_type = track_type;
    }

    public void setWaveform_url(String waveform_url) {
        this.waveform_url = waveform_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public void setStream_url(String stream_url) {
        this.stream_url = stream_url;
    }

    public void setVideo_url(String video_url) {
        this.video_url = video_url;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
    }

    public void setCommentable(String commentable) {
        this.commentable = commentable;
    }

    public void setIsrc(String isrc) {
        this.isrc = isrc;
    }

    public void setKey_signature(String key_signature) {
        this.key_signature = key_signature;
    }

    public void setComment_count(String comment_count) {
        this.comment_count = comment_count;
    }

    public void setDownload_count(String download_count) {
        this.download_count = download_count;
    }

    public void setPlayback_count(String playback_count) {
        this.playback_count = playback_count;
    }

    public void setFavoritings_count(String favoritings_count) {
        this.favoritings_count = favoritings_count;
    }

    public void setOriginal_format(String original_format) {
        this.original_format = original_format;
    }

    public void setOriginal_content_size(String original_content_size) {
        this.original_content_size = original_content_size;
    }

    public void setAsset_data(String asset_data) {
        this.asset_data = asset_data;
    }

    public void setArtwork_data(String artwork_data) {
        this.artwork_data = artwork_data;
    }

    public void setUser_favorite(String user_favorite) {
        this.user_favorite = user_favorite;
    }

    public void setCodings(List<SCV2Track.MediaTranscoding> codings) {
        this.codings = codings;
    }

    public void setPublisher_Metadata(PublisherMetadata metadata) {
        this.publisher_metadata = metadata;
    }
}
