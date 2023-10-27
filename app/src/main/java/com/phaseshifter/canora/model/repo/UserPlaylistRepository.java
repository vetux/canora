package com.phaseshifter.canora.model.repo;

import android.util.Log;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.model.compression.Gzip;
import com.phaseshifter.canora.model.io.PlaylistFileFormat;
import com.phaseshifter.canora.utils.serialization.IObjectSerializer;
import com.phaseshifter.canora.utils.serialization.ObjectSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserPlaylistRepository {
    private final String LOG_TAG = "AudiPlaylistRepo";
    private final File playlistsFile;
    private final IObjectSerializer serializer;
    private final Map<UUID, Playlist> playlists = new HashMap<>();

    public UserPlaylistRepository(File playlistsFile) {
        if (playlistsFile == null)
            throw new IllegalArgumentException();
        serializer = new ObjectSerializer();
        this.playlistsFile = playlistsFile;
        try {
            playlists.putAll(readPlaylists());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<Playlist> getAll() {
        return new ArrayList<>(playlists.values());
    }

    public Playlist get(UUID key) {
        return playlists.get(key);
    }

    public Playlist set(UUID key, Playlist playlist) {
        Log.v(LOG_TAG, "Create Playlist " + key + " " + playlist);

        if (playlist == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedTracks = prepareData(playlist.getTracks());

        PlaylistMetadata metadata = new PlaylistMetadata(key,
                playlist.getMetadata().getTitle(),
                playlist.getMetadata().getArtwork());
        Playlist generatedPlaylist = new Playlist(metadata, modifiedTracks);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylists(playlists.values());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    public Playlist add(Playlist playlist) {
        Log.v(LOG_TAG, "Add Playlist " + playlist);

        if (playlist == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedTracks = prepareData(playlist.getTracks());

        UUID uuid = UUID.randomUUID();

        if (playlists.containsKey(uuid))
            throw new RuntimeException("UUID Collision for " + uuid);

        PlaylistMetadata metadata = new PlaylistMetadata(uuid,
                playlist.getMetadata().getTitle(),
                playlist.getMetadata().getArtwork());
        Playlist generatedPlaylist = new Playlist(metadata, modifiedTracks);
        playlists.put(uuid, generatedPlaylist);

        try {
            writePlaylists(playlists.values());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    public void replace(UUID key, Playlist value) {
        Log.v(LOG_TAG, "Replace Playlist " + key + " " + value);

        if (value == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedData = prepareData(value.getTracks());

        PlaylistMetadata metadata = new PlaylistMetadata(key,
                value.getMetadata().getTitle(),
                value.getMetadata().getArtwork());
        Playlist generatedPlaylist = new Playlist(metadata, modifiedData);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylists(playlists.values());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(UUID key) {
        Log.v(LOG_TAG, "Remove Playlist " + key);
        playlists.remove(key);
        getPlaylistFile(key).delete();
    }

    public void remove(List<UUID> keys) {
        Log.v(LOG_TAG, "Remove Playlists " + keys);
        if (keys == null)
            throw new IllegalArgumentException();
        for (UUID key : keys) {
            Log.v(LOG_TAG, "Delete Playlist " + key);
            playlists.remove(key);
            getPlaylistFile(key).delete();
        }
    }

    public long getSize() {
        return playlists.values().size();
    }

    private List<PlayerData> prepareData(List<PlayerData> orig) {
        List<PlayerData> modifiedTracks = new ArrayList<>();
        List<UUID> usedUUIDS = new ArrayList<>();
        for (PlayerData track : orig) {
            UUID genUUID = UUID.randomUUID();
            if (usedUUIDS.contains(genUUID))
                throw new RuntimeException("UUID Collision !!!");
            usedUUIDS.add(genUUID);
            PlayerMetadata existingMetadata = track.getMetadata();
            PlayerMetadata modifiedMetadata = new PlayerMetadata(
                    genUUID,
                    existingMetadata.getTitle(),
                    existingMetadata.getArtist(),
                    existingMetadata.getAlbum(),
                    existingMetadata.getGenres(),
                    existingMetadata.getDuration(),
                    existingMetadata.getArtwork()
            );
            modifiedTracks.add(new PlayerData(modifiedMetadata, track.getDataSource()));
        }
        return modifiedTracks;
    }

    private Map<UUID, Playlist> readPlaylists() throws IOException {
        Map<UUID, Playlist> ret = new HashMap<>();
        String txt = new String(Gzip.decompress(Files.readAllBytes(playlistsFile.toPath())));
        List<Playlist> playlists = PlaylistFileFormat.deserialize(txt);
        for (Playlist pl : playlists) {
            ret.put(pl.getMetadata().getId(), pl);
        }
        return ret;
    }

    private void writePlaylists(Collection<Playlist> playlists) throws IOException {
        if (playlists == null) {
            throw new IllegalArgumentException();
        }
        FileOutputStream stream = new FileOutputStream(playlistsFile);
        byte[] buffer = PlaylistFileFormat.serialize(playlists).getBytes(StandardCharsets.UTF_8);
        buffer = Gzip.compress(buffer);
        stream.write(buffer);
        stream.close();
    }

    private File getPlaylistFile(UUID id) {
        return new File(playlistsFile, id.toString());
    }

    private byte[] getBytesFromStream(InputStream istream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int numberOfCharactersRed;
        byte[] data = new byte[16384];
        while ((numberOfCharactersRed = istream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, numberOfCharactersRed);
        }
        istream.close();
        return buffer.toByteArray();
    }
}