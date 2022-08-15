package com.phaseshifter.canora.ui.presenters;

import android.util.Log;

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

    private ContentSelector uiIndicator = new ContentSelector(MainPage.TRACKS, null);
    private ContentSelector contentIndicator = null;

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
            uiIndicator = state.uiIndicator;
            contentIndicator = state.uiIndicator;
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
        if (uiIndicator.isPlaylistView()) {
            switch (uiIndicator.getPage()) {
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
        } else {
            switch (uiIndicator.getPage()) {
                case TRACKS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getTracks(), sortingOptions);
                    break;
                case PLAYLISTS:
                    sortedTracks = ListSorter.sortAudioData(userPlaylistRepository.get(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case ARTISTS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getArtist(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case ALBUMS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getAlbum(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case GENRES:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getGenre(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case SOUNDCLOUD_SEARCH:
                    sortedTracks = scAudioDataRepo.getSearchResults();
                    break;
                case SOUNDCLOUD_CHARTS:
                    sortedTracks = scAudioDataRepo.getChartsPlaylist(uiIndicator.getUuid()).getData();
                    break;
                case YOUTUBE_SEARCH:
                    break;
            }
            if (uiIndicator.getPage() != MainPage.SOUNDCLOUD_SEARCH) {
                formattedTracks = ListFilter.filterAudioData(sortedTracks, filterOptions);
            } else {
                formattedTracks = sortedTracks;
            }
        }
        contentViewModel.visibleTracks.set(formattedTracks);
        contentViewModel.visiblePlaylists.set(formattedPlaylists);
        if (!uiIndicator.isPlaylistView() && contentViewModel.visibleTracks.get().isEmpty()) {
            if (uiIndicator.getPage() == MainPage.SOUNDCLOUD_SEARCH){
                appViewModel.notFoundText.set("Enter text to begin searching soundcloud...");
            } else {
                appViewModel.notFoundText.set("No tracks found");
            }
        } else if (uiIndicator.isPlaylistView() && contentViewModel.visiblePlaylists.get().isEmpty()) {
            appViewModel.notFoundText.set("No playlists found");
        } else {
            appViewModel.notFoundText.set(null);
        }
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
        savedState.uiIndicator = uiIndicator;
        savedState.contentIndicator = contentIndicator;
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
        assert(text != null);
        filterOptions.filterFor = text;
        if (uiIndicator.getPage() != MainPage.SOUNDCLOUD_SEARCH) {
            updateVisibleContent();
        }
    }

    @Override
    public void onSearchTextEditingFinished() {
        if (uiIndicator.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
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

        if (uiIndicator.getPage() == MainPage.PLAYLISTS) {
            if (uiIndicator.isPlaylistView() && appViewModel.isSelecting.get()) {
                actions.add(OptionsMenu.Action.DELETE);
            } else if (!uiIndicator.isPlaylistView()) {
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
        if (!uiIndicator.equals(contentIndicator)
                && !uiIndicator.isPlaylistView()
                && uiIndicator.getPage() != MainPage.TRACKS) {
            uiIndicator = new ContentSelector(uiIndicator.getPage(), null);
            appViewModel.isSelecting.set(false);
            appViewModel.contentSelector.set(uiIndicator);
            updateVisibleContent();
        } else if (contentIndicator != null && !uiIndicator.equals(contentIndicator)) {
            uiIndicator = contentIndicator;
            appViewModel.isSelecting.set(false);
            appViewModel.contentSelector.set(uiIndicator);
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
            boolean reformat = !uiIndicator.equals(contentIndicator);
            contentIndicator = uiIndicator;
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
        if (uiIndicator.getPage() == MainPage.PLAYLISTS) {
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
            uiIndicator = new ContentSelector(uiIndicator.getPage(), processedData.get(index).getMetadata().getId());
            if (uiIndicator.getPage() == MainPage.SOUNDCLOUD_CHARTS) {
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(uiIndicator.getUuid()));
                    mainThread.execute(() -> {
                        updateVisibleContent();
                        appViewModel.contentSelector.set(uiIndicator);
                    });
                });
            } else {
                updateVisibleContent();
                appViewModel.contentSelector.set(uiIndicator);
            }
        }
    }

    @Override
    public void onPlaylistContentLongClick(int index) {
        HashSet<ContextMenu.Action> actions = new HashSet<>();
        actions.add(ContextMenu.Action.SELECT);
        if (uiIndicator.getPage() == MainPage.PLAYLISTS) {
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
        if (uiIndicator.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
            if (!isScrollLoading) {
                isScrollLoading = true;
                if (!scAudioDataRepo.isSearchLimitReached()) {
                    scAudioDataRepo.refreshSearch(appViewModel.searchText.get());
                    updateVisibleContent();
                }
            }
        } else if (uiIndicator.getPage() == MainPage.SOUNDCLOUD_CHARTS && !uiIndicator.isPlaylistView()) {
            if (!isScrollLoading) {
                if (!scAudioDataRepo.isChartsLimitReached()) {
                    scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(uiIndicator.getUuid()));
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
                uiIndicator = new ContentSelector(MainPage.TRACKS, null);
                appViewModel.contentSelector.set(uiIndicator);
                updateVisibleContent();
                break;
            case PLAYLISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.PLAYLISTS, null);
                appViewModel.contentSelector.set(uiIndicator);
                updateVisibleContent();
                break;
            case ALBUMS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.ALBUMS, null);
                appViewModel.contentSelector.set(uiIndicator);
                updateVisibleContent();
                break;
            case ARTISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.ARTISTS, null);
                appViewModel.contentSelector.set(uiIndicator);
                updateVisibleContent();
                break;
            case GENRES:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.GENRES, null);
                appViewModel.contentSelector.set(uiIndicator);
                updateVisibleContent();
                break;
            case SOUNDCLOUD_SEARCH:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(true);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.SOUNDCLOUD_SEARCH, null);
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshSearch(appViewModel.searchText.get());
                    mainThread.execute(() -> {
                        appViewModel.contentSelector.set(uiIndicator);
                        updateVisibleContent();
                    });
                });
                break;
            case SOUNDCLOUD_CHARTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                uiIndicator = new ContentSelector(MainPage.SOUNDCLOUD_CHARTS, null);
                appViewModel.contentSelector.set(uiIndicator);
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
                if (uiIndicator.isPlaylistView()) {
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
                if (uiIndicator.isPlaylistView()) {
                    view.startEditor(contentViewModel.visiblePlaylists.get().get(index), theme);
                } else {
                    AudioData track = contentViewModel.visibleTracks.get().get(index);
                    view.startEditor(track, metadataEditor.getMask(track), theme);
                }
                break;
            case DELETE:
                if (uiIndicator.isPlaylistView()) {
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
                    AudioPlaylist currentPlaylist = MainSelector.getPlaylistForIndicator(uiIndicator, deviceAudioRepository, userPlaylistRepository);
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
                            if (uiIndicator.isPlaylistView()) {
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
                            if (uiIndicator.isPlaylistView()) {
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
                if (uiIndicator.isPlaylistView()) {
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
                AudioPlaylist playlist = getPlaylistForIndicator(uiIndicator, deviceAudioRepository, userPlaylistRepository);
                view.startEditor(playlist, theme);
                break;
            case DELETE:
                if (appViewModel.isSelecting.get()) {
                    if (uiIndicator.isPlaylistView()) {
                        List<AudioPlaylist> playlistsToDelete = new ArrayList<>();
                        for (AudioPlaylist pl : sortedPlaylists) {
                            if (selection.contains(pl.getMetadata().getId()))
                                playlistsToDelete.add(pl);
                        }
                        appViewModel.isSelecting.set(false);
                        view.showDialog_DeletePlaylists(playlistsToDelete, () -> {
                            boolean resetUiIndicator = false;
                            for (AudioPlaylist pl : playlistsToDelete) {
                                if (contentIndicator.getPage() == MainPage.PLAYLISTS
                                        && Objects.equals(contentIndicator.getUuid(), pl.getMetadata().getId()))
                                    resetUiIndicator = true;
                                userPlaylistRepository.remove(pl.getMetadata().getId());
                            }
                            if (resetUiIndicator)
                                contentIndicator = null;
                            else
                                updateVisibleContent();
                            view.showMessage("Playlists Deleted", "Deleted playlists");
                        }, () -> {
                        });
                    } else {
                        AudioPlaylist currentPlaylist = getPlaylistForIndicator(uiIndicator, deviceAudioRepository, userPlaylistRepository);
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
                    AudioPlaylist playlistToDelete = getPlaylistForIndicator(uiIndicator, deviceAudioRepository, userPlaylistRepository);

                    List<AudioPlaylist> data = new ArrayList<>();
                    data.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(data, () -> {
                        userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                        if (Objects.equals(uiIndicator, contentIndicator)) {
                            contentIndicator = null;
                            uiIndicator = new ContentSelector(uiIndicator.getPage(), null);
                            appViewModel.contentSelector.set(uiIndicator);
                        } else {
                            uiIndicator = new ContentSelector(uiIndicator.getPage(), null);
                            appViewModel.contentSelector.set(uiIndicator);
                        }
                        view.showMessage("Deleted Playlist", "Playlist deleted");
                    }, () -> {
                    });
                }
                break;
        }
    }
}