package com.phaseshifter.canora.model.repo;

import android.util.Log;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadata;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataSimple;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataSimple;
import com.phaseshifter.canora.model.compression.Gzip;
import com.phaseshifter.canora.utils.serialization.IObjectSerializer;
import com.phaseshifter.canora.utils.serialization.ObjectSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AudioPlaylistRepo implements AudioPlaylistRepository {
    private final String LOG_TAG = "AudiPlaylistRepo";

    private final File playlistFile;

    private final IObjectSerializer serializer;

    private HashMap<UUID, AudioPlaylist> playlists = null;

    public AudioPlaylistRepo(File playlistFile) {
        serializer = new ObjectSerializer();
        this.playlistFile = playlistFile;
    }

    @Override
    public List<AudioPlaylist> getAll() {
        checkContainer();
        return new ArrayList<>(playlists.values());
    }

    @Override
    public AudioPlaylist get(UUID key) {
        checkContainer();
        return playlists.get(key);
    }

    @Override
    public AudioPlaylist set(UUID key, AudioPlaylist playlist) {
        Log.v(LOG_TAG, "Create Playlist " + key + " " + playlist);
        checkContainer();
        if (playlist == null)
            throw new IllegalArgumentException();

        ImageData artwork = playlist.getMetadata().getArtwork();

        List<AudioData> modifiedTracks = prepareData(playlist.getData(), artwork);

        PlaylistMetadataSimple metadata = new PlaylistMetadataSimple(key, playlist.getMetadata().getTitle(), artwork);
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedTracks);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylists(new FileOutputStream(playlistFile), playlists);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    @Override
    public AudioPlaylist add(AudioPlaylist playlist) {
        Log.v(LOG_TAG, "Add Playlist " + playlist);
        checkContainer();
        if (playlist == null)
            throw new IllegalArgumentException();

        ImageData image = playlist.getMetadata().getArtwork();

        List<AudioData> modifiedTracks = prepareData(playlist.getData(), image);

        UUID uuid = UUID.randomUUID();

        if (playlists.containsKey(uuid))
            throw new RuntimeException("UUID Collision for " + uuid);

        PlaylistMetadataSimple metadata = new PlaylistMetadataSimple(uuid, playlist.getMetadata().getTitle(), image);
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedTracks);
        playlists.put(uuid, generatedPlaylist);

        try {
            writePlaylists(new FileOutputStream(playlistFile), playlists);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generatedPlaylist;
    }

    @Override
    public void replace(UUID key, AudioPlaylist value) {
        Log.v(LOG_TAG, "Replace Playlist " + key + " " + value);
        checkContainer();
        if (value == null)
            throw new IllegalArgumentException();

        ImageData image = value.getMetadata().getArtwork();

        List<AudioData> modifiedData = prepareData(value.getData(), image);

        PlaylistMetadataSimple metadata = new PlaylistMetadataSimple(key, value.getMetadata().getTitle(), image);
        AudioPlaylist generatedPlaylist = new AudioPlaylist(metadata, modifiedData);
        playlists.put(key, generatedPlaylist);

        try {
            writePlaylists(new FileOutputStream(playlistFile), playlists);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(UUID key) {
        Log.v(LOG_TAG, "Remove Playlist " + key);
        checkContainer();
        playlists.remove(key);
        try {
            writePlaylists(new FileOutputStream(playlistFile), playlists);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(List<UUID> keys) {
        Log.v(LOG_TAG, "Remove Playlists " + keys);
        checkContainer();
        if (keys == null)
            throw new IllegalArgumentException();
        for (UUID key : keys) {
            Log.v(LOG_TAG, "Delete Playlist " + key);
            playlists.remove(key);
        }
        try {
            writePlaylists(new FileOutputStream(playlistFile), playlists);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getSize() {
        return playlistFile.length();
    }

    private void checkContainer() {
        if (playlists == null) {
            try {
                playlists = readPlaylists(new FileInputStream(playlistFile));
            } catch (IOException e) {
                e.printStackTrace();
                playlists = new HashMap<>();
            }
        }
    }

    private List<AudioData> prepareData(List<AudioData> orig, ImageData artwork) {
        List<AudioData> modifiedTracks = new ArrayList<>();
        List<UUID> usedUUIDS = new ArrayList<>();
        for (AudioData track : orig) {
            UUID genUUID = UUID.randomUUID();
            if (usedUUIDS.contains(genUUID))
                throw new RuntimeException("UUID Collision !!!");
            usedUUIDS.add(genUUID);
            AudioMetadata existingMetadata = track.getMetadata();
            AudioMetadataSimple modifiedMetadata = new AudioMetadataSimple(
                    genUUID,
                    existingMetadata.getTitle(),
                    existingMetadata.getArtist(),
                    existingMetadata.getAlbum(),
                    existingMetadata.getGenres(),
                    existingMetadata.getLength(),
                    artwork
            );
            modifiedTracks.add(new AudioData(modifiedMetadata, track.getDataSource()));
        }
        return modifiedTracks;
    }

    private HashMap<UUID, AudioPlaylist> readPlaylists(InputStream istream) {
        if (istream == null)
            throw new IllegalArgumentException();
        byte[] playlistBytes;
        try {
            playlistBytes = getBytesFromStream(istream);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
        if (playlistBytes.length == 0) {
            Log.v(LOG_TAG, "No playlist data found.");
            return new HashMap<>();
        }
        Log.v(LOG_TAG, "Compressed playlists byte size: " + playlistBytes.length + " bytes.");
        try {
            playlistBytes = Gzip.decompress(playlistBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
        Log.v(LOG_TAG, "Decompressed playlists byte size: " + playlistBytes.length + " bytes.");
        try {
            return (HashMap<UUID, AudioPlaylist>) serializer.deserialize(playlistBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void writePlaylists(OutputStream ostream, HashMap<UUID, AudioPlaylist> playlists) throws IOException {
        if (ostream == null || playlists == null)
            throw new IllegalArgumentException();
        byte[] buffer = serializer.serialize(playlists);
        buffer = Gzip.compress(buffer);
        ostream.write(buffer);
        ostream.close();
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