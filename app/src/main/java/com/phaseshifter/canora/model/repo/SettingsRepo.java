package com.phaseshifter.canora.model.repo;

import android.content.Context;
import android.content.SharedPreferences;
import com.phaseshifter.canora.data.settings.*;

import java.util.Map;
import java.util.Set;

public class SettingsRepo implements SettingsRepository {
    public final String SHARED_PREFS_NAME = "com.phaseshifter.canora#PREFS";

    private final SharedPreferences prefs;

    public SettingsRepo(Context c) {
        prefs = c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void reset() {
        prefs.edit().clear().commit();
    }

    @Override
    public Map<String, ?> getAll() {
        return prefs.getAll();
    }

    @Override
    public boolean getBoolean(BooleanSetting setting) {
        return prefs.getBoolean(setting.key, setting.defaultValue);
    }

    @Override
    public void putBoolean(BooleanSetting setting, boolean value) {
        prefs.edit().putBoolean(setting.key, value).commit();
    }

    @Override
    public float getFloat(FloatSetting setting) {
        return prefs.getFloat(setting.key, setting.defaultValue);
    }

    @Override
    public void putFloat(FloatSetting setting, float value) {
        prefs.edit().putFloat(setting.key, value).commit();
    }

    @Override
    public int getInt(IntegerSetting setting) {
        return prefs.getInt(setting.key, setting.defaultValue);
    }

    @Override
    public void putInt(IntegerSetting setting, int value) {
        prefs.edit().putInt(setting.key, value).commit();
    }

    @Override
    public long getLong(LongSetting setting) {
        return prefs.getLong(setting.key, setting.defaultValue);
    }

    @Override
    public void putLong(LongSetting setting, long value) {
        prefs.edit().putLong(setting.key, value).commit();
    }

    @Override
    public Set<String> getStringSet(StringSetSetting setting) {
        return prefs.getStringSet(setting.key, setting.defaultValue);
    }

    @Override
    public void putStringSet(StringSetSetting setting, Set<String> value) {
        prefs.edit().putStringSet(setting.key, value).commit();
    }

    @Override
    public String getString(StringSetting setting) {
        return prefs.getString(setting.key, setting.defaultValue);
    }

    @Override
    public void putString(StringSetting setting, String value) {
        prefs.edit().putString(setting.key, value).commit();
    }
}