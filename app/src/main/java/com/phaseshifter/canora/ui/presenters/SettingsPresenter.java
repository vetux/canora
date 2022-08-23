package com.phaseshifter.canora.ui.presenters;

import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.util.Log;

import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.data.settings.StringSetting;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.repo.SettingsRepository;
import com.phaseshifter.canora.model.repo.ThemeRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.service.MediaPlayerService;
import com.phaseshifter.canora.ui.contracts.SettingsContract;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;
import com.phaseshifter.canora.utils.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SettingsPresenter implements SettingsContract.Presenter {
    private final String LOG_TAG = "SettingsPresenter";

    public static class State implements Serializable {
        public SettingsPage page;
        public AppTheme activeTheme;
        public List<AppTheme> availableThemes;
        public float volume;
        public boolean devMode;
        public boolean useAnimations;
        public int playlistCount;
        public long playlistSize;
        public String scClientID;
        public String ytApiKey;
        public List<Pair<String, Object>> modifiedSettings;
        public boolean equalizerEnabled;
        public int equalizerPreset;
        public String[] equalizerPresets;

        public void copy(State state) {
            page = state.page;
            activeTheme = state.activeTheme;
            availableThemes = state.availableThemes;
            volume = state.volume;
            devMode = state.devMode;
            useAnimations = state.useAnimations;
            this.playlistCount = state.playlistCount;
            this.modifiedSettings = state.modifiedSettings;
            this.scClientID = state.scClientID;
            this.ytApiKey = state.ytApiKey;
            equalizerEnabled = state.equalizerEnabled;
            equalizerPreset = state.equalizerPreset;
            equalizerPresets = state.equalizerPresets;
        }
    }

    private final SettingsContract.View view;

    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final UserPlaylistRepository audioPlaylistRepository;
    private final MediaPlayerService service;
    private final AudioManager audioManager;

    private final State state = new State();

    public SettingsPresenter(SettingsContract.View view,
                             SettingsRepository settingsRepository,
                             ThemeRepository themeRepository,
                             UserPlaylistRepository audioPlaylistRepository,
                             MediaPlayerService service,
                             AudioManager audioManager,
                             State state) {
        this.view = view;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.audioPlaylistRepository = audioPlaylistRepository;
        this.service = service;
        this.audioManager = audioManager;
        if (state != null)
            this.state.copy(state);
    }

    public SettingsPresenter(SettingsContract.View view,
                             SettingsRepository settingsRepository,
                             ThemeRepository themeRepository,
                             UserPlaylistRepository audioPlaylistRepository,
                             MediaPlayerService service,
                             AudioManager audioManager,
                             Serializable state) {
        this(view, settingsRepository, themeRepository, audioPlaylistRepository, service, audioManager, (State) state);
    }

    //START Presenter Interface

    @Override
    public void start() {
        Log.v(LOG_TAG, "start");
        loadApply();
    }

    @Override
    public void stop() {
        Log.v(LOG_TAG, "stop");
    }

    @Override
    public Serializable saveState() {
        Log.v(LOG_TAG, "saveState");
        return state;
    }

    @Override
    public void resetSettings() {
        Log.v(LOG_TAG, "resetSettings");
        view.showDialog_confirmation_settingsreset(() -> {
            settingsRepository.reset();
            loadApply();
        });
    }

    @Override
    public void onThemeSelected(AppTheme theme) {
        Log.v(LOG_TAG, "onThemeSelected " + theme);
        settingsRepository.putInt(IntegerSetting.THEME, theme.id);
        loadApply();
    }

    @Override
    public void onVolumeChange(float v) {
        Log.v(LOG_TAG, "onVolumeChange " + v);
        settingsRepository.putFloat(FloatSetting.VOLUME, v);
        service.setVolume(v);
        loadState();
    }

    @Override
    public void onNavigateBack() {
        Log.v(LOG_TAG, "onNavigateBack");
        if (state.page != null) {
            state.page = null;
            applyState(view, state);
        } else {
            view.finish();
        }
    }

    @Override
    public void onPageSelected(SettingsPage page) {
        Log.v(LOG_TAG, "onPageSelected " + page);
        state.page = page;
        view.showPage(page);
    }

    @Override
    public void onUseAnimationsChange(boolean useAnimations) {
        Log.v(LOG_TAG, "onUseAnimationsChange " + useAnimations);
        settingsRepository.putBoolean(BooleanSetting.SHOWANIMATIONS, useAnimations);
        loadState();
    }

    @Override
    public void onDeveloperModeChange(boolean devMode) {
        Log.v(LOG_TAG, "onDeveloperModeChange " + devMode);
        if (devMode && !state.devMode) {
            view.showDialog_warning_devmode(() -> {
                settingsRepository.putBoolean(BooleanSetting.DEVELOPERMODE, true);
                loadApply();
            }, () -> applyState(view, state));
        } else {
            settingsRepository.putBoolean(BooleanSetting.DEVELOPERMODE, devMode);
            loadApply();
        }
    }

    @Override
    public void onSoundCloudClientIDChange(String clientID) {
        if (clientID != null && clientID.isEmpty())
            settingsRepository.remove(StringSetting.SC_CLIENTID.key);
        else
            settingsRepository.putString(StringSetting.SC_CLIENTID, clientID);
        state.scClientID = clientID;
    }

    @Override
    public void onYoutubeApiKeyChange(String apiKey) {
        if (apiKey != null && apiKey.isEmpty()) {
            settingsRepository.remove(StringSetting.YOUTUBE_API_KEY.key);
        } else {
            settingsRepository.putString(StringSetting.YOUTUBE_API_KEY, apiKey);
        }
        state.ytApiKey = apiKey;
    }

    @Override
    public void onEqualizerEnabledChange(boolean equalizerEnabled) {
        settingsRepository.putBoolean(BooleanSetting.EQUALIZER_ENABLED, equalizerEnabled);
        state.equalizerEnabled = equalizerEnabled;
        service.setEqualizerEnabled(equalizerEnabled);
        view.setEqualizerEnabled(equalizerEnabled);
    }

    @Override
    public void onEqualizerPresetChange(int preset) {
        settingsRepository.putInt(IntegerSetting.EQUALIZER_PRESET_INDEX, preset);
        state.equalizerPreset = preset;
        service.setEqualizerPreset(preset);
        view.setEqualizerPreset(preset);
    }

    //STOP Presenter Interface

    private void loadApply() {
        loadState();
        applyState(view, state);
    }

    private void loadState() {
        state.availableThemes = themeRepository.getAll();
        int themeID = settingsRepository.getInt(IntegerSetting.THEME);
        for (AppTheme theme : state.availableThemes) {
            if (theme.id == themeID) {
                state.activeTheme = theme;
                break;
            }
        }
        state.volume = settingsRepository.getFloat(FloatSetting.VOLUME);
        state.devMode = settingsRepository.getBoolean(BooleanSetting.DEVELOPERMODE);
        state.useAnimations = settingsRepository.getBoolean(BooleanSetting.SHOWANIMATIONS);
        state.playlistCount = audioPlaylistRepository.getAll().size();
        state.playlistSize = audioPlaylistRepository.getSize();
        state.modifiedSettings = new ArrayList<>();
        state.ytApiKey = settingsRepository.getString(StringSetting.YOUTUBE_API_KEY);
        if (Objects.equals(state.ytApiKey, StringSetting.YOUTUBE_API_KEY.defaultValue)) {
            state.ytApiKey = null;
        }
        state.scClientID = settingsRepository.getString(StringSetting.SC_CLIENTID);
        Map<String, ?> values = settingsRepository.getAll();
        if (values != null && values.size() > 0) {
            for (Map.Entry<String, ?> value : values.entrySet()) {
                state.modifiedSettings.add(new Pair<>(value.getKey(), value.getValue()));
            }
        }
        state.equalizerEnabled = settingsRepository.getBoolean(BooleanSetting.EQUALIZER_ENABLED);
        state.equalizerPreset = settingsRepository.getInt(IntegerSetting.EQUALIZER_PRESET_INDEX);
        int id = audioManager.generateAudioSessionId(); // Documentation does not specify how to "ungenerate" an audio session id.
        Equalizer equalizer = new Equalizer(0, id);
        state.equalizerPresets = new String[equalizer.getNumberOfPresets()];
        for (short i = 0; i < equalizer.getNumberOfPresets(); i++) {
            state.equalizerPresets[i] = equalizer.getPresetName(i);
        }
    }

    private void applyState(SettingsContract.View view, State state) {
        view.setTheme(state.activeTheme);
        if (state.page != null)
            view.showPage(state.page);
        else
            view.showMain();
        view.setActiveTheme(state.activeTheme);
        view.setAvailableThemes(state.availableThemes);
        view.setVolume(state.volume);
        view.setDeveloperMode(state.devMode);
        view.setUseAnimations(state.useAnimations);
        view.setLog_playlist(state.playlistCount, state.playlistSize);
        view.setLog_modifiedSettings(state.modifiedSettings);
        view.setSoundCloudClientID(state.scClientID);
        view.setYoutubeApiKey(state.ytApiKey);
        view.setEqualizerPresets(state.equalizerPresets);
        view.setEqualizerPreset(state.equalizerPreset);
        view.setEqualizerEnabled(state.equalizerEnabled);
    }
}