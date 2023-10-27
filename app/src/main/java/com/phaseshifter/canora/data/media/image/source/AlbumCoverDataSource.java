package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.utils.RunnableArg;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumCoverDataSource implements ImageDataSource, Serializable {
    private static final long serialVersionUID = 1;

    public final String trackUri;

    private transient boolean imageLoaded = false;
    private transient byte[] imageData = null;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();

    public AlbumCoverDataSource(String trackUri) {
        this.trackUri = trackUri;
    }

    @Override
    public void getBitmap(Context context, RunnableArg<Bitmap> onReady, RunnableArg<Exception> onError) {
        pool.submit(() -> {
            if (!imageLoaded) {
                imageLoaded = true;
                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(context.getApplicationContext(), Uri.parse(trackUri));
                    imageData = mmr.getEmbeddedPicture();
                } catch (Exception e) {
                    onError.run(e);
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
                onError.run(new RuntimeException("No embedded image found"));
            }
        });
    }

    @Override
    public InputStream getStream(Context context) throws Exception {
        if (!imageLoaded) {
            imageLoaded = true;
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context.getApplicationContext(), Uri.parse(trackUri));
            imageData = mmr.getEmbeddedPicture();
        }

        if (imageData == null) {
            throw new RuntimeException(("No embedded image found"));
        }

        return new ByteArrayInputStream(imageData);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AlbumCoverDataSource data = (AlbumCoverDataSource) obj;
        return Objects.equals(trackUri, data.trackUri);
    }
}
