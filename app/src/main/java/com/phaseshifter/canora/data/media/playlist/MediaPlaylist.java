package com.phaseshifter.canora.data.media.playlist;

import com.phaseshifter.canora.data.media.MediaData;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadata;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public abstract class MediaPlaylist implements Serializable {
    private static final long serialVersionUID = 1;

    protected final PlaylistMetadata metadata;
    protected final List<? extends MediaData> data;

    public MediaPlaylist(PlaylistMetadata metadata, List<? extends MediaData> dataSource) {
        this.metadata = metadata;
        this.data = dataSource;
    }

    public PlaylistMetadata getMetadata() {
        return metadata;
    }

    public List<? extends MediaData> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaPlaylist that = (MediaPlaylist) o;
        return Objects.equals(metadata, that.metadata) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, data);
    }
}