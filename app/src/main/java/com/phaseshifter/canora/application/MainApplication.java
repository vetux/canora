package com.phaseshifter.canora.application;

import android.app.Application;
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

    public MainApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioDataRepo = new DeviceAudioRepository(new MediaStoreContentProvider(this, new ContentUriFactory()));
        audioPlaylistRepository = new UserPlaylistRepository(new File(getPlaylistPath()));
        scAudioDataRepo = new SoundCloudAudioRepository();
        ytRepo = new YoutubeSearchRepository(new YoutubeApiClient());
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
            }
        } catch (Exception e) {
            Log.e("MainApplication", "" + e.getMessage());
        }
        return YoutubeDL.getInstance();
    }
}