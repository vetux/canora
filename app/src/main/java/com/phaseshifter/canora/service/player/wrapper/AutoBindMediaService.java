package com.phaseshifter.canora.service.player.wrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.service.player.ExoPlayerService;
import com.phaseshifter.canora.service.player.MediaPlayerService;
import com.phaseshifter.canora.service.player.state.PlayerState;
import com.phaseshifter.canora.ui.utils.ServiceBinder;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.Observer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around the MediaPlayerService.
 * Automatically binds to the service if not bound when calling control functions,
 * without having the caller know anything about android.
 * <p>
 * The functions return immediately and the caller will be notified via the player state callbacks.
 */
public class AutoBindMediaService implements ServiceConnection, MediaPlayerService {
    private enum BindingState {
        IDLE,
        BINDING
    }

    private final Handler mainHandler;
    private final ServiceBinder serviceBinder;

    private final Observable<PlayerState> stateProxy;

    private final Observer<PlayerState> proxyObserver = new Observer<PlayerState>() {
        @Override
        public void update(Observable<PlayerState> o, PlayerState arg) {
            stateProxy.set(arg);
        }
    };

    private final ThreadPoolExecutor exec;

    private final Observable<ExoPlayerService> serviceRef;    //Accessed only from activity_main thread.

    private BindingState bindingState;    //Accessed only from activity_main thread.

    public AutoBindMediaService(Context context) {
        serviceBinder = new ServiceBinder(context, ExoPlayerService.class, this);
        mainHandler = new Handler(Looper.getMainLooper());
        stateProxy = new Observable<>();
        exec = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        serviceRef = new Observable<>();
        bindingState = BindingState.IDLE;
    }

    public void bind() {
        verifyMainThread();
        bindingState = BindingState.BINDING;
        serviceBinder.bindService();
    }

    public void unbind() {
        verifyMainThread();
        bindingState = BindingState.IDLE;
        MediaPlayerService service = serviceRef.get();
        if (service != null)
            service.getState().removeObserver(proxyObserver);
        serviceRef.set(null);
        serviceBinder.unbindService();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        verifyMainThread();
        bindingState = BindingState.IDLE;
        ExoPlayerService boundService = ((ExoPlayerService.LocalBinder) service).getService();
        serviceRef.set(boundService);
        stateProxy.set(boundService.getState().get());
        boundService.getState().addObserver(proxyObserver);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        verifyMainThread();
        bindingState = BindingState.IDLE;
        serviceRef.set(null);
    }

    @Override
    public void shutdown() {
        verifyMainThread();
        if (serviceRef.get() != null) {
            serviceRef.get().shutdown();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().shutdown();
                    });
            });
        }
    }

    @Override
    public void setContent(List<AudioData> pl) {
        verifyMainThread();
        if (serviceRef.get() != null) {
            serviceRef.get().setContent(pl);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().setContent(pl);
                    });
            });
        }
    }

    @Override
    public void play(UUID id) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().play(id);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null) {
                            serviceRef.get().play(id);
                        }
                    });
            });
        }
    }

    @Override
    public void next() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().next();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().next();
                    });
            });
        }
    }

    @Override
    public void previous() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().previous();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().previous();
                    });
            });
        }
    }

    @Override
    public void pauseResume() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().pauseResume();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().pauseResume();
                    });
            });
        }
    }

    @Override
    public void pause() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().pause();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().pause();
                    });
            });
        }
    }

    @Override
    public void resume() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().resume();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().resume();
                    });
            });
        }
    }

    @Override
    public void seek(float p) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().seek(p);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().seek(p);
                    });
            });
        }
    }

    @Override
    public void seek(long ms) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().seek(ms);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().seek(ms);
                    });
            });
        }
    }

    @Override
    public void stop() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().stop();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().stop();
                    });
            });
        }
    }

    @Override
    public void switchShuffle() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().switchShuffle();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().switchShuffle();
                    });
            });
        }
    }

    @Override
    public void setShuffle(boolean state) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().setShuffle(state);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().setShuffle(state);
                    });
            });
        }
    }

    @Override
    public void switchRepeat() {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().switchRepeat();
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().switchRepeat();
                    });
            });
        }
    }

    @Override
    public void setRepeat(boolean state) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().setRepeat(state);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().setRepeat(state);
                    });
            });
        }
    }

    @Override
    public void setVolume(float vol) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().setVolume(vol);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().setVolume(vol);
                    });
            });
        }
    }

    @Override
    public void setEqualizerPreset(int preset) {
        verifyMainThread();
        if (serviceRef.get() != null){
            serviceRef.get().setEqualizerPreset(preset);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null)
                            serviceRef.get().setEqualizerPreset(preset);
                    });
            });
        }
    }

    @Override
    public Observable<PlayerState> getState() {
        verifyMainThread();
        return stateProxy;
    }

    private boolean requireService() {
        verifyNonMainThread();
        CountDownLatch bindFlag = new CountDownLatch(1);
        mainHandler.post(() -> {
            if (serviceRef.get() == null) {
                serviceRef.addObserver(new Observer<ExoPlayerService>() {
                    @Override
                    public void update(Observable<ExoPlayerService> o, ExoPlayerService arg) {
                        serviceRef.removeObserver(this);
                        bindFlag.countDown();
                    }
                });
                if (bindingState == BindingState.IDLE)
                    bind();
            } else {
                bindFlag.countDown();
            }
        });
        try {
            bindFlag.await();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void verifyMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new RuntimeException("Non Main Thread! : " + Looper.myLooper());
    }

    private void verifyNonMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException("Main Thread! : " + Looper.myLooper());
    }
}