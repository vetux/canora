package com.phaseshifter.canora.data.media.audio.source;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDataSource;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@TargetApi(23)
public class AudioDataSourceMediaDataSource implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final MediaDataSource mediaDataSource;

    public AudioDataSourceMediaDataSource(MediaDataSource mediaDataSource) {
        this.mediaDataSource = mediaDataSource;
    }

    public MediaDataSource getMediaDataSource() {
        return mediaDataSource;
    }

    @Override
    public List<MediaSource> getExoPlayerSources(Context context) throws Exception {
        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new DataSource() {
                    @Override
                    public void addTransferListener(TransferListener transferListener) {

                    }

                    @Override
                    public long open(DataSpec dataSpec) throws IOException {
                        return 0;
                    }

                    @Override
                    public int read(byte[] buffer, int offset, int readLength) throws IOException {
                        return mediaDataSource.readAt(0, buffer, offset, readLength);
                    }

                    @Nullable
                    @Override
                    public Uri getUri() {
                        return null;
                    }

                    @Override
                    public void close() throws IOException {
                        mediaDataSource.close();
                    }
                };
            }
        };
        List<MediaSource> ret = new ArrayList<>();
        ret.add(new ProgressiveMediaSource.Factory(factory).createMediaSource(null));
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDataSourceMediaDataSource that = (AudioDataSourceMediaDataSource) o;
        return Objects.equals(mediaDataSource, that.mediaDataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaDataSource);
    }

    @Override
    public String toString() {
        return "AudioDataSourceMediaDataSource{" +
                "mediaDataSource=" + mediaDataSource +
                '}';
    }
}