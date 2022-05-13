package com.phaseshifter.canora.ui.utils.glide.loader;

import android.content.Context;
import androidx.annotation.NonNull;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

public class ImageDataModelLoaderFactory implements ModelLoaderFactory<ImageData, InputStream> {
    private Context context;

    public ImageDataModelLoaderFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<ImageData, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new ImageDataModelLoader(context);
    }

    @Override
    public void teardown() {
    }
}
