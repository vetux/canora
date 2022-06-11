package com.phaseshifter.canora.ui.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.RecoverableSecurityException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.editor.AudioMetadataMask;
import com.phaseshifter.canora.model.editor.JaudioTaggerEditor;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.model.repo.SettingsRepo;
import com.phaseshifter.canora.model.repo.ThemeRepo;
import com.phaseshifter.canora.service.wrapper.AutoBindingServiceWrapper;
import com.phaseshifter.canora.ui.activities.editors.AudioDataEditorActivity;
import com.phaseshifter.canora.ui.activities.editors.AudioPlaylistEditorActivity;
import com.phaseshifter.canora.ui.arrayadapters.AudioDataArrayAdapter;
import com.phaseshifter.canora.ui.arrayadapters.AudioPlaylistArrayAdapter;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;
import com.phaseshifter.canora.ui.data.formatting.SortDef;
import com.phaseshifter.canora.ui.menu.AddToMenuListener;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.ui.presenters.MainPresenter;
import com.phaseshifter.canora.ui.redux.core.StateListener;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;
import com.phaseshifter.canora.ui.utils.CustomNavigationDrawer;
import com.phaseshifter.canora.ui.utils.dialog.MainDialogFactory;
import com.phaseshifter.canora.ui.utils.motionlayout.MainMotionLayoutController;
import com.phaseshifter.canora.ui.utils.popup.ListPopupFactory;
import com.phaseshifter.canora.ui.viewmodels.ContentViewModel;
import com.phaseshifter.canora.ui.viewmodels.PlayerStateViewModel;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.android.AttributeConversion;
import com.phaseshifter.canora.utils.android.metrics.AndroidFPSMeter;
import com.phaseshifter.canora.utils.android.metrics.AndroidMemoryMeter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.phaseshifter.canora.utils.IntegerConversion.safeLongToInt;
import static com.phaseshifter.canora.utils.android.Miscellaneous.leftpadZero;

//TODO: Implement ACTION_VIEW intent handling
public class MainActivity extends Activity implements MainContract.View,
        SeekBar.OnSeekBarChangeListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        AbsListView.OnScrollListener,
        View.OnClickListener,
        TextWatcher,
        CustomNavigationDrawer.OnItemClickListener {
    public static final int REQUESTCODE_PERMISSIONS = 0;
    public static final int REQUESTCODE_PERMISSIONS_SCOPEDSTORAGE = 1;
    public static final int REQUESTCODE_EDIT_AUDIODATA = 2;
    public static final int REQUESTCODE_EDIT_AUDIOPLAYLIST = 3;

    private static final String BUNDLE_PRESENTERSTATE = "REDX";

    private final String LOG_TAG = "MainActivity";

    private final AndroidFPSMeter fpsMeter = new AndroidFPSMeter();
    private final AndroidMemoryMeter memoryMeter = new AndroidMemoryMeter();

    private ContentViewModel contentViewModel;
    private PlayerStateViewModel playerStateViewModel;

    private MainContract.Presenter presenter;

    private AutoBindingServiceWrapper serviceWrapper;

    private AudioDataArrayAdapter trackAdapter;
    private AudioPlaylistArrayAdapter playlistAdapter;

    private ValueAnimator animator;

    private MainMotionLayoutController motionLayoutController;

    private ContentObserver externalContentObserver;

    private View lastItemLongClickView = null;
    private final int[] lastTouchCoords = new int[2];

    private Runnable scopedStorageCallback;

    private Timer searchTimer = new Timer();
    private final long SEARCH_FINISHED_DELAY = 1000; // milliseconds

    private int trackListViewScrollState = SCROLL_STATE_IDLE;

    //START Android Interfaces

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        Serializable savedState = savedInstanceState == null
                ? null
                : savedInstanceState.getSerializable(BUNDLE_PRESENTERSTATE);
        serviceWrapper = new AutoBindingServiceWrapper(this);
        MainApplication application = (MainApplication) getApplication();
        contentViewModel = new ContentViewModel(this, application.getAudioDataRepo(), application.getAudioPlaylistRepository(), application.getScAudioRepository());
        playerStateViewModel = new PlayerStateViewModel(this);
        setViewModelListeners(contentViewModel, playerStateViewModel);
        List<StateListener<MainStateImmutable>> viewModels = new ArrayList<>();
        viewModels.add(contentViewModel);
        viewModels.add(playerStateViewModel);
        presenter = new MainPresenter(this,
                savedState,
                serviceWrapper,
                application.getAudioDataRepo(),
                application.getAudioPlaylistRepository(),
                new SettingsRepo(this),
                new ThemeRepo(),
                application.getScAudioRepository(),
                new JaudioTaggerEditor(this),
                this::runOnUiThread,
                viewModels
        );
        trackAdapter = new AudioDataArrayAdapter(this, new ArrayList<>());
        playlistAdapter = new AudioPlaylistArrayAdapter(this, new ArrayList<>());
        externalContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                presenter.onMediaStoreDataChange();
            }
        };
    }

    @Override
    protected void onStart() {
        Log.v(LOG_TAG, "onStart");
        super.onStart();
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, externalContentObserver);
        presenter.start();
    }

    @Override
    protected void onStop() {
        Log.v(LOG_TAG, "onStop");
        getContentResolver().unregisterContentObserver(externalContentObserver);
        presenter.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");
        serviceWrapper.unbind();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.v(LOG_TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(LOG_TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.v(LOG_TAG, "onRestart");
        super.onRestart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.v(LOG_TAG, "onRequestPermissionsResult " + requestCode + " " + Arrays.toString(permissions) + " " + Arrays.toString(grantResults));
        switch (requestCode) {
            case REQUESTCODE_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.onPermissionRequestResult(true);
                } else {
                    presenter.onPermissionRequestResult(false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        boolean drawerOpen = ((DrawerLayout) findViewById(R.id.drawerLayout)).isDrawerOpen(GravityCompat.START);
        if (drawerOpen)
            ((DrawerLayout) findViewById(R.id.drawerLayout)).closeDrawer(GravityCompat.START);
        else
            presenter.onBackPress();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(BUNDLE_PRESENTERSTATE, presenter.saveState());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MainApplication application = (MainApplication) getApplication();
        switch (requestCode) {
            case REQUESTCODE_PERMISSIONS_SCOPEDSTORAGE:
                if (resultCode == Activity.RESULT_OK)
                    scopedStorageCallback.run();
                scopedStorageCallback = null;
                break;
            case REQUESTCODE_EDIT_AUDIODATA:
                AudioData track = (AudioData) application.getBundle(AudioDataEditorActivity.BUNDLE_OUTPUT);
                application.removeBundle(AudioDataEditorActivity.BUNDLE_INPUT);
                application.removeBundle(AudioDataEditorActivity.BUNDLE_OUTPUT);
                switch (resultCode) {
                    case AudioDataEditorActivity.RESULTCODE_EDIT:
                        presenter.onEditorResult(track, false, false, false);
                        break;
                    case AudioDataEditorActivity.RESULTCODE_DELETE:
                        presenter.onEditorResult(track, false, false, true);
                        break;
                    case AudioDataEditorActivity.RESULTCODE_CANCEL:
                        presenter.onEditorResult(track, false, true, false);
                        break;
                    case AudioDataEditorActivity.RESULTCODE_ERROR:
                        presenter.onEditorResult(track, true, false, false);
                        break;
                }
                break;
            case REQUESTCODE_EDIT_AUDIOPLAYLIST:
                AudioPlaylist playlist = (AudioPlaylist) application.getBundle(AudioPlaylistEditorActivity.BUNDLE_OUTPUT);
                application.removeBundle(AudioPlaylistEditorActivity.BUNDLE_INPUT);
                application.removeBundle(AudioPlaylistEditorActivity.BUNDLE_OUTPUT);
                switch (resultCode) {
                    case AudioPlaylistEditorActivity.RESULTCODE_EDIT:
                        presenter.onEditorResult(playlist, false, false, false);
                        break;
                    case AudioPlaylistEditorActivity.RESULTCODE_DELETE:
                        presenter.onEditorResult(playlist, false, false, true);
                        break;
                    case AudioPlaylistEditorActivity.RESULTCODE_CANCEL:
                        presenter.onEditorResult(playlist, false, true, false);
                        break;
                    case AudioPlaylistEditorActivity.RESULTCODE_ERROR:
                        presenter.onEditorResult(playlist, true, false, false);
                        break;
                }
                break;
            default:
                Log.v(LOG_TAG, "Unrecognized request code: " + requestCode);
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastTouchCoords[0] = (int) ev.getRawX();
            lastTouchCoords[1] = (int) ev.getRawY();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (view.getId()) {
            case R.id.display_listview_tracks:
                trackListViewScrollState = scrollState;
                break;
            case R.id.display_gridview_playlists:
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        switch (view.getId()) {
            case R.id.display_listview_tracks:
                if ((visibleItemCount == (totalItemCount - firstVisibleItem) && trackListViewScrollState > 0)) {
                    presenter.onTrackContentScrollToBottom();
                }
                break;
            case R.id.display_gridview_playlists:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.display_listview_tracks:
                presenter.onTrackContentClick(position);
                break;
            case R.id.display_gridview_playlists:
                presenter.onPlaylistContentClick(position);
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.display_listview_tracks:
                lastItemLongClickView = view;
                presenter.onTrackContentLongClick(position);
                break;
            case R.id.display_gridview_playlists:
                lastItemLongClickView = view;
                presenter.onPlaylistContentLongClick(position);
                break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            switch (seekBar.getId()) {
                case R.id.control_seekbar_progressdynamic:
                    presenter.onTrackSeek((float) progress / seekBar.getMax());
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.control_seekbar_progressdynamic:
                presenter.onTrackSeekStart();
                break;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.control_seekbar_progressdynamic:
                presenter.onTrackSeekStop();
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        EditText searchText = findViewById(R.id.toolbar_edittext_search);
        if (s.hashCode() == searchText.getText().hashCode()) {
            presenter.onSearchTextChange(s.toString());
        } else {
            throw new RuntimeException("TextWatcher received unrecognized CharSequence: " + s);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        searchTimer.cancel();
        searchTimer = new Timer();
        searchTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            presenter.onSearchTextEditingFinished();
                        });
                    }
                },
                SEARCH_FINISHED_DELAY
        );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.control_button_prev:
                presenter.onPrev();
                break;
            case R.id.control_button_play:
                presenter.onPlay();
                break;
            case R.id.control_button_next:
                presenter.onNext();
                break;
            case R.id.control_button_shuffle:
                presenter.onShuffleSwitch();
                break;
            case R.id.control_button_repeat:
                presenter.onRepeatSwitch();
                break;
            case R.id.toolbar_button_nav:
                presenter.onNavigationButtonClick();
                break;
            case R.id.toolbar_button_menu:
                presenter.onOptionsButtonClick();
                break;
            case R.id.toolbar_button_search:
                presenter.onSearchButtonClick();
                break;
            case R.id.control_button_volume:
                presenter.onVolumeButtonClick();
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(View view) {
        switch (view.getId()) {
            case R.id.nav_button_tracks:
                presenter.onNavigationClick(NavigationItem.TRACKS);
                break;
            case R.id.nav_button_playlists:
                presenter.onNavigationClick(NavigationItem.PLAYLISTS);
                break;
            case R.id.nav_button_albums:
                presenter.onNavigationClick(NavigationItem.ALBUMS);
                break;
            case R.id.nav_button_artists:
                presenter.onNavigationClick(NavigationItem.ARTISTS);
                break;
            case R.id.nav_button_genres:
                presenter.onNavigationClick(NavigationItem.GENRES);
                break;
            case R.id.nav_button_soundcloud_search:
                presenter.onNavigationClick(NavigationItem.SOUNDCLOUD_SEARCH);
                break;
            case R.id.nav_button_soundcloud_charts:
                presenter.onNavigationClick(NavigationItem.SOUNDCLOUD_CHARTS);
                break;
            case R.id.nav_button_settings:
                presenter.onNavigationClick(NavigationItem.SETTINGS);
                break;
            case R.id.nav_button_rate:
                presenter.onNavigationClick(NavigationItem.RATE);
                break;
            case R.id.nav_button_info:
                presenter.onNavigationClick(NavigationItem.INFO);
                break;
        }
        return true;
    }

    //END Android Interfaces

    //START View interface

    @Override
    public void setTheme(AppTheme theme) {
        runOnUiThread(() -> {
            if (theme != null) {
                setTheme(theme.styleResID);
                setContentView(R.layout.activity_main);
                setListeners();
            }
        });
    }

    @Override
    public void setDebugDisplay(boolean debugDisplay) {
        runOnUiThread(() -> {
            fpsMeter.reset();
            memoryMeter.reset();
            ViewGroup view = findViewById(R.id.debugViews);
            if (view != null) {
                if (debugDisplay) {
                    fpsMeter.startMeasure();
                    view.setVisibility(View.VISIBLE);
                    fpsMeter.startPrint(100, view.findViewById(R.id.debugTextFPS));
                    memoryMeter.startPrint(100, view.findViewById(R.id.debugTextMemory), this);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void setSearchMax(boolean searchMax) {
        runOnUiThread(() -> {
            if (motionLayoutController != null) {
                if (searchMax)
                    motionLayoutController.openSearch();
                else
                    motionLayoutController.closeSearch();
            }
        });
    }

    @Override
    public void setControlMax(boolean controlMax) {
        runOnUiThread(() -> {
            if (motionLayoutController != null) {
                if (controlMax)
                    motionLayoutController.openControls();
                else
                    motionLayoutController.closeControls();
            }
        });
    }

    @Override
    public void setNavigationMax(boolean navigationMax) {
        runOnUiThread(() -> {
            DrawerLayout view = findViewById(R.id.drawerLayout);
            if (view != null) {
                if (navigationMax) {
                    view.openDrawer(GravityCompat.START);
                } else {
                    view.closeDrawer(GravityCompat.START);
                }
            }
        });
    }

    @Override
    public void showTrackContent() {
        runOnUiThread(() -> {
            if (motionLayoutController != null) {
                motionLayoutController.showTracks();
            }
        });
    }

    @Override
    public void showPlaylistContent() {
        runOnUiThread(() -> {
            if (motionLayoutController != null) {
                motionLayoutController.showPlaylists();
            }
        });
    }

    @Override
    public void showTrackContentDetails(int index) {
        runOnUiThread(() -> {
        });
    }

    @Override
    public void showPlaylistContentDetails(int index) {
        runOnUiThread(() -> {
        });
    }

    @Override
    public void showMenuTrackContent(int index, ContextMenu menu) {
        runOnUiThread(() -> {
            int[] topLeftPositionOfItemView = new int[2];
            lastItemLongClickView.getLocationOnScreen(topLeftPositionOfItemView);
            int offsetx = lastTouchCoords[0] - topLeftPositionOfItemView[0];
            int offsety = (lastTouchCoords[1] - topLeftPositionOfItemView[1]) - lastItemLongClickView.getHeight();
            ListPopupWindow popup = ListPopupFactory.getContextMenu(
                    this,
                    lastItemLongClickView,
                    offsetx,
                    offsety,
                    menu,
                    new ContextMenu.ContextMenuListener() {
                        @Override
                        public void onAction(ContextMenu.Action action) {
                            presenter.onMenuAction(index, action, menu);
                        }
                    });
            popup.show();
        });
    }

    @Override
    public void showMenuPlaylistContent(int index, ContextMenu menu) {
        runOnUiThread(() -> {
            int[] topLeftPositionOfItemView = new int[2];
            lastItemLongClickView.getLocationOnScreen(topLeftPositionOfItemView);
            int offsetx = lastTouchCoords[0] - topLeftPositionOfItemView[0];
            int offsety = (lastTouchCoords[1] - topLeftPositionOfItemView[1]) - lastItemLongClickView.getHeight();
            ListPopupWindow popup = ListPopupFactory.getContextMenu(
                    this,
                    lastItemLongClickView,
                    offsetx,
                    offsety,
                    menu,
                    new ContextMenu.ContextMenuListener() {
                        @Override
                        public void onAction(ContextMenu.Action action) {
                            presenter.onMenuAction(index, action, menu);
                        }
                    });
            popup.show();
        });
    }

    @Override
    public void showMenuOptions(OptionsMenu menu) {
        runOnUiThread(() -> {
            ListPopupWindow popup = ListPopupFactory.getOptionsMenu(this,
                    findViewById(R.id.toolbar_view_menuanchor),
                    25,
                    -25,
                    menu,
                    new OptionsMenu.OptionsMenuListener() {
                        @Override
                        public void onAction(OptionsMenu.Action action) {
                            presenter.onMenuAction(action, menu);
                        }
                    });
            popup.show();
        });
    }

    @Override
    public void showMenuAddSelectionToPlaylist(boolean showCreateNew, List<AudioPlaylist> existingPlaylists, AddToMenuListener listener) {
        runOnUiThread(() -> {
            showAddToPlaylistMenu(findViewById(R.id.toolbar_view_menuanchor),
                    25,
                    -25,
                    0,
                    showCreateNew,
                    existingPlaylists,
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (showCreateNew) {
                                if (position == 0)
                                    listener.onAddToNew();
                                else
                                    listener.onAddToExisting(position - 1);
                            } else {
                                listener.onAddToExisting(position);
                            }
                        }
                    });
        });
    }

    @Override
    public void showMessage(String title, String text) {
        runOnUiThread(() -> {
            Toast.makeText(this, title + " : " + text, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_createdPlaylist(String playlistTitle, int playlistTracks) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0createdPlaylist, playlistTitle, playlistTracks), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_deletedPlaylist(String title) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0deletedPlaylist, title), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_deletedPlaylists(int count) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0deletedPlaylists, count), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_deletedTracks(int count) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0deletedItems, count), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_deletedTracksFrom(String playlistTitle, int count) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0deletedItemsFrom, count, playlistTitle), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showMessage_addedTracks(String playlistTarget, int count) {
        runOnUiThread(() -> {
            Toast.makeText(this, getString(R.string.main_toast_text0addedItems, count, playlistTarget), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showWarning(String text) {
        runOnUiThread(() -> {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showError(String text) {
        runOnUiThread(() -> {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void showDialog_error_permissions() {
        runOnUiThread(() -> {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle(getString(R.string.main_dialog_error_title0error));
            ab.setMessage(getString(R.string.main_dialog_error_text0permissionDenied));
            ab.setPositiveButton(getString(android.R.string.ok), null);
            AlertDialog ad = ab.create();
            ad.show();
        });
    }

    @Override
    public void showDialog_Exit() {
        runOnUiThread(() -> {
            Dialog dia = MainDialogFactory.getExitConfirmation(this, new MainDialogFactory.ExitConfirmationListener() {
                @Override
                public void onRequestMinimize() {
                    moveTaskToBack(true);
                }

                @Override
                public void onRequestExit() {
                    serviceWrapper.shutdown();
                    finish();
                }

                @Override
                public void onCancel() {
                }
            });
            dia.show();
        });
    }

    @Override
    public void showDialog_FilterOptions(FilterDef curDef, MainDialogFactory.FilterOptionsListener listener) {
        runOnUiThread(() -> {
            Dialog dia = MainDialogFactory.getFilterOptions(this, listener, curDef);
            dia.show();
        });
    }

    @Override
    public void showDialog_SortOptions(SortDef curDef, MainDialogFactory.SortingOptionsListener listener) {
        runOnUiThread(() -> {
            Dialog dia = MainDialogFactory.getSortingOptions(this, listener, curDef);
            dia.show();
        });
    }

    @Override
    public void showDialog_CreatePlaylist(List<AudioData> data, MainDialogFactory.PlaylistCreateListener listener) {
        runOnUiThread(() -> {
            Dialog dia = MainDialogFactory.getPlaylistCreate(this, listener, data);
            dia.show();
        });
    }

    @Override
    public void showDialog_DeletePlaylists(List<AudioPlaylist> playlists, MainDialogFactory.DeletePlaylistsListener listener) {
        Dialog dia = MainDialogFactory.getPlaylistsDelete(this, listener, playlists.size());
        dia.show();
    }

    @Override
    public void showDialog_DeleteTracksFromPlaylist(AudioPlaylist playlist, List<AudioData> tracks, MainDialogFactory.DeleteTracksFromPlaylistListener listener) {
        Dialog dia = MainDialogFactory.getTracksDeleteFromPlaylist(this, listener, tracks.size());
        dia.show();
    }

    @Override
    public void showDialog_volume(float currentValue) {
        Dialog dia = MainDialogFactory.getVolumeSettings(this, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    presenter.onVolumeSeek((float) progress / seekBar.getMax());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        }, currentValue);
        dia.show();
    }

    @Override
    public void startEditor(AudioData data, AudioMetadataMask mask, AppTheme theme) {
        runOnUiThread(() -> {
            AudioDataEditorActivity.ActivityBundle bundle = new AudioDataEditorActivity.ActivityBundle(theme, data, mask);
            MainApplication application = (MainApplication) getApplication();
            application.putBundle(AudioDataEditorActivity.BUNDLE_INPUT, bundle);
            startActivityForResult(new Intent(this, AudioDataEditorActivity.class), REQUESTCODE_EDIT_AUDIODATA);
        });
    }

    @Override
    public void startEditor(AudioPlaylist data, AppTheme theme) {
        runOnUiThread(() -> {
            AudioPlaylistEditorActivity.ActivityBundle bundle = new AudioPlaylistEditorActivity.ActivityBundle(theme, data);
            MainApplication application = (MainApplication) getApplication();
            application.putBundle(AudioPlaylistEditorActivity.BUNDLE_INPUT, bundle);
            startActivityForResult(new Intent(this, AudioPlaylistEditorActivity.class), REQUESTCODE_EDIT_AUDIOPLAYLIST);
        });
    }

    @Override
    public void startSettings() {
        runOnUiThread(() -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    @Override
    public void startInfo() {
        runOnUiThread(() -> {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
    }

    @Override
    public void startRate() {
        runOnUiThread(() -> {
            final String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        });
    }

    @Override
    public void checkPermissions() {
        runOnUiThread(() -> {
            presenter.onPermissionCheckResult(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        });
    }

    @Override
    public void requestPermissions() {
        runOnUiThread(() -> {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.REQUESTCODE_PERMISSIONS);
        });
    }

    @Override
    public void handleSecurityException(SecurityException exception, Runnable onSuccess) {
        runOnUiThread(() -> {
            if (exception instanceof RecoverableSecurityException) {
                IntentSender intentSender = ((RecoverableSecurityException) exception).getUserAction().getActionIntent().getIntentSender();
                try {
                    scopedStorageCallback = onSuccess;
                    startIntentSenderForResult(intentSender, REQUESTCODE_PERMISSIONS_SCOPEDSTORAGE, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e0) {
                    e0.printStackTrace();
                    scopedStorageCallback = null;
                }
            }
        });
    }

    //END View Interface

    private void setListeners() {
        List<ImageButton> imageButtons = new ArrayList<>();
        imageButtons.add(findViewById(R.id.control_button_prev));
        imageButtons.add(findViewById(R.id.control_button_play));
        imageButtons.add(findViewById(R.id.control_button_next));
        imageButtons.add(findViewById(R.id.control_button_shuffle));
        imageButtons.add(findViewById(R.id.control_button_repeat));
        imageButtons.add(findViewById(R.id.toolbar_button_search));
        imageButtons.add(findViewById(R.id.control_button_volume));
        imageButtons.add(findViewById(R.id.toolbar_button_menu));
        imageButtons.add(findViewById(R.id.display_button_floating_addto));
        imageButtons.add(findViewById(R.id.toolbar_button_nav));

        for (ImageButton button : imageButtons) {
            button.setOnClickListener(this);
        }

        EditText searchText = findViewById(R.id.toolbar_edittext_search);
        searchText.addTextChangedListener(this);

        CustomNavigationDrawer drawer = new CustomNavigationDrawer(findViewById(R.id.nav_content_root));
        drawer.setOnClickListener(this);

        ListView listView = findViewById(R.id.display_listview_tracks);
        GridView gridView = findViewById(R.id.display_gridview_playlists);

        listView.setOnScrollListener(this);
        gridView.setOnScrollListener(this);

        listView.setAdapter(trackAdapter);
        gridView.setAdapter(playlistAdapter);

        listView.setOnItemClickListener(this);
        gridView.setOnItemClickListener(this);

        listView.setOnItemLongClickListener(this);
        gridView.setOnItemLongClickListener(this);

        SeekBar progressSeekBar = findViewById(R.id.control_seekbar_progressdynamic);
        progressSeekBar.setOnSeekBarChangeListener(this);

        SeekBar staticSeekBar = findViewById(R.id.control_seekbar_progressstatic);
        staticSeekBar.setOnTouchListener((v, event) -> true);

        MotionLayout motionLayout = findViewById(R.id.motion_layout_main);
        motionLayoutController = new MainMotionLayoutController(this, presenter, motionLayout, searchText, listView, gridView);

        ProgressBar playbackLoad = findViewById(R.id.control_progressbar_playbackload);
        playbackLoad.setVisibility(View.GONE);
    }

    private void setViewModelListeners(ContentViewModel contentViewModel, PlayerStateViewModel playerStateViewModel) {
        contentViewModel.contentName.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                TextView title = findViewById(R.id.toolbar_textview_title);
                if (title != null) {
                    title.setText(value);
                    title.setSelected(true);
                }
            }
        });
        contentViewModel.isContentLoading.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                ProgressBar pb = findViewById(R.id.toolbar_progressbar_contentload);
                if (pb != null) {
                    pb.setVisibility(value ? View.VISIBLE : View.GONE);
                }
            }
        });
        contentViewModel.isSelecting.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                trackAdapter.setSelectionMode(value);
                trackAdapter.notifyDataSetChanged();
                playlistAdapter.setSelectionMode(value);
                playlistAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.visibleTracks.addObserver(new Observable.Observer<List<AudioData>>() {
            @Override
            public void update(Observable<List<AudioData>> observable, List<AudioData> value) {
                trackAdapter.getContentRef().clear();
                trackAdapter.getContentRef().addAll(value);
                trackAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.contentTracksHighlight.addObserver(new Observable.Observer<Integer>() {
            @Override
            public void update(Observable<Integer> observable, Integer value) {
                trackAdapter.setHighlightedIndex(value);
                trackAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.contentTracksSelection.addObserver(new Observable.Observer<HashSet<Integer>>() {
            @Override
            public void update(Observable<HashSet<Integer>> observable, HashSet<Integer> value) {
                trackAdapter.setSelectionIndex(value);
                trackAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.visiblePlaylists.addObserver(new Observable.Observer<List<AudioPlaylist>>() {
            @Override
            public void update(Observable<List<AudioPlaylist>> observable, List<AudioPlaylist> value) {
                playlistAdapter.getContentRef().clear();
                playlistAdapter.getContentRef().addAll(value);
                playlistAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.contentPlaylistHighlight.addObserver(new Observable.Observer<Integer>() {
            @Override
            public void update(Observable<Integer> observable, Integer value) {
                playlistAdapter.setHighlightedIndex(value);
                playlistAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.contentPlaylistsSelection.addObserver(new Observable.Observer<HashSet<Integer>>() {
            @Override
            public void update(Observable<HashSet<Integer>> observable, HashSet<Integer> value) {
                playlistAdapter.setSelectionIndex(value);
                playlistAdapter.notifyDataSetChanged();
            }
        });
        contentViewModel.navigationHighlightPosition.addObserver(new Observable.Observer<AudioContentSelector>() {
            @Override
            public void update(Observable<AudioContentSelector> observable, AudioContentSelector value) {
                ViewGroup drawerItems = findViewById(R.id.nav_content_root);
                if (drawerItems != null) {
                    CustomNavigationDrawer drawer = new CustomNavigationDrawer(drawerItems);
                    drawer.setCheckedSelector(value);
                }
            }
        });
        contentViewModel.searchText.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                EditText searchText = findViewById(R.id.toolbar_edittext_search);
                if (searchText != null) {
                    searchText.removeTextChangedListener(MainActivity.this);
                    searchText.setText(value);
                    searchText.addTextChangedListener(MainActivity.this);
                    if (searchText.isFocused()) {
                        searchText.setSelection(searchText.getText().length());
                    }
                }
            }
        });
        contentViewModel.notFoundText.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                TextView notFoundText = findViewById(R.id.display_text_notfound);
                if (notFoundText != null) {
                    notFoundText.setVisibility(value == null ? View.GONE : View.VISIBLE);
                    notFoundText.setText(value);
                }
            }
        });
        playerStateViewModel.buffering.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                ProgressBar buf = findViewById(R.id.control_progressbar_playbackload);
                if (buf != null) {
                    buf.setVisibility(value ? View.VISIBLE : View.GONE);
                }
            }
        });
        playerStateViewModel.trackText.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                TextView text = findViewById(R.id.control_text_songtext);
                if (text != null) {
                    text.setText(value);
                    text.setSelected(true);
                }
            }
        });
        playerStateViewModel.trackTitle.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                TextView text = findViewById(R.id.control_text_title);
                if (text != null) {
                    text.setText(value);
                    text.setSelected(true);
                }
            }
        });
        playerStateViewModel.trackArtist.addObserver(new Observable.Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                TextView text = findViewById(R.id.control_text_artist);
                if (text != null) {
                    text.setText(value);
                    text.setSelected(true);
                }
            }
        });
        playerStateViewModel.trackArtwork.addObserver(new Observable.Observer<Bitmap>() {
            @Override
            public void update(Observable<Bitmap> observable, Bitmap value) {
                ImageView cover = findViewById(R.id.control_imageview_cover);
                if (cover != null) {
                    cover.setImageBitmap(value);
                }
            }
        });
        playerStateViewModel.trackLength.addObserver(new Observable.Observer<Long>() {
            @Override
            public void update(Observable<Long> observable, Long value) {
                SeekBar seekBarDraggable = findViewById(R.id.control_seekbar_progressdynamic);
                SeekBar seekBarStatic = findViewById(R.id.control_seekbar_progressstatic);
                TextView digit0 = findViewById(R.id.control_text_digit0);
                TextView digit1 = findViewById(R.id.control_text_digit1);
                if (seekBarDraggable != null
                        && seekBarStatic != null
                        && digit0 != null
                        && digit1 != null)
                    animator = refreshValueAnimators(
                            animator,
                            playerStateViewModel.isPlaying.get(),
                            playerStateViewModel.trackPosition.get(),
                            value,
                            seekBarDraggable,
                            seekBarStatic,
                            digit0,
                            digit1);
            }
        });
        playerStateViewModel.trackPosition.addObserver(new Observable.Observer<Long>() {
            @Override
            public void update(Observable<Long> observable, Long value) {
                SeekBar seekBarDraggable = findViewById(R.id.control_seekbar_progressdynamic);
                SeekBar seekBarStatic = findViewById(R.id.control_seekbar_progressstatic);
                TextView digit0 = findViewById(R.id.control_text_digit0);
                TextView digit1 = findViewById(R.id.control_text_digit1);
                if (seekBarDraggable != null
                        && seekBarStatic != null
                        && digit0 != null
                        && digit1 != null)
                    animator = refreshValueAnimators(
                            animator,
                            playerStateViewModel.isPlaying.get(),
                            value,
                            playerStateViewModel.trackLength.get(),
                            seekBarDraggable,
                            seekBarStatic,
                            digit0,
                            digit1);
            }
        });
        playerStateViewModel.isPlaying.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                ImageButton playbutton = findViewById(R.id.control_button_play);
                if (playbutton != null) {
                    if (value)
                        playbutton.setImageResource(R.drawable.main_btnpause);
                    else
                        playbutton.setImageResource(R.drawable.main_btnplay);
                }

                SeekBar seekBarDraggable = findViewById(R.id.control_seekbar_progressdynamic);
                SeekBar seekBarStatic = findViewById(R.id.control_seekbar_progressstatic);
                TextView digit0 = findViewById(R.id.control_text_digit0);
                TextView digit1 = findViewById(R.id.control_text_digit1);
                if (seekBarDraggable != null
                        && seekBarStatic != null
                        && digit0 != null
                        && digit1 != null)
                    animator = refreshValueAnimators(
                            animator,
                            value,
                            playerStateViewModel.trackPosition.get(),
                            playerStateViewModel.trackLength.get(),
                            seekBarDraggable,
                            seekBarStatic,
                            digit0,
                            digit1);
            }
        });
        playerStateViewModel.isShuffling.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                ImageButton shuffleButton = findViewById(R.id.control_button_shuffle);
                if (shuffleButton != null) {
                    if (value) {
                        shuffleButton.setImageTintList(
                                ColorStateList.valueOf(AttributeConversion.getColorForAtt(R.attr.colorSecondary, MainActivity.this))
                        );
                    } else {
                        shuffleButton.setImageTintList(
                                ColorStateList.valueOf(AttributeConversion.getColorForAtt(R.attr.onColorPrimary, MainActivity.this))
                        );
                    }
                }
            }
        });
        playerStateViewModel.isRepeating.addObserver(new Observable.Observer<Boolean>() {
            @Override
            public void update(Observable<Boolean> observable, Boolean value) {
                ImageButton repeatButton = findViewById(R.id.control_button_repeat);
                if (repeatButton != null) {
                    if (value)
                        repeatButton.setImageTintList(
                                ColorStateList.valueOf(AttributeConversion.getColorForAtt(R.attr.colorSecondary, MainActivity.this))
                        );
                    else
                        repeatButton.setImageTintList(
                                ColorStateList.valueOf(AttributeConversion.getColorForAtt(R.attr.onColorPrimary, MainActivity.this))
                        );
                }
            }
        });
        playerStateViewModel.volume.addObserver(new Observable.Observer<Float>() {
            @Override
            public void update(Observable<Float> observable, Float value) {
            }
        });
    }

    private ValueAnimator refreshValueAnimators(ValueAnimator instance,
                                                boolean isPlaying,
                                                long pos,
                                                long dur,
                                                SeekBar seekbarDraggable,
                                                SeekBar seekbarStatic,
                                                TextView digit1tv,
                                                TextView digit2tv) {
        if (instance != null)
            instance.cancel();
        instance = ValueAnimator.ofInt(0, 1000);
        instance.setDuration(dur);
        instance.setInterpolator(new LinearInterpolator());
        instance.addUpdateListener(animation -> {
            int percentageDone = (int) animation.getAnimatedValue();
            seekbarDraggable.setProgress(percentageDone);
            seekbarStatic.setProgress(percentageDone);
            if (digit1tv != null
                    && digit1tv.getVisibility() == View.VISIBLE) {
                long minutesP = (animation.getCurrentPlayTime() / 1000) / 60;
                long secondsP = (animation.getCurrentPlayTime() / 1000) % 60;
                String digit1 = leftpadZero(minutesP) + ":" + leftpadZero(secondsP);
                digit1tv.setText(digit1);
            }
        });

        int percentageDone = 0;
        if (pos > 0 && dur > 0) {
            percentageDone = safeLongToInt(pos / dur) * 1000;
        }
        seekbarDraggable.setProgress(percentageDone);
        seekbarStatic.setProgress(percentageDone);

        if (digit1tv != null && digit2tv != null) {
            long minutesT = (dur / 1000) / 60;
            long secondsT = (dur / 1000) % 60;
            long minutesP = (pos / 1000) / 60;
            long secondsP = (pos / 1000) % 60;
            String digit1 = leftpadZero(minutesP) + ":" + leftpadZero(secondsP);
            String digit2 = leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
            digit1tv.setText(digit1);
            digit2tv.setText(digit2);
        }

        instance.start();
        instance.setCurrentPlayTime(pos); //Calling setCurrentPlayTime() before start() has no effect on API 21.

        if (!isPlaying) {
            instance.pause();
        }
        return instance;
    }

    private void showAddToPlaylistMenu(View anchor,
                                       int offsetx,
                                       int offsety,
                                       int marginDP,
                                       boolean showAddToNew,
                                       List<AudioPlaylist> existingPlaylists,
                                       AdapterView.OnItemClickListener listener) {
        View prompt = View.inflate(this, R.layout.popupitem, null);
        ((TextView) prompt.findViewById(R.id.title)).setText(getString(R.string.main_popup_addto_prompt0addTo));
        prompt.findViewById(R.id.subArrow).setVisibility(View.GONE);
        prompt.findViewById(R.id.root).setBackgroundColor(AttributeConversion.getColorForAtt(R.attr.colorPrimaryAlt, this));
        prompt.findViewById(R.id.rippleBackground).setBackground(null);
        ListPopupWindow popupWindow = ListPopupFactory.getAddToPlaylistMenu(this, anchor, prompt, offsetx, offsety, marginDP, showAddToNew, existingPlaylists, listener);
        popupWindow.show();
    }
}