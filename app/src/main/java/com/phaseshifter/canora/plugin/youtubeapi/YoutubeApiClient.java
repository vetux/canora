package com.phaseshifter.canora.plugin.youtubeapi;

import android.net.Uri;

import com.phaseshifter.canora.net.http.HttpClient;
import com.phaseshifter.canora.net.http.HttpClientBuilder;
import com.phaseshifter.canora.net.http.HttpMethod;
import com.phaseshifter.canora.net.http.HttpRequest;
import com.phaseshifter.canora.net.http.HttpResponse;
import com.phaseshifter.canora.net.http.HttpStatusCode;
import com.phaseshifter.canora.utils.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YoutubeApiClient {
    public static final String ENDPOINT_YOUTUBE_API_SEARCH = "https://www.googleapis.com/youtube/v3/search";
    public static final String ENDPOINT_YOUTUBE = "http://www.youtube.com/watch";

    public YoutubeApiClient() {
        client = new HttpClientBuilder().build();
    }

    public YoutubeApiClient(HttpClient client) {
        this.client = client;
    }

    public YoutubeResponse execute(YoutubeRequest query) throws IOException, JSONException {
        return createResponse(client.doRequest(createRequest(query)));
    }

    public Uri getVideoUrl(YoutubeVideo video) {
        return Uri.parse(ENDPOINT_YOUTUBE + "?v=" + video.id);
    }

    public Uri getPlaylistUrl(YoutubePlaylist playlist) {
        return Uri.parse(ENDPOINT_YOUTUBE + "?list=" + playlist.id);
    }

    public HttpRequest createRequest(YoutubeRequest query) {
        List<Pair<String, String>> parameters = new ArrayList<>();

        parameters.add(new Pair<>("key", query.key));

        parameters.add(new Pair<>("part", "id, snippet"));

        if (query.searchText != null)
            parameters.add(new Pair<>("q", query.searchText));
        if (query.pageToken != null)
            parameters.add(new Pair<>("pageToken", query.pageToken));
        if (query.channelId != null)
            parameters.add(new Pair<>("channelId", query.channelId));
        if (query.regionCode != null)
            parameters.add(new Pair<>("regionCode", query.regionCode));
        if (query.maxResults != null)
            parameters.add(new Pair<>("maxResults", String.valueOf(query.maxResults)));
        if (query.order != null)
            parameters.add(new Pair<>("order", query.order.str));
        if (query.type != null)
            parameters.add(new Pair<>("type", query.type.str));
        if (query.videoDuration != null)
            parameters.add(new Pair<>("videoDuration", query.videoDuration.str));

        return new HttpRequest(HttpMethod.GET,
                ENDPOINT_YOUTUBE_API_SEARCH,
                parameters);
    }

    public YoutubeResponse createResponse(HttpResponse response) throws JSONException, IOException {
        if (response.getStatusCode() == HttpStatusCode.OK.code) {
            YoutubeResponse ret = new YoutubeResponse();
            String json = response.getBodyString();

            JSONObject root = new JSONObject(json);

            ret.prevPage = root.optString("prevPageToken");
            ret.nextPage = root.optString("nextPageToken");

            JSONArray items = root.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject id = item.getJSONObject("id");
                JSONObject snippet = item.getJSONObject("snippet");
                JSONObject thumbnails = snippet.getJSONObject("thumbnails");
                String kind = id.getString("kind");
                if (kind.equals("youtube#video")) {
                    YoutubeVideo video = new YoutubeVideo();

                    video.id = id.getString("videoId");
                    video.title = snippet.getString("title");
                    video.description = snippet.getString("description");
                    video.publishedAt = snippet.getString("publishedAt");
                    video.channel = new YoutubeChannel();
                    video.channel.id = snippet.getString("channelId");
                    video.channel.name = snippet.getString("channelTitle");

                    video.thumbnail = new YoutubeThumbnail();

                    JSONObject tn = thumbnails.getJSONObject("default");
                    video.thumbnail.defaultThumbnail.uri = Uri.parse(tn.getString("url"));
                    video.thumbnail.defaultThumbnail.width = tn.optInt("width");
                    video.thumbnail.defaultThumbnail.height = tn.optInt("height");

                    tn = thumbnails.getJSONObject("medium");
                    video.thumbnail.medium.uri = Uri.parse(tn.getString("url"));
                    video.thumbnail.medium.width = tn.optInt("width");
                    video.thumbnail.medium.height = tn.optInt("height");

                    tn = thumbnails.getJSONObject("high");
                    video.thumbnail.high.uri = Uri.parse(tn.getString("url"));
                    video.thumbnail.high.width = tn.optInt("width");
                    video.thumbnail.high.height = tn.optInt("height");

                    ret.videos.add(video);
                } else if (kind.equals("youtube#channel")) {
                    YoutubeChannel channel = new YoutubeChannel();

                    channel.id = id.getString("channelId");
                    channel.name = snippet.getString("channelTitle");

                    ret.channels.add(channel);
                } else if (kind.equals("youtube#playlist")) {
                    YoutubePlaylist playlist = new YoutubePlaylist();

                    playlist.id = id.getString("videoId");
                    playlist.title = snippet.getString("title");
                    playlist.description = snippet.getString("description");
                    playlist.publishedAt = snippet.getString("publishedAt");
                    playlist.channel = new YoutubeChannel();
                    playlist.channel.id = snippet.getString("channelId");
                    playlist.channel.name = snippet.getString("channelTitle");

                    playlist.thumbnail = new YoutubeThumbnail();

                    JSONObject tn = thumbnails.getJSONObject("default");
                    playlist.thumbnail.defaultThumbnail.uri = Uri.parse(tn.getString("url"));
                    playlist.thumbnail.defaultThumbnail.width = tn.optInt("width");
                    playlist.thumbnail.defaultThumbnail.height = tn.optInt("height");

                    tn = thumbnails.getJSONObject("medium");
                    playlist.thumbnail.medium.uri = Uri.parse(tn.getString("url"));
                    playlist.thumbnail.medium.width = tn.optInt("width");
                    playlist.thumbnail.medium.height = tn.optInt("height");

                    tn = thumbnails.getJSONObject("high");
                    playlist.thumbnail.high.uri = Uri.parse(tn.getString("url"));
                    playlist.thumbnail.high.width = tn.optInt("width");
                    playlist.thumbnail.high.height = tn.optInt("height");

                    ret.playlists.add(playlist);
                }
            }
            return ret;
        } else {
            return null;
        }
    }

    private HttpClient client;
}
