package com.phaseshifter.canora.ui.utils.glide;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.ui.utils.glide.loader.ImageDataModelLoaderFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

@GlideModule
public class MainGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, GlideBuilder builder) {
        builder.setLogLevel(Log.ERROR);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(ImageData.class, InputStream.class, new ImageDataModelLoaderFactory(context));
        super.registerComponents(context, glide, registry);
    }
}