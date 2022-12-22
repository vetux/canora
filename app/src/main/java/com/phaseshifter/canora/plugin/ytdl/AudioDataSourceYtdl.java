package com.phaseshifter.canora.plugin.ytdl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSource;
import com.phaseshifter.canora.utils.RunnableArg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AudioDataSourceYtdl implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final String url;
    private transient String streamUrl = null;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    public String getUrl() {
        return url;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public AudioDataSourceYtdl(String url) {
        this.url = url;
    }

    @Override
    public void prepare(Runnable onReady, RunnableArg<Exception> onError) {
        if (streamUrl == null) {
            pool.execute(() -> {
                try {
                    YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-f", "bestaudio");
                    VideoInfo streamInfo = ytdl.getInfo(request);
                    streamUrl = streamInfo.getUrl();
                    if (onReady != null)
                        onReady.run();
                } catch (Exception e) {
                    Log.e("StreamDownloader", e.getMessage());
                    if (onError != null)
                        onError.run(e);
                }
            });
        }
    }

    @Override
    public void finish() {
    }

    @Override
    public void failed() {
        streamUrl = null;
    }

    @Override
    public void getExoPlayerSources(Context context, RunnableArg<List<MediaSource>> onReady, RunnableArg<Exception> onException) {
        pool.execute(() -> {
            if (streamUrl == null) {
                throw new RuntimeException("Failed to retrieve stream data for track " + url);
            }
            List<MediaSource> ret = new ArrayList<>();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
            ret.add(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl))));
            onReady.run(ret);
        });
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
