package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.ContentDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2Track;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class AudioDataSourceSC implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final List<SCV2Track.MediaTranscoding> codings;

    private static SCV2Client client;
    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    private SCV2Client getClient() {
        if (client == null) {
            client = new SCV2Client();
            updateClientId();
        }
        return client;
    }

    private void updateClientId() {
        try {
            Future<?> f = pool.submit(() -> {
                try {
                    client.setClientID(client.getNewClientID());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            while (!f.isDone()) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AudioDataSourceSC(List<SCV2Track.MediaTranscoding> codings) {
        this.codings = codings;
    }

    @Override
    public MediaSource getExoPlayerSource(Context context) throws Exception {
        SCV2Client client = getClient();

        AtomicReference<String> stream = new AtomicReference<>();
        Future<?> f = pool.submit(() -> {
            try {
                String url = client.getTemporaryStreamUrl(codings).url;
                stream.set(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        while (!f.isDone()) {
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(stream.get())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceSC that = (AudioDataSourceSC) o;
        return Objects.equals(codings, that.codings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codings);
    }

    @Override
    public String toString() {
        return "AudioDataSourceUri{" +
                "codings=" + codings +
                '}';
    }
}
