package com.phaseshifter.canora.application;

import android.app.Application;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import com.phaseshifter.canora.model.provider.MediaStoreContentProvider;
import com.phaseshifter.canora.model.repo.DeviceAudioRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.model.repo.YoutubeSearchRepository;
import com.phaseshifter.canora.plugin.youtubeapi.YoutubeApiClient;
import com.phaseshifter.canora.utils.android.ContentUriFactory;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MainApplication extends Application {
    public static MainApplication instance;
    private DeviceAudioRepository audioDataRepo;
    private UserPlaylistRepository audioPlaylistRepository;
    private SoundCloudAudioRepository scAudioDataRepo;
    private YoutubeSearchRepository ytRepo;

    //Store objects passed between activities here as intent bundles cant hold more than 1MB
    private final HashMap<String, Object> bundle = new HashMap<>();

    private boolean instanceInit = false;

    private final String DOUBLE_LINE_SEP = "  ";
    private final String SINGLE_LINE_SEP = " ";

    public void uncaughtException(Thread t, final Throwable e) {
        //https://stackoverflow.com/a/13417090
        StackTraceElement[] arr = e.getStackTrace();
        final StringBuffer report = new StringBuffer(e.toString());
        final String lineSeperator = "-------------------------------\n\n";
        report.append(DOUBLE_LINE_SEP);
        report.append("--------- Stack trace ---------\n\n");
        for (int i = 0; i < arr.length; i++) {
            report.append("    ");
            report.append(arr[i].toString());
            report.append(SINGLE_LINE_SEP);
        }
        report.append(lineSeperator);

        report.append("--------- Cause ---------\n\n");
        Throwable cause = e.getCause();
        if (cause != null) {
            report.append(cause);
            report.append(DOUBLE_LINE_SEP);
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report.append("    ");
                report.append(arr[i].toString());
                report.append(SINGLE_LINE_SEP);
            }
        }

        report.append(lineSeperator);
        report.append("--------- Device ---------\n\n");
        report.append("Brand: ");
        report.append(Build.BRAND);
        report.append(SINGLE_LINE_SEP);
        report.append("Device: ");
        report.append(Build.DEVICE);
        report.append(SINGLE_LINE_SEP);
        report.append("Model: ");
        report.append(Build.MODEL);
        report.append(SINGLE_LINE_SEP);
        report.append("Id: ");
        report.append(Build.ID);
        report.append(SINGLE_LINE_SEP);
        report.append("Product: ");
        report.append(Build.PRODUCT);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);
        report.append("--------- Firmware ---------\n\n");
        report.append("SDK: ");
        report.append(Build.VERSION.SDK);
        report.append(SINGLE_LINE_SEP);
        report.append("Release: ");
        report.append(Build.VERSION.RELEASE);
        report.append(SINGLE_LINE_SEP);
        report.append("Incremental: ");
        report.append(Build.VERSION.INCREMENTAL);
        report.append(SINGLE_LINE_SEP);
        report.append(lineSeperator);

        Log.e("Report ::", report.toString());

        try {
            String fileName = Calendar.getInstance().getTime().toString();
            fileName = fileName.replace(' ', '_');
            fileName = fileName.replace(':', '_');
            fileName = fileName.replace('+', '_');
            File file = new File(getCrashLogsPath() + fileName + ".txt");
            if (file.createNewFile()) {
                FileOutputStream stream = new FileOutputStream(file);
                byte[] bytes = report.toString().getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
                stream.flush();
                stream.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    public MainApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);
        super.onCreate();
        audioDataRepo = new DeviceAudioRepository(new MediaStoreContentProvider(this, new ContentUriFactory()));
        audioPlaylistRepository = new UserPlaylistRepository(new File(getPlaylistPath()));
        scAudioDataRepo = new SoundCloudAudioRepository();
        ytRepo = new YoutubeSearchRepository(new YoutubeApiClient());

        try {
            File crashDir = new File(getCrashLogsPath());
            File[] files = crashDir.listFiles();
            if (files != null) {
                Log.v("app", files.length + " Crash logs");
            } else {
                Log.v("app", "Crash directory is file");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public YoutubeSearchRepository getYtRepo() {
        return ytRepo;
    }

    public String getPlaylistPath() {
        return getFilesDir().getAbsolutePath() + "/playlists/";
    }

    public String getCrashLogsPath() throws IOException {
        String ret = getFilesDir().getAbsolutePath() + "/crashlogs/";
        File file = new File(ret);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return ret;
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

    public YoutubeDL getYoutubeDlInstance() {
        try {
            if (!instanceInit) {
                YoutubeDL.getInstance().init(this);
                FFmpeg.getInstance().init(this);
                YoutubeDL.getInstance().updateYoutubeDL(this);
                instanceInit = true;
            }
        } catch (Exception e) {
            Log.e("MainApplication", "" + e.getMessage());
        }
        return YoutubeDL.getInstance();
    }
}