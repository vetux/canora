package com.phaseshifter.canora.plugin.ytdl;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSource;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCConnectionException;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2StreamProtocol;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2TrackStreamData;
import com.phaseshifter.canora.utils.Pair;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.json.JSONException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AudioDataSourceYtdl implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();
    private static CountDownLatch latch = new CountDownLatch(0);

    private final String url;
    private transient String streamUrl = null;

    private void syncWithPool() {
        while (latch.getCount() != 0) {
            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public AudioDataSourceYtdl(String url) {
        this.url = url;
    }

    @Override
    public void prepare() {
        syncWithPool();
        if (streamUrl == null) {
            latch = new CountDownLatch(1);
            pool.submit(() -> {
                try {
                    YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-f", "best");
                    VideoInfo streamInfo = ytdl.getInfo(request);
                    streamUrl = streamInfo.getUrl();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latch.countDown();
            });
        }
    }

    @Override
    public List<MediaSource> getExoPlayerSources(Context context) {
        prepare();
        syncWithPool();
        if (streamUrl == null) {
            throw new RuntimeException("Failed to retrieve stream data for track " + url);
        }
        List<MediaSource> ret = new ArrayList<>();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
        ret.add(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl))));
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceYtdl that = (AudioDataSourceYtdl) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "AudioDataSourceYtdl{" +
                "codings=" + url +
                '}';
    }
}
