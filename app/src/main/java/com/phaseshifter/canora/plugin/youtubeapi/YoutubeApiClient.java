package com.phaseshifter.canora.plugin.youtubeapi;

import android.net.Uri;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.metadata.ImageMetadataMemory;
import com.phaseshifter.canora.data.media.image.source.ImageDataSource;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.net.http.HttpClient;
import com.phaseshifter.canora.net.http.HttpClientBuilder;
import com.phaseshifter.canora.net.http.HttpMethod;
import com.phaseshifter.canora.net.http.HttpRequest;
import com.phaseshifter.canora.net.http.HttpResponse;
import com.phaseshifter.canora.net.http.HttpStatusCode;
import com.phaseshifter.canora.plugin.ytdl.AudioDataSourceYtdl;
import com.phaseshifter.canora.utils.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class YoutubeApiClient {
    public static final String ENDPOINT_YOUTUBE_API_SEARCH = "https://www.googleapis.com/youtube/v3/search";
    public static final String ENDPOINT_YOUTUBE_API_VIDEOS = "https://www.googleapis.com/youtube/v3/videos";
    public static final String ENDPOINT_YOUTUBE_API_PLAYLIST_ITEMS = "https://www.googleapis.com/youtube/v3/playlistItems";

    public static final String ENDPOINT_YOUTUBE = "http://www.youtube.com/watch";

    public YoutubeApiClient() {
        client = new HttpClientBuilder().build();
    }

    public YoutubeApiClient(HttpClient client) {
        this.client = client;
    }

    public YoutubeResponse execute(YoutubeRequest query) throws IOException, JSONException {
        return createSearchResponse(client.doRequest(createSearchRequest(query)));
    }

    public AudioData getAudioData(String key, YoutubeVideo video) throws IOException, JSONException {
        List<YoutubeVideo> videos = new ArrayList<>();
        videos.add(video);
        return createVideoResponse(client.doRequest(createVideoRequest(key, videos))).get(0);
    }

    public List<AudioData> getAudioData(String key, List<YoutubeVideo> videos) throws IOException, JSONException {
        return createVideoResponse(client.doRequest(createVideoRequest(key, videos)));
    }

    public AudioPlaylist getAudioPlaylist(String key, YoutubePlaylist playlist) {
        throw new RuntimeException("Not Implemented");
    }

    public Uri getVideoUrl(YoutubeVideo video) {
        return Uri.parse(ENDPOINT_YOUTUBE + "?v=" + video.id);
    }

    public Uri getPlaylistUrl(YoutubePlaylist playlist) {
        return Uri.parse(ENDPOINT_YOUTUBE + "?list=" + playlist.id);
    }

    private HttpRequest createVideoRequest(String key, List<YoutubeVideo> videos) {
        List<Pair<String, String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>("key", key));
        parameters.add(new Pair<>("part", "snippet,contentDetails"));
        StringBuilder idStr = new StringBuilder();
        for (YoutubeVideo vid : videos) {
            idStr.append(vid.id).append(",");
        }
        parameters.add(new Pair<>("id", idStr.toString()));
        return new HttpRequest(HttpMethod.GET, ENDPOINT_YOUTUBE_API_VIDEOS, parameters);
    }

    private List<AudioData> createVideoResponse(HttpResponse response) throws IOException, JSONException {
        JSONObject json = new JSONObject(response.getBodyString());
        JSONArray items = json.getJSONArray("items");

        List<AudioData> ret = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject video = items.getJSONObject(i);
            JSONObject snippet = video.getJSONObject("snippet");
            JSONObject contentDetails = video.getJSONObject("contentDetails");
            JSONObject thumbnail = snippet.getJSONObject("thumbnails").getJSONObject("default");
            String uri = ENDPOINT_YOUTUBE + "?v=" + video.get("id");
            AudioMetadataMemory metadata = new AudioMetadataMemory();
            metadata.setId(UUID.randomUUID());
            metadata.setTitle(snippet.getString("title"));
            metadata.setArtist(snippet.getString("channelTitle"));
            Duration duration = Duration.parse(contentDetails.getString("duration"));
            metadata.setLength(duration.getSeconds() * 1000);
            metadata.setArtwork(new ImageData(new ImageMetadataMemory(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(thumbnail.getString("url")))));
            ret.add(new AudioData(metadata, new AudioDataSourceYtdl(uri)));
        }
        return ret;
    }

    private HttpRequest createSearchRequest(YoutubeRequest query) {
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

    private YoutubeResponse createSearchResponse(HttpResponse response) throws JSONException, IOException {
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
            throw new RuntimeException("Received Status: " + String.valueOf(response.getStatusCode()) + " from endpoint");
        }
    }

    private HttpClient client;
}
