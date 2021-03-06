package com.phaseshifter.canora.service;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.*;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.PlaybackException;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.service.mediasession.MediaSessionCallback;
import com.phaseshifter.canora.service.playback.PlaybackController;
import com.phaseshifter.canora.service.playback.SimplePlaybackController;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.activities.MainActivity;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.android.bitmap.BitmapUtils;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//TODO:Testing: Create Unit Tests for Service.
//TODO: Implement volume fade
public class ExoPlayerService extends Service implements MediaPlayerService, AudioManager.OnAudioFocusChangeListener {
    private static final String packagenameBroadcasts = "com.phaseshifter.canora.mediaserv";

    //START Broadcast interface
    //IN
    public static final String COMMAND_PLAYBACK_TOGGLE = packagenameBroadcasts + ".TOGGLE";
    public static final String COMMAND_PLAYBACK_RESUME = packagenameBroadcasts + ".RESUME";
    public static final String COMMAND_PLAYBACK_PAUSE = packagenameBroadcasts + ".PAUSE";
    public static final String COMMAND_PLAYBACK_STOP = packagenameBroadcasts + ".STOP";
    public static final String COMMAND_NEXT = packagenameBroadcasts + ".NEXT";
    public static final String COMMAND_PREV = packagenameBroadcasts + ".PREV";
    public static final String COMMAND_QUIT = packagenameBroadcasts + ".QUIT";
    //OUT
    public static final String STATUS_PLAYBACK_UPDATE = packagenameBroadcasts + ".PLAYBACKUPDATE";
    public static final String STATUS_PLAYBACK_ERROR = packagenameBroadcasts + ".PLAYBACKERROR";
    public static final String STATUS_EXIT = packagenameBroadcasts + ".EXIT";
    //STOP Broadcast interface

    private static final int NOTIFICATION_ID = 71829;
    private static final String NOTIFICATION_CHANNEL_ID = "CANORA_0_NOSOUND";
    private static final String NOTIFICATION_CHANNEL_NAME = "Canora Notifications";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Canora Notification Channel";

    private final String LOG_TAG = "MediaPlayerService";

    private final Observable<PlayerState> state = new Observable<>();

    private Handler mainThread;

    private ExoPlayer exoPlayer;

    private BroadcastReceiver brcv;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    private PlaybackController playbackController;

    private float volume = 0.5f;

    private Boolean isForeground = false; //Should be implemented by android.app.Service class, but it is not.

    private boolean focusGainWait = false;

    private final List<MediaSource> trackSources = new ArrayList<>();
    private int trackSourceIndex;

    private ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    //Binder
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ExoPlayerService getService() {
            return ExoPlayerService.this;
        }
    }

    //Lifecycle
    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "onCreate");
        playbackController = new SimplePlaybackController();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(this)
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            } else {
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
        mediaSession = new MediaSession(this, "CanoraMS");
        mediaSession.setCallback(new MediaSessionCallback(this));
        mediaSession.setActive(true);
        mainThread = new Handler(Looper.getMainLooper());
        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.v(LOG_TAG, "onPlayerStateChanged " + playWhenReady + " " + playbackState);
                if (playWhenReady
                        && playbackState == ExoPlayer.STATE_ENDED) {
                    if (playbackController.getRepeat()) {
                        exoPlayer.seekTo(0);
                        exoPlayer.setPlayWhenReady(true);
                    } else {
                        next();
                    }
                }
                onStateModified();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.v(LOG_TAG, "onIsPlayingChanged " + isPlaying);
                onStateModified();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.v(LOG_TAG, "onPlayerError " + error);
                error.printStackTrace();
                broadcastError();
                onStateModified();
                if (!trackSources.isEmpty()) {
                    trackSourceIndex++;
                    if (trackSourceIndex >= 0 && trackSourceIndex < trackSources.size()) {
                        updateTrackSource();
                    } else {
                        next();
                    }
                }
            }
        });
        exoPlayer.setVolume(volume);
        onStateModified();
        registerBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");
        try {
            exoPlayer.stop(true);
            exoPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        mediaSession.setActive(false);
        mediaSession.release();
        try {
            if (brcv != null) {
                unregisterReceiver(brcv);
                brcv = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            clearNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(false);
        stopSelf();
    }

    //Public Control Functions

    @Override
    public void shutdown() {
        stop();
        stopForeground(false);
        stopSelf();
    }

    @Override
    public void setContent(List<AudioData> pl) {
        Log.v(LOG_TAG, "setContent " + pl);
        if (playbackController.getContent() != null && playbackController.getContent().equals(pl)) {
            Log.v(LOG_TAG, "Content Identical, returning...");
        } else {
            Log.v(LOG_TAG, "Setting Content: " + pl.hashCode() + " " + pl.size() + " tracks");
            if (!playbackController.setContent(pl)) {
                Log.v(LOG_TAG, "Current Track not found in new dataset. Stopping player...");
                exoPlayer.stop(true);
            }
            onStateModified();
        }
    }

    @Override
    public void play(UUID id) {
        Log.v(LOG_TAG, "play " + id);
        AudioData n = playbackController.setNext(id);
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Track: " + n.getMetadata().getTitle());
            createPlayer(n);
        } else {
            Log.e(LOG_TAG, "Null track");
        }
    }

    @Override
    public void next() {
        Log.v(LOG_TAG, "next");
        AudioData n = playbackController.getNext();
        playbackController.peekNext().getDataSource().prepare();
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Track: " + n.getMetadata().getTitle());
            createPlayer(n);
        } else {
            Log.e(LOG_TAG, "Null track");
        }
    }

    @Override
    public void previous() {
        Log.v(LOG_TAG, "previous");
        AudioData n = playbackController.getPrev();
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Track: " + n.getMetadata().getTitle());
            createPlayer(n);
        } else {
            Log.e(LOG_TAG, "Null track");
        }
    }

    @Override
    public void pauseResume() {
        Log.v(LOG_TAG, "pauseResume");
        if (exoPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    @Override
    public void pause() {
        Log.v(LOG_TAG, "pause");
        exoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void resume() {
        Log.v(LOG_TAG, "resume");
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void seek(float percentage) {
        if (playbackController.getCurrentTrack() != null) {
            long ms = (long) (playbackController.getCurrentTrack().getMetadata().getLength() * percentage);
            seek(ms);
        }
    }

    @Override
    public void seek(long ms) {
        Log.v(LOG_TAG, "seek " + ms + " / " + exoPlayer.getDuration());
        if (ms <= exoPlayer.getDuration()) {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.seekTo(ms);
        }
    }

    @Override
    public void stop() {
        Log.v(LOG_TAG, "stop");
        exoPlayer.stop();
    }

    @Override
    public void switchShuffle() {
        playbackController.setShuffle(!playbackController.getShuffle());
    }

    @Override
    public void setShuffle(boolean shuffle) {
        Log.v(LOG_TAG, "setShuffle " + shuffle);
        playbackController.setShuffle(shuffle);
        onStateModified();
    }

    @Override
    public void switchRepeat() {
        playbackController.setRepeat(!playbackController.getRepeat());
    }

    @Override
    public void setRepeat(boolean repeat) {
        Log.v(LOG_TAG, "setRepeat " + repeat);
        playbackController.setRepeat(repeat);
        onStateModified();
    }

    @Override
    public void setVolume(float vol) {
        if (vol <= 1) {
            Log.v(LOG_TAG, "setVolume " + vol);
            volume = vol;
            exoPlayer.setVolume(vol);
            onStateModified();
        } else {
            throw new IllegalArgumentException("Invalid volume: " + vol);
        }
    }

    @Override
    public Observable<PlayerState> getState() {
        return state;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.v(LOG_TAG, "onAudioFocusChange " + focusChange);
        if (focusChange <= 0) {
            if (state.get().isPlaying()) {
                focusGainWait = true;
                pause();
            }
        } else {
            if (focusGainWait) {
                focusGainWait = false;
                resume();
            }
        }
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() != Looper.getMainLooper())
            mainThread.post(runnable);
        else
            runnable.run();
    }

    private void broadcastPlayerStatus() {
        runOnMainThread(() -> {
            Intent intent = new Intent(STATUS_PLAYBACK_UPDATE);
            sendBroadcast(intent);
        });
    }

    private void broadcastError() {
        runOnMainThread(() -> {
            Intent intent = new Intent(STATUS_PLAYBACK_ERROR);
            sendBroadcast(intent);
        });
    }

    private void onStateModified() {
        PlayerState newState = new PlayerState(playbackController, exoPlayer, volume);
        runOnMainThread(() -> {
            boolean changed = !Objects.equals(this.state.get(), newState);
            this.state.set(newState);
            if (changed) {
                updateMediaSession(newState);
                updateNotification(newState);
                broadcastPlayerStatus();
            }
        });
    }

    private void updateNotification(PlayerState state) {
        pool.submit(() -> {
            AudioData track = state.getCurrentTrack();
            Bitmap artworkBitmap = null;
            if (track != null) {
                try {
                    if (track.getMetadata().getArtwork() != null)
                        artworkBitmap = track.getMetadata().getArtwork().getDataSource().getBitmap(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (artworkBitmap == null)
                    artworkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.artwork_unset);
            } else {
                artworkBitmap = BitmapUtils.getBitmapForResource(this, R.drawable.artwork_unset);
            }
            boolean playing = state.isPlaying();

            final Bitmap artwork = artworkBitmap;

            runOnMainThread(() -> {
                final Notification.Builder notificationBuilder;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .addAction(new Notification.Action.Builder(
                                    Icon.createWithResource(this, R.drawable.skip_previous),
                                    "prev", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PREV), PendingIntent.FLAG_IMMUTABLE)
                            ).build());
                    if (playing) {
                        notificationBuilder.addAction(new Notification.Action.Builder(
                                Icon.createWithResource(this, R.drawable.pause),
                                "pause", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PLAYBACK_PAUSE), PendingIntent.FLAG_IMMUTABLE))
                                .build());
                    } else {
                        notificationBuilder.addAction(new Notification.Action.Builder(
                                Icon.createWithResource(this, R.drawable.play_arrow),
                                "play", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PLAYBACK_RESUME), PendingIntent.FLAG_IMMUTABLE))
                                .build());
                    }
                    notificationBuilder.addAction(new Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.skip_next),
                            "next", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_NEXT), PendingIntent.FLAG_IMMUTABLE))
                            .build());
                } else {
                    notificationBuilder = new Notification.Builder(this)
                            .addAction(R.drawable.skip_previous, "prev", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PREV), PendingIntent.FLAG_IMMUTABLE));
                    if (playing) {
                        notificationBuilder.addAction(R.drawable.pause, "pause", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PLAYBACK_PAUSE), PendingIntent.FLAG_IMMUTABLE));
                    } else {
                        notificationBuilder.addAction(R.drawable.play_arrow, "play", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_PLAYBACK_RESUME), PendingIntent.FLAG_IMMUTABLE));
                    }
                    notificationBuilder.addAction(R.drawable.skip_next, "next", PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_NEXT), PendingIntent.FLAG_IMMUTABLE));
                }

                notificationBuilder.setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));
                if (track != null) {
                    notificationBuilder
                            .setContentTitle(track.getMetadata().getTitle())
                            .setContentText(track.getMetadata().getArtist());
                } else {
                    notificationBuilder
                            .setContentTitle("")
                            .setContentText("");
                }

                if (artwork != null) {
                    notificationBuilder.setLargeIcon(artwork);
                }

                notificationBuilder.setSmallIcon(R.drawable.notification_smallicon);

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.setAction("android.intent.action.MAIN");
                resultIntent.addCategory("android.intent.category.LAUNCHER");
                notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                notificationBuilder.setDeleteIntent(PendingIntent.getBroadcast(this, 0, new Intent(COMMAND_QUIT), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                Notification notification = notificationBuilder.build();


                NotificationManager nfm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nfm != null) {
                    nfm.notify(NOTIFICATION_ID, notification);
                }
                if (playing) {
                    Log.v(LOG_TAG, "Start Foreground");
                    if (!isForeground)
                        startForeground(NOTIFICATION_ID, notification);
                    isForeground = true;
                } else {
                    Log.v(LOG_TAG, "Stop Foreground");
                    if (isForeground)
                        stopForeground(false);
                    isForeground = false;
                }
            });
        });
    }

    private void updateMediaSession(PlayerState state) {
        mediaSession.setMetadata(getMediaSessionMetadata(state.getCurrentTrack()));
        mediaSession.setPlaybackState(getMediaSessionState(state.isPlaying(), state.getPlayerPosition()));
    }

    private PlaybackState getMediaSessionState(boolean playing, long pos) {
        return new PlaybackState.Builder()
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, pos, 1f, System.currentTimeMillis())
                .setActions(PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_PLAY_PAUSE)
                .build();
    }

    private MediaMetadata getMediaSessionMetadata(AudioData track) {
        String title = null;
        String artist = null;
        Bitmap artwork = null;
        if (track != null) {
            title = track.getMetadata().getTitle();
            artist = track.getMetadata().getArtist();
            try {
                if (track.getMetadata().getArtwork() != null)
                    artwork = track.getMetadata().getArtwork().getDataSource().getBitmap(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Load default artwork into media session?
        /*if (artwork == null)
            artwork = BitmapFactory.decodeResource(getResources(), R.drawable.artwork_unset);*/
        MediaMetadata.Builder b = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist);
        if (artwork != null) {
            b.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork);
        }
        return b.build();
    }

    private void clearNotification() {
        NotificationManager nfm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nfm != null)
            nfm.cancel(NOTIFICATION_ID);
    }

    private void registerBroadcastReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null)
                    return;
                Log.v(LOG_TAG, "onReceive " + intent);
                switch (intent.getAction()) {
                    case COMMAND_QUIT:
                        sendBroadcast(new Intent(STATUS_EXIT));
                        stopForeground(false);
                        stopSelf();
                        break;
                    case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    case COMMAND_PLAYBACK_PAUSE:
                        pause();
                        break;
                    case COMMAND_PLAYBACK_TOGGLE:
                        pauseResume();
                        break;
                    case COMMAND_PLAYBACK_RESUME:
                        resume();
                        break;
                    case COMMAND_PLAYBACK_STOP:
                        stop();
                        break;
                    case COMMAND_NEXT:
                        next();
                        break;
                    case COMMAND_PREV:
                        previous();
                        break;
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction(COMMAND_PLAYBACK_TOGGLE);
        flt.addAction(COMMAND_PLAYBACK_PAUSE);
        flt.addAction(COMMAND_PLAYBACK_RESUME);
        flt.addAction(COMMAND_NEXT);
        flt.addAction(COMMAND_PREV);
        flt.addAction(COMMAND_QUIT);
        flt.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(brcv, flt);
    }

    private boolean createPlayer(AudioData file) {
        file.getDataSource().prepare();

        exoPlayer.stop(true);
        exoPlayer.setPlayWhenReady(true);

        try {
            trackSources.clear();
            trackSources.addAll(file.getDataSource().getExoPlayerSources(this));
        } catch (Exception e) {
            trackSources.clear();
            Log.e(LOG_TAG, "Failed to retrieve MediaSources");
            e.printStackTrace();
            return false;
        }

        trackSourceIndex = 0;

        Log.v(LOG_TAG, "Prepared MediaSources for track " + file.getMetadata().getTitle());

        updateTrackSource();

        return true;
    }

    private void updateTrackSource() {
        Log.v(LOG_TAG, "Updating MediaSource Current: " + trackSourceIndex + " Sources: " + trackSources);
        exoPlayer.stop(true);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare(trackSources.get(trackSourceIndex));
    }
}