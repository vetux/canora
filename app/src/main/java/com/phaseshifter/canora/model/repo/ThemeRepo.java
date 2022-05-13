package com.phaseshifter.canora.model.repo;

import android.util.Log;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.data.theme.ThemeDefinition;
import com.phaseshifter.canora.data.theme.ThemeID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeRepo implements ThemeRepository {
    private final String LOG_TAG = "ThemeRepo";

    private final Map<Integer, AppTheme> themes;

    public ThemeRepo() {
        themes = new HashMap<>();

        HashMap<Integer, ThemeID> themeDefinition = ThemeDefinition.THEMES;

        if (themeDefinition.size() == 0) {
            Log.v(LOG_TAG, "No Themes in definition found.");
            return;
        }

        for (Map.Entry<Integer, ThemeID> entry : themeDefinition.entrySet()) {
            Integer key = entry.getKey();
            ThemeID theme = entry.getValue();
            Log.v(LOG_TAG, "Processing Theme: " + theme);
            if (theme.SKU == null) {
                themes.put(key, new AppTheme(key, theme.displayNameResID, theme.styleResID, theme.previewResID));
            } else {
                throw new RuntimeException("Not implemented");
            }
        }
    }

    @Override
    public List<AppTheme> getAll() {
        return new ArrayList<>(themes.values());
    }

    @Override
    public AppTheme get(int id) {
        return themes.get(id);
    }
}