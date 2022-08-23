package com.phaseshifter.canora.data.settings;

public enum BooleanSetting {
    SHUFFLE("BOOL_SHUFFLE", false),
    REPEAT("BOOL_REPEAT", false),
    SHOWANIMATIONS("BOOL_SHOWANIM_CSTM", true),
    DEVELOPERMODE("BOOL_DEVMODE", false),
    SHOWWARNING_PLAYLISTS("BOOL_WARNPL", true),
    EQUALIZER_ENABLED("EQUALIZER_ENABLED", false);

    public final String key;
    public final boolean defaultValue;

    BooleanSetting(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}