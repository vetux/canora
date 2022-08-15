package com.phaseshifter.canora.application;

import android.app.Application;
import android.os.Handler;

import com.phaseshifter.canora.model.provider.MediaStoreContentProvider;
import com.phaseshifter.canora.model.repo.DeviceAudioRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.ui.data.DownloadProgress;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.RunnableArg;
import com.phaseshifter.canora.utils.android.ContentUriFactory;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainApplication extends Application {
    private DeviceAudioRepository audioDataRepo;
    private UserPlaylistRepository audioPlaylistRepository;
    private SoundCloudAudioRepository scAudioDataRepo;

    //Store objects passed between activities here as intent bundles cant hold more than 1MB
    private final HashMap<String, Object> bundle = new HashMap<>();

    public final Observable<List<DownloadProgress>> downloads = new Observable<>(new ArrayList<>());

    private final ThreadPoolExecutor downloadExec;

    private boolean instanceInit = false;

    public MainApplication() {
        this.downloadExec = new ThreadPoolExecutor(1, 4, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioDataRepo = new DeviceAudioRepository(new MediaStoreContentProvider(this, new ContentUriFactory()));
        audioPlaylistRepository = new UserPlaylistRepository(new File(getPlaylistPath()));
        scAudioDataRepo = new SoundCloudAudioRepository();
    }

    public DeviceAudioRepository getAudioDataRepo() {
        return audioDataRepo;
    }

    public UserPlaylistRepository getAudioPlaylistRepository() {
        return audioPlaylistRepository;
    }

    public SoundCloudAudioRepository getScAudioRepository() {
        return scAudioDataRepo;
    }

    public String getPlaylistPath() {
        return getFilesDir().getAbsolutePath() + "/playlists/";
    }

    public void removeBundle(String key) {
        bundle.remove(key);
    }

    public Object getBundle(String key) {
        return bundle.get(key);
    }

    public void putBundle(String key, Object value) {
        bundle.put(key, value);
    }

    public void startDownload(String url,
                              YoutubeDLRequest request,
                              String tempFile,
                              OutputStream outputStream,
                              RunnableArg<Exception> exceptionHandler,
                              Runnable completionHandler) {
        DownloadProgress downloadProgress = new DownloadProgress();
        downloadProgress.url = url;
        downloads.get().add(downloadProgress);
        downloads.notifyObservers();
        downloadExec.submit(() -> {
            try {
                YoutubeDL.getInstance().execute(request, (progress, etaInSeconds, line) -> {
                    new Handler(getMainLooper()).post(() -> {
                        downloadProgress.progress = progress;
                        downloadProgress.etaInSeconds = etaInSeconds;
                        downloads.notifyObservers();
                    });
                });
                new Handler(getMainLooper()).post(() -> {
                    downloadProgress.progress = 100;
                    downloadProgress.etaInSeconds = 0;
                    downloads.notifyObservers();
                });
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                fis.close();
                new Handler(getMainLooper()).post(() -> {
                    downloads.get().remove(downloadProgress);
                    downloads.notifyObservers();
                    completionHandler.run();
                });
            } catch (Exception e) {
                exceptionHandler.run(e);
            }
        });
    }

    public YoutubeDL getYoutubeDlInstance() throws YoutubeDLException {
        if (!instanceInit)
            YoutubeDL.getInstance().init(this);
        return YoutubeDL.getInstance();
    }
}