package com.phaseshifter.canora.ui.contracts;

import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;
import com.phaseshifter.canora.utils.Pair;

import java.io.Serializable;
import java.util.List;

public interface SettingsContract {
    interface View {
        void setTheme(AppTheme theme);

        void showMain();

        void showPage(SettingsPage page);

        void setActiveTheme(AppTheme theme);

        void setAvailableThemes(List<AppTheme> themes);

        void setUseAnimations(boolean useAnimations);

        void setVolume(float v);

        void setDeveloperMode(boolean devMode);

        void setSoundCloudClientID(String clientID);

        void setYoutubeApiKey(String apiKey);

        void setEqualizerEnabled(boolean enabled);

        void setEqualizerPresets(String[] presets);

        void setEqualizerPreset(int preset);

        void setLog_playlist(int count, long size);

        void setLog_modifiedSettings(List<Pair<String, Object>> settings);

        void showDialog_confirmation_settingsreset(Runnable onReset);

        void showDialog_warning_devmode(Runnable onEnable, Runnable onCancel);

        void finish();
    }

    interface Presenter {
        void start();

        void stop();

        Serializable saveState();

        void resetSettings();

        void onThemeSelected(AppTheme theme);

        void onVolumeChange(float v);

        void onNavigateBack();

        void onPageSelected(SettingsPage page);

        void onUseAnimationsChange(boolean useAnimations);

        void onDeveloperModeChange(boolean devMode);

        void onSoundCloudClientIDChange(String clientID);

        void onYoutubeApiKeyChange(String apiKey);

        void onEqualizerEnabledChange(boolean equalizerEnabled);

        void onEqualizerPresetChange(int preset);
    }
}