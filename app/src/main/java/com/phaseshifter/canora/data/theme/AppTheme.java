package com.phaseshifter.canora.data.theme;

import java.io.Serializable;
import java.util.Objects;

public class AppTheme implements Serializable {
    public final int id;
    public final int displayNameResID;
    public final int styleResID;
    public final int previewResID;

    public AppTheme(int id, int displayNameResID, int styleResID, int previewResID) {
        this.id = id;
        this.styleResID = styleResID;
        this.displayNameResID = displayNameResID;
        this.previewResID = previewResID;
    }

    public AppTheme(AppTheme copy) {
        this(copy.id, copy.displayNameResID, copy.styleResID, copy.previewResID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppTheme appTheme = (AppTheme) o;
        return id == appTheme.id &&
                displayNameResID == appTheme.displayNameResID &&
                styleResID == appTheme.styleResID &&
                previewResID == appTheme.previewResID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayNameResID, styleResID, previewResID);
    }

    @Override
    public String toString() {
        return "AppTheme{" +
                "id=" + id +
                ", displayNameResID=" + displayNameResID +
                ", styleResID=" + styleResID +
                ", previewResID=" + previewResID +
                '}';
    }
}