package com.phaseshifter.canora.soundcloud.api_v2.json;

import com.phaseshifter.canora.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.soundcloud.api.data.SCUser;
import com.phaseshifter.canora.soundcloud.api.data.SCUserMutable;
import com.phaseshifter.canora.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.soundcloud.api_v2.data.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SCV2JsonParser {
    public List<SCV2Track> getTracks(String json) throws SCParsingException {
        try {
            List<SCV2Track> ret = new ArrayList<>();
            JSONArray collection = new JSONObject(json).getJSONArray("collection");
            for (int i = 0; i < collection.length(); i++) {
                JSONObject object = collection.getJSONObject(i);
                ret.add(getTrack(object.toString()));
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SCParsingException("Failed to extract tracks from json");
        }
    }

    public SCV2Charts getCharts(String json) throws SCParsingException {
        try {
            JSONObject root = new JSONObject(json);
            SCV2ChartsMutable ret = new SCV2ChartsMutable();
            SCGenre genre = SCGenre.fromString(root.optString("genre"));
            ret.setGenre(genre);
            ret.setKind(root.optString("kind"));
            ret.setLast_updated(root.optString("last_updated"));
            List<SCV2ChartTrack> tracks = new ArrayList<>();
            JSONArray collection = root.getJSONArray("collection");
            for (int i = 0; i < collection.length(); i++) {
                JSONObject entry = collection.getJSONObject(i);
                SCV2ChartTrack track = new SCV2ChartTrack(entry.getInt("score"), getTrack(entry.getJSONObject("track").toString()));
                tracks.add(track);
            }
            ret.setTracks(tracks);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SCParsingException("Failed to extract charts from json");
        }
    }

    public SCV2Track getTrack(String json) throws SCParsingException {
        try {
            JSONObject root = new JSONObject(json);
            SCV2TrackMutable track = new SCV2TrackMutable();
            track.setId(root.optInt("id"));
            track.setCreated_at(root.optString("created_at"));
            track.setUser_id(root.optString("user_id"));
            JSONObject userObject = root.optJSONObject("user");
            if (userObject != null)
                track.setUser(getUser(userObject.toString()));
            else
                track.setUser(null);
            track.setTitle(root.optString("title"));
            track.setPermalink(root.optString("permalink"));
            track.setPermalink_url(root.optString("permalink_url"));
            track.setUri(root.optString("uri"));
            track.setSharing(root.optString("sharing"));
            track.setEmbeddable_by(root.optString("embeddable_by"));
            track.setPurchase_url(root.optString("purchase_url"));
            track.setArtwork_url(root.optString("artwork_url"));
            track.setDescription(root.optString("description"));
            track.setLabel(root.optString("label"));
            track.setDuration(root.optString("duration"));
            track.setTag_list(root.optString("tag_list"));
            track.setLabel_id(root.optString("label_id"));
            track.setLabel_name(root.optString("label_name"));
            track.setRelease(root.optString("release"));
            track.setRelease_day(root.optString("release_day"));
            track.setRelease_month(root.optString("release_month"));
            track.setRelease_year(root.optString("release_year"));
            track.setStreamable(root.optString("streamable"));
            track.setDownloadable(root.optString("downloadable"));
            track.setState(root.optString("state"));
            track.setLicense(root.optString("license"));
            track.setTrack_type(root.optString("track_type"));
            track.setWaveform_url(root.optString("waveform_url"));
            track.setDownload_url(root.optString("download_url"));
            track.setStream_url(root.optString("stream_url"));
            track.setVideo_url(root.optString("video_url"));
            track.setBpm(root.optString("bpm"));
            track.setCommentable(root.optString("commentable"));
            track.setIsrc(root.optString("isrc"));
            track.setKey_signature(root.optString("key_signature"));
            track.setComment_count(root.optString("comment_count"));
            track.setDownload_count(root.optString("download_count"));
            track.setPlayback_count(root.optString("playback_count"));
            track.setFavoritings_count(root.optString("favoritings_count"));
            track.setOriginal_format(root.optString("original_format"));
            track.setOriginal_content_size(root.optString("original_content_size"));
            track.setAsset_data(root.optString("asset_data"));
            track.setArtwork_data(root.optString("artwork_data"));
            track.setUser_favorite(root.optString("user_favorite"));
            track.setStream_url(root.optString("stream_url"));
            track.setDownload_url(root.optString("download_url"));
            track.setStreamable(root.optString("streamable"));

            JSONObject metadata = root.optJSONObject("publisher_metadata");
            if (metadata != null) {
                track.setPublisher_Metadata(
                        new SCV2Track.PublisherMetadata(
                                metadata.optString("artist"),
                                metadata.optString("publisher"),
                                metadata.optString("album_title"),
                                metadata.optString("release_title")
                        )
                );
            }

            List<SCV2Track.MediaTranscoding> codings = new ArrayList<>();
            JSONObject media = root.optJSONObject("media");
            if (media != null) {
                JSONArray transcodings = media.getJSONArray("transcodings");
                for (int i = 0; i < transcodings.length(); i++) {
                    JSONObject coding = transcodings.getJSONObject(i);
                    SCV2Track.MediaTranscoding mediaTranscoding = new SCV2Track.MediaTranscoding(
                            coding.optString("url"),
                            coding.optString("preset"),
                            coding.optString("duration"),
                            coding.getJSONObject("format").optString("protocol"),
                            coding.getJSONObject("format").optString("mimeType"),
                            coding.optString("quality")
                    );
                    codings.add(mediaTranscoding);
                }
                track.setCodings(codings);
            } else {
                track.setCodings(null);
            }
            return track;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SCParsingException("Failed to extract track from json: " + e.getLocalizedMessage());
        }
    }

    public String getStreamUrlFromStager(String json) throws SCParsingException {
        JSONObject root = new JSONObject(json);
        try {
            return root.getString("url");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SCParsingException("Failed to extract stream url from stager json: " + e.getLocalizedMessage());
        }
    }

    public SCUser getUser(String json) {
        SCUserMutable ret = new SCUserMutable();
        JSONObject root = new JSONObject(json);
        ret.setId(root.optString("id"));
        ret.setPermalink(root.optString("permalink"));
        ret.setUsername(root.optString("username"));
        ret.setUri(root.optString("uri"));
        ret.setPermalink_url(root.optString("permalink_url"));
        ret.setAvatar_url(root.optString("avatar_url"));
        ret.setCountry(root.optString("country"));
        ret.setFull_name(root.optString("full_name"));
        ret.setCity(root.optString("city"));
        ret.setDescription(root.optString("description"));
        ret.setDiscogs_name(root.optString("discogs-name"));
        ret.setMyspace_name(root.optString("myspace-name"));
        ret.setWebsite(root.optString("website"));
        ret.setWebsite_title(root.optString("website-title"));
        ret.setOnline(root.optString("online"));
        ret.setTrack_count(root.optString("track_count"));
        ret.setPlaylist_count(root.optString("playlist_count"));
        ret.setFollowers_count(root.optString("followers_count"));
        ret.setFollowings_count(root.optString("followings_count"));
        ret.setPublic_favorites_count(root.optString("public_favorites_count"));
        return ret;
    }
}
