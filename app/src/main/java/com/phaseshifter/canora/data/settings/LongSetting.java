package com.phaseshifter.canora.data.settings;

public enum LongSetting {
    ;

    public final String key;
    public final long defaultValue;

    LongSetting(String key, long defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}