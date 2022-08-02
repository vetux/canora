package com.phaseshifter.canora.ui.data;

import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;

/**
 * The set of state saved in the view.
 * Modified by presenter and settings.
 */
public class StateBundle {
    public AppTheme theme;
    public boolean devMode;
    public FilterOptions filterOptions;
    public SortingOptions sortingOptions;
    public SelectionIndicator indicator;
}
