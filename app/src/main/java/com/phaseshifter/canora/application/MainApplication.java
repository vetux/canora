package com.phaseshifter.canora.application;

import android.app.Application;

import com.phaseshifter.canora.model.provider.MediaStoreContentProvider;
import com.phaseshifter.canora.model.repo.DeviceAudioRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.utils.android.ContentUriFactory;

import java.io.File;
import java.util.HashMap;

public class MainApplication extends Application {
    private DeviceAudioRepository audioDataRepo;
    private UserPlaylistRepository audioPlaylistRepository;
    private SCAudioDataRepo scAudioDataRepo;

    //Store objects passed between activities here as intent bundles cant hold more than 1MB
    private final HashMap<String, Object> bundle = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        audioDataRepo = new DeviceAudioRepository(new MediaStoreContentProvider(this, new ContentUriFactory()));
        audioPlaylistRepository = new UserPlaylistRepository(new File(getPlaylistPath()));
        scAudioDataRepo = new SCAudioDataRepo();
    }

    public DeviceAudioRepository getAudioDataRepo() {
        return audioDataRepo;
    }

    public UserPlaylistRepository getAudioPlaylistRepository() {
        return audioPlaylistRepository;
    }

    public SCAudioDataRepo getScAudioRepository() {
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
}