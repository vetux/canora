package com.phaseshifter.canora.data.media.image;

import java.util.UUID;

public class ImageMetadata {
    public UUID id;
    public int width;
    public int height;

    public ImageMetadata() {
    }

    public ImageMetadata(UUID id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    public ImageMetadata(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}