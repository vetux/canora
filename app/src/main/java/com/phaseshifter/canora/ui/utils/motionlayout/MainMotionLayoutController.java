package com.phaseshifter.canora.ui.utils.motionlayout;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.utils.android.Miscellaneous;

/**
 * Responsible for controlling the motion layout.
 */
public class MainMotionLayoutController implements MotionLayout.TransitionListener {
    public static class LayoutState {
        public boolean showingPlaylists;
        public boolean searchMax;
        public boolean controlMax;

        public LayoutState(boolean showingPlaylists, boolean searchMax, boolean controlMax) {
            this.showingPlaylists = showingPlaylists;
            this.searchMax = searchMax;
            this.controlMax = controlMax;
        }

        public LayoutState(LayoutState copy) {
            this(copy.showingPlaylists, copy.searchMax, copy.controlMax);
        }

        public LayoutState() {
            this(false, false, false);
        }

        public void copy(LayoutState state) {
            showingPlaylists = state.showingPlaylists;
            searchMax = state.searchMax;
            controlMax = state.controlMax;
        }

        @Override
        public String toString() {
            return "LayoutState{" +
                    "showingPlaylists=" + showingPlaylists +
                    ", searchMax=" + searchMax +
                    ", controlMax=" + controlMax +
                    '}';
        }
    }

    private final String LOG_TAG = "MotionLayoutController";

    private final Context context;
    private final MainContract.Presenter presenter;

    private final MotionLayout layout;
    private final EditText searchText;

    private final ListView listView;
    private final GridView gridView;

    private final LayoutState state = new LayoutState();

    private int lastID = -1;

    private boolean visOverride = false;

    public MainMotionLayoutController(Context context,
                                      MainContract.Presenter presenter,
                                      MotionLayout layout,
                                      EditText searchText,
                                      ListView listView,
                                      GridView gridView) {
        this.context = context;
        this.presenter = presenter;
        this.layout = layout;
        this.searchText = searchText;
        this.listView = listView;
        this.gridView = gridView;

        this.layout.setTransitionListener(this);
    }

    public void showTracks() {
        if (this.state.showingPlaylists) {
            LayoutState state = new LayoutState(this.state);
            state.showingPlaylists = false;
            onLayoutStateChange(state);
        }
    }

    public void showPlaylists() {
        if (!this.state.showingPlaylists) {
            LayoutState state = new LayoutState(this.state);
            state.showingPlaylists = true;
            onLayoutStateChange(state);
        }
    }

    public void openSearch() {
        if (!this.state.searchMax) {
            LayoutState state = new LayoutState(this.state);
            state.searchMax = true;
            onLayoutStateChange(state);
        }
    }

    public void closeSearch() {
        if (this.state.searchMax) {
            LayoutState state = new LayoutState(this.state);
            state.searchMax = false;
            onLayoutStateChange(state);
        }
    }

    public void openControls() {
        if (!this.state.controlMax) {
            LayoutState state = new LayoutState(this.state);
            state.controlMax = true;
            onLayoutStateChange(state);
        }
    }

    public void closeControls() {
        if (this.state.controlMax) {
            LayoutState state = new LayoutState(this.state);
            state.controlMax = false;
            onLayoutStateChange(state);
        }
    }

    public LayoutState getState() {
        return new LayoutState(state);
    }

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startID, int endID) {
        Log.v(LOG_TAG, "onTransitionStarted " + motionLayout.hashCode() + " " + context.getResources().getResourceName(startID) + " " + context.getResources().getResourceName(endID));
        if (startID == R.id.state_tracks_default
                || startID == R.id.state_tracks_controlmax
                || startID == R.id.state_tracks_searchmax) {
            listView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }
        visOverride = false;
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout, int startID, int endID, float v) {
        //Log.v(LOG_TAG, "onTransitionChange " + motionLayout.hashCode() + " " + context.getResources().getResourceName(startID) + " " + context.getResources().getResourceName(endID) + " " + v);
        if (startID == R.id.state_playlists_default
                || endID == R.id.state_playlists_default
                && !visOverride) {
            listView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.VISIBLE);
            visOverride = true;
        } else {
            visOverride = false;
        }
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentID) {
        Log.v(LOG_TAG, "onTransitionCompleted " + motionLayout.hashCode() + " " + context.getResources().getResourceName(currentID));
        lastID = currentID;
        presenter.onTransportControlChange(currentID == R.id.state_playlists_controlmax
                || currentID == R.id.state_tracks_controlmax);
        presenter.onSearchChange(currentID == R.id.state_tracks_searchmax
                || currentID == R.id.state_playlists_searchmax);
        if (currentID == R.id.state_tracks_searchmax
                || currentID == R.id.state_playlists_searchmax) {
            searchText.requestFocus();
            searchText.selectAll();
            searchText.setCursorVisible(true);
            Miscellaneous.toggleKeyboardView(context, searchText, true);
        }
        if (currentID == R.id.state_tracks_default
                || currentID == R.id.state_tracks_controlmax
                || currentID == R.id.state_tracks_searchmax) {
            listView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }
        visOverride = false;
    }

    @Override
    public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) {
    }

    private void onLayoutStateChange(LayoutState state) {
        int prevID = lastID == -1 ? R.id.state_tracks_default : lastID;
        if (!state.showingPlaylists) {
            if (state.searchMax && state.controlMax) {
                layout.transitionToState(R.id.state_tracks_controlmax);
            } else if (state.searchMax) {
                layout.transitionToState(R.id.state_tracks_searchmax);
            } else {
                if (state.controlMax) {
                    if (prevID != R.id.state_tracks_controlmax) {
                        layout.setTransition(prevID, R.id.state_tracks_controlmax);
                        layout.transitionToEnd();
                    }
                    layout.transitionToState(R.id.state_tracks_controlmax);
                } else {
                    layout.transitionToState(R.id.state_tracks_default);
                }
            }
        } else {
            if (state.searchMax && state.controlMax) {
                layout.transitionToState(R.id.state_playlists_controlmax);
            } else if (state.searchMax) {
                layout.transitionToState(R.id.state_playlists_searchmax);
            } else {
                if (state.controlMax) {
                    layout.transitionToState(R.id.state_playlists_controlmax);
                } else {
                    layout.transitionToState(R.id.state_playlists_default);
                }
            }
        }
        if (searchText != null) {
            if (state.searchMax) {
                if (!this.state.searchMax) {
                    searchText.requestFocus();
                    searchText.selectAll();
                    searchText.setCursorVisible(true);
                    Miscellaneous.toggleKeyboardView(context, searchText, true);
                }
            } else {
                if (this.state.searchMax) {
                    searchText.clearFocus();
                    Miscellaneous.toggleKeyboardView(context, searchText, false);
                }
            }
        }
        this.state.copy(state);
    }
}