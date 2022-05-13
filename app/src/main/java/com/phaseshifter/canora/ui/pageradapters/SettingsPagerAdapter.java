package com.phaseshifter.canora.ui.pageradapters;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.ui.activities.SettingsActivity;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;

public class SettingsPagerAdapter extends PagerAdapter {
    private final SettingsActivity activity;
    private SettingsPage currentPage;

    public SettingsPagerAdapter(SettingsActivity activity, SettingsPage page) {
        this.activity = activity;
        this.currentPage = page;
    }

    public SettingsPagerAdapter(SettingsActivity activity) {
        this(activity, null);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ViewGroup view = ((ViewGroup) activity.getLayoutInflater().inflate(getPageLayoutResID(currentPage, position), container)).findViewById(getPageResID(currentPage, position));
        activity.setupTab(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        activity.clearTab((ViewGroup) object);
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        if (currentPage == null)
            return 0;
        switch (currentPage) {
            case DISPLAY:
                return 1;
            case SOUND:
                return 2;
            case SYSTEM:
                if (activity.isDevMode())
                    return 2;
                else
                    return 1;
            default:
                return 0;
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (currentPage) {
            case DISPLAY:
                return activity.getString(R.string.settings_tab_title0themeSelection);
            case SOUND:
                if (position == 0)
                    return activity.getString(R.string.settings_tab_title0general);
                else
                    return activity.getString(R.string.settings_header0equalizer);
            case SYSTEM:
                if (position == 0)
                    return activity.getString(R.string.settings_tab_title0general);
                else
                    return activity.getString(R.string.settings_tab_title0logs);
        }
        return "Error";
    }

    public void setPage(SettingsPage page) {
        currentPage = page;
        notifyDataSetChanged();
    }

    private int getPageLayoutResID(SettingsPage currentPage, int position) {
        switch (currentPage) {
            case DISPLAY:
                return R.layout.settings_tab_display_theme;
            case SOUND:
                if (position == 0)
                    return R.layout.settings_tab_audio_general;
                else
                    return R.layout.settings_tab_audio_equalizer;
            case SYSTEM:
                if (position == 0)
                    return R.layout.settings_tab_system_general;
                else
                    return R.layout.settings_tab_system_log;
        }
        throw new RuntimeException("Error");
    }

    private int getPageResID(SettingsPage currentPage, int position) {
        switch (currentPage) {
            case DISPLAY:
                if (position == 0)
                    return R.id.settings_tab_display_theme;
                else
                    return R.id.settings_tab_display_misc;
            case SOUND:
                if (position == 0)
                    return R.id.settings_tab_audio_general;
                else
                    return R.id.settings_tab_audio_equalizer;
            case SYSTEM:
                if (position == 0)
                    return R.id.settings_tab_system_general;
                else
                    return R.id.settings_tab_system_log;
        }
        throw new RuntimeException("Error");
    }
}