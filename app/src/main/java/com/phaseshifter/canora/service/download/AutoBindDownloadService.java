package com.phaseshifter.canora.service.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.phaseshifter.canora.ui.utils.ServiceBinder;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.Observer;
import com.phaseshifter.canora.utils.RunnableArg;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AutoBindDownloadService implements ServiceConnection, DownloadService {
    private enum BindingState {
        IDLE,
        BINDING
    }

    private final Handler mainHandler;
    private final ServiceBinder serviceBinder;

    private final ThreadPoolExecutor exec;

    private final Observable<YTDLDownloadService> serviceRef;    //Accessed only from activity_main thread.

    private AutoBindDownloadService.BindingState bindingState;    //Accessed only from activity_main thread.

    public AutoBindDownloadService(Context context) {
        serviceBinder = new ServiceBinder(context, YTDLDownloadService.class, this);
        mainHandler = new Handler(Looper.getMainLooper());
        exec = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        serviceRef = new Observable<>();
        bindingState = AutoBindDownloadService.BindingState.IDLE;
    }

    public void bind() {
        verifyMainThread();
        bindingState = AutoBindDownloadService.BindingState.BINDING;
        serviceBinder.bindService();
    }

    public void unbind() {
        verifyMainThread();
        bindingState = AutoBindDownloadService.BindingState.IDLE;
        YTDLDownloadService service = serviceRef.get();
        if (service != null)
            serviceRef.set(null);
        serviceBinder.unbindService();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        verifyMainThread();
        bindingState = AutoBindDownloadService.BindingState.IDLE;
        YTDLDownloadService boundService = ((YTDLDownloadService.LocalBinder) service).getService();
        serviceRef.set(boundService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        verifyMainThread();
        bindingState = AutoBindDownloadService.BindingState.IDLE;
        serviceRef.set(null);
    }

    @Override
    public void getDownloads(RunnableArg<HashSet<Download>> callback) {
        verifyMainThread();
        if (serviceRef.get() != null) {
            serviceRef.get().getDownloads(callback);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null) {
                            serviceRef.get().getDownloads(callback);
                        }
                    });
            });
        }
    }

    @Override
    public void downloadAudio(Uri outputUri,
                              OutputStream outputStream,
                              String url,
                              Runnable completionCallback,
                              RunnableArg<Exception> exceptionCallback) {
        verifyMainThread();
        if (serviceRef.get() != null) {
            serviceRef.get().downloadAudio(outputUri, outputStream, url, completionCallback, exceptionCallback);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null) {
                            serviceRef.get().downloadAudio(outputUri, outputStream, url, completionCallback, exceptionCallback);
                        }
                    });
            });
        }
    }

    @Override
    public void downloadVideo(Uri outputUri, OutputStream outputStream, String url, Runnable completionCallback, RunnableArg<Exception> exceptionCallback) {
        verifyMainThread();
        if (serviceRef.get() != null) {
            serviceRef.get().downloadAudio(outputUri, outputStream, url, completionCallback, exceptionCallback);
        } else {
            exec.execute(() -> {
                if (requireService())
                    mainHandler.post(() -> {
                        if (serviceRef.get() != null) {
                            serviceRef.get().downloadVideo(outputUri, outputStream, url, completionCallback, exceptionCallback);
                        }
                    });
            });
        }
    }

    private boolean requireService() {
        verifyNonMainThread();
        CountDownLatch bindFlag = new CountDownLatch(1);
        mainHandler.post(() -> {
            if (serviceRef.get() == null) {
                serviceRef.addObserver(new Observer<YTDLDownloadService>() {
                    @Override
                    public void update(Observable<YTDLDownloadService> o, YTDLDownloadService arg) {
                        serviceRef.removeObserver(this);
                        bindFlag.countDown();
                    }
                });
                if (bindingState == AutoBindDownloadService.BindingState.IDLE)
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
