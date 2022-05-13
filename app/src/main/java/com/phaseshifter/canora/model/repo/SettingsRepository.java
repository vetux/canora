package com.phaseshifter.canora.model.repo;

import com.phaseshifter.canora.data.settings.*;

import java.util.Map;
import java.util.Set;

public interface SettingsRepository {
    void reset();

    Map<String, ?> getAll();

    boolean getBoolean(BooleanSetting setting);

    void putBoolean(BooleanSetting setting, boolean value);

    float getFloat(FloatSetting setting);

    void putFloat(FloatSetting setting, float value);

    int getInt(IntegerSetting setting);

    void putInt(IntegerSetting setting, int value);

    long getLong(LongSetting setting);

    void putLong(LongSetting setting, long value);

    Set<String> getStringSet(StringSetSetting setting);

    void putStringSet(StringSetSetting setting, Set<String> value);

    String getString(StringSetting setting);

    void putString(StringSetting setting, String value);
}