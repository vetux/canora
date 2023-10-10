package com.phaseshifter.canora.ui.presenters;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.util.Log;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.data.settings.StringSetting;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.repo.SettingsRepository;
import com.phaseshifter.canora.model.repo.ThemeRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.service.player.MediaPlayerService;
import com.phaseshifter.canora.ui.contracts.SettingsContract;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;
import com.phaseshifter.canora.utils.Pair;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SettingsPresenter implements SettingsContract.Presenter {
    private final String LOG_TAG = "SettingsPresenter";

    public static class State implements Serializable {
        public SettingsPage page;
        public AppTheme activeTheme;
        public List<AppTheme> availableThemes;
        public float volume;
        public boolean devMode;
        public boolean enableMediaSessionCallbacks;
        public boolean useAnimations;
        public int playlistCount;
        public long playlistSize;
        public String scClientID;
        public String ytApiKey;
        public List<Pair<String, Object>> modifiedSettings;
        public int equalizerPreset;
        public String[] equalizerPresets;
        public int crashLogCount;

        public void copy(State state) {
            page = state.page;
            activeTheme = state.activeTheme;
            availableThemes = state.availableThemes;
            volume = state.volume;
            devMode = state.devMode;
            enableMediaSessionCallbacks = state.enableMediaSessionCallbacks;
            useAnimations = state.useAnimations;
            this.playlistCount = state.playlistCount;
            this.modifiedSettings = state.modifiedSettings;
            this.scClientID = state.scClientID;
            this.ytApiKey = state.ytApiKey;
            equalizerPreset = state.equalizerPreset;
            equalizerPresets = state.equalizerPresets;
            crashLogCount = state.crashLogCount;
        }
    }

    private final SettingsContract.View view;

    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final UserPlaylistRepository audioPlaylistRepository;
    private final MediaPlayerService service;
    private final AudioManager audioManager;
    private final Context context;

    private final State state = new State();

    public SettingsPresenter(SettingsContract.View view,
                             SettingsRepository settingsRepository,
                             ThemeRepository themeRepository,
                             UserPlaylistRepository audioPlaylistRepository,
                             MediaPlayerService service,
                             AudioManager audioManager,
                             Context context,
                             State state) {
        this.view = view;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.audioPlaylistRepository = audioPlaylistRepository;
        this.service = service;
        this.audioManager = audioManager;
        this.context = context;
        if (state != null)
            this.state.copy(state);
    }

    public SettingsPresenter(SettingsContract.View view,
                             SettingsRepository settingsRepository,
                             ThemeRepository themeRepository,
                             UserPlaylistRepository audioPlaylistRepository,
                             MediaPlayerService service,
                             AudioManager audioManager,
                             Context context,
                             Serializable state) {
        this(view, settingsRepository, themeRepository, audioPlaylistRepository, service, audioManager, context, (State) state);
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
        return state;
    }

    @Override
    public void resetSettings() {
        view.showDialog_confirmation_settingsreset(() -> {
            settingsRepository.reset();
            loadApply();
        });
    }

    @Override
    public void onThemeSelected(AppTheme theme) {
        settingsRepository.putInt(IntegerSetting.THEME, theme.id);
        loadApply();
    }

    @Override
    public void onVolumeChange(float v) {
        settingsRepository.putFloat(FloatSetting.VOLUME, v);
        service.setVolume(v);
        loadState();
    }

    @Override
    public void onNavigateBack() {
        if (state.page != null) {
            state.page = null;
            applyState(view, state);
        } else {
            view.finish();
        }
    }

    @Override
    public void onPageSelected(SettingsPage page) {
        state.page = page;
        view.showPage(page);
    }

    @Override
    public void onUseAnimationsChange(boolean useAnimations) {
        settingsRepository.putBoolean(BooleanSetting.SHOWANIMATIONS, useAnimations);
        loadState();
    }

    @Override
    public void onDeveloperModeChange(boolean devMode) {
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
    public void onEnableMediaSessionCallbacksChange(boolean enable) {
        if (!enable && state.enableMediaSessionCallbacks) {
            view.showDialog_warning_mediasession(() -> {
                settingsRepository.putBoolean(BooleanSetting.ENABLE_MEDIASESSION_CALLBACK, false);
                loadApply();
            }, () -> applyState(view, state));
        } else {
            settingsRepository.putBoolean(BooleanSetting.ENABLE_MEDIASESSION_CALLBACK, enable);
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
    public void onEqualizerPresetChange(int preset) {
        settingsRepository.putInt(IntegerSetting.EQUALIZER_PRESET_INDEX, preset - 1);
        state.equalizerPreset = preset - 1;
        service.setEqualizerPreset(preset - 1);
    }

    @Override
    public void onExportCrashLogs() {
        String fileName = android.icu.util.Calendar.getInstance().getTime().toString();
        fileName = fileName.replace(' ', '_');
        fileName = fileName.replace(':', '_');
        fileName = fileName.replace('+', '_');
        view.createDocument("application/zip", "Canora_Crash_Logs_" + fileName + ".zip");
    }

    @Override
    public void onDocumentCreated(Uri uri) {
        File[] files = null;

        MainApplication app = (MainApplication) context.getApplicationContext();
        File logDir = new File(app.getCrashLogsDir());
        if (logDir.exists()) {
            files = logDir.listFiles();
        }

        if (files != null) {
            try {
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(view.openDocument(uri)));

                BufferedInputStream in;

                final int BUFFER_SIZE = 4096;

                byte[] buffer = new byte[BUFFER_SIZE];
                for (File file : files) {
                    FileInputStream fis = new FileInputStream(file);
                    in = new BufferedInputStream(fis, BUFFER_SIZE);
                    try {
                        ZipEntry entry = new ZipEntry(file.getName());
                        out.putNextEntry(entry);
                        int count;
                        while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                            out.write(buffer, 0, count);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        in.close();
                    }
                }

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                view.showMessage(view.getString(R.string.settings_crashlog_export_fail));
            } finally {
                view.showMessage(view.getString(R.string.settings_crashlog_export_success));
            }
        }
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
        state.enableMediaSessionCallbacks = settingsRepository.getBoolean(BooleanSetting.ENABLE_MEDIASESSION_CALLBACK);
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
        state.equalizerPreset = settingsRepository.getInt(IntegerSetting.EQUALIZER_PRESET_INDEX);
        int id = audioManager.generateAudioSessionId(); // Documentation does not specify how to "ungenerate" an audio session id.
        Equalizer equalizer = new Equalizer(0, id);
        if (state.equalizerPreset >= equalizer.getNumberOfPresets()) {
            state.equalizerPreset = -1;
        }
        state.equalizerPresets = new String[equalizer.getNumberOfPresets() + 1];
        state.equalizerPresets[0] = view.getString(R.string.equalizerDefault);
        for (short i = 0; i < equalizer.getNumberOfPresets(); i++) {
            state.equalizerPresets[i + 1] = equalizer.getPresetName(i);
        }

        state.crashLogCount = 0;
        MainApplication app = (MainApplication) context.getApplicationContext();
        File logDir = new File(app.getCrashLogsDir());
        if (logDir.exists()) {
            File[] files = logDir.listFiles();
            if (files != null)
                state.crashLogCount = files.length;
        }
    }

    private void applyState(SettingsContract.View view, State state) {
        view.setTheme(state.activeTheme);
        view.setActiveTheme(state.activeTheme);
        view.setAvailableThemes(state.availableThemes);
        view.setVolume(state.volume);
        view.setDeveloperMode(state.devMode);
        view.setEnableMediaSessionCallbacks(state.enableMediaSessionCallbacks);
        view.setUseAnimations(state.useAnimations);
        view.setLog_playlist(state.playlistCount, state.playlistSize);
        view.setLog_modifiedSettings(state.modifiedSettings);
        view.setSoundCloudClientID(state.scClientID);
        view.setYoutubeApiKey(state.ytApiKey);
        view.setEqualizerPresets(state.equalizerPresets);
        view.setEqualizerPreset(state.equalizerPreset + 1);
        view.setCrashLogCount(state.crashLogCount);
        if (state.page != null)
            view.showPage(state.page);
        else
            view.showMain();
    }
}