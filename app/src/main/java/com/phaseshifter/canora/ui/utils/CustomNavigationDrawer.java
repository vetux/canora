package com.phaseshifter.canora.ui.utils;

import android.view.View;
import android.view.ViewGroup;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.ui.data.MainPage;

public class CustomNavigationDrawer {
    private final ViewGroup root;

    public interface OnItemClickListener {
        boolean onNavigationItemSelected(View view);
    }

    public CustomNavigationDrawer(ViewGroup layout) {
        root = layout;
    }

    //Unselect all views and select the supplied one
    public void setCheckedId(int resID) {
        for (int i = 0; i < root.getChildCount(); i++) {
            root.getChildAt(i).setSelected(false);
        }
        root.findViewById(resID).setSelected(true);
    }

    public void setCheckedSelector(MainPage selector) {
        for (int i = 0; i < root.getChildCount(); i++) {
            root.getChildAt(i).setSelected(false);
        }
        switch (selector) {
            case TRACKS:
                root.findViewById(R.id.nav_button_tracks).setSelected(true);
                break;
            case PLAYLISTS:
                root.findViewById(R.id.nav_button_playlists).setSelected(true);
                break;
            case ARTISTS:
                root.findViewById(R.id.nav_button_artists).setSelected(true);
                break;
            case ALBUMS:
                root.findViewById(R.id.nav_button_albums).setSelected(true);
                break;
            case GENRES:
                root.findViewById(R.id.nav_button_genres).setSelected(true);
                break;
            case SOUNDCLOUD_SEARCH:
                root.findViewById(R.id.nav_button_soundcloud_search).setSelected(true);
                break;
            case SOUNDCLOUD_CHARTS:
                root.findViewById(R.id.nav_button_soundcloud_charts).setSelected(true);
                break;
        }
    }

    public void setOnClickListener(OnItemClickListener listener) {
        for (int i = 0; i < root.getChildCount(); i++) {
            root.getChildAt(i).setOnClickListener(listener::onNavigationItemSelected);
        }
    }

    public void clearOnClickListener() {
        for (int i = 0; i < root.getChildCount(); i++) {
            root.getChildAt(i).setOnClickListener(null);
        }
    }
}