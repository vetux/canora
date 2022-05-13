package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;

public class ImageDataSourceByteArray implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final byte[] imageData;

    public ImageDataSourceByteArray(byte[] imageData) {
        if (imageData == null)
            throw new IllegalArgumentException();
        this.imageData = imageData;
    }

    public byte[] getImageData() {
        return imageData;
    }

    @Override
    public Bitmap getBitmap(Context context) {
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
    }

    @Override
    public InputStream getStream(Context context) {
        return new ByteArrayInputStream(imageData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageDataSourceByteArray that = (ImageDataSourceByteArray) o;
        return Arrays.equals(imageData, that.imageData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(imageData);
    }
}