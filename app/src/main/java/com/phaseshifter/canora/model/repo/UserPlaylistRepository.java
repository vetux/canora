package com.phaseshifter.canora.model.repo;

import android.util.Log;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.metadata.PlayerMetadata;
import com.phaseshifter.canora.data.media.player.metadata.PlayerMetadataMemory;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.model.compression.Gzip;
import com.phaseshifter.canora.utils.serialization.IObjectSerializer;
import com.phaseshifter.canora.utils.serialization.ObjectSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserPlaylistRepository {
    private final String LOG_TAG = "AudiPlaylistRepo";
    private final File playlistsDirectory;
    private final IObjectSerializer serializer;
    private final Map<UUID, AudioPlaylist> playlists = new HashMap<>();

    public UserPlaylistRepository(File playlistsDirectory) {
        if (playlistsDirectory == null)
            throw new IllegalArgumentException();
        serializer = new ObjectSerializer();
        this.playlistsDirectory = playlistsDirectory;
        if (!playlistsDirectory.isDirectory() && !playlistsDirectory.mkdirs()) {
            Log.v(LOG_TAG, "Failed to create playlists directory at " + playlistsDirectory);
        }
        playlists.putAll(readPlaylists());
    }

    public List<AudioPlaylist> getAll() {
        return new ArrayList<>(playlists.values());
    }

    public AudioPlaylist get(UUID key) {
        return playlists.get(key);
    }

    public AudioPlaylist set(UUID key, AudioPlaylist playlist) {
        Log.v(LOG_TAG, "Create Playlist " + key + " " + playlist);

        if (playlist == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedTracks = prepareData(playlist.getData());

        PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(key,
                playlist.getMetadata().getTitle(),
                playlist.getMetadata().getArtwork());
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedTracks);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylist(generatedPlaylist);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    public AudioPlaylist add(AudioPlaylist playlist) {
        Log.v(LOG_TAG, "Add Playlist " + playlist);

        if (playlist == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedTracks = prepareData(playlist.getData());

        UUID uuid = UUID.randomUUID();

        if (playlists.containsKey(uuid))
            throw new RuntimeException("UUID Collision for " + uuid);

        PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(uuid,
                playlist.getMetadata().getTitle(),
                playlist.getMetadata().getArtwork());
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedTracks);
        playlists.put(uuid, generatedPlaylist);

        try {
            writePlaylist(generatedPlaylist);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    public void replace(UUID key, AudioPlaylist value) {
        Log.v(LOG_TAG, "Replace Playlist " + key + " " + value);

        if (value == null)
            throw new IllegalArgumentException();

        List<PlayerData> modifiedData = prepareData(value.getData());

        PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(key,
                value.getMetadata().getTitle(),
                value.getMetadata().getArtwork());
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedData);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylist(generatedPlaylist);
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
            PlayerMetadataMemory modifiedMetadata = new PlayerMetadataMemory(
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

    private Map<UUID, AudioPlaylist> readPlaylists() {
        Map<UUID, AudioPlaylist> ret = new HashMap<>();
        File[] playlistFiles = playlistsDirectory.listFiles();
        if (playlistFiles != null) {
            for (File playlistFile : playlistFiles) {
                try {
                    AudioPlaylist pl = readPlaylist(new FileInputStream(playlistFile));
                    ret.put(pl.getMetadata().getId(), pl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    private AudioPlaylist readPlaylist(InputStream istream) throws Exception {
        if (istream == null)
            throw new IllegalArgumentException();

        byte[] playlistBytes = getBytesFromStream(istream);
        if (playlistBytes.length == 0) {
            throw new Exception("No playlist data found.");
        }

        Log.v(LOG_TAG, "Compressed playlist byte size: " + playlistBytes.length + " bytes.");

        playlistBytes = Gzip.decompress(playlistBytes);

        Log.v(LOG_TAG, "Decompressed playlists byte size: " + playlistBytes.length + " bytes.");

        return (AudioPlaylist) serializer.deserialize(playlistBytes);
    }

    private void writePlaylist(AudioPlaylist playlist) throws IOException {
        if (playlist == null) {
            throw new IllegalArgumentException();
        }
        File file = getPlaylistFile(playlist.getMetadata().getId());
        try {
            file.delete();
        } catch (Exception e){
            e.printStackTrace();
        }
        if (!file.createNewFile()) {
            throw new IOException("Playlist file already exists at " + file);
        }
        FileOutputStream stream = new FileOutputStream(file);
        byte[] buffer = serializer.serialize(playlist);
        buffer = Gzip.compress(buffer);
        stream.write(buffer);
        stream.close();
    }

    private File getPlaylistFile(UUID id) {
        return new File(playlistsDirectory, id.toString());
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