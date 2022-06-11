package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.ContentDataSource;
import com.google.android.exoplayer2.upstream.DataSource;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AudioDataSource backed by a MediaStore Uri.
 * As it is not guaranteed for a uri to point to the same data over time so this should not be used to persistently identify single tracks.
 * The issue is that google decided to force Scoped Storage on us and therefore we cant save filepaths which wont change unless the user
 * actually moves the files.
 * <p>
 * Therefore there are 2 options: Simply ignore the uri inconsistency and use uris to persistenly identify data,
 * or save the whole audio data inside the playlists which would of course duplicate the data.
 */
public class AudioDataSourceUri implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private transient Uri uri;

    public AudioDataSourceUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public List<MediaSource> getExoPlayerSources(Context context) {
        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new ContentDataSource(context);
            }
        };
        List<MediaSource> ret = new ArrayList<>();
        ret.add(new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(uri)));
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceUri that = (AudioDataSourceUri) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "AudioDataSourceUri{" +
                "uri=" + uri +
                '}';
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        uri = Uri.parse((String) in.readObject());
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(uri.toString());
    }
}