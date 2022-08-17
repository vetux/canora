package com.phaseshifter.canora.plugin.ytdl;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSource;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class AudioDataSourceYtdl implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final String url;
    private transient String streamUrl = null;

    public String getUrl() {
        return url;
    }

    public AudioDataSourceYtdl(String url) {
        this.url = url;
    }

    @Override
    public void prepare() throws Exception {
        if (streamUrl == null) {
            YoutubeDL ytdl = MainApplication.instance.getYoutubeDlInstance();
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("-f", "best");
            VideoInfo streamInfo = ytdl.getInfo(request);
            streamUrl = streamInfo.getUrl();
        }
    }

    @Override
    public void finish() {
        streamUrl = null;
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
