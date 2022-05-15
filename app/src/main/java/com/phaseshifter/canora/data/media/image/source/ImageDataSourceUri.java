package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class ImageDataSourceUri implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private Uri uri;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

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
        if (uri.getScheme().equals("https")
                || uri.getScheme().equals("http")) {

            AtomicReference<Bitmap> bitmap = new AtomicReference<>();
            Future<?> f = pool.submit(() -> {
                try {
                    URL url = new URL(uri.toString());
                    bitmap.set(BitmapFactory.decodeStream(url.openConnection().getInputStream()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            while (!f.isDone()) {
            }
            return bitmap.get();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(src);
            } else {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            }
        }
    }

    @Override
    public InputStream getStream(Context context) throws Exception {
        if (uri.getScheme().equals("https")
                || uri.getScheme().equals("http")) {
            URL url = new URL(uri.toString());
            return url.openStream();
        } else {
            return context.getContentResolver().openInputStream(uri);
        }
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