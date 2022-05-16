package com.phaseshifter.canora.plugin.soundcloud;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSource;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AudioDataSourceSC implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private static SCV2Client client;
    private static ExecutorService pool = Executors.newSingleThreadExecutor();
    private static CountDownLatch latch = new CountDownLatch(0);

    private final List<SCV2Track.MediaTranscoding> codings;
    private final AtomicReference<String> streamUrl = new AtomicReference<>();

    private SCV2Client getClient() {
        if (client == null) {
            client = new SCV2Client();
            updateClientId();
        }
        return client;
    }

    private void updateClientId() {
        try {
            client.setClientID(client.getNewClientID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncWithPool() {
        while (latch.getCount() != 0) {
            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public AudioDataSourceSC(List<SCV2Track.MediaTranscoding> codings) {
        this.codings = codings;
    }

    @Override
    public void prepare() {
        syncWithPool();

        if (streamUrl.get() == null) {
            latch = new CountDownLatch(1);
            pool.submit(() -> {
                try {
                    SCV2Client client = getClient();
                    String url = client.getTemporaryStreamUrl(codings).url;
                    streamUrl.set(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
        }
    }

    @Override
    public MediaSource getExoPlayerSource(Context context) {
        prepare();

        syncWithPool();

        if (streamUrl.get() == null) {
            throw new RuntimeException("Failed to retrieve stream url for track " + codings);
        }

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl.get())));
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
