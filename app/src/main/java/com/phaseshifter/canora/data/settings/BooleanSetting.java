package com.phaseshifter.canora.data.settings;

public enum BooleanSetting {
    SHUFFLE("BOOL_SHUFFLE", false),
    REPEAT("BOOL_REPEAT", false),
    SHOWANIMATIONS("BOOL_SHOWANIM_CSTM", true),
    DEVELOPERMODE("BOOL_DEVMODE", false),
    ENABLE_MEDIASESSION_CALLBACK("BOOL_ENABLE_MEDIASESSION_CALLBACK", true);

    public final String key;
    public final boolean defaultValue;

    BooleanSetting(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}