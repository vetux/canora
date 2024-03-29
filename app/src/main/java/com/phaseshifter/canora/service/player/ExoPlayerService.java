package com.phaseshifter.canora.service.player;

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
import android.media.audiofx.Equalizer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.*;
import android.util.Log;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Tracks;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.service.player.mediasession.MediaSessionCallback;
import com.phaseshifter.canora.service.player.playback.PlaybackController;
import com.phaseshifter.canora.service.player.playback.DefaultPlaybackController;
import com.phaseshifter.canora.service.player.state.PlayerState;
import com.phaseshifter.canora.ui.activities.MainActivity;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.android.bitmap.BitmapUtils;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: Implement volume fade
//TODO: Replace ExoPlayer dependency because ExoPlayer (v2.18.0) segfaults randomly while playing from local device (Update to v2.19.1 might have fixed this.)
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
    private static final String NOTIFICATION_CHANNEL_NAME = "Canora Player";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Show Player Notification with transport controls";

    private static final long PREV_THRESHOLD = 3000; // When using previous() if current position is larger than threshold the track is sought to the start instead of playing the previous track.

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

    private PlayerData playingTrack = null;

    private ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    private Equalizer equalizer;
    private int equalizerPreset = -1;

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
        playbackController = new DefaultPlaybackController();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        }

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(channel);

        mediaSession = new MediaSession(this, "CanoraMediaSession");
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
                onStateModified(state.get().isPlaying());
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.v(LOG_TAG, "onIsPlayingChanged " + isPlaying);
                onStateModified(state.get().isPlaying());
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.v(LOG_TAG, "onPlayerError " + error);
                if (playingTrack != null) {
                    playingTrack.getDataSource().failed();
                }
                error.printStackTrace();
                broadcastError();
                onStateModified(state.get().isPlaying());

                LoadTask task = currentTask;

                // Wait for current loading task to finish
                if (task != null) {
                    while (task.latch.getCount() > 0) {
                        try {
                            task.latch.await();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Attempt to use a different source for the given track
                trackSourceIndex++;
                if (trackSourceIndex >= trackSources.size()) {
                    next();
                } else {
                    updateTrackSource();
                }
            }

            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                boolean equalizerEnabled = equalizerPreset >= 0;
                equalizer = new Equalizer(100, audioSessionId);
                equalizer.setEnabled(equalizerEnabled);
                equalizer.usePreset((short) equalizerPreset);
            }
        });
        exoPlayer.setVolume(volume);
        onStateModified(false);
        registerBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");
        try {
            exoPlayer.stop();
            exoPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        audioManager.abandonAudioFocusRequest(focusRequest);

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
    public void setContent(List<PlayerData> pl) {
        Log.v(LOG_TAG, "setContent " + pl);
        if (playbackController.getContent() != null && playbackController.getContent().equals(pl)) {
            Log.v(LOG_TAG, "Content Identical, returning...");
        } else {
            Log.v(LOG_TAG, "Setting Content: " + pl.hashCode() + " " + pl.size() + " tracks");
            if (!playbackController.setContent(pl)) {
                Log.v(LOG_TAG, "Current Track not found in new dataset. Stopping player...");
                exoPlayer.stop();
                exoPlayer.seekTo(0);
            }
            onStateModified(state.get().isPlaying());
        }
    }

    @Override
    public void play(UUID id) {
        Log.v(LOG_TAG, "play " + id);
        PlayerData n = playbackController.setNext(id);
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Track: " + n.getMetadata().getTitle());
            createPlayer(n);
            resume();
        } else {
            Log.e(LOG_TAG, "Null track");
        }
    }

    @Override
    public void next() {
        Log.v(LOG_TAG, "next");
        PlayerData n = playbackController.getNext();
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
        if (exoPlayer.getCurrentPosition() > PREV_THRESHOLD) {
            exoPlayer.seekTo(0);
            onStateModified(state.get().isPlaying());
        } else {
            PlayerData n = playbackController.getPrev();
            if (n != null) {
                Log.v(LOG_TAG, "Setting up Track: " + n.getMetadata().getTitle());
                createPlayer(n);
            } else {
                Log.e(LOG_TAG, "Null track");
            }
        }
    }

    @Override
    public void pauseResume() {
        Log.v(LOG_TAG, "pauseResume");
        if (state.get().isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    @Override
    public void pause() {
        Log.v(LOG_TAG, "pause");
        exoPlayer.setPlayWhenReady(false);
        onStateModified(false);
    }

    @Override
    public void resume() {
        Log.v(LOG_TAG, "resume");
        exoPlayer.setPlayWhenReady(true);
        onStateModified(true);
    }

    @Override
    public void seek(float percentage) {
        if (playbackController.getCurrentTrack() != null) {
            long ms = (long) (playbackController.getCurrentTrack().getMetadata().getDuration() * percentage);
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
        exoPlayer.seekTo(0);
    }

    @Override
    public void switchShuffle() {
        playbackController.setShuffle(!playbackController.getShuffle());
    }

    @Override
    public void setShuffle(boolean shuffle) {
        Log.v(LOG_TAG, "setShuffle " + shuffle);
        playbackController.setShuffle(shuffle);
        onStateModified(state.get().isPlaying());
    }

    @Override
    public void switchRepeat() {
        playbackController.setRepeat(!playbackController.getRepeat());
    }

    @Override
    public void setRepeat(boolean repeat) {
        Log.v(LOG_TAG, "setRepeat " + repeat);
        playbackController.setRepeat(repeat);
        onStateModified(state.get().isPlaying());
    }

    @Override
    public void setVolume(float vol) {
        if (vol <= 1) {
            Log.v(LOG_TAG, "setVolume " + vol);
            volume = vol;
            exoPlayer.setVolume(vol);
            onStateModified(state.get().isPlaying());
        } else {
            throw new IllegalArgumentException("Invalid volume: " + vol);
        }
    }

    @Override
    public void setEqualizerPreset(int preset) {
        equalizerPreset = preset;
        int id = exoPlayer.getAudioSessionId();
        if (id != C.AUDIO_SESSION_ID_UNSET) {
            boolean equalizerEnabled = preset >= 0;
            equalizer = new Equalizer(100, id);
            equalizer.setEnabled(equalizerEnabled);
            if (equalizerEnabled) {
                try {
                    equalizer.usePreset((short) equalizerPreset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            onStateModified(state.get().isPlaying());
        }
    }

    @Override
    public void setVideoSurfaceView(SurfaceView view) {
        runOnMainThread(() -> {
            exoPlayer.setVideoSurfaceView(view);
        });
    }

    @Override
    public void setEnableMediaSessionControls(boolean enable) {
        if (enable) {
            mediaSession.setCallback(new MediaSessionCallback(this));
        } else {
            mediaSession.setCallback(null);
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

    private boolean isPlayingVideo() {
        for (Tracks.Group group : exoPlayer.getCurrentTracks().getGroups()) {
            if (group.getType() == C.TRACK_TYPE_VIDEO) {
                return true;
            }
        }
        return false;
    }

    private void onStateModified(boolean playing) {
        Format format = exoPlayer.getVideoFormat();
        PlayerState newState = new PlayerState(playbackController,
                exoPlayer,
                playing,
                volume,
                currentTask != null && !currentTask.completed,
                equalizerPreset,
                isPlayingVideo(),
                format == null ? 0 : format.width, format == null ? 0 : format.height);

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
            PlayerData track = state.getCurrentTrack();
            if (track != null && track.getMetadata().getArtwork() != null) {
                track.getMetadata().getArtwork().getDataSource().getBitmap(this, (bitmap) -> {
                            if (bitmap != null) {
                                showNotification(state.isPlaying(), track, bitmap);
                            } else {
                                showNotification(state.isPlaying(), track, BitmapUtils.getBitmapForResource(this, R.drawable.artwork_unset));
                            }
                        },
                        (exception) -> {
                            showNotification(state.isPlaying(), track, BitmapUtils.getBitmapForResource(this, R.drawable.artwork_unset));
                        });
            } else if (track != null) {
                showNotification(state.isPlaying(), track, BitmapUtils.getBitmapForResource(this, R.drawable.artwork_unset));
            }
        });
    }

    private void showNotification(boolean playing, PlayerData track, Bitmap artwork) {
        runOnMainThread(() -> {
            final Notification.Builder notificationBuilder;
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
                // For some mysterious reason when youtube stream is running while the activity is not running this call to startForeground throws ForegroundServiceStartNotAllowedException
                try {
                    if (!isForeground)
                        startForeground(NOTIFICATION_ID, notification);
                    isForeground = true;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to start foreground: " + e.getMessage());
                    isForeground = false;
                }
            } else {
                Log.v(LOG_TAG, "Stop Foreground");
                if (isForeground)
                    stopForeground(false);
                isForeground = false;
            }
        });
    }

    private void updateMediaSession(PlayerState state) {
        PlayerData track = state.getCurrentTrack();

        if (track != null) {
            final String title = track.getMetadata().getTitle();
            final String artist = track.getMetadata().getArtist();
            final long duration = track.getMetadata().getDuration();
            if (track.getMetadata().getArtwork() != null) {
                track.getMetadata().getArtwork().getDataSource().getBitmap(this, (bitmap) -> {
                            runOnMainThread(() -> {
                                MediaMetadata.Builder b = new MediaMetadata.Builder()
                                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                                        .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
                                if (bitmap != null) {
                                    b.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                                            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
                                } else {
                                    Bitmap defBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.artwork_unset);

                                    b.putBitmap(MediaMetadata.METADATA_KEY_ART, defBitmap)
                                            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, defBitmap);
                                }
                                mediaSession.setMetadata(b.build());
                                int plState;
                                if (state.getPlaybackState() == com.phaseshifter.canora.service.player.state.PlaybackState.STATE_BUFFERING) {
                                    plState = PlaybackState.STATE_BUFFERING;
                                } else if (state.isPlaying()) {
                                    plState = PlaybackState.STATE_PLAYING;
                                } else {
                                    plState = PlaybackState.STATE_PAUSED;
                                }
                                mediaSession.setPlaybackState(getMediaSessionState(plState, state.getPlayerPosition()));
                            });
                        },
                        (exception) -> {
                            runOnMainThread(() -> {
                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.artwork_unset);

                                MediaMetadata.Builder b = new MediaMetadata.Builder()
                                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                                        .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);

                                b.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);

                                mediaSession.setMetadata(b.build());

                                int plState;
                                if (state.getPlaybackState() == com.phaseshifter.canora.service.player.state.PlaybackState.STATE_BUFFERING) {
                                    plState = PlaybackState.STATE_BUFFERING;
                                } else if (state.isPlaying()) {
                                    plState = PlaybackState.STATE_PLAYING;
                                } else {
                                    plState = PlaybackState.STATE_PAUSED;
                                }
                                mediaSession.setPlaybackState(getMediaSessionState(plState, state.getPlayerPosition()));
                            });
                        });
            } else {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.artwork_unset);

                MediaMetadata.Builder b = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration);

                b.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);

                mediaSession.setMetadata(b.build());

                int plState;
                if (state.getPlaybackState() == com.phaseshifter.canora.service.player.state.PlaybackState.STATE_BUFFERING) {
                    plState = PlaybackState.STATE_BUFFERING;
                } else if (state.isPlaying()) {
                    plState = PlaybackState.STATE_PLAYING;
                } else {
                    plState = PlaybackState.STATE_PAUSED;
                }
                mediaSession.setPlaybackState(getMediaSessionState(plState, state.getPlayerPosition()));
            }
        }
    }

    private PlaybackState getMediaSessionState(int state, long pos) {
        return new PlaybackState.Builder()
                .setState(state, pos, 1f, SystemClock.elapsedRealtime())
                .setActions(PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackState.ACTION_PLAY_PAUSE
                        | PlaybackState.ACTION_SEEK_TO)
                .build();
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

    private class LoadTask {
        public boolean cancel = false;
        public boolean completed = false;
        public Semaphore mutex = new Semaphore(1);
        public PlayerData track = null;
        public CountDownLatch latch = new CountDownLatch(1);
    }

    private LoadTask currentTask = null;

    private void createPlayer(PlayerData next) {
        exoPlayer.stop();
        exoPlayer.seekTo(0);
        exoPlayer.setPlayWhenReady(state.get().isPlaying());

        LoadTask task = new LoadTask();

        task.track = next;

        LoadTask prevTask = currentTask;

        currentTask = task;

        onStateModified(state.get().isPlaying());

        pool.submit(() -> {
            // Cancel previous task
            if (prevTask != null) {
                boolean acquiredLock = false;
                while (!acquiredLock) {
                    try {
                        prevTask.mutex.acquire();
                        acquiredLock = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                prevTask.cancel = true;
                prevTask.mutex.release();
            }

            PlayerData track = task.track;
            playingTrack = track;

            track.getDataSource().getExoPlayerSources(this, (sources) -> {
                boolean acquiredLock = false;
                while (!acquiredLock) {
                    try {
                        task.mutex.acquire();
                        acquiredLock = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                runOnMainThread(() -> {
                    Log.v(LOG_TAG, "Prepared MediaSources for track " + track.getMetadata().getTitle());

                    if (!task.cancel) {
                        trackSources.clear();
                        trackSources.addAll(sources);
                        trackSourceIndex = 0;

                        updateTrackSource();
                        onStateModified(state.get().isPlaying());
                    }

                    task.completed = true;
                    task.mutex.release();
                    task.latch.countDown();
                });
            }, (exception) -> {
                boolean acquiredLock = false;
                while (!acquiredLock) {
                    try {
                        task.mutex.acquire();
                        acquiredLock = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                runOnMainThread(() -> {
                    Log.e(LOG_TAG, "Failed to retrieve MediaSources");

                    if (!task.cancel) {
                        trackSources.clear();
                        trackSourceIndex = 0;
                        next();
                    }

                    task.completed = true;
                    task.mutex.release();
                    task.latch.countDown();
                });
            });
        });
    }

    private void updateTrackSource() {
        Log.v(LOG_TAG, "Updating MediaSource Current: " + trackSourceIndex + " Sources: " + trackSources);
        exoPlayer.stop();
        exoPlayer.seekTo(0);
        exoPlayer.prepare(trackSources.get(trackSourceIndex));
    }
}