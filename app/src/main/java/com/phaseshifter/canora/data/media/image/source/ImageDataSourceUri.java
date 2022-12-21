package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.phaseshifter.canora.utils.RunnableArg;

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

    private String uriStr;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    public ImageDataSourceUri(Uri uri) {
        if (uri == null)
            throw new IllegalArgumentException();
        this.uriStr = uri.toString();
    }

    public Uri getUri() {
        return Uri.parse(uriStr);
    }

    @Override
    public void getBitmap(Context context,
                          RunnableArg<Bitmap> onReady,
                          RunnableArg<Exception> onError) {
        if (context == null)
            throw new IllegalArgumentException();
        Uri uri = getUri();
        if (uri.getScheme().equals("https")
                || uri.getScheme().equals("http")) {
            pool.submit(() -> {
                Bitmap bitmap = null;
                try {
                    URL url = new URL(uri.toString());
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                    onError.run(e);
                }
                onReady.run(bitmap);
            });
        } else {
            pool.submit(() ->  {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(), uri);
                    return ImageDecoder.decodeBitmap(src);
                } else {
                    return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                }
            });
        }
    }

    @Override
    public InputStream getStream(Context context) throws Exception {
        Uri uri = getUri();
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
        return uriStr.equals(that.uriStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriStr);
    }

    @Override
    public String toString() {
        return "ImageDataSourceUri{" +
                "uri=" + uriStr +
                '}';
    }
}