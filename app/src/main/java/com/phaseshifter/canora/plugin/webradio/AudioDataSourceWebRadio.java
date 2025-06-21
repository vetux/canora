package com.phaseshifter.canora.plugin.webradio;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCConnectionException;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2StreamProtocol;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2TrackStreamData;
import com.phaseshifter.canora.utils.Pair;
import com.phaseshifter.canora.utils.RunnableArg;

import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.sfuhrm.radiobrowser4j.Station;

public class AudioDataSourceWebRadio implements PlayerDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private boolean hls = false;
    private String url = "";

    public AudioDataSourceWebRadio(Station station) {
        this.hls = Objects.equals(station.getHls(), "1");
        this.url = station.getUrl();
    }

    public AudioDataSourceWebRadio(String url, boolean hls){
        this.hls = hls;
        this.url = url;
    }

    public boolean getHLS() {
        return hls;
    }

    public String getURL() {
        return url;
    }

    @Override
    public boolean isStream() {
        return true;
    }

    @Override
    public void getExoPlayerSources(Context context, RunnableArg<List<MediaSource>> onReady, RunnableArg<Exception> onException) {
        List<MediaSource> ret = new ArrayList<>();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
        if (hls) {
            ret.add(new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(url))));
        } else {
            ret.add(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(url))));
        }
        onReady.run(ret);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceWebRadio that = (AudioDataSourceWebRadio) o;
        return Objects.equals(hls, that.hls) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hls, url);
    }

    @Override
    public String toString() {
        return "AudioDataSourceWebRadio{" +
                "hls=" + hls + " url=" + url +
                '}';
    }
}
