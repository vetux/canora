package com.phaseshifter.canora.data.media.image;

import com.phaseshifter.canora.data.media.image.source.ImageDataSource;

import java.io.Serializable;
import java.util.Objects;

public final class ImageData implements Serializable {
    private static final long serialVersionUID = 1;

    private ImageMetadata metadata;
    private ImageDataSource dataSource;

    public ImageData() {
    }

    public ImageData(ImageMetadata metadata, ImageDataSource dataSource) {
        if (metadata == null
                || dataSource == null)
            throw new IllegalArgumentException();
        this.metadata = metadata;
        this.dataSource = dataSource;
    }

    public ImageMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ImageMetadata metadata) {
        this.metadata = metadata;
    }

    public ImageDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(ImageDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageData imageData = (ImageData) o;
        return Objects.equals(metadata, imageData.metadata) &&
                Objects.equals(dataSource, imageData.dataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, dataSource);
    }

    @Override
    public String toString() {
        return "ImageData{" +
                "metadata=" + metadata +
                ", dataSource=" + dataSource +
                '}';
    }
}