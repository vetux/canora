package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.phaseshifter.canora.utils.RunnableArg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumCoverDataSource implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    private final String trackUri;

    private boolean imageLoaded = false;
    private byte[] imageData = null;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    public AlbumCoverDataSource(String trackUri) {
        this.trackUri = trackUri;
    }

    @Override
    public void getBitmap(Context context, RunnableArg<Bitmap> onReady, RunnableArg<Exception> onError) {
        pool.submit(() -> {
            if (!imageLoaded) {
                try{
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(context.getApplicationContext(), Uri.parse(trackUri));
                    imageData = mmr.getEmbeddedPicture();
                    imageLoaded = true;
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
            if (imageData != null) {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                } catch (Exception e) {
                    onError.run(e);
                }
                onReady.run(bitmap);
            } else {
                onReady.run(null);
            }
        });
    }

    @Override
    public InputStream getStream(Context context) throws Exception {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context.getApplicationContext(), Uri.parse(trackUri));
        imageData = mmr.getEmbeddedPicture();
        if (imageData != null){
            return new ByteArrayInputStream(imageData);
        } else {
            return null;
        }
    }
}