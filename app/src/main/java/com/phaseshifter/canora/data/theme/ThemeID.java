package com.phaseshifter.canora.data.theme;

import java.util.Objects;

public class ThemeID {
    public final int styleResID;
    public final int displayNameResID;
    public final int previewResID;
    public final String SKU;

    public ThemeID(int displayNameResID, int styleResID, int previewResID, String SKU) {
        this.displayNameResID = displayNameResID;
        this.styleResID = styleResID;
        this.previewResID = previewResID;
        this.SKU = SKU;
    }

    public ThemeID(int displayNameResID, int styleResID, int previewResID) {
        this(displayNameResID, styleResID, previewResID, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThemeID themeID = (ThemeID) o;
        return styleResID == themeID.styleResID &&
                displayNameResID == themeID.displayNameResID &&
                previewResID == themeID.previewResID &&
                Objects.equals(SKU, themeID.SKU);
    }

    @Override
    public int hashCode() {
        return Objects.hash(styleResID, displayNameResID, previewResID, SKU);
    }

    @Override
    public String toString() {
        return "ThemeID{" +
                "styleResID=" + styleResID +
                ", displayNameResID=" + displayNameResID +
                ", previewResID=" + previewResID +
                ", SKU='" + SKU + '\'' +
                '}';
    }
}