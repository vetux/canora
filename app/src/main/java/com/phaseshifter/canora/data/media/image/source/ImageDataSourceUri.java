package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.*;
import java.util.Objects;

public class ImageDataSourceUri implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private Uri uri;

    public ImageDataSourceUri(Uri uri) {
        if (uri == null)
            throw new IllegalArgumentException();
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public Bitmap getBitmap(Context context) throws IOException {
        if (context == null)
            throw new IllegalArgumentException();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source s = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(s);
        } else {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        }
    }

    @Override
    public InputStream getStream(Context context) throws Exception {
        return context.getContentResolver().openInputStream(uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageDataSourceUri that = (ImageDataSourceUri) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "ImageDataSourceUri{" +
                "uri=" + uri +
                '}';
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        os.writeObject(uri.toString());
    }

    private void readObject(ObjectInputStream is) throws ClassNotFoundException, IOException {
        uri = Uri.parse((String) is.readObject());
    }
}