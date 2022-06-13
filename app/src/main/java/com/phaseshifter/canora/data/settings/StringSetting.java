package com.phaseshifter.canora.data.settings;

public enum StringSetting {
    SC_CLIENTID("STRING_SC_CLIENTID", null);

    public final String key;
    public final String defaultValue;

    StringSetting(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}