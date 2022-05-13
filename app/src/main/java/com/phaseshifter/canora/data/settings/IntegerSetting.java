package com.phaseshifter.canora.data.settings;

import static com.phaseshifter.canora.ui.data.formatting.FilterDef.FILTER_TITLE;
import static com.phaseshifter.canora.ui.data.formatting.SortDef.*;

public enum IntegerSetting {
    FILTER_BY("INT_FLTR_BY", FILTER_TITLE),
    SORT_BY("INT_SRT_BY", SORT_TITLE),
    SORT_DIR("INT_SRT_DIR", SORT_DIR_DOWN),
    SORT_TECH("INT_SRT_TECH", SORT_TECH_ALPHA),
    THEME("INT_THM_ID", 0),
    EQUALIZER_PRESET_INDEX("INT_EQU_PRESET", 0),
    ;

    public final String key;
    public final int defaultValue;

    IntegerSetting(String key, int defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }
}