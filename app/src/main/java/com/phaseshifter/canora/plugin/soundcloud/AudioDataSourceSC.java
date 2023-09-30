package com.phaseshifter.canora.plugin.soundcloud;

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

public class AudioDataSourceSC implements PlayerDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private static SCV2Client client;

    private final List<SCV2Track.MediaTranscoding> codings = new ArrayList<>();
    private transient List<Pair<SCV2StreamProtocol, String>> streams = null;

    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final Object mutex = new Object();

    public String getUrls() {
        StringBuilder ret = new StringBuilder();
        for (SCV2Track.MediaTranscoding coding : codings) {
            ret.append(coding.getUrl());
            ret.append(", ");
        }
        return ret.toString();
    }

    private SCV2Client getClient() {
        synchronized (mutex){
            if (client == null) {
                client = new SCV2Client();
                updateClientId();
            }
            return client;
        }
    }

    private void updateClientId() {
        try {
            client.setClientID(client.getNewClientID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStreams() throws SCConnectionException, SCParsingException, JSONException, IOException {
        SCV2Client client = getClient();
        getStreams().clear();
        for (SCV2TrackStreamData data : client.getTemporaryStreamUrls(codings)) {
            getStreams().add(new Pair<>(data.protocol, data.url));
        }
    }

    private List<Pair<SCV2StreamProtocol, String>> getStreams() {
        // Transient members are initialized to nada and readObject is not called for magical reason so we nullcheck.
        if (streams == null) {
            streams = new ArrayList<>();
        }
        return streams;
    }

    public AudioDataSourceSC(List<SCV2Track.MediaTranscoding> codings) {
        this.codings.addAll(codings);
    }

    @Override
    public void getExoPlayerSources(Context context, RunnableArg<List<MediaSource>> onReady, RunnableArg<Exception> onException) {
        pool.execute(() -> {
            if (getStreams().isEmpty()) {
                try {
                    loadStreams();
                } catch (Exception e) {
                    e.printStackTrace();
                    updateClientId();
                    try {
                        loadStreams();
                    } catch (Exception ex) {
                        getStreams().clear();
                        ex.printStackTrace();
                        onException.run(ex);
                    }
                }
            }
            if (getStreams().isEmpty()) {
                onException.run(new RuntimeException("Failed to retrieve stream data for track " + codings));
            } else {
                List<MediaSource> ret = new ArrayList<>();
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, "clank");
                for (Pair<SCV2StreamProtocol, String> stream : getStreams()) {
                    switch (stream.first) {
                        case HLS:
                            ret.add(new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(stream.second))));
                            break;
                        case PROGRESSIVE:
                            ret.add(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.parse(stream.second))));
                            break;
                    }
                }
                if (ret.isEmpty()){
                    onException.run(new RuntimeException("Failed to retrieve stream data for track " + codings));
                } else {
                    onReady.run(ret);
                }
            }
        });
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
