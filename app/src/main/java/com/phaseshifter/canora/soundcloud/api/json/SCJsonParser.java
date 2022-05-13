package com.phaseshifter.canora.soundcloud.api.json;

import com.phaseshifter.canora.soundcloud.api.data.SCTrack;
import com.phaseshifter.canora.soundcloud.api.data.SCTrackMutable;
import com.phaseshifter.canora.soundcloud.api.exceptions.SCParsingException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the parsing of the raw json strings into the corresponding java objects.
 */
public class SCJsonParser {
    public List<SCTrack> parseTracks(String json) throws SCParsingException {
        List<SCTrack> ret = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray values = root.getJSONArray("collection");
            for (int i = 0; i < values.length(); i++) {
                JSONObject object = values.getJSONObject(i);
                SCTrackMutable track = new SCTrackMutable();
                track.setId(object.optInt("id"));
                track.setTitle(object.optString("title"));
                track.setArtwork_url(object.optString("artwork_url"));
                track.setUri(object.optString("uri"));
                track.setStream_url(object.optString("stream_url"));
                track.setDownload_url(object.optString("download_url"));
                track.setStreamable(object.optString("streamable"));
                track.setPermalink_url(object.optString("permalink_url"));
                ret.add(track);
            }
        } catch (Exception e) {
            throw new SCParsingException("Error parsing json: " + e.getLocalizedMessage());
        }
        return ret;
    }
}
