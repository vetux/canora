package com.phaseshifter.canora.ui.data.formatting;

import com.phaseshifter.canora.data.settings.IntegerSetting;

import java.io.Serializable;
import java.util.Objects;

public class SortDef implements Serializable {
    //SortBy
    public static final int SORT_TITLE = 0;
    public static final int SORT_ARTIST = 1;
    public static final int SORT_LENGTH = 2;
    public static final int SORT_FILENAME = 4;
    //SortDir
    public static final int SORT_DIR_UP = 0;
    public static final int SORT_DIR_DOWN = 1;
    //SortingTechnique
    public static final int SORT_TECH_ALPHA = 0;
    public static final int SORT_TECH_NUM = 1;

    public int sortby;
    public int sortdir;
    public int sorttech;

    public SortDef(int SortBy, int SortDir, int SortTech) {
        sortby = SortBy;
        sortdir = SortDir;
        sorttech = SortTech;
    }

    public SortDef() {
        this(IntegerSetting.SORT_BY.defaultValue, IntegerSetting.SORT_DIR.defaultValue, IntegerSetting.SORT_TECH.defaultValue);
    }

    public SortDef(SortDef copy) {
        this(copy.sortby, copy.sortdir, copy.sorttech);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortDef sortDef = (SortDef) o;
        return sortby == sortDef.sortby &&
                sortdir == sortDef.sortdir &&
                sorttech == sortDef.sorttech;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortby, sortdir, sorttech);
    }
}