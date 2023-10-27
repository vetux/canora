package com.phaseshifter.canora.data.media.player;

import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;

import java.io.Serializable;
import java.util.Objects;

public final class PlayerData implements Serializable {
    private static final long serialVersionUID = 1;

    private PlayerMetadata metadata;
    private PlayerDataSource dataSource;

    public PlayerData() {
    }

    public PlayerData(PlayerMetadata metadata, PlayerDataSource dataSource) {
        if (metadata == null
                || dataSource == null)
            throw new IllegalArgumentException();
        this.metadata = metadata;
        this.dataSource = dataSource;
    }

    public PlayerData(PlayerData copy) {
        this(copy.metadata, copy.dataSource);
    }

    public PlayerMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(PlayerMetadata metadata) {
        this.metadata = metadata;
    }

    public PlayerDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(PlayerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerData audioData = (PlayerData) o;
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