package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;

import com.google.android.exoplayer2.source.MediaSource;

public interface AudioDataSource {
    /**
     * Prepare the data source for playback.
     * <p>
     * Called some time before getExoPlayerSource is called.
     */
    default void prepare() {
    }

    /**
     * Creates a new MediaSource object based on the underlying data source.
     * The passed context could be abstracted into the AudioDataSource implementations,
     * but for this the AudioDataSource objects would have to be constructed with a persistent context (Separate "MediaBrowserService" or application),
     * and also the context would have to be restored when deserializing.
     *
     * @param context The context to use when creating the MediaSource object.
     * @return The created MediaSource object, non null.
     * @throws Exception If the MediaSource object could not be instantiated.
     */
    MediaSource getExoPlayerSource(Context context) throws Exception;
}