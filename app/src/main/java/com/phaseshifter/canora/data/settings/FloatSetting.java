package com.phaseshifter.canora.data.settings;

public enum FloatSetting {
    VOLUME("FLOAT_VOLUME", 0.5f),
    ;

    public final String key;
    public final float defaultValue;

    FloatSetting(String key, float defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}