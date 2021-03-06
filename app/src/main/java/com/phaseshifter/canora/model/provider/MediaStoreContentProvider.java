package com.phaseshifter.canora.model.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import androidx.core.util.Pair;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceUri;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.metadata.ImageMetadataMemory;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadata;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.utils.android.ContentUriFactory;

import java.util.*;

public class MediaStoreContentProvider implements IContentProvider {
    private final String LOG_TAG = "ContentProvider";

    private final Context C;
    private final ContentUriFactory uriFactory;

    public MediaStoreContentProvider(Context c, ContentUriFactory uriFactory) {
        if (c == null || uriFactory == null)
            throw new IllegalArgumentException();
        this.C = c;
        this.uriFactory = uriFactory;
    }

    //MediaStore.Audio.Media.DURATION suddenly "Added in API 29" but it works perfectly fine on API 21.
    @SuppressLint("InlinedApi")
    @Override
    public List<AudioData> getTracks() {
        Log.v(LOG_TAG, "Get Tracks");
        List<AudioData> ret = new ArrayList<>();
        String[] mediaProjection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION
        };
        Cursor mediaCursor;
        try {
            mediaCursor = C.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            mediaCursor = null;
        }
        if (mediaCursor != null) {
            Log.v(LOG_TAG, "MEDIA STORE AUDIO CONTENTS: " + mediaCursor.getCount() + " FILES");
            List<UUID> usedUUIDS = new ArrayList<>();
            final int indexID = mediaCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            final int indexArtist = mediaCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int indexAlbum = mediaCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int indexAlbumID = mediaCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            final int indexTitle = mediaCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int indexDuration = mediaCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            while (mediaCursor.moveToNext()) {
                try {
                    UUID genUUID = UUID.randomUUID();
                    if (usedUUIDS.contains(genUUID))
                        throw new RuntimeException("UUID Collision !!!");
                    usedUUIDS.add(genUUID);
                    int audioID = mediaCursor.getInt(indexID);
                    ImageMetadataMemory imageMetadata = new ImageMetadataMemory(UUID.randomUUID());
                    ImageDataSourceUri imageSource = new ImageDataSourceUri(uriFactory.getAlbumArtworkUriFromAlbumId(mediaCursor.getInt(indexAlbumID)));
                    ImageData image = new ImageData(imageMetadata, imageSource);
                    AudioMetadataMemory metadata = new AudioMetadataMemory(
                            genUUID,
                            mediaCursor.getString(indexTitle),
                            mediaCursor.getString(indexArtist),
                            mediaCursor.getString(indexAlbum),
                            null,
                            Long.parseLong(mediaCursor.getString(indexDuration)),
                            image
                    );
                    AudioDataSourceUri source = new AudioDataSourceUri(uriFactory.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioID));
                    ret.add(new AudioData(metadata, source));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mediaCursor.close();
        } else {
            Log.e(LOG_TAG, "Failed MediaStore query");
        }
        return ret;
    }

    @Override
    public List<AudioPlaylist> getAlbums(List<AudioData> cache) {
        Log.v(LOG_TAG, "Get Albums");
        if (cache == null)
            throw new IllegalArgumentException();
        HashMap<String, List<AudioData>> albums = new HashMap<>();
        for (AudioData track : cache) {
            String title = track.getMetadata().getAlbum();
            if (title != null && title.length() > 0) {
                List<AudioData> albumTracks = albums.get(title);
                if (albumTracks != null) {
                    albumTracks.add(track);
                } else {
                    List<AudioData> tracks = new ArrayList<>();
                    tracks.add(track);
                    albums.put(title, tracks);
                }
            }
        }
        return getPlaylists(albums);
    }

    @Override
    public List<AudioPlaylist> getArtists(List<AudioData> cache) {
        Log.v(LOG_TAG, "Get Artists");
        if (cache == null)
            throw new IllegalArgumentException();
        HashMap<String, List<AudioData>> artists = new HashMap<>();
        for (AudioData track : cache) {
            String title = track.getMetadata().getArtist();
            if (title != null && title.length() > 0) {
                List<AudioData> artistTracks = artists.get(title);
                if (artistTracks != null) {
                    artistTracks.add(track);
                } else {
                    List<AudioData> tracks = new ArrayList<>();
                    tracks.add(track);
                    artists.put(title, tracks);
                }
            }
        }
        return getPlaylists(artists);
    }

    /**
     * For Each genre there is two MediaStore queries made. As there should only be a limited amount of possible genres this should not have a big impact on performance.
     */
    @Override
    public List<AudioPlaylist> getGenres() {
        Log.v(LOG_TAG, "Get Genres");
        List<AudioPlaylist> ret = new ArrayList<>();
        Uri genresURI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        String[] columns = {MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME};
        Cursor cursor = C.getContentResolver().query(genresURI, columns, null, null, null);
        if (cursor != null) {
            final int indexID = cursor.getColumnIndex(MediaStore.Audio.Genres._ID);
            final int indexName = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
            List<Pair<Long, String>> genres = new ArrayList<>();
            while (cursor.moveToNext()) {
                genres.add(new Pair<>(cursor.getLong(indexID), cursor.getString(indexName)));
            }
            cursor.close();
            List<UUID> usedUUIDS = new ArrayList<>();
            for (Pair<Long, String> genre : genres) {
                if (genre != null) {
                    UUID genUUID = UUID.randomUUID();
                    if (usedUUIDS.contains(genUUID))
                        throw new RuntimeException("UUID Collision!!!");
                    usedUUIDS.add(genUUID);
                    PlaylistMetadata metadata = new PlaylistMetadataMemory(genUUID, genre.second, null);
                    ret.add(new AudioPlaylist(metadata, getTracksOfGenreID(genre.first)));
                }
            }
        }
        return ret;
    }

    private List<AudioData> getTracksOfGenreID(Long genreID) {
        if (genreID == null)
            return null;

        Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreID);
        final String[] columns = {
                MediaStore.Audio.Genres.Members._ID,
                MediaStore.Audio.Genres.Members.ARTIST,
                MediaStore.Audio.Genres.Members.ALBUM,
                MediaStore.Audio.Genres.Members.ALBUM_ID,
                MediaStore.Audio.Genres.Members.TITLE,
                MediaStore.Audio.Genres.Members.DURATION
        };

        Cursor mediaCursor = C.getContentResolver().query(uri, columns, null, null, null);

        if (mediaCursor == null)
            throw new RuntimeException("Genre Query Failed for genreID " + genreID);

        final int indexID = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members._ID);
        final int indexArtist = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ARTIST);
        final int indexAlbum = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM);
        final int indexAlbumID = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID);
        final int indexTitle = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members.TITLE);
        final int indexDuration = mediaCursor.getColumnIndex(MediaStore.Audio.Genres.Members.DURATION);

        List<UUID> usedUUIDS = new ArrayList<>();
        List<AudioData> ret = new ArrayList<>();

        while (mediaCursor.moveToNext()) {
            try {
                UUID genUUID = UUID.randomUUID();
                if (usedUUIDS.contains(genUUID))
                    throw new RuntimeException("UUID Collision !!!");
                usedUUIDS.add(genUUID);

                int audioID = mediaCursor.getInt(indexID);

                ImageMetadataMemory imageMetadata = new ImageMetadataMemory(UUID.randomUUID());
                ImageDataSourceUri imageSource = new ImageDataSourceUri(uriFactory.getAlbumArtworkUriFromAlbumId(mediaCursor.getInt(indexAlbumID)));
                ImageData image = new ImageData(imageMetadata, imageSource);
                AudioMetadataMemory metadata = new AudioMetadataMemory(
                        genUUID,
                        mediaCursor.getString(indexTitle),
                        mediaCursor.getString(indexArtist),
                        mediaCursor.getString(indexAlbum),
                        null,
                        Long.parseLong(mediaCursor.getString(indexDuration)),
                        image);
                AudioDataSourceUri source = new AudioDataSourceUri(uriFactory.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioID));
                ret.add(new AudioData(metadata, source));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mediaCursor.close();
        return ret;
    }

    private List<AudioPlaylist> getPlaylists(HashMap<String, List<AudioData>> data) {
        List<UUID> usedUUIDS = new ArrayList<>();
        List<AudioPlaylist> ret = new ArrayList<>();
        for (Map.Entry<String, List<AudioData>> entry : data.entrySet()) {
            String playlistTitle = entry.getKey();
            List<AudioData> playlistTracks = entry.getValue();
            if (playlistTracks != null) {
                UUID playlistID = UUID.randomUUID();
                if (usedUUIDS.contains(playlistID))
                    throw new RuntimeException("UUID Collision");
                usedUUIDS.add(playlistID);
                ImageData playlistArtwork = null;
                try {
                    ImageData trackArtwork = playlistTracks.get(0).getMetadata().getArtwork();
                    if (trackArtwork != null) {
                        playlistArtwork = new ImageData(new ImageMetadataMemory(UUID.randomUUID()), trackArtwork.getDataSource());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                PlaylistMetadata metadata = new PlaylistMetadataMemory(playlistID, playlistTitle, playlistArtwork);
                ret.add(new AudioPlaylist(metadata, playlistTracks));
            }
        }
        return ret;
    }
}