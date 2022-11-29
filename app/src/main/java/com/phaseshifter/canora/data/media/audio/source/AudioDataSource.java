package com.phaseshifter.canora.data.media.audio.source;

import android.content.Context;

import com.google.android.exoplayer2.source.MediaSource;

import java.util.List;

public interface AudioDataSource {
    /**
     * Prepare the data source for playback.
     * <p>
     * Called some time before getExoPlayerSource is called.
     */
    default void prepare() throws Exception {
    }

    default void finish() {
    }

    /**
     * Called when the player fails to stream from the returned media sources.
     * Network based sources can then for example refresh the url.
     */
    default void failed() {
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
    List<MediaSource> getExoPlayerSources(Context context) throws Exception;
}