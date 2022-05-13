package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.ContentDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.phaseshifter.canora.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2Track;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2TrackStreamData;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class AudioDataSourceSC implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final SCV2Track track;

    public AudioDataSourceSC(SCV2Track track) {
        this.track = track;
    }

    @Override
    public MediaSource getExoPlayerSource(Context context) throws Exception {
        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new ContentDataSource(context);
            }
        };

        SCV2Client client = new SCV2Client();
        client.setClientID(client.getNewClientID());
        SCV2TrackStreamData stream = client.getTemporaryStreamUrl(track);
        return new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(stream.url));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceSC that = (AudioDataSourceSC) o;
        return Objects.equals(track, that.track);
    }

    @Override
    public int hashCode() {
        return Objects.hash(track);
    }

    @Override
    public String toString() {
        return "AudioDataSourceUri{" +
                "track=" + track +
                '}';
    }
}
