package com.phaseshifter.canora.data.media.image.metadata;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ImageMetadataMemory implements ImageMetadata, Serializable {
    private static final long serialVersionUID = 1;

    private final UUID id;

    public ImageMetadataMemory(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageMetadataMemory that = (ImageMetadataMemory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ImageMetadataSimple{" +
                "id=" + id +
                '}';
    }
}