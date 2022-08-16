package com.phaseshifter.canora.data.settings;

public enum StringSetting {
    SC_CLIENTID("STRING_SC_CLIENTID", null),
    YOUTUBE_API_KEY("YOUTUBE_API_KEY", "AIzaSyA5dKvxiKC6GkBoPSpzhNuLGID9BHCA12A");

    public final String key;
    public final String defaultValue;

    StringSetting(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}