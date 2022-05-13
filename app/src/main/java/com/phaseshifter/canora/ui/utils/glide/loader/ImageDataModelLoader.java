package com.phaseshifter.canora.ui.utils.glide.loader;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

public class ImageDataModelLoader implements ModelLoader<ImageData, InputStream> {
    private final Context context;

    public ImageDataModelLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull ImageData imageData, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(imageData.getMetadata().getId()), new ImageDataDataFetcher(imageData, context));
    }

    @Override
    public boolean handles(@NonNull ImageData imageData) {
        return true;
    }
}