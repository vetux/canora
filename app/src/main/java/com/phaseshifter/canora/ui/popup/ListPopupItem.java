package com.phaseshifter.canora.ui.popup;

import android.graphics.drawable.Drawable;

public class ListPopupItem {
    private final boolean isSubMenu;
    private final String title;
    private final Drawable icon;

    public ListPopupItem(boolean isSubMenu, String title, Drawable icon) {
        this.isSubMenu = isSubMenu;
        this.title = title;
        this.icon = icon;
    }

    public ListPopupItem(String title) {
        this(false, title, null);
    }

    public boolean isSubMenu() {
        return isSubMenu;
    }

    public String getTitle() {
        return title;
    }

    public Drawable getIcon() {
        return icon;
    }
}