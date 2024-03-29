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
import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;
import com.phaseshifter.canora.utils.RunnableArg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDataSourceYtdl implements PlayerDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final String url;
    private transient String streamUrl = null;

    private static final ExecutorService pool = Executors.newCachedThreadPool();

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
    public void failed() {
        streamUrl = null;
    }

    @Override
    public void getExoPlayerSources(Context context, RunnableArg<List<MediaSource>> onReady, RunnableArg<Exception> onException) {
        pool.execute(() -> {
            if (streamUrl == null) {
                try {
                    YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-f", "best");
                    VideoInfo streamInfo = ytdl.getInfo(request);
                    streamUrl = streamInfo.getUrl();
                } catch (Exception e) {
                    if (onException != null)
                        onException.run(e);
                }
            }
            if (streamUrl == null) {
                if (onException != null)
                    onException.run(new RuntimeException("Failed to retrieve stream data for track " + url));
            } else {
                List<MediaSource> ret = new ArrayList<>();
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
                ret.add(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl))));
                onReady.run(ret);
            }
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
