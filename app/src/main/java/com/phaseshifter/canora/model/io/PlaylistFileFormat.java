package com.phaseshifter.canora.model.io;

import android.net.Uri;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.ImageMetadata;
import com.phaseshifter.canora.data.media.image.source.AlbumCoverDataSource;
import com.phaseshifter.canora.data.media.image.source.ImageDataSource;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceByteArray;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSourceFile;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.plugin.soundcloud.AudioDataSourceSC;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;
import com.phaseshifter.canora.plugin.ytdl.AudioDataSourceYtdl;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class PlaylistFileFormat {
    public static final int VERSION = 0;

    public static ObjectNode serializeImageData(ImageData data, ObjectMapper mapper) {
        ObjectNode ret = mapper.createObjectNode();
        ret.putPOJO("metadata", data.getMetadata());
        if (data.getDataSource() instanceof AlbumCoverDataSource) {
            AlbumCoverDataSource src = (AlbumCoverDataSource) data.getDataSource();
            ret.put("type", 0);
            ret.put("uri", src.trackUri);
        } else if (data.getDataSource() instanceof ImageDataSourceByteArray) {
            ImageDataSourceByteArray src = (ImageDataSourceByteArray) data.getDataSource();
            String base64 = Base64.getEncoder().encodeToString(src.imageData);
            ret.put("type", 1);
            ret.put("data", base64);
        } else if (data.getDataSource() instanceof ImageDataSourceUri) {
            ImageDataSourceUri src = (ImageDataSourceUri) data.getDataSource();
            ret.put("type", 2);
            ret.put("uri", src.uriStr);
        }
        return ret;
    }

    public static ImageData deserializeImageData(JsonNode node, ObjectMapper mapper) throws JsonProcessingException {
        if (node != null) {
            ImageMetadata metadata = mapper.treeToValue(node.get("metadata"), ImageMetadata.class);
            ImageDataSource source = null;

            int type = node.get("type").asInt();
            switch (type) {
                case 0:
                    source = new AlbumCoverDataSource(node.get("uri").asText());
                    break;
                case 1:
                    source = new ImageDataSourceByteArray(Base64.getDecoder().decode(node.get("data").asText()));
                    break;
                case 2:
                    source = new ImageDataSourceUri(Uri.parse(node.get("uri").asText()));
                    break;
            }

            return new ImageData(metadata, source);
        } else {
            return null;
        }
    }

    public static ObjectNode serializePlayerMetadata(PlayerMetadata data, ObjectMapper mapper) {
        ObjectNode ret = mapper.createObjectNode();
        ret.put("id", data.id.toString());
        ret.put("title", data.title);
        ret.put("artist", data.artist);
        ret.put("album", data.album);
        ret.putPOJO("genres", data.genres);
        ret.put("duration", data.duration);
        if (data.artwork != null) {
            ret.set("artwork", serializeImageData(data.artwork, mapper));
        }
        return ret;
    }

    public static PlayerMetadata deserializePlayerMetadata(JsonNode node, ObjectMapper mapper) throws JsonProcessingException {
        PlayerMetadata ret = new PlayerMetadata();
        ret.id = UUID.fromString(node.get("id").asText());
        ret.title = node.get("title").asText();
        ret.artist = node.get("artist").asText();
        ret.album = node.get("album").asText();
        ret.genres = mapper.treeToValue(node.get("genres"), String[].class);
        ret.duration = node.get("duration").asLong();
        ret.artwork = deserializeImageData(node.get("artwork"), mapper);
        return ret;
    }

    public static ObjectNode serializePlayerData(PlayerData data, ObjectMapper mapper) {
        ObjectNode ret = mapper.createObjectNode();
        ret.set("metadata", serializePlayerMetadata(data.getMetadata(), mapper));

        ObjectNode playerSourceNode = mapper.createObjectNode();
        PlayerDataSource playerSource = data.getDataSource();
        if (playerSource instanceof PlayerDataSourceFile) {
            PlayerDataSourceFile fTrack = (PlayerDataSourceFile) playerSource;
            playerSourceNode.put("path", fTrack.getFile().getAbsolutePath());
            ret.put("type", 0);
        } else if (playerSource instanceof PlayerDataSourceUri) {
            PlayerDataSourceUri fTrack = (PlayerDataSourceUri) playerSource;
            playerSourceNode.put("uri", fTrack.getUri().toString());
            ret.put("type", 1);
        } else if (playerSource instanceof AudioDataSourceSC) {
            AudioDataSourceSC fTrack = (AudioDataSourceSC) playerSource;
            playerSourceNode.putPOJO("codings", fTrack.getCodings());
            ret.put("type", 2);
        } else if (playerSource instanceof AudioDataSourceYtdl) {
            AudioDataSourceYtdl fTrack = (AudioDataSourceYtdl) playerSource;
            playerSourceNode.put("url", fTrack.getUrl());
            ret.put("type", 3);
        }

        ret.set("source", playerSourceNode);

        return ret;
    }

    public static PlayerData deserializePlayerData(JsonNode node, ObjectMapper mapper) throws JsonProcessingException {
        PlayerMetadata metadata = deserializePlayerMetadata(node.get("metadata"), mapper);
        PlayerDataSource source = null;

        JsonNode sourceNode = node.get("source");

        int type = node.get("type").asInt();
        switch (type) {
            case 0:
                source = new PlayerDataSourceFile(new File(sourceNode.get("path").asText()));
                break;
            case 1:
                source = new PlayerDataSourceUri(Uri.parse(sourceNode.get("uri").asText()));
                break;
            case 2:
                List<SCV2Track.MediaTranscoding> codings = new ArrayList<>();
                for (Iterator<JsonNode> iterator = sourceNode.get("codings").elements(); iterator.hasNext(); ) {
                    JsonNode coding = iterator.next();
                    codings.add(mapper.treeToValue(coding, SCV2Track.MediaTranscoding.class));
                }
                source = new AudioDataSourceSC(codings);
                break;
            case 3:
                source = new AudioDataSourceYtdl(sourceNode.get("url").asText());
                break;
        }

        return new PlayerData(metadata, source);
    }

    public static ObjectNode serializePlaylistMetadata(PlaylistMetadata data, ObjectMapper mapper) {
        ObjectNode ret = mapper.createObjectNode();
        ret.put("id", data.id.toString());
        ret.put("title", data.title);
        if (data.artwork != null) {
            ret.set("artwork", serializeImageData(data.artwork, mapper));
        }
        return ret;
    }

    public static PlaylistMetadata deserializePlaylistMetadata(JsonNode node, ObjectMapper mapper) throws JsonProcessingException {
        PlaylistMetadata ret = new PlaylistMetadata();
        ret.id = UUID.fromString(node.get("id").asText());
        ret.title = node.get("title").asText();
        ret.artwork = deserializeImageData(node.get("artwork"), mapper);
        return ret;
    }

    public static ObjectNode serializePlaylist(Playlist playlist, ObjectMapper mapper) {
        ObjectNode ret = mapper.createObjectNode();
        ret.set("metadata", serializePlaylistMetadata(playlist.getMetadata(), mapper));
        ArrayNode tracks = mapper.createArrayNode();
        for (PlayerData track : playlist.getTracks()) {
            tracks.add(serializePlayerData(track, mapper));
        }
        ret.set("tracks", tracks);
        return ret;
    }

    public static Playlist deserializePlaylist(JsonNode node, ObjectMapper mapper) throws JsonProcessingException {
        PlaylistMetadata metadata = deserializePlaylistMetadata(node.get("metadata"), mapper);
        List<PlayerData> tracks = new ArrayList<>();
        for (Iterator<JsonNode> iter = node.get("tracks").elements(); iter.hasNext(); ) {
            JsonNode trackNode = iter.next();
            PlayerData track = deserializePlayerData(trackNode, mapper);
            if (track.getDataSource() != null) {
                tracks.add(track);
            }
        }
        return new Playlist(metadata, tracks);
    }

    public static String serialize(Collection<Playlist> playlists) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (Playlist playlist : playlists) {
            arrayNode.add(serializePlaylist(playlist, mapper));
        }
        ObjectNode ret = mapper.createObjectNode();
        ret.put("version", VERSION);
        ret.set("playlists", arrayNode);
        return mapper.writeValueAsString(ret);
    }

    public static List<Playlist> deserialize(String txt) throws JsonProcessingException {
        List<Playlist> ret = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(txt);
        JsonNode playlists = root.get("playlists");
        for (Iterator<JsonNode> it = playlists.elements(); it.hasNext(); ) {
            ret.add(deserializePlaylist(it.next(), mapper));
        }
        return ret;
    }
}
