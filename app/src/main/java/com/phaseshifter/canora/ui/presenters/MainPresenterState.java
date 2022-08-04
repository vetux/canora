package com.phaseshifter.canora.ui.presenters;

import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;

import java.io.Serializable;

public class MainPresenterState implements Serializable {
    public SelectionIndicator uiIndicator;
    public SelectionIndicator contentIndicator;
}
