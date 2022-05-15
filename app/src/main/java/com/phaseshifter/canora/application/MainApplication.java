package com.phaseshifter.canora.application;

import android.app.Application;

import com.phaseshifter.canora.model.provider.MediaStoreContentProvider;
import com.phaseshifter.canora.model.repo.AudioDataRepo;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepo;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.utils.android.ContentUriFactory;

import java.io.File;
import java.util.HashMap;

public class MainApplication extends Application {
    private AudioDataRepository audioDataRepo;
    private AudioPlaylistRepository audioPlaylistRepository;
    private SCAudioDataRepo scAudioDataRepo;

    //Store objects passed between activities here as intent bundles cant hold more than 1MB
    private final HashMap<String, Object> bundle = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        audioDataRepo = new AudioDataRepo(new MediaStoreContentProvider(this, new ContentUriFactory()));
        audioPlaylistRepository = new AudioPlaylistRepo(new File(getPlaylistPath()));
        scAudioDataRepo = new SCAudioDataRepo();
    }

    public AudioDataRepository getAudioDataRepo() {
        return audioDataRepo;
    }

    public AudioPlaylistRepository getAudioPlaylistRepository() {
        return audioPlaylistRepository;
    }

    public SCAudioDataRepo getScAudioRepository() {
        return scAudioDataRepo;
    }

    public String getPlaylistPath() {
        return getFilesDir().getAbsolutePath() + "/Playlists";
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