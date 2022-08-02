package com.phaseshifter.canora.ui.presenters;

import android.util.Log;

import com.google.android.exoplayer2.Player;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.data.settings.StringSetting;
import com.phaseshifter.canora.model.editor.AudioMetadataEditor;
import com.phaseshifter.canora.model.formatting.ListFilter;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.service.MediaPlayerService;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.StateBundle;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.ui.dialog.MainDialogFactory;
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

    private final MediaPlayerService service;

    private final Executor mainThread;

    private final ThreadPoolExecutor presExec;

    private final AppViewModel appViewModel;
    private final ContentViewModel contentViewModel;
    private final PlayerStateViewModel playerStateViewModel;

    private final AudioMetadataEditor metadataEditor;

    private FilterOptions filterOptions;
    private SortingOptions sortingOptions;

    private SelectionIndicator uiIndicator = new SelectionIndicator(AudioContentSelector.TRACKS, null);
    private SelectionIndicator contentIndicator = null;

    private int presenterTasks = 0;

    public MainPresenter(MainContract.View view,
                         Serializable savedState,
                         MediaPlayerService service,
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
        scAudioDataRepo.setClientID(settingsRepository.getString(StringSetting.SC_CLIENTID));
        presExec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        if (savedState instanceof StateBundle) {
            final StateBundle state = (StateBundle) savedState;

            view.setTheme(state.theme);
            appViewModel.devMode.set(state.devMode);

            filterOptions = state.filterOptions;
            sortingOptions = state.sortingOptions;

            // Repositories are stored application wide so the saved indicator uuid should be valid.
            uiIndicator = state.indicator;
            contentIndicator = state.indicator;
        }
    }

    private void runPresenterTask(Runnable task) {
        appViewModel.isContentLoading.set(true);
        presenterTasks++;
        presExec.submit(() -> {
            task.run();
            if (--presenterTasks == 0) {
                appViewModel.isContentLoading.set(false);
            }
        });
    }

    private void reformatContent() {
        // TODO: Preformat content
        List<AudioData> formattedTracks = null;
        List<AudioPlaylist> formattedPlaylists = null;
        if (uiIndicator.isPlaylistView()) {
            switch (uiIndicator.getSelector()) {
                case PLAYLISTS:
                    formattedPlaylists = ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), sortingOptions);
                    break;
                case ARTISTS:
                    formattedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getArtists(), sortingOptions);
                    break;
                case ALBUMS:
                    formattedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getAlbums(), sortingOptions);
                    break;
                case GENRES:
                    formattedPlaylists = ListSorter.sortAudioPlaylist(deviceAudioRepository.getGenres(), sortingOptions);
                    break;
                case SOUNDCLOUD_CHARTS:
                    formattedPlaylists = ListSorter.sortAudioPlaylist(scAudioDataRepo.getChartsPlaylists(), sortingOptions);
                    break;
            }
            formattedPlaylists = ListFilter.filterAudioPlaylist(formattedPlaylists, filterOptions);
        } else {
            switch (uiIndicator.getSelector()) {
                case TRACKS:
                    formattedTracks = ListSorter.sortAudioData(deviceAudioRepository.getTracks(), sortingOptions);
                    break;
                case PLAYLISTS:
                    formattedTracks = ListSorter.sortAudioData(userPlaylistRepository.get(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case ARTISTS:
                    formattedTracks = ListSorter.sortAudioData(deviceAudioRepository.getArtist(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case ALBUMS:
                    formattedTracks = ListSorter.sortAudioData(deviceAudioRepository.getAlbum(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case GENRES:
                    formattedTracks = ListSorter.sortAudioData(deviceAudioRepository.getGenre(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case SOUNDCLOUD_SEARCH:
                    formattedTracks = scAudioDataRepo.getSearchResults();
                    break;
                case SOUNDCLOUD_CHARTS:
                    formattedTracks = ListSorter.sortAudioData(scAudioDataRepo.getChartsPlaylist(uiIndicator.getUuid()).getData(), sortingOptions);
                    break;
                case YOUTUBE_SEARCH:
                    break;
            }
            if (uiIndicator.getSelector() != AudioContentSelector.SOUNDCLOUD_SEARCH) {
                formattedTracks = ListFilter.filterAudioData(formattedTracks, filterOptions);
            }
        }
        contentViewModel.visibleTracks.set(formattedTracks);
        contentViewModel.visiblePlaylists.set(formattedPlaylists);
        if (--presenterTasks == 0) {
            appViewModel.isContentLoading.set(false);
        }
    }

    @Override
    public void update(Observable<PlayerState> observable, PlayerState value) {
        playerStateViewModel.applyPlayerState(value);
    }

    //START Presenter Interface

    @Override
    public synchronized void start() {
        playerStateViewModel.applyPlayerState(service.getState().get());
        service.getState().addObserver(this);
        view.checkPermissions();
        runPresenterTask(() -> {
            deviceAudioRepository.refresh();
            mainThread.execute(this::reformatContent);
        });
    }

    @Override
    public synchronized void stop() {
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
        filterOptions.filterFor = text;
        if (uiIndicator.getSelector() != AudioContentSelector.SOUNDCLOUD_SEARCH) {
            reformatContent();
        }
    }

    @Override
    public void onSearchTextEditingFinished() {
        if (uiIndicator.getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH) {
            String text = filterOptions.filterFor;
            runPresenterTask(() -> {
                scAudioDataRepo.refreshSearch(text);
                mainThread.execute(this::reformatContent);
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

        if (uiIndicator.getSelector() == AudioContentSelector.PLAYLISTS) {
            if (uiIndicator.isPlaylistView() && appViewModel.isSelecting.get()) {
                actions.add(OptionsMenu.Action.DELETE);
            } else if (!uiIndicator.isPlaylistView()) {
                actions.add(OptionsMenu.Action.EDIT_PLAYLIST);
                actions.add(OptionsMenu.Action.DELETE);
            }
        }
        view.showOptionsMenu(actions,
                (OptionsMenu.Action action) -> {

                },
                () -> {

                });
    }

    @Override
    public void onSearchButtonClick() {
        store.dispatch(actionCreator.switchFiltering());
    }

    @Override
    public void onFloatingAddToButtonClick() {

    }

    @Override
    public void onVolumeButtonClick() {
        MainStateImmutable currentState = store.getState();
        view.showDialog_volume(currentState.getVolume());
    }

    @Override
    public void onBackPress() {
        MainStateImmutable currentState = store.getState();
        if (currentState.isControlsMaximized()) {
            store.dispatch(actionCreator.getChangeControlMax(false));
        } else if (currentState.isFiltering()) {
            store.dispatch(actionCreator.getChangeFilterState(false, currentState.getFilterDefinition()));
        } else if (currentState.isSelecting()) {
            store.dispatch(actionCreator.getChangeSelectionMode(false));
        } else if (!currentState.getUiIndicator().equals(currentState.getContentIndicator())
                && !currentState.getUiIndicator().isPlaylistView()
                && currentState.getUiIndicator().getSelector() != AudioContentSelector.TRACKS) {
            store.dispatch(actionCreator.getChangeIndicators(currentState.getContentIndicator(), new SelectionIndicator(currentState.getUiIndicator().getSelector(), null)));
            store.dispatch(actionCreator.getChangeSelectionMode(false));
        } else if (currentState.getContentIndicator() != null && !currentState.getUiIndicator().equals(currentState.getContentIndicator())) {
            store.dispatch(actionCreator.getChangeIndicators(currentState.getContentIndicator(), currentState.getContentIndicator()));
            store.dispatch(actionCreator.getChangeSelectionMode(false));
        } else {
            view.showDialog_Exit();
        }
    }

    @Override
    public void onPermissionCheckResult(boolean permissionsGranted) {
        if (permissionsGranted) {
            store.dispatch(actionCreator.refreshAndFetchRepoData());
        } else {
            view.requestPermissions();
        }
    }

    @Override
    public void onPermissionRequestResult(boolean permissionsGranted) {
        if (permissionsGranted) {
            store.dispatch(actionCreator.refreshAndFetchRepoData());
        } else {
            view.showDialog_error_permissions();
        }
    }

    @Override
    public void onTrackContentClick(int index) {
        MainStateImmutable currentState = store.getState();
        List<AudioData> processedData = currentState.getVisibleTracks();

        //Assertions
        if (processedData == null
                || index > processedData.size() - 1)
            throw new AssertionError();

        if (currentState.isSelecting()) {
            UUID trackid = processedData.get(index).getMetadata().getId();
            HashSet<UUID> copy = new HashSet<>(currentState.getSelection());
            if (copy.contains(trackid)) {
                copy.remove(trackid);
            } else {
                copy.add(trackid);
            }
            store.dispatch(actionCreator.getChangeSelection(copy));
        } else {
            if (!currentState.getUiIndicator().equals(currentState.getContentIndicator())) {
                store.dispatch(actionCreator.getChangeIndicators(currentState.getUiIndicator(), currentState.getUiIndicator()));
            }
            currentState = store.getState();
            service.setContent(currentState.getSortedTracks());
            service.play(processedData.get(index).getMetadata().getId());
        }
    }

    @Override
    public void onTrackContentLongClick(int index) {
        MainStateImmutable currentState = store.getState();
        HashSet<ContextMenu.Action> actions = new HashSet<>();
        actions.add(ContextMenu.Action.SELECT);
        actions.add(ContextMenu.Action.EDIT);
        if (currentState.getUiIndicator().getSelector() == AudioContentSelector.PLAYLISTS) {
            actions.add(ContextMenu.Action.DELETE);
        }
        ContextMenu menu = new ContextMenu(actions);
        view.showMenuTrackContent(index, menu);
    }

    @Override
    public void onPlaylistContentClick(int index) {
        MainStateImmutable currentState = store.getState();
        List<AudioPlaylist> processedData = currentState.getVisiblePlaylists();

        //Assertions
        if (processedData == null
                || index > processedData.size() - 1)
            throw new AssertionError();

        if (currentState.isSelecting()) {
            UUID id = processedData.get(index).getMetadata().getId();
            HashSet<UUID> copy = new HashSet<>(currentState.getSelection());
            if (copy.contains(id)) {
                copy.remove(id);
            } else {
                copy.add(id);
            }
            store.dispatch(actionCreator.getChangeSelection(copy));
        } else {
            store.dispatch(actionCreator.getChangeIndicators(currentState.getContentIndicator(),
                    new SelectionIndicator(currentState.getUiIndicator().getSelector(), processedData.get(index).getMetadata().getId())));
            if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS) {
                store.dispatch(actionCreator.refreshAndFetchRepoData());
            }
        }
    }

    @Override
    public void onPlaylistContentLongClick(int index) {
        MainStateImmutable currentState = store.getState();
        HashSet<ContextMenu.Action> actions = new HashSet<>();
        actions.add(ContextMenu.Action.SELECT);
        if (currentState.getUiIndicator().getSelector() == AudioContentSelector.PLAYLISTS) {
            actions.add(ContextMenu.Action.EDIT);
            actions.add(ContextMenu.Action.DELETE);
        }
        ContextMenu menu = new ContextMenu(actions);
        view.showMenuPlaylistContent(index, menu);
    }

    @Override
    public void onTrackContentScrollToBottom() {
        if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH) {
            if (!store.getState().isScrollLoading()) {
                if (!scAudioDataRepo.isSearchLimitReached()) {
                    store.dispatch(new MainAction(MainActionType.SEARCH_INCREMENT_PAGE));
                    store.dispatch(actionCreator.refreshAndFetchRepoData());
                }
            }
        } else if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS && !store.getState().getUiIndicator().isPlaylistView()) {
            if (!store.getState().isScrollLoading()) {
                if (!scAudioDataRepo.isChartsLimitReached()) {
                    store.dispatch(actionCreator.refreshAndFetchRepoData());
                }
            }
        }
    }

    @Override
    public void onMenuAction(int index, ContextMenu.Action action) {
        MainStateImmutable currentState = store.getState();
        switch (action) {
            case SELECT:
                HashSet<UUID> selection = new HashSet<>(currentState.getSelection());
                if (currentState.getUiIndicator().isPlaylistView()) {
                    AudioPlaylist clickedPlaylist = currentState.getVisiblePlaylists().get(index);
                    if (selection.contains(clickedPlaylist.getMetadata().getId()))
                        selection.remove(clickedPlaylist.getMetadata().getId());
                    else
                        selection.add(clickedPlaylist.getMetadata().getId());
                } else {
                    AudioData clickedTrack = currentState.getVisibleTracks().get(index);
                    if (selection.contains(clickedTrack.getMetadata().getId()))
                        selection.remove(clickedTrack.getMetadata().getId());
                    else
                        selection.add(clickedTrack.getMetadata().getId());
                }
                store.dispatch(actionCreator.getChangeSelectionMode(true));
                store.dispatch(actionCreator.getChangeSelection(selection));
                break;
            case INFO:
                if (currentState.getUiIndicator().isPlaylistView())
                    view.showPlaylistContentDetails(index);
                else
                    view.showContentMenu(index);
                break;
            case EDIT:
                if (currentState.getUiIndicator().isPlaylistView()) {
                    List<AudioPlaylist> playlists = ListSorter.sortAudioPlaylist(currentState.getContentPlaylists(), currentState.getSortingDefinition());
                    if (currentState.isFiltering())
                        playlists = ListFilter.filterAudioPlaylist(playlists, currentState.getFilterDefinition());
                    view.startEditor(playlists.get(index), currentState.getTheme());
                } else {
                    List<AudioData> tracks = ListSorter.sortAudioData(currentState.getContentTracks(), currentState.getSortingDefinition());
                    if (currentState.isFiltering())
                        tracks = ListFilter.filterAudioData(tracks, currentState.getFilterDefinition());
                    AudioData track = tracks.get(index);
                    view.startEditor(track, metadataEditor.getMask(track), currentState.getTheme());
                }
                break;
            case DELETE:
                if (currentState.getUiIndicator().isPlaylistView()) {
                    AudioPlaylist playlistToDelete = currentState.getVisiblePlaylists().get(index);
                    List<AudioPlaylist> playlists = new ArrayList<>();
                    playlists.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(playlists, new MainDialogFactory.DeletePlaylistsListener() {
                        @Override
                        public void onDelete() {
                            userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                            store.dispatch(actionCreator.fetchAudioData());
                            view.showMessage_deletedPlaylist(playlistToDelete.getMetadata().getTitle());
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                } else {
                    AudioPlaylist currentPlaylist = getPlaylistForIndicator(currentState.getUiIndicator(), deviceAudioRepository, userPlaylistRepository);
                    if (currentPlaylist == null)
                        throw new AssertionError();
                    List<AudioData> tracks = currentState.getVisibleTracks();
                    List<AudioData> tracksToDelete = new ArrayList<>();
                    tracksToDelete.add(tracks.get(index));
                    view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, new MainDialogFactory.DeleteTracksFromPlaylistListener() {
                        @Override
                        public void onDelete() {
                            List<AudioData> cleanTracks = currentPlaylist.getData();
                            for (AudioData delTrack : tracksToDelete)
                                cleanTracks.remove(delTrack);
                            AudioPlaylist clean = new AudioPlaylist(currentPlaylist.getMetadata(), cleanTracks);
                            userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                            store.dispatch(actionCreator.fetchAudioData());
                            view.showMessage_deletedTracksFrom(currentPlaylist.getMetadata().getTitle(), tracksToDelete.size());
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onMediaStoreDataChange() {
        Log.v(LOG_TAG, "onMediaStoreDataChange");
        store.dispatch(actionCreator.refreshAndFetchRepoData());
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
                        store.dispatch(actionCreator.refreshAndFetchRepoData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SecurityException se) {
                        Log.v(LOG_TAG, "Oh Noes!!! SecurityException: " + se);
                        view.handleSecurityException(se, () -> {
                            Log.v(LOG_TAG, "OnHandledSecurityException: " + se);
                            try {
                                metadataEditor.writeMetadata(((AudioDataSourceUri) data.getDataSource()).getUri(), data.getMetadata());
                                store.dispatch(actionCreator.refreshAndFetchRepoData());
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
            store.dispatch(actionCreator.refreshAndFetchRepoData());
        }
    }

    @Override
    public void onTransportControlChange(boolean controlMax) {
        store.dispatch(actionCreator.getChangeControlMax(controlMax));
    }

    @Override
    public void onNavigationClick(NavigationItem item) {
        MainStateImmutable currentState = store.getState();
        switch (item) {
            case TRACKS:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.TRACKS, null)));
                break;
            case PLAYLISTS:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.PLAYLISTS, null)));
                break;
            case ALBUMS:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.ALBUMS, null)));
                break;
            case ARTISTS:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.ARTISTS, null)));
                break;
            case GENRES:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.GENRES, null)));
                break;
            case SOUNDCLOUD_SEARCH:
                view.setNavigationMax(false);
                view.setSearchMax(true);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.SOUNDCLOUD_SEARCH, null)));
                store.dispatch(actionCreator.refreshAndFetchRepoData());
                break;
            case SOUNDCLOUD_CHARTS:
                view.setNavigationMax(false);
                store.dispatch(actionCreator.getChangeControlMax(false));
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                store.dispatch(actionCreator.getChangeIndicators(
                        currentState.getContentIndicator(),
                        new SelectionIndicator(AudioContentSelector.SOUNDCLOUD_CHARTS, null)));
                store.dispatch(actionCreator.refreshAndFetchRepoData());
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

    @Override
    public Serializable saveState() {
        settingsRepository.putString(StringSetting.SC_CLIENTID, scAudioDataRepo.getClientID());
        return (MainState) lastState;
    }

    //STOP Presenter Interface

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
                        reformatContent();
                    }
                });
                break;
            case OPEN_FILTEROPTIONS:
                view.showDialog_FilterOptions(filterOptions,
                        (options) -> {
                            boolean changed = filterOptions != options;
                            if (changed){
                                settingsRepository.putInt(IntegerSetting.FILTER_BY, options.filterBy);
                                filterOptions = options;
                                reformatContent();
                            }
                        });
                break;
            case ADD_SELECTION:
                view.showAddSelectionMenu(ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), sortingOptions),
                        () -> {
                            List<AudioData> selectedData = new ArrayList<>();
                            if (uiIndicator.isPlaylistView()) {
                                for (AudioPlaylist playlist : currentState.getContentPlaylists()) {
                                    if (currentState.getSelection().contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getData());
                                }
                            } else {
                                for (AudioData track : currentState.getContentTracks()) {
                                    if (currentState.getSelection().contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }
                            view.showDialog_CreatePlaylist(selectedData, new MainDialogFactory.PlaylistCreateListener() {
                                @Override
                                public void onCreate(String title, List<AudioData> data) {
                                    PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(null,
                                            title,
                                            null
                                    );
                                    AudioPlaylist playlist = new AudioPlaylist(metadata, data);
                                    userPlaylistRepository.add(playlist);
                                    store.dispatch(actionCreator.getChangeSelectionMode(false));
                                    store.dispatch(actionCreator.fetchAudioData());
                                    view.showMessage_createdPlaylist(playlist.getMetadata().getTitle(), data.size());
                                }

                                @Override
                                public void onCancel() {
                                }
                            });
                        },
                        (playlist) -> {
                            MainStateImmutable currentState = store.getState();
                            List<AudioData> selectedData = new ArrayList<>();
                            if (currentState.getUiIndicator().isPlaylistView()) {
                                for (AudioPlaylist playlist : currentState.getContentPlaylists()) {
                                    if (currentState.getSelection().contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getData());
                                }
                            } else {
                                for (AudioData track : currentState.getContentTracks()) {
                                    if (currentState.getSelection().contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }

                            AudioPlaylist target = ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), currentState.getSortingDefinition()).get(index);

                            AudioPlaylist targetCopy = new AudioPlaylist(target);
                            targetCopy.getData().addAll(selectedData);

                            userPlaylistRepository.replace(target.getMetadata().getId(), targetCopy);

                            store.dispatch(actionCreator.getChangeSelectionMode(false));
                            store.dispatch(actionCreator.fetchAudioData());

                            view.showMessage_addedTracks(target.getMetadata().getTitle(), selectedData.size());
                        });
                view.showAddSelectionMenu(true,
                        ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), currentState.getSortingDefinition()),
                        new AddToMenuListener() {
                            @Override
                            public void onAddToNew() {
                                MainStateImmutable currentState = store.getState();
                                List<AudioData> selectedData = new ArrayList<>();
                                if (currentState.getUiIndicator().isPlaylistView()) {
                                    for (AudioPlaylist playlist : currentState.getContentPlaylists()) {
                                        if (currentState.getSelection().contains(playlist.getMetadata().getId()))
                                            selectedData.addAll(playlist.getData());
                                    }
                                } else {
                                    for (AudioData track : currentState.getContentTracks()) {
                                        if (currentState.getSelection().contains(track.getMetadata().getId()))
                                            selectedData.add(track);
                                    }
                                }
                                view.showDialog_CreatePlaylist(selectedData, new MainDialogFactory.PlaylistCreateListener() {
                                    @Override
                                    public void onCreate(String title, List<AudioData> data) {
                                        PlaylistMetadataMemory metadata = new PlaylistMetadataMemory(null,
                                                title,
                                                null
                                        );
                                        AudioPlaylist playlist = new AudioPlaylist(metadata, data);
                                        userPlaylistRepository.add(playlist);
                                        store.dispatch(actionCreator.getChangeSelectionMode(false));
                                        store.dispatch(actionCreator.fetchAudioData());
                                        view.showMessage_createdPlaylist(playlist.getMetadata().getTitle(), data.size());
                                    }

                                    @Override
                                    public void onCancel() {
                                    }
                                });
                            }

                            @Override
                            public void onAddToExisting(int index) {
                                MainStateImmutable currentState = store.getState();
                                List<AudioData> selectedData = new ArrayList<>();
                                if (currentState.getUiIndicator().isPlaylistView()) {
                                    for (AudioPlaylist playlist : currentState.getContentPlaylists()) {
                                        if (currentState.getSelection().contains(playlist.getMetadata().getId()))
                                            selectedData.addAll(playlist.getData());
                                    }
                                } else {
                                    for (AudioData track : currentState.getContentTracks()) {
                                        if (currentState.getSelection().contains(track.getMetadata().getId()))
                                            selectedData.add(track);
                                    }
                                }

                                AudioPlaylist target = ListSorter.sortAudioPlaylist(userPlaylistRepository.getAll(), currentState.getSortingDefinition()).get(index);

                                AudioPlaylist targetCopy = new AudioPlaylist(target);
                                targetCopy.getData().addAll(selectedData);

                                userPlaylistRepository.replace(target.getMetadata().getId(), targetCopy);

                                store.dispatch(actionCreator.getChangeSelectionMode(false));
                                store.dispatch(actionCreator.fetchAudioData());

                                view.showMessage_addedTracks(target.getMetadata().getTitle(), selectedData.size());
                            }
                        });
                break;
            case SELECT_START:
                store.dispatch(actionCreator.getChangeSelectionMode(true));
                break;
            case SELECT_STOP:
                store.dispatch(actionCreator.getChangeSelectionMode(false));
                break;
            case SELECT_ALL:
                HashSet<UUID> selection = new HashSet<>();
                if (currentState.getUiIndicator().isPlaylistView()) {
                    for (AudioPlaylist playlist : currentState.getContentPlaylists()) {
                        selection.add(playlist.getMetadata().getId());
                    }
                } else {
                    for (AudioData track : currentState.getContentTracks()) {
                        selection.add(track.getMetadata().getId());
                    }
                }
                store.dispatch(actionCreator.getChangeSelection(selection));
                break;
            case DESELECT_ALL:
                store.dispatch(actionCreator.getChangeSelection(new HashSet<>()));
                break;
            case EDIT_PLAYLIST:
                AudioPlaylist playlist = getPlaylistForIndicator(currentState.getUiIndicator(), deviceAudioRepository, userPlaylistRepository);
                view.startEditor(playlist, currentState.getTheme());
                break;
            case DELETE:
                if (currentState.isSelecting()) {
                    if (currentState.getUiIndicator().isPlaylistView()) {
                        List<AudioPlaylist> playlistsToDelete = new ArrayList<>();
                        for (AudioPlaylist pl : currentState.getContentPlaylists()) {
                            if (currentState.getSelection().contains(pl.getMetadata().getId()))
                                playlistsToDelete.add(pl);
                        }
                        store.dispatch(actionCreator.getChangeSelectionMode(false));
                        view.showDialog_DeletePlaylists(playlistsToDelete, new MainDialogFactory.DeletePlaylistsListener() {
                            @Override
                            public void onDelete() {
                                boolean resetUiIndicator = false;
                                for (AudioPlaylist playlist : playlistsToDelete) {
                                    if (currentState.getContentIndicator().getSelector() == AudioContentSelector.PLAYLISTS
                                            && Objects.equals(currentState.getContentIndicator().getUuid(), playlist.getMetadata().getId()))
                                        resetUiIndicator = true;
                                    userPlaylistRepository.remove(playlist.getMetadata().getId());
                                }
                                if (resetUiIndicator)
                                    store.dispatch(actionCreator.getChangeIndicators(null, currentState.getUiIndicator()));
                                else
                                    store.dispatch(actionCreator.fetchAudioData());
                                view.showMessage_deletedPlaylists(playlistsToDelete.size());
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                    } else {
                        AudioPlaylist currentPlaylist = getPlaylistForIndicator(currentState.getUiIndicator(), deviceAudioRepository, userPlaylistRepository);
                        if (currentPlaylist != null) {
                            List<AudioData> tracksToDelete = new ArrayList<>();
                            for (AudioData track : currentPlaylist.getData()) {
                                if (currentState.getSelection().contains(track.getMetadata().getId())) {
                                    tracksToDelete.add(track);
                                }
                            }
                            store.dispatch(actionCreator.getChangeSelectionMode(false));
                            view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, new MainDialogFactory.DeleteTracksFromPlaylistListener() {
                                @Override
                                public void onDelete() {
                                    List<AudioData> cleanTracks = currentPlaylist.getData();
                                    for (AudioData delTrack : tracksToDelete)
                                        cleanTracks.remove(delTrack);
                                    AudioPlaylist clean = new AudioPlaylist(currentPlaylist.getMetadata(), cleanTracks);
                                    userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                                    store.dispatch(actionCreator.fetchAudioData());
                                    view.showMessage_deletedTracksFrom(clean.getMetadata().getTitle(), tracksToDelete.size());
                                }

                                @Override
                                public void onCancel() {
                                }
                            });
                        }
                    }
                } else {
                    AudioPlaylist playlistToDelete = getPlaylistForIndicator(currentState.getUiIndicator(), deviceAudioRepository, userPlaylistRepository);

                    List<AudioPlaylist> data = new ArrayList<>();
                    data.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(data, new MainDialogFactory.DeletePlaylistsListener() {
                        @Override
                        public void onDelete() {
                            userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                            MainStateImmutable currentState = store.getState();
                            if (Objects.equals(currentState.getUiIndicator(), currentState.getContentIndicator())) {
                                store.dispatch(actionCreator.getChangeIndicators(
                                        null,
                                        new SelectionIndicator(currentState.getUiIndicator().getSelector(), null))
                                );
                                store.dispatch(actionCreator.getChangeSelectionMode(false));
                            } else {
                                store.dispatch(actionCreator.getChangeIndicators(
                                        currentState.getContentIndicator(),
                                        new SelectionIndicator(currentState.getUiIndicator().getSelector(), null))
                                );
                            }
                            view.showMessage_deletedPlaylist(playlistToDelete.getMetadata().getTitle());
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }
                break;
        }
    }
}