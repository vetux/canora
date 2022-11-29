package com.phaseshifter.canora.service.player.mediasession;

import android.content.Intent;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.phaseshifter.canora.service.player.ExoPlayerService;

public class MediaSessionCallback extends MediaSession.Callback {
    private final ExoPlayerService service;

    public MediaSessionCallback(ExoPlayerService service) {
        super();
        this.service = service;
    }

    @Override
    public void onCommand(@NonNull String command, @Nullable Bundle args, @Nullable ResultReceiver cb) {
        super.onCommand(command, args, cb);
    }

    @Override
    public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
        return super.onMediaButtonEvent(mediaButtonIntent);
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
    }

    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        super.onPrepareFromMediaId(mediaId, extras);
    }

    @Override
    public void onPrepareFromSearch(String query, Bundle extras) {
        super.onPrepareFromSearch(query, extras);
    }

    @Override
    public void onPrepareFromUri(Uri uri, Bundle extras) {
        super.onPrepareFromUri(uri, extras);
    }

    @Override
    public void onPlay() {
        service.resume();
    }

    @Override
    public void onPlayFromSearch(String query, Bundle extras) {
        super.onPlayFromSearch(query, extras);
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
    }

    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
        super.onPlayFromUri(uri, extras);
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
    }

    @Override
    public void onPause() {
        service.pause();
    }

    @Override
    public void onSkipToNext() {
        service.next();
    }

    @Override
    public void onSkipToPrevious() {
        service.previous();
    }

    @Override
    public void onFastForward() {
        super.onFastForward();
    }

    @Override
    public void onRewind() {
        super.onRewind();
    }

    @Override
    public void onStop() {
        service.stop();
    }

    @Override
    public void onSeekTo(long pos) {
        service.seek(pos);
    }

    @Override
    public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
        super.onCustomAction(action, extras);
    }
}