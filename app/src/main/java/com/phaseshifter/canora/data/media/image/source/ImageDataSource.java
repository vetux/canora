package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.InputStream;

public interface ImageDataSource {
    Bitmap getBitmap(Context context) throws Exception;

    InputStream getStream(Context context) throws Exception;
}