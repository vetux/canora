package com.phaseshifter.canora.data.media.image.source;

import android.content.Context;
import android.graphics.Bitmap;

import com.phaseshifter.canora.utils.RunnableArg;

import java.io.InputStream;

public interface ImageDataSource {
    void getBitmap(Context context,
                   RunnableArg<Bitmap> onReady,
                   RunnableArg<Exception> onError);

    /**
     * Get the stream pointing to the image data,
     * May block the calling thread
     *
     * @param context
     * @return
     * @throws Exception
     */
    InputStream getStream(Context context) throws Exception;

    boolean equals(Object other);
}