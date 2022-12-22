package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.phaseshifter.canora.utils.RunnableArg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDataSourceFile implements AudioDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final File file;

    public AudioDataSourceFile(File file) {
        this.file = file;
    }

    public java.io.File getFile() {
        return file;
    }

    @Override
    public void getExoPlayerSources(Context context, RunnableArg<List<MediaSource>> onReady, RunnableArg<Exception> onException) {
        String targetFilePath = file.getAbsolutePath();
        File targetFile = new File(targetFilePath);
        if (!targetFile.exists()) {
            onException.run( new FileNotFoundException("File not found: " + targetFile.getAbsolutePath()));;
        } else {
            DataSource.Factory factory = new DataSource.Factory() {
                @Override
                public DataSource createDataSource() {
                    return new FileDataSource();
                }
            };
            List<MediaSource> ret = new ArrayList<>();
            ret.add(new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(Uri.fromFile(targetFile))));
            onReady.run(ret);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        AudioDataSourceFile audioFile = (AudioDataSourceFile) o;
        return Objects.equals(file, audioFile.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), file);
    }

    @Override
    public String toString() {
        return "AudioDataSourceFile{" +
                "file=" + file +
                '}';
    }
}