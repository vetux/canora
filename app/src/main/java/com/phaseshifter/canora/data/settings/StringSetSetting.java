package com.phaseshifter.canora.data.settings;

import java.util.Set;

public enum StringSetSetting {
    ;

    public final String key;
    public final Set<String> defaultValue;

    StringSetSetting(String key, Set<String> defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}