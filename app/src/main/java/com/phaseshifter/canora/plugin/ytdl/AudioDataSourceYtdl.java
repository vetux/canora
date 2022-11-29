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
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class AudioDataSourceYtdl implements AudioDataSource, Serializable {
    private static class StreamDownloader {
        public static void prepare(String url) {
            if (!streamLatches.containsKey(url)) {
                CountDownLatch latch = new CountDownLatch(1);
                pool.submit(() -> {
                    try {
                        YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
                        YoutubeDLRequest request = new YoutubeDLRequest(url);
                        request.addOption("-f", "bestaudio");
                        VideoInfo streamInfo = ytdl.getInfo(request);
                        String streamUrl = streamInfo.getUrl();
                        streamUrls.put(url, streamUrl);
                    } catch (Exception e) {
                        Log.e("StreamDownloader", e.getMessage());
                    }
                    latch.countDown();
                });
                streamLatches.put(url, latch);
            }
        }

        public static String get(String url) {
            if (!streamUrls.containsKey(url)) {
                CountDownLatch latch = streamLatches.get(url);
                while (latch.getCount() != 0) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Log.e("StreamDownloader", e.getMessage());
                    }
                }
            }
            return streamUrls.get(url);
        }

        public static void failed(String url) {
            if (streamLatches.containsKey(url)){
                CountDownLatch latch = streamLatches.get(url);
                while (latch.getCount() != 0) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Log.e("StreamDownloader", e.getMessage());
                    }
                }
            }
            streamUrls.remove(url);
            streamLatches.remove(url);
            prepare(url);
        }

        private static final HashMap<String, String> streamUrls = new HashMap<>();
        private static final HashMap<String, CountDownLatch> streamLatches = new HashMap<>();
        private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    private static final long serialVersionUID = 1;

    private final String url;
    private transient String streamUrl = null;

    public String getUrl() {
        return url;
    }

    public AudioDataSourceYtdl(String url) {
        this.url = url;
        StreamDownloader.prepare(url);
    }

    @Override
    public void prepare() throws Exception {
        if (streamUrl == null) {
            streamUrl = StreamDownloader.get(url);
        }
    }

    @Override
    public void finish() {}

    @Override
    public void failed() {
        StreamDownloader.failed(url);
    }

    @Override
    public List<MediaSource> getExoPlayerSources(Context context) {
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
