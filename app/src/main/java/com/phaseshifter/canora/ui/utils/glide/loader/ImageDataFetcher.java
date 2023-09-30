package com.phaseshifter.canora.ui.utils.glide.loader;

import android.content.Context;
import androidx.annotation.NonNull;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

public class ImageDataFetcher implements DataFetcher<InputStream> {
    private final ImageData imageData;
    private final Context context;

    private InputStream stream;

    public ImageDataFetcher(ImageData imageData, Context context) {
        this.imageData = imageData;
        this.context = context;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            stream = imageData.getDataSource().getStream(context);
            if (stream == null){
                callback.onLoadFailed(new RuntimeException("No image data found"));
            } else {
                callback.onDataReady(stream);
            }
        } catch (Exception e) {
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
