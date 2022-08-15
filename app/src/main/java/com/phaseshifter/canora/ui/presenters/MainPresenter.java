package com.phaseshifter.canora.ui.presenters;

import android.content.res.Resources;
import android.util.Log;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.data.settings.StringSetting;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.editor.AudioMetadataEditor;
import com.phaseshifter.canora.model.formatting.ListFilter;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.service.wrapper.AutoBindingServiceWrapper;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.ui.data.MainPage;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.ContentSelector;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.ui.selectors.MainSelector;
import com.phaseshifter.canora.ui.viewmodels.AppViewModel;
import com.phaseshifter.canora.ui.viewmodels.ContentViewModel;
import com.phaseshifter.canora.ui.viewmodels.PlayerStateViewModel;
import com.phaseshifter.canora.model.repo.*;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.Observer;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.phaseshifter.canora.ui.selectors.MainSelector.getPlaylistForIndicator;

public class MainPresenter implements MainContract.Presenter, Observer<PlayerState> {
    private final String LOG_TAG = "MainPresenter";
    private final MainContract.View view;

    private final DeviceAudioRepository deviceAudioRepository;
    private final UserPlaylistRepository userPlaylistRepository;
    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final SoundCloudAudioRepository scAudioDataRepo;

    private final AutoBindingServiceWrapper service;

    private final Executor mainThread;

    private final ThreadPoolExecutor presExec;

    private final AppViewModel appViewModel;
    private final ContentViewModel contentViewModel;
    private final PlayerStateViewModel playerStateViewModel;

    private final AudioMetadataEditor metadataEditor;

    private FilterOptions filterOptions = new FilterOptions();
    private SortingOptions sortingOptions = new SortingOptions();

    private ContentSelector uiContentSelector = new ContentSelector(MainPage.TRACKS, null);
    private ContentSelector playingContentSelector = null;

    private HashSet<UUID> selection = new HashSet<>();

    private List<AudioData> sortedTracks = new ArrayList<>();
    private List<AudioPlaylist> sortedPlaylists = new ArrayList<>();

    private AppTheme theme = null;

    private int presenterTasks = 0;

    private boolean isScrollLoading = false;

    public MainPresenter(MainContract.View view,
                         Serializable savedState,
                         AutoBindingServiceWrapper service,
                         DeviceAudioRepository audioDataRepository,
                         UserPlaylistRepository audioPlaylistRepository,
                         SettingsRepository settingsRepository,
                         ThemeRepository themeRepository,
                         SoundCloudAudioRepository scAudioDataRepo,
                         AudioMetadataEditor metadataEditor,
                         Executor mainThread,
                         AppViewModel appViewModel,
                         ContentViewModel contentViewModel,
                         PlayerStateViewModel playerStateViewModel) {
        this.view = view;
        this.service = service;
        this.deviceAudioRepository = audioDataRepository;
        this.userPlaylistRepository = audioPlaylistRepository;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.scAudioDataRepo = scAudioDataRepo;
        this.metadataEditor = metadataEditor;
        this.mainThread = mainThread;
        this.appViewModel = appViewModel;
        this.contentViewModel = contentViewModel;
        this.playerStateViewModel = playerStateViewModel;
        this.presExec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        if (savedState instanceof MainPresenterState) {
            final MainPresenterState state = (MainPresenterState) savedState;
            // Repositories are stored application wide so the saved indicator uuid should be valid.
            uiContentSelector = state.uiIndicator;
            playingContentSelector = state.uiIndicator;
        }
    }

    private void runPresenterTask(Runnable task) {
        appViewModel.isContentLoading.set(true);
        presenterTasks++;
        presExec.submit(() -> {
            task.run();
            if (--presenterTasks == 0) {
                mainThread.execute(() -> {
                    appViewModel.isContentLoading.set(false);
                });
            }
        });
    }

    private void updateVisibleContent() {
        // TODO: Preformat content
        List<AudioData> formattedTracks = new ArrayList<>();
        List<AudioPlaylist> formattedPlaylists = new ArrayList<>();
        if (uiContentSelector.isPlaylistView()) {
            switch (uiContentSelector.getPage()) {
                case PLAYLISTS:
                    sortedPlaylists = ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), sortingOptions);
                    break;
                case ARTISTS:
                    sortedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getArtists(), sortingOptions);
                    break;
                case ALBUMS:
                    sortedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getAlbums(), sortingOptions);
                    break;
                case GENRES:
                    sortedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getGenres(), sortingOptions);
                    break;
                case SOUNDCLOUD_CHARTS:
                    sortedPlaylists = ListSorter.sortAudioPlaylist(scAudioDataRepo.getChartsPlaylists(), sortingOptions);
                    break;
            }
            formattedPlaylists = ListFilter.filterAudioPlaylist(sortedPlaylists, filterOptions);
            contentViewModel.visiblePlaylists.set(formattedPlaylists);
        } else {
            switch (uiContentSelector.getPage()) {
                case TRACKS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getTracks(), sortingOptions);
                    break;
                case PLAYLISTS:
                    sortedTracks = ListSorter.sortAudioData(userPlaylistRepository.get(uiContentSelector.getUuid()).getData(), sortingOptions);
                    break;
                case ARTISTS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getArtist(uiContentSelector.getUuid()).getData(), sortingOptions);
                    break;
                case ALBUMS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getAlbum(uiContentSelector.getUuid()).getData(), sortingOptions);
                    break;
                case GENRES:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getGenre(uiContentSelector.getUuid()).getData(), sortingOptions);
                    break;
                case SOUNDCLOUD_SEARCH:
                    sortedTracks = scAudioDataRepo.getSearchResults();
                    break;
                case SOUNDCLOUD_CHARTS:
                    sortedTracks = scAudioDataRepo.getChartsPlaylist(uiContentSelector.getUuid()).getData();
                    break;
                case YOUTUBE_SEARCH:
                    break;
            }
            if (uiContentSelector.getPage() != MainPage.SOUNDCLOUD_SEARCH) {
                formattedTracks = ListFilter.filterAudioData(sortedTracks, filterOptions);
            } else {
                formattedTracks = sortedTracks;
            }
            contentViewModel.visibleTracks.set(formattedTracks);
        }

        if (!uiContentSelector.isPlaylistView() && contentViewModel.visibleTracks.get().isEmpty()) {
            if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
                appViewModel.notFoundText.set("Enter text to begin searching soundcloud...");
            } else {
                appViewModel.notFoundText.set("No tracks found");
            }
        } else if (uiContentSelector.isPlaylistView() && contentViewModel.visiblePlaylists.get().isEmpty()) {
            appViewModel.notFoundText.set("No playlists found");
        } else {
            appViewModel.notFoundText.set(null);
        }
    }

    private void setViewModelContentSelector(ContentSelector selector) {
        if (selector.isPlaylistView()) {
            switch (selector.getPage()) {
                case TRACKS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0tracks));
                    break;
                case PLAYLISTS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0playlists));
                    break;
                case ARTISTS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0artists));
                    break;
                case ALBUMS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0albums));
                    break;
                case GENRES:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0genres));
                    break;
                case SOUNDCLOUD_CHARTS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0sc));
                    break;
            }
        } else {
            switch (selector.getPage()) {
                case TRACKS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0tracks));
                    break;
                case PLAYLISTS:
                    contentViewModel.contentName.set(userPlaylistRepository.get(selector.getUuid()).getMetadata().getTitle());
                    break;
                case ARTISTS:
                    contentViewModel.contentName.set(deviceAudioRepository.getArtist(selector.getUuid()).getMetadata().getTitle());
                    break;
                case ALBUMS:
                    contentViewModel.contentName.set(deviceAudioRepository.getAlbum(selector.getUuid()).getMetadata().getTitle());
                    break;
                case GENRES:
                    contentViewModel.contentName.set(deviceAudioRepository.getGenre(selector.getUuid()).getMetadata().getTitle());
                    break;
                case SOUNDCLOUD_SEARCH:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0sc));
                    break;
                case SOUNDCLOUD_CHARTS:
                    contentViewModel.contentName.set(scAudioDataRepo.getChartsPlaylist(selector.getUuid()).getMetadata().getTitle());
                    break;
                case YOUTUBE_SEARCH:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0youtube));
                    break;
                case YOUTUBE_DL:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0youtube_dl));
                    break;
            }
        }
        appViewModel.contentSelector.set(selector);
    }

    @Override
    public void update(Observable<PlayerState> observable, PlayerState value) {
        playerStateViewModel.applyPlayerState(value);
    }

    //START Presenter Interface

    @Override
    public synchronized void start() {
        scAudioDataRepo.setClientID(settingsRepository.getString(StringSetting.SC_CLIENTID));

        theme = themeRepository.get(settingsRepository.getInt(IntegerSetting.THEME));

        sortingOptions.sortby = settingsRepository.getInt(IntegerSetting.SORT_BY);
        sortingOptions.sortdir = settingsRepository.getInt(IntegerSetting.SORT_DIR);
        sortingOptions.sorttech = settingsRepository.getInt(IntegerSetting.SORT_TECH);

        filterOptions.filterBy = settingsRepository.getInt(IntegerSetting.FILTER_BY);

        appViewModel.devMode.set(settingsRepository.getBoolean(BooleanSetting.DEVELOPERMODE));

        setViewModelContentSelector(uiContentSelector);

        service.bind();
        PlayerState playerState = service.getState().get();
        if (playerState != null)
            playerStateViewModel.applyPlayerState(service.getState().get());
        service.getState().addObserver(this);
        view.checkPermissions();
        runPresenterTask(() -> {
            deviceAudioRepository.refresh();
            mainThread.execute(() -> {
                view.setTheme(theme);
                appViewModel.notifyObservers();
                contentViewModel.notifyObservers();
                playerStateViewModel.notifyObservers();
                updateVisibleContent();
            });
        });
    }

    @Override
    public synchronized void stop() {
        settingsRepository.putString(StringSetting.SC_CLIENTID, scAudioDataRepo.getClientID());

        MainPresenterState savedState = new MainPresenterState();
        savedState.uiIndicator = uiContentSelector;
        savedState.contentIndicator = playingContentSelector;
        view.saveState(savedState);

        service.getState().removeObserver(this);
    }

    @Override
    public void onTrackSeekStart() {
        service.pause();
    }

    @Override
    public void onTrackSeek(float p) {
        service.seek(p);
    }

    @Override
    public void onTrackSeekStop() {
        service.resume();
    }

    @Override
    public void onPrev() {
        service.previous();
    }

    @Override
    public void onPlay() {
        service.pauseResume();
    }

    @Override
    public void onNext() {
        service.next();
    }

    @Override
    public void onShuffleSwitch() {
        boolean shuffle = !playerStateViewModel.isShuffling.get();
        settingsRepository.putBoolean(BooleanSetting.SHUFFLE, shuffle);
        service.setShuffle(shuffle);
    }

    @Override
    public void onRepeatSwitch() {
        boolean repeat = !playerStateViewModel.isRepeating.get();
        settingsRepository.putBoolean(BooleanSetting.REPEAT, repeat);
        service.setRepeat(repeat);
    }

    @Override
    public void onVolumeSeek(float p) {
        if (p > 1.0f || p < 0.0f)
            throw new RuntimeException("Received invalid Volume value: " + p);
        settingsRepository.putFloat(FloatSetting.VOLUME, p);
        service.setVolume(p);
    }

    @Override
    public void onSearchTextChange(String text) {
        assert (text != null);
        filterOptions.filterFor = text;
        if (uiContentSelector.getPage() != MainPage.SOUNDCLOUD_SEARCH) {
            updateVisibleContent();
        }
    }

    @Override
    public void onSearchTextEditingFinished() {
        if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
            String text = filterOptions.filterFor;
            runPresenterTask(() -> {
                scAudioDataRepo.refreshSearch(text);
                mainThread.execute(this::updateVisibleContent);
            });
        }
    }

    @Override
    public void onOptionsButtonClick() {
        HashSet<OptionsMenu.Action> actions = new HashSet<>();
        actions.add(OptionsMenu.Action.OPEN_SETTINGS);
        actions.add(OptionsMenu.Action.OPEN_SORTOPTIONS);
        actions.add(OptionsMenu.Action.OPEN_FILTEROPTIONS);

        if (appViewModel.isSelecting.get()) {
            actions.add(OptionsMenu.Action.ADD_SELECTION);
            actions.add(OptionsMenu.Action.SELECT_ALL);
            actions.add(OptionsMenu.Action.DESELECT_ALL);
            actions.add(OptionsMenu.Action.SELECT_STOP);
        } else {
            actions.add(OptionsMenu.Action.SELECT_START);
        }

        if (uiContentSelector.getPage() == MainPage.PLAYLISTS) {
            if (uiContentSelector.isPlaylistView() && appViewModel.isSelecting.get()) {
                actions.add(OptionsMenu.Action.DELETE);
            } else if (!uiContentSelector.isPlaylistView()) {
                actions.add(OptionsMenu.Action.EDIT_PLAYLIST);
                actions.add(OptionsMenu.Action.DELETE);
            }
        }
        view.showOptionsMenu(actions,
                this::onMenuAction,
                () -> {
                });
    }

    @Override
    public void onSearchButtonClick() {
        boolean search = !appViewModel.isSearching.get();
        appViewModel.isSearching.set(search);
        if (!appViewModel.searchText.get().isEmpty()) {
            updateVisibleContent();
        }
    }

    @Override
    public void onFloatingAddToButtonClick() {
        onMenuAction(OptionsMenu.Action.ADD_SELECTION);
    }

    @Override
    public void onBackPress() {
        if (!uiContentSelector.equals(playingContentSelector)
                && !uiContentSelector.isPlaylistView()
                && uiContentSelector.getPage() != MainPage.TRACKS) {
            uiContentSelector = new ContentSelector(uiContentSelector.getPage(), null);
            appViewModel.isSelecting.set(false);
            setViewModelContentSelector(uiContentSelector);
            updateVisibleContent();
        } else if (playingContentSelector != null
                && !uiContentSelector.equals(playingContentSelector)) {
            uiContentSelector = playingContentSelector;
            appViewModel.isSelecting.set(false);
            setViewModelContentSelector(uiContentSelector);
            updateVisibleContent();
        } else {
            view.shutdown();
        }
    }

    @Override
    public void onPermissionCheckResult(boolean permissionsGranted) {
        if (permissionsGranted) {
            updateVisibleContent();
        } else {
            view.requestPermissions();
        }
    }

    @Override
    public void onPermissionRequestResult(boolean permissionsGranted) {
        if (permissionsGranted) {
            updateVisibleContent();
        } else {
            view.showWarning("Permissions not granted, some functionality might not be available");
        }
    }

    @Override
    public void onTrackContentClick(int index) {
        List<AudioData> processedData = contentViewModel.visibleTracks.get();

        if (processedData == null || index > processedData.size() - 1)
            throw new AssertionError();

        if (appViewModel.isSelecting.get()) {
            UUID trackid = processedData.get(index).getMetadata().getId();
            if (selection.contains(trackid)) {
                selection.remove(trackid);
            } else {
                selection.add(trackid);
            }
            HashSet<Integer> indices = getSelectedTrackIndices();
            contentViewModel.contentTracksSelection.set(indices);
        } else {
            boolean reformat = !uiContentSelector.equals(playingContentSelector);
            playingContentSelector = uiContentSelector;
            if (reformat) {
                updateVisibleContent();
            }
            service.setContent(sortedTracks);
            service.play(processedData.get(index).getMetadata().getId());
        }
    }

    @Override
    public void onTrackContentLongClick(int index) {
        HashSet<ContextMenu.Action> actions = new HashSet<>();
        actions.add(ContextMenu.Action.SELECT);
        actions.add(ContextMenu.Action.EDIT);
        if (uiContentSelector.getPage() == MainPage.PLAYLISTS) {
            actions.add(ContextMenu.Action.DELETE);
        }
        view.showContentContextMenu(index, actions, (action) -> {
            onContextMenuAction(index, action);
        }, () -> {
        });
    }

    @Override
    public void onPlaylistContentClick(int index) {
        List<AudioPlaylist> processedData = contentViewModel.visiblePlaylists.get();

        //Assertions
        if (processedData == null
                || index > processedData.size() - 1)
            throw new AssertionError();

        if (appViewModel.isSelecting.get()) {
            UUID playlistId = processedData.get(index).getMetadata().getId();
            if (selection.contains(playlistId)) {
                selection.remove(playlistId);
            } else {
                selection.add(playlistId);
            }
            HashSet<Integer> indices = getSelectedPlaylistIndices();
            contentViewModel.contentPlaylistsSelection.set(indices);
        } else {
            uiContentSelector = new ContentSelector(uiContentSelector.getPage(), processedData.get(index).getMetadata().getId());
            if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_CHARTS) {
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(uiContentSelector.getUuid()));
                    mainThread.execute(() -> {
                        updateVisibleContent();
                        setViewModelContentSelector(uiContentSelector);
                    });
                });
            } else {
                updateVisibleContent();
                setViewModelContentSelector(uiContentSelector);
            }
        }
    }

    @Override
    public void onPlaylistContentLongClick(int index) {
        HashSet<ContextMenu.Action> actions = new HashSet<>();
        actions.add(ContextMenu.Action.SELECT);
        if (uiContentSelector.getPage() == MainPage.PLAYLISTS) {
            actions.add(ContextMenu.Action.EDIT);
            actions.add(ContextMenu.Action.DELETE);
        }
        view.showContentContextMenu(index, actions, (action) -> {
            onContextMenuAction(index, action);
        }, () -> {
        });
    }

    @Override
    public void onTrackContentScrollToBottom() {
        if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
            if (!isScrollLoading) {
                isScrollLoading = true;
                if (!scAudioDataRepo.isSearchLimitReached()) {
                    scAudioDataRepo.refreshSearch(appViewModel.searchText.get());
                    updateVisibleContent();
                }
            }
        } else if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_CHARTS && !uiContentSelector.isPlaylistView()) {
            if (!isScrollLoading) {
                if (!scAudioDataRepo.isChartsLimitReached()) {
                    scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(uiContentSelector.getUuid()));
                    updateVisibleContent();
                }
            }
        }
    }

    @Override
    public void onMediaStoreDataChange() {
        Log.v(LOG_TAG, "onMediaStoreDataChange");
        deviceAudioRepository.refresh();
        updateVisibleContent();
    }

    @Override
    public void onEditorResult(AudioData data, boolean error, boolean canceled, boolean delete) {
        Log.v(LOG_TAG, "onEditorResult " + data + " " + error + " " + canceled + " " + delete);
        if (data == null)
            return;
        if (!error && !canceled) {
            if (!delete) {
                if (data.getDataSource() instanceof AudioDataSourceUri) {
                    Log.v(LOG_TAG, "Writing Uri ...");
                    try {
                        metadataEditor.writeMetadata(((AudioDataSourceUri) data.getDataSource()).getUri(), data.getMetadata());
                        deviceAudioRepository.refresh();
                        updateVisibleContent();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SecurityException se) {
                        Log.v(LOG_TAG, "Oh Noes!!! SecurityException: " + se);
                        view.handleSecurityException(se, () -> {
                            Log.v(LOG_TAG, "OnHandledSecurityException: " + se);
                            try {
                                metadataEditor.writeMetadata(((AudioDataSourceUri) data.getDataSource()).getUri(), data.getMetadata());
                                deviceAudioRepository.refresh();
                                updateVisibleContent();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onEditorResult(AudioPlaylist data, boolean error, boolean canceled, boolean delete) {
        Log.v(LOG_TAG, "onEditorResult: " + data + " " + error + " " + canceled + " " + delete);
        if (data == null)
            return;
        if (!error && !canceled) {
            if (delete) {
                Log.v(LOG_TAG, "Deleting playlist " + data);
                userPlaylistRepository.remove(data.getMetadata().getId());
            } else {
                Log.v(LOG_TAG, "Replacing Playlist " + data);
                userPlaylistRepository.replace(data.getMetadata().getId(), data);
            }
            updateVisibleContent();
        }
    }

    @Override
    public void onTransportControlChange(boolean controlMax) {
    }

    @Override
    public void onNavigationClick(NavigationItem item) {
        switch (item) {
            case TRACKS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.TRACKS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case PLAYLISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.PLAYLISTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case ALBUMS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.ALBUMS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case ARTISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.ARTISTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case GENRES:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.GENRES, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case SOUNDCLOUD_SEARCH:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(true);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.SOUNDCLOUD_SEARCH, null);
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshSearch(appViewModel.searchText.get());
                    mainThread.execute(() -> {
                        setViewModelContentSelector(uiContentSelector);
                        updateVisibleContent();
                    });
                });
                break;
            case SOUNDCLOUD_CHARTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiContentSelector = new ContentSelector(MainPage.SOUNDCLOUD_CHARTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case SETTINGS:
                view.setNavigationMax(false);
                view.startSettings();
                break;
            case RATE:
                view.setNavigationMax(false);
                view.startRate();
                break;
            case INFO:
                view.setNavigationMax(false);
                view.startInfo();
                break;
        }
    }

    //STOP Presenter Interface

    private HashSet<Integer> getSelectedTrackIndices() {
        HashSet<Integer> indices = new HashSet<>();
        for (UUID id : selection) {
            List<AudioData> list = contentViewModel.visibleTracks.get();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getMetadata().getId().equals(id)) {
                    indices.add(i);
                    break;
                }
            }
        }
        return indices;
    }

    private HashSet<Integer> getSelectedPlaylistIndices() {
        HashSet<Integer> indices = new HashSet<>();
        for (UUID id : selection) {
            List<AudioPlaylist> list = contentViewModel.visiblePlaylists.get();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getMetadata().getId().equals(id)) {
                    indices.add(i);
                    break;
                }
            }
        }
        return indices;
    }

    private void onContextMenuAction(int index, ContextMenu.Action action) {
        switch (action) {
            case SELECT:
                if (uiContentSelector.isPlaylistView()) {
                    AudioPlaylist clickedPlaylist = contentViewModel.visiblePlaylists.get().get(index);
                    if (selection.contains(clickedPlaylist.getMetadata().getId()))
                        selection.remove(clickedPlaylist.getMetadata().getId());
                    else
                        selection.add(clickedPlaylist.getMetadata().getId());
                    contentViewModel.contentPlaylistsSelection.set(getSelectedPlaylistIndices());
                } else {
                    AudioData clickedTrack = contentViewModel.visibleTracks.get().get(index);
                    if (selection.contains(clickedTrack.getMetadata().getId()))
                        selection.remove(clickedTrack.getMetadata().getId());
                    else
                        selection.add(clickedTrack.getMetadata().getId());

                    contentViewModel.contentTracksSelection.set(getSelectedTrackIndices());
                }
                appViewModel.isSelecting.set(true);
                break;
            case EDIT:
                if (uiContentSelector.isPlaylistView()) {
                    view.startEditor(contentViewModel.visiblePlaylists.get().get(index), theme);
                } else {
                    AudioData track = contentViewModel.visibleTracks.get().get(index);
                    view.startEditor(track, metadataEditor.getMask(track), theme);
                }
                break;
            case DELETE:
                if (uiContentSelector.isPlaylistView()) {
                    AudioPlaylist playlistToDelete = contentViewModel.visiblePlaylists.get().get(index);
                    List<AudioPlaylist> playlistsToDelete = new ArrayList<>();
                    playlistsToDelete.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(playlistsToDelete, () -> {
                                userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                                updateVisibleContent();
                                view.showMessage("Deleted Playlist", "Playlist deleted");
                            },
                            () -> {
                            });
                } else {
                    AudioPlaylist currentPlaylist = MainSelector.getPlaylistForIndicator(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                    if (currentPlaylist == null)
                        throw new AssertionError();
                    List<AudioData> tracks = contentViewModel.visibleTracks.get();
                    List<AudioData> tracksToDelete = new ArrayList<>();
                    tracksToDelete.add(tracks.get(index));
                    view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, () -> {
                        List<AudioData> cleanTracks = currentPlaylist.getData();
                        for (AudioData delTrack : tracksToDelete)
                            cleanTracks.remove(delTrack);
                        AudioPlaylist clean = new AudioPlaylist(currentPlaylist.getMetadata(), cleanTracks);
                        userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                        updateVisibleContent();
                        view.showMessage("Deleted Tracks", "Deleted tracks from playlist");
                    }, () -> {
                    });
                }
                break;
        }
    }

    private void onMenuAction(OptionsMenu.Action action) {
        switch (action) {
            case OPEN_SETTINGS:
                view.startSettings();
                break;
            case OPEN_SORTOPTIONS:
                view.showDialog_SortOptions(sortingOptions, (options) -> {
                    boolean changed = sortingOptions != options;
                    if (changed) {
                        settingsRepository.putInt(IntegerSetting.SORT_BY, options.sortby);
                        settingsRepository.putInt(IntegerSetting.SORT_DIR, options.sortdir);
                        settingsRepository.putInt(IntegerSetting.SORT_TECH, options.sorttech);
                        sortingOptions = options;
                        updateVisibleContent();
                    }
                });
                break;
            case OPEN_FILTEROPTIONS:
                view.showDialog_FilterOptions(filterOptions,
                        (options) -> {
                            boolean changed = filterOptions != options;
                            if (changed) {
                                settingsRepository.putInt(IntegerSetting.FILTER_BY, options.filterBy);
                                filterOptions = options;
                                updateVisibleContent();
                            }
                        });
                break;
            case ADD_SELECTION:
                view.showAddSelectionMenu(ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), sortingOptions),
                        () -> {
                            List<AudioData> selectedData = new ArrayList<>();
                            if (uiContentSelector.isPlaylistView()) {
                                for (AudioPlaylist playlist : sortedPlaylists) {
                                    if (selection.contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getData());
                                }
                            } else {
                                for (AudioData track : sortedTracks) {
                                    if (selection.contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }
                            view.showDialog_CreatePlaylist(selectedData, (title) -> {
                                        PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(null,
                                                title,
                                                null
                                        );
                                        AudioPlaylist playlist = new AudioPlaylist(metadata, selectedData);
                                        userPlaylistRepository.add(playlist);
                                        appViewModel.isSelecting.set(false);
                                        updateVisibleContent();
                                        view.showMessage("Created Playlist", "Created playlist");
                                    },
                                    () -> {
                                    });
                        },
                        (targetPlaylist) -> {
                            List<AudioData> selectedData = new ArrayList<>();
                            if (uiContentSelector.isPlaylistView()) {
                                for (AudioPlaylist playlist : sortedPlaylists) {
                                    if (selection.contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getData());
                                }
                            } else {
                                for (AudioData track : sortedTracks) {
                                    if (selection.contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }

                            AudioPlaylist targetCopy = new AudioPlaylist(targetPlaylist);
                            targetCopy.getData().addAll(selectedData);

                            userPlaylistRepository.replace(targetPlaylist.getMetadata().getId(), targetCopy);

                            appViewModel.isSelecting.set(false);
                            updateVisibleContent();
                            view.showMessage("Added tracks", "Added tracks to playlist");
                        });
                break;
            case SELECT_START:
                appViewModel.isSelecting.set(true);
                selection.clear();
                contentViewModel.contentTracksSelection.get().clear();
                contentViewModel.contentTracksSelection.notifyObservers();
                contentViewModel.contentPlaylistsSelection.get().clear();
                contentViewModel.contentPlaylistsSelection.notifyObservers();
                break;
            case SELECT_STOP:
                appViewModel.isSelecting.set(false);
                break;
            case SELECT_ALL:
                if (uiContentSelector.isPlaylistView()) {
                    for (AudioPlaylist playlist : sortedPlaylists) {
                        selection.add(playlist.getMetadata().getId());
                    }
                    contentViewModel.contentPlaylistsSelection.set(getSelectedPlaylistIndices());
                } else {
                    for (AudioData track : sortedTracks) {
                        selection.add(track.getMetadata().getId());
                    }
                    contentViewModel.contentTracksSelection.set(getSelectedTrackIndices());
                }
                break;
            case DESELECT_ALL:
                contentViewModel.contentTracksSelection.get().clear();
                contentViewModel.contentTracksSelection.notifyObservers();
                break;
            case EDIT_PLAYLIST:
                AudioPlaylist playlist = getPlaylistForIndicator(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                view.startEditor(playlist, theme);
                break;
            case DELETE:
                if (appViewModel.isSelecting.get()) {
                    if (uiContentSelector.isPlaylistView()) {
                        List<AudioPlaylist> playlistsToDelete = new ArrayList<>();
                        for (AudioPlaylist pl : sortedPlaylists) {
                            if (selection.contains(pl.getMetadata().getId()))
                                playlistsToDelete.add(pl);
                        }
                        appViewModel.isSelecting.set(false);
                        view.showDialog_DeletePlaylists(playlistsToDelete, () -> {
                            boolean resetUiIndicator = false;
                            for (AudioPlaylist pl : playlistsToDelete) {
                                if (playingContentSelector.getPage() == MainPage.PLAYLISTS
                                        && Objects.equals(playingContentSelector.getUuid(), pl.getMetadata().getId()))
                                    resetUiIndicator = true;
                                userPlaylistRepository.remove(pl.getMetadata().getId());
                            }
                            if (resetUiIndicator)
                                playingContentSelector = null;
                            else
                                updateVisibleContent();
                            view.showMessage("Playlists Deleted", "Deleted playlists");
                        }, () -> {
                        });
                    } else {
                        AudioPlaylist currentPlaylist = getPlaylistForIndicator(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                        if (currentPlaylist != null) {
                            List<AudioData> tracksToDelete = new ArrayList<>();
                            for (AudioData track : currentPlaylist.getData()) {
                                if (selection.contains(track.getMetadata().getId())) {
                                    tracksToDelete.add(track);
                                }
                            }
                            appViewModel.isSelecting.set(false);
                            view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, () -> {
                                List<AudioData> cleanTracks = currentPlaylist.getData();
                                for (AudioData delTrack : tracksToDelete)
                                    cleanTracks.remove(delTrack);
                                AudioPlaylist clean = new AudioPlaylist(currentPlaylist.getMetadata(), cleanTracks);
                                userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                                updateVisibleContent();
                                view.showMessage("Deleted tracks", "Deleted tracks from playlist");
                            }, () -> {
                            });
                        }
                    }
                } else {
                    AudioPlaylist playlistToDelete = getPlaylistForIndicator(uiContentSelector, deviceAudioRepository, userPlaylistRepository);

                    List<AudioPlaylist> data = new ArrayList<>();
                    data.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(data, () -> {
                        userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                        if (Objects.equals(uiContentSelector, playingContentSelector)) {
                            playingContentSelector = null;
                            uiContentSelector = new ContentSelector(uiContentSelector.getPage(), null);
                            setViewModelContentSelector(uiContentSelector);
                        } else {
                            uiContentSelector = new ContentSelector(uiContentSelector.getPage(), null);
                            setViewModelContentSelector(uiContentSelector);
                        }
                        view.showMessage("Deleted Playlist", "Playlist deleted");
                    }, () -> {
                    });
                }
                break;
        }
    }
}