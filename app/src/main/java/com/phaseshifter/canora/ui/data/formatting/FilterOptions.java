package com.phaseshifter.canora.ui.data.formatting;

import java.io.Serializable;
import java.util.Objects;

public class FilterOptions implements Serializable {
    //SearchBy
    public static final int FILTER_TITLE = 0;
    public static final int FILTER_ARTIST = 1;
    public static final int FILTER_FILENAME = 2;
    public static final int FILTER_GENRE = 3;
    public static final int FILTER_ALBUM = 4;
    public static final int FILTER_TITLE_ARTIST = 5;
    public static final int FILTER_ANY = 6;

    public int filterBy;
    public String filterFor;

    public FilterOptions(int filterBy, String filterFor) {
        this.filterFor = filterFor;
        this.filterBy = filterBy;
    }

    public FilterOptions(FilterOptions copy) {
        this(copy.filterBy, copy.filterFor);
    }

    public FilterOptions() {
        this(0, "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterOptions filterDef = (FilterOptions) o;
        return filterBy == filterDef.filterBy &&
                Objects.equals(filterFor, filterDef.filterFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filterBy, filterFor);
    }
}