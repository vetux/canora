package com.phaseshifter.canora.ui.arrayadapters;

import android.graphics.drawable.Drawable;

public class SettingAdapterItem {
    private final String title;
    private final String subTitle;
    private final Drawable icon;

    public SettingAdapterItem(String title, String subTitle, Drawable icon) {
        this.title = title;
        this.subTitle = subTitle;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public Drawable getIcon() {
        return icon;
    }
}