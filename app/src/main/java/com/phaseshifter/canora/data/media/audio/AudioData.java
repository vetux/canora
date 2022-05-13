package com.phaseshifter.canora.data.media.audio;

import com.phaseshifter.canora.data.media.MediaData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadata;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSource;

import java.util.Objects;

public final class AudioData extends MediaData {
    private static final long serialVersionUID = 1;

    private final AudioMetadata metadata;
    private final AudioDataSource dataSource;

    public AudioData(AudioMetadata metadata, AudioDataSource dataSource) {
        if (metadata == null
                || dataSource == null)
            throw new IllegalArgumentException();
        this.metadata = metadata;
        this.dataSource = dataSource;
    }

    public AudioData(AudioData copy) {
        this(copy.metadata, copy.dataSource);
    }

    public AudioMetadata getMetadata() {
        return metadata;
    }

    public AudioDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioData audioData = (AudioData) o;
        return Objects.equals(metadata, audioData.metadata) &&
                Objects.equals(dataSource, audioData.dataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, dataSource);
    }

    @Override
    public String toString() {
        return "AudioData{" +
                "metadata=" + metadata +
                ", dataSource=" + dataSource +
                '}';
    }
}