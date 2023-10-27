package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.phaseshifter.canora.utils.RunnableArg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDataSourceByteArray implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    public final byte[] imageData;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    public ImageDataSourceByteArray(byte[] imageData) {
        if (imageData == null)
            throw new IllegalArgumentException();
        this.imageData = imageData;
    }

    public byte[] getImageData() {
        return imageData;
    }

    @Override
    public void getBitmap(Context context,
                            RunnableArg<Bitmap> onReady,
                            RunnableArg<Exception> onError) {
        pool.submit(() ->  {
            Bitmap bitmap = null;
            try{
                bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            } catch(Exception e){
                onError.run(e);
            }
            onReady.run(bitmap);
        });
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