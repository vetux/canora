package com.phaseshifter.canora.data.theme;

import com.phaseshifter.canora.R;

import java.util.HashMap;

public class ThemeDefinition {
    public static final HashMap<Integer, ThemeID> THEMES = new HashMap<Integer, ThemeID>() {{
        //  put(99, new ThemeID("Debug", "1"));
        put(0, new ThemeID(R.string.special0themename_dark, R.style.Dark, R.drawable.themepreview_dark));
        put(1, new ThemeID(R.string.special0themename_light, R.style.Light, R.drawable.themepreview_light));
        put(2, new ThemeID(R.string.special0themename_marineblue, R.style.MarineBlue, R.drawable.themepreview_marineblue));
        put(3, new ThemeID(R.string.special0themename_camo, R.style.Camo, R.drawable.texture_camo));
    }};
}