package com.phaseshifter.canora.data.settings;

public enum StringSetting {
    ;

    public final String key;
    public final String defaultValue;

    StringSetting(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}