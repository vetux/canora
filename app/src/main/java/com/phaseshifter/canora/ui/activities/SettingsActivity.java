package com.phaseshifter.canora.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.repo.SettingsRepository;
import com.phaseshifter.canora.model.repo.ThemeRepository;
import com.phaseshifter.canora.service.player.wrapper.AutoBindMediaService;
import com.phaseshifter.canora.ui.arrayadapters.SettingAdapterItem;
import com.phaseshifter.canora.ui.arrayadapters.SettingArrayAdapter;
import com.phaseshifter.canora.ui.arrayadapters.ThemeArrayAdapter;
import com.phaseshifter.canora.ui.contracts.SettingsContract;
import com.phaseshifter.canora.ui.data.constants.SettingsPage;
import com.phaseshifter.canora.ui.pageradapters.SettingsPagerAdapter;
import com.phaseshifter.canora.ui.presenters.SettingsPresenter;
import com.phaseshifter.canora.ui.utils.motionlayout.SettingsMotionLayoutController;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.Observer;
import com.phaseshifter.canora.utils.Pair;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements SettingsContract.View,
        SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,
        AdapterView.OnItemClickListener {
    private final String LOG_TAG = "SettingsActivity";

    private final String BUNDLE_PRESENTERSTATE = "BUNDLE_PRESENTERSTATE";

    public static final int REQUESTCODE_CREATE_DOCUMENT = 0;

    private final Observable<AppTheme> activeTheme = new Observable<>();
    private final Observable<List<AppTheme>> availableThemes = new Observable<>();
    private final Observable<Float> volume = new Observable<>();
    private final Observable<Boolean> devMode = new Observable<>();
    private final Observable<Boolean> enableMediaSessionCallback = new Observable<>();
    private final Observable<Boolean> useAnimations = new Observable<>();
    private final Observable<String> scClientID = new Observable<>();
    private final Observable<String> ytApiKey = new Observable<>();
    private final Observable<Pair<Integer, Long>> playlistData = new Observable<>();
    private final Observable<List<Pair<String, Object>>> modifiedSettings = new Observable<>();
    private final Observable<Integer> equalizerPreset = new Observable<>(-1);
    private final Observable<String[]> equalizerPresets = new Observable<>(new String[0]);
    private final Observable<Integer> crashLogCount = new Observable<>(0);

    private SettingsContract.Presenter presenter;

    private SettingsMotionLayoutController motionLayoutController;

    private SettingsPagerAdapter pagerAdapter;

    private AutoBindMediaService service;

    //START Activity Interface

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        service = new AutoBindMediaService(this);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        presenter = new SettingsPresenter(this,
                new SettingsRepository(this),
                new ThemeRepository(),
                ((MainApplication) getApplication()).getPlaylistRepository(),
                service,
                audioManager,
                this,
                savedInstanceState == null ? null : savedInstanceState.getSerializable(BUNDLE_PRESENTERSTATE));
        pagerAdapter = new SettingsPagerAdapter(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        service.bind();
        presenter.start();
    }

    @Override
    protected void onStop() {
        presenter.stop();
        service.unbind();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        presenter.onNavigateBack();
    }

    //STOP Activity Interface

    //START Listeners

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_button_nav:
                onBackPressed();
                break;
            case R.id.buttonSettingsReset:
                presenter.resetSettings();
                break;
            case R.id.logExportBtn:
                presenter.onExportCrashLogs();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.devModeSwitch:
                presenter.onDeveloperModeChange(isChecked);
                break;
            case R.id.mediaSessionSwitch:
                presenter.onEnableMediaSessionCallbacksChange(isChecked);
                break;
            case R.id.animationsCheckbox:
                presenter.onUseAnimationsChange(isChecked);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.volBar:
                presenter.onVolumeChange((float) progress / seekBar.getMax());
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.themeGridViewSelection:
                ThemeArrayAdapter adapter = (ThemeArrayAdapter) parent.getAdapter();
                presenter.onThemeSelected(adapter.getContentRef().get(position));
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(BUNDLE_PRESENTERSTATE, presenter.saveState());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch(requestCode){
            case REQUESTCODE_CREATE_DOCUMENT:
                if (data != null) {
                    Uri resultUri = data.getData();
                    if (resultUri != null) {
                        getContentResolver().takePersistableUriPermission(resultUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        try {
                            presenter.onDocumentCreated(resultUri);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //STOP Listeners

    //START View Interface

    @Override
    public void setTheme(AppTheme theme) {
        Log.v(LOG_TAG, "setTheme " + theme);
        if (theme != activeTheme.get()) {
            setTheme(theme.styleResID);
            setContentView(R.layout.activity_settings);

            motionLayoutController = new SettingsMotionLayoutController(findViewById(R.id.root));
            ImageButton button = findViewById(R.id.toolbar_button_nav);
            button.setOnClickListener(this);
            ViewPager pager = findViewById(R.id.settingsViewPager);
            pager.setAdapter(pagerAdapter);
            activeTheme.set(theme);

            TabLayout tabs = findViewById(R.id.tabLayout);

            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.onColorPrimary, typedValue, true);
            int colA = typedValue.data;
            getTheme().resolveAttribute(R.attr.colorSecondary, typedValue, true);
            int colB = typedValue.data;

            tabs.setTabTextColors(colA, colB);
            tabs.setSelectedTabIndicatorColor(colB);
        }
    }

    @Override
    public void showMain() {
        TextView title = findViewById(R.id.toolbar_textview_title);
        title.setText(R.string.settings_toolbar_title0settings);
        motionLayoutController.setPage(null);

        ListView listView = findViewById(R.id.settingsListView);
        List<SettingAdapterItem> adapterItems = new ArrayList<>();
        adapterItems.add(new SettingAdapterItem(getString(R.string.settings_list_item_title0displaySettings), getString(R.string.settings_list_item_subTitle0displaySettings), getDrawable(R.drawable.baseline_brightness_medium_white_48dp)));
        adapterItems.add(new SettingAdapterItem(getString(R.string.settings_list_item_title0audioSettings), getString(R.string.settings_list_item_subTitle0audioSettings), getDrawable(R.drawable.baseline_volume_up_white_48dp)));
        adapterItems.add(new SettingAdapterItem(getString(R.string.settings_list_item_title0systemSettings), getString(R.string.settings_list_item_subTitle0systemSettings), getDrawable(R.drawable.baseline_account_tree_white_48dp)));

        SettingArrayAdapter arrayAdapter = new SettingArrayAdapter(this, adapterItems);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        presenter.onPageSelected(SettingsPage.DISPLAY);
                        break;
                    case 1:
                        presenter.onPageSelected(SettingsPage.SOUND);
                        break;
                    case 2:
                        presenter.onPageSelected(SettingsPage.SYSTEM);
                        break;
                }
            }
        });
    }

    @Override
    public void showPage(SettingsPage page) {
        TextView title = findViewById(R.id.toolbar_textview_title);
        switch (page) {
            case DISPLAY:
                title.setText(R.string.settings_toolbar_title0displaySettings);
                break;
            case SOUND:
                title.setText(R.string.settings_toolbar_title0audioSettings);
                break;
            case SYSTEM:
                title.setText(R.string.settings_toolbar_title0systemSettings);
                break;
        }
        motionLayoutController.setPage(page);
        pagerAdapter.setPage(page);
        ViewPager vp = findViewById(R.id.settingsViewPager);
        vp.setAdapter(pagerAdapter);
    }

    @Override
    public void setActiveTheme(AppTheme theme) {
        activeTheme.set(theme);
    }

    @Override
    public void setAvailableThemes(List<AppTheme> themes) {
        availableThemes.set(themes);
    }

    @Override
    public void setUseAnimations(boolean useAnimations) {
        this.useAnimations.set(useAnimations);
    }

    @Override
    public void setVolume(float v) {
        volume.set(v);
    }

    @Override
    public void setDeveloperMode(boolean devMode) {
        this.devMode.set(devMode);
        pagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void setEnableMediaSessionCallbacks(boolean enable) {
        this.enableMediaSessionCallback.set(enable);
        pagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void setSoundCloudClientID(String clientID) {
        scClientID.set(clientID);
    }

    @Override
    public void setYoutubeApiKey(String apiKey) {
        ytApiKey.set(apiKey);
    }

    @Override
    public void setEqualizerPresets(String[] presets) {
        equalizerPresets.set(presets);
    }

    @Override
    public void setEqualizerPreset(int preset) {
        equalizerPreset.set(preset);
    }

    @Override
    public void setLog_playlist(int count, long size) {
        Log.v(LOG_TAG, "setLog_playlist " + count + " " + size);
        playlistData.set(new Pair<>(count, size));
    }

    @Override
    public void setLog_modifiedSettings(List<Pair<String, Object>> settings) {
        Log.v(LOG_TAG, "setLog_modifiedSettings " + settings);
        modifiedSettings.set(settings);
    }

    @Override
    public void showDialog_confirmation_settingsreset(Runnable onReset) {
        Log.v(LOG_TAG, "showDialog_confirmation_settingsreset " + onReset);
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog)
                .setTitle(R.string.settings_dialog_settingsreset_title0warning)
                .setMessage(R.string.settings_dialog_settingsreset_text0confirmReset)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onReset.run();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create()
                .show();
    }

    @Override
    public void showDialog_warning_devmode(Runnable onEnable, Runnable onCancel) {
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog)
                .setTitle(R.string.settings_dialog_warning0title)
                .setMessage(R.string.settings_dialog_warning_devmode0text)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> onEnable.run())
                .setNegativeButton(android.R.string.no, (dialog, which) -> onCancel.run())
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onCancel.run();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void showDialog_warning_mediasession(Runnable onDisable, Runnable onCancel) {
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog)
                .setTitle(R.string.settings_dialog_warning0title)
                .setMessage(R.string.settings_dialog_warning_mediasession0text)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> onDisable.run())
                .setNegativeButton(android.R.string.no, (dialog, which) -> onCancel.run())
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onCancel.run();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void setCrashLogCount(int count) {
        crashLogCount.set(count);
    }

    @Override
    public void createDocument(String mime, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUESTCODE_CREATE_DOCUMENT);
    }

    @Override
    public OutputStream openDocument(Uri uri) throws FileNotFoundException {
        return getContentResolver().openOutputStream(uri);
    }

    @Override
    public void showMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    //STOP View Interface

    public void setupTab(ViewGroup tab) {
        switch (tab.getId()) {
            case R.id.settings_tab_audio_equalizer:
                ViewGroup container = tab.findViewById(R.id.container_equalizer);
                Spinner spinner = tab.findViewById(R.id.equalizerSpinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_preset, equalizerPresets.get());
                spinner.setAdapter(adapter);
                equalizerPresets.addObserver((obs, v) -> {
                    ArrayAdapter<String> adapt = new ArrayAdapter<>(this, R.layout.spinner_item_preset, v);
                    spinner.setAdapter(adapt);
                });
                spinner.setSelection(equalizerPreset.get());
                equalizerPreset.addObserver((obs, v) -> {
                    spinner.setSelection(v);
                });
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        presenter.onEqualizerPresetChange(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // ¯\_(ツ)_/¯
                    }

                });
                break;
            case R.id.settings_tab_audio_general:
                SeekBar seekBar = tab.findViewById(R.id.volBar);
                seekBar.setOnSeekBarChangeListener(this);
                seekBar.setProgress((int) (seekBar.getMax() * volume.get()));
                Observer<Float> volumeObserver = new Observer<Float>() {
                    @Override
                    public void update(Observable<Float> o, Float arg) {
                        seekBar.setProgress((int) (seekBar.getMax() * (Float) arg));
                    }
                };
                volume.addObserver(volumeObserver);
                break;
            case R.id.settings_tab_display_theme:
                GridView themeGridView = tab.findViewById(R.id.themeGridViewSelection);
                themeGridView.setOnItemClickListener(this);
                List<AppTheme> allThemes = new ArrayList<>(availableThemes.get());
                ThemeArrayAdapter themeArrayAdapter = new ThemeArrayAdapter(this, allThemes);
                themeArrayAdapter.setHighlightedItem(allThemes.indexOf(activeTheme.get()));
                themeGridView.setAdapter(themeArrayAdapter);
                Observer<List<AppTheme>> availableThemesObserver = new Observer<List<AppTheme>>() {
                    @Override
                    public void update(Observable<List<AppTheme>> o, List<AppTheme> arg) {
                        themeArrayAdapter.getContentRef().clear();
                        themeArrayAdapter.getContentRef().addAll((List<AppTheme>) arg);
                        themeArrayAdapter.notifyDataSetChanged();
                    }
                };
                Observer<AppTheme> activeThemeObserver = new Observer<AppTheme>() {
                    @Override
                    public void update(Observable<AppTheme> o, AppTheme arg) {
                        themeArrayAdapter.setHighlightedItem(themeArrayAdapter.getContentRef().indexOf(arg));
                    }
                };
                availableThemes.addObserver(availableThemesObserver);
                activeTheme.addObserver(activeThemeObserver);
                break;
            case R.id.settings_tab_display_misc:
                CheckBox animationsCheckbox = findViewById(R.id.animationsCheckbox);
                animationsCheckbox.setChecked(useAnimations.get());
                animationsCheckbox.setOnCheckedChangeListener(this);
                Observer<Boolean> animobs = new Observer<Boolean>() {
                    @Override
                    public void update(Observable<Boolean> o, Boolean arg) {
                        animationsCheckbox.setChecked(arg);
                    }
                };
                useAnimations.addObserver(animobs);
                break;
            case R.id.settings_tab_system_general:
                Button resetButton = findViewById(R.id.buttonSettingsReset);
                resetButton.setOnClickListener(this);

                Switch devModeSwitch = findViewById(R.id.devModeSwitch);
                devModeSwitch.setChecked(devMode.get());
                devModeSwitch.setOnCheckedChangeListener(this);

                Switch mediaSessionSwitch = findViewById(R.id.mediaSessionSwitch);
                mediaSessionSwitch.setChecked(enableMediaSessionCallback.get());
                mediaSessionSwitch.setOnCheckedChangeListener(this);

                Observer<Boolean> devObs = new Observer<Boolean>() {
                    @Override
                    public void update(Observable<Boolean> o, Boolean arg) {
                        devModeSwitch.setChecked(arg);
                    }
                };
                devMode.addObserver(devObs);

                Observer<Boolean> mediaSessionObs = new Observer<Boolean>() {
                    @Override
                    public void update(Observable<Boolean> observable, Boolean value) {
                        mediaSessionSwitch.setChecked(value);
                    }
                };
                enableMediaSessionCallback.addObserver(mediaSessionObs);

                break;
            case R.id.settings_tab_system_log:
                TextView logText = findViewById(R.id.logText);
                int playlists = playlistData.get().first;
                long playlistBytes = playlistData.get().second;
                StringBuilder text = new StringBuilder(getString(R.string.settings_text0playlistLog, playlists, ((float) playlistBytes / 1000 / 1000)) + "\n\n");
                List<Pair<String, Object>> settings = modifiedSettings.get();
                if (settings != null) {
                    for (Pair<String, Object> setting : settings) {
                        text.append(setting.first).append(" = ").append(setting.second).append("\n");
                    }
                }
                logText.setText(text.toString());
                Observer<Pair<Integer, Long>> playlistObserver = new Observer<Pair<Integer, Long>>() {
                    @Override
                    public void update(Observable<Pair<Integer, Long>> o, Pair<Integer, Long> arg) {
                        int playlists = arg.first;
                        long playlistBytes = arg.second;
                        StringBuilder text = new StringBuilder(getString(R.string.settings_text0playlistLog, playlists, ((float) playlistBytes / 1000 / 1000)) + "\n\n");
                        List<Pair<String, Object>> settings = modifiedSettings.get();
                        if (settings != null) {
                            for (Pair<String, Object> setting : settings) {
                                text.append(setting.first).append(" = ").append(setting.second).append("\n");
                            }
                        }
                        logText.setText(text.toString());
                    }
                };
                Observer<List<Pair<String, Object>>> settingsObserver = new Observer<List<Pair<String, Object>>>() {
                    @Override
                    public void update(Observable<List<Pair<String, Object>>> o, List<Pair<String, Object>> arg) {
                        int playlists = playlistData.get().first;
                        long playlistBytes = playlistData.get().second;
                        StringBuilder text = new StringBuilder(getString(R.string.settings_text0playlistLog, playlists, ((float) playlistBytes / 1000 / 1000)) + "\n\n");
                        for (Pair<String, Object> setting : arg) {
                            text.append(setting.first).append(" = ").append(setting.second).append("\n");
                        }
                        logText.setText(text.toString());
                    }
                };
                playlistData.addObserver(playlistObserver);
                modifiedSettings.addObserver(settingsObserver);

                TextView logCrashText = findViewById(R.id.logText_crashlogs);

                logCrashText.setText(getString(R.string.settings_crashlog_count, crashLogCount.get()));

                crashLogCount.addObserver(new Observer<Integer>() {
                    @Override
                    public void update(Observable<Integer> observable, Integer value) {
                        logCrashText.setText(getString(R.string.settings_crashlog_count, value));
                    }
                });

                Button logExportBtn = findViewById(R.id.logExportBtn);
                logExportBtn.setOnClickListener(this);
                break;
            case R.id.settings_tab_system_soundcloud:
                EditText scText = findViewById(R.id.edittext_soundcloudid);
                scText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        presenter.onSoundCloudClientIDChange(s.toString());
                    }
                });
                scText.setText(scClientID.get());
                scClientID.addObserver(new Observer<String>() {
                    @Override
                    public void update(Observable<String> observable, String value) {
                        scText.setText(value);
                    }
                });
                break;
            case R.id.settings_tab_system_youtubeapi:
                EditText ytText = findViewById(R.id.edittext_youtubekey);
                ytText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        presenter.onYoutubeApiKeyChange(s.toString());
                    }
                });
                ytText.setText(ytApiKey.get());
                ytApiKey.addObserver(new Observer<String>() {
                    @Override
                    public void update(Observable<String> observable, String value) {
                        ytText.setText(value);
                    }
                });
            case R.id.settings_tab_system_mediastore:
                break;
            default:
                throw new RuntimeException("Unrecognized ID: " + tab.getId() + " " + getResources().getResourceName(tab.getId()));
        }
    }

    public void clearTab(ViewGroup tab) {
        activeTheme.removeAllObservers();
        availableThemes.removeAllObservers();
        volume.removeAllObservers();
        devMode.removeAllObservers();
        enableMediaSessionCallback.removeAllObservers();
        useAnimations.removeAllObservers();
        playlistData.removeAllObservers();
        modifiedSettings.removeAllObservers();
        ytApiKey.removeAllObservers();
        scClientID.removeAllObservers();
        equalizerPreset.removeAllObservers();
        equalizerPresets.removeAllObservers();
    }

    public boolean isDevMode() {
        return devMode.get();
    }
}