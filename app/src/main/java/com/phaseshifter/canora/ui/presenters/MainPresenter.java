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
import com.phaseshifter.canora.model.editor.AudioMetadataEditor;
import com.phaseshifter.canora.model.formatting.ListFilter;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.model.repo.SettingsRepository;
import com.phaseshifter.canora.model.repo.ThemeRepository;
import com.phaseshifter.canora.service.MediaPlayerService;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.constants.NavigationItem;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.ui.menu.ContextMenu;
import com.phaseshifter.canora.ui.menu.OptionsMenu;
import com.phaseshifter.canora.ui.dialog.MainDialogFactory;
import com.phaseshifter.canora.ui.viewmodels.ContentViewModel;
import com.phaseshifter.canora.ui.viewmodels.PlayerStateViewModel;
import com.phaseshifter.canora.utils.Observable;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.phaseshifter.canora.ui.utils.selectors.MainSelector.getPlaylistForIndicator;

public class MainPresenter implements MainContract.Presenter {
    private final String LOG_TAG = "MainPresenter";
    private final MainContract.View view;
    private final Store<MainStateImmutable> store;

    private final AudioDataRepository audioDataRepository;
    private final AudioPlaylistRepository audioPlaylistRepository;
    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final SCAudioDataRepo scAudioDataRepo;

    private final MediaPlayerService service;

    private final Executor mainThread;

    private final ThreadPoolExecutor presExec;

    private final ContentViewModel contentViewModel;
    private final PlayerStateViewModel playerStateViewModel;

    private final Observable.Observer<PlayerState> serviceStateObserver = new Observable.Observer<PlayerState>() {
        @Override
        public void update(Observable<PlayerState> o, PlayerState arg) {
            store.dispatch(actionCreator.setPlaybackState(arg));
        }
    };

    private final AudioMetadataEditor metadataEditor;

    private MainStateImmutable lastState = null;

    public MainPresenter(MainContract.View view,
                         Serializable savedState,
                         MediaPlayerService service,
                         AudioDataRepository audioDataRepository,
                         AudioPlaylistRepository audioPlaylistRepository,
                         SettingsRepository settingsRepository,
                         ThemeRepository themeRepository,
                         SCAudioDataRepo scAudioDataRepo,
                         AudioMetadataEditor metadataEditor,
                         Executor mainThread,
                         ContentViewModel contentViewModel,
                         PlayerStateViewModel playerStateViewModel) {
        this.view = view;
        this.service = service;
        this.audioDataRepository = audioDataRepository;
        this.audioPlaylistRepository = audioPlaylistRepository;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.scAudioDataRepo = scAudioDataRepo;
        this.metadataEditor = metadataEditor;
        this.mainThread = mainThread;
        scAudioDataRepo.setClientID(settingsRepository.getString(StringSetting.SC_CLIENTID));
        presExec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        final MainState cachedState;
        if (savedState instanceof MainState)
            cachedState = (MainState) savedState;
        else
            cachedState = null;
        store = new StoreFactory<MainStateImmutable>()
                .setReducer(new MainReducer())
                .setPreloadedState(cachedState)
                .addMiddleware(new Thunk<>())
                .addMiddleware(new ActionLogger<>())
                .build();
        store.subscribe(this);
        for (StateListener<MainStateImmutable> listener : viewmodels) {
            store.subscribe(listener);
        }

        this.actionCreator = new MainActionCreator(store, audioDataRepository, audioPlaylistRepository, settingsRepository, themeRepository, scAudioDataRepo, service, presExec, mainThread);
    }

    @Override
    public void update(MainStateImmutable updatedState) {
        Log.v(LOG_TAG, "State Update " + updatedState);
        boolean themeChange = false;
        if (updatedState.getTheme() != null) {
            if (lastState == null
                    || !updatedState.getTheme().equals(lastState.getTheme())) {
                view.setTheme(updatedState.getTheme());
                themeChange = true;
            }
        }

        if (lastState == null || lastState.isDevMode() != updatedState.isDevMode())
            view.setDebugDisplay(updatedState.isDevMode());

        if (themeChange
                || lastState == null
                || !Objects.equals(updatedState.isFiltering(), lastState.isFiltering()))
            view.setSearchMax(updatedState.isFiltering());

        if (themeChange
                || lastState == null
                || !Objects.equals(updatedState.isControlsMaximized(), lastState.isControlsMaximized()))
            view.setControlMax(updatedState.isControlsMaximized());

        if (themeChange || lastState == null || lastState.getUiIndicator() != updatedState.getUiIndicator()) {
            if (updatedState.getUiIndicator().isPlaylistView()) {
                view.showPlaylistContent();
            } else {
                view.showTrackContent();
            }
        }
        lastState = updatedState;
    }

    //START Presenter Interface

    @Override
    public synchronized void start() {
        store.dispatch(actionCreator.setPlaybackState(null)); //Invalidate PlaybackState
        store.dispatch(actionCreator.setPlaybackState(service.getState().get())); //Get PlaybackState in case it already exists
        service.getState().addObserver(serviceStateObserver);
        view.checkPermissions();
    }

    @Override
    public synchronized void stop() {
        service.getState().removeObserver(serviceStateObserver);
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
        MainStateImmutable currentState = store.getState();
        boolean shuffle = !currentState.isShuffling();

        settingsRepository.putBoolean(BooleanSetting.SHUFFLE, shuffle);
        service.setShuffle(shuffle);
        store.dispatch(actionCreator.getChangeShuffle(shuffle));
    }

    @Override
    public void onRepeatSwitch() {
        MainStateImmutable currentState = store.getState();
        boolean repeat = !currentState.isRepeating();

        settingsRepository.putBoolean(BooleanSetting.REPEAT, repeat);
        service.setRepeat(repeat);
        store.dispatch(actionCreator.getChangeRepeat(repeat));
    }

    @Override
    public void onVolumeSeek(float p) {
        if (p > 1.0f || p < 0.0f)
            throw new RuntimeException("Received invalid Volume value: " + p);
        settingsRepository.putFloat(FloatSetting.VOLUME, p);
        service.setVolume(p);
        store.dispatch(actionCreator.getChangeVolume(p));
    }

    @Override
    public void onSearchTextChange(String text) {
        store.dispatch(actionCreator.searchTextChange(text));
    }

    @Override
    public void onSearchTextEditingFinished() {
        if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH) {
            if (!store.getState().getFilterDefinition().filterFor.equals(scAudioDataRepo.getSearchText())) {
                store.dispatch(new MainAction(MainActionType.SEARCH_RESET_PAGE));
            }
            store.dispatch(actionCreator.refreshAndFetchRepoData());
        }
    }

    @Override
    public void onNavigationButtonClick() {
        view.setNavigationMax(true);
    }

    @Override
    public void onOptionsButtonClick() {
        MainStateImmutable currentState = store.getState();
        HashSet<OptionsMenu.Action> actions = new HashSet<>();
        actions.add(OptionsMenu.Action.OPEN_SETTINGS);
        actions.add(OptionsMenu.Action.OPEN_SORTOPTIONS);
        actions.add(OptionsMenu.Action.OPEN_FILTEROPTIONS);
        if (currentState.isSelecting()) {
            actions.add(OptionsMenu.Action.ADD_SELECTION);
            actions.add(OptionsMenu.Action.SELECT_ALL);
            actions.add(OptionsMenu.Action.DESELECT_ALL);
            actions.add(OptionsMenu.Action.SELECT_STOP);
        } else {
            actions.add(OptionsMenu.Action.SELECT_START);
        }
        if (currentState.getUiIndicator().getSelector() == AudioContentSelector.PLAYLISTS) {
            if (currentState.getUiIndicator().isPlaylistView()
                    && currentState.isSelecting()) {
                actions.add(OptionsMenu.Action.DELETE);
            } else if (!currentState.getUiIndicator().isPlaylistView()) {
                actions.add(OptionsMenu.Action.EDIT_PLAYLIST);
                actions.add(OptionsMenu.Action.DELETE);
            }
        }
        OptionsMenu menu = new OptionsMenu(actions);
        view.showOptionsMenu(menu);
    }

    @Override
    public void onSearchButtonClick() {
        store.dispatch(actionCreator.switchFiltering());
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
    public void onMenuAction(OptionsMenu.Action action) {
        MainStateImmutable currentState = store.getState();
        switch (action) {
            case OPEN_SETTINGS:
                view.startSettings();
                break;
            case OPEN_SORTOPTIONS:
                view.showDialog_SortOptions(currentState.getSortingDefinition(), new MainDialogFactory.SortingOptionsListener() {
                    @Override
                    public void onApply(SortingOptions updatedData) {
                        settingsRepository.putInt(IntegerSetting.SORT_BY, updatedData.sortby);
                        settingsRepository.putInt(IntegerSetting.SORT_DIR, updatedData.sortdir);
                        settingsRepository.putInt(IntegerSetting.SORT_TECH, updatedData.sorttech);
                        store.dispatch(actionCreator.getChangeSortingState(updatedData));
                    }
                });
                break;
            case OPEN_FILTEROPTIONS:
                view.showDialog_FilterOptions(currentState.getFilterDefinition(), new MainDialogFactory.FilterOptionsListener() {
                    @Override
                    public void onApply(FilterOptions updatedData) {
                        MainStateImmutable currentState = store.getState();
                        settingsRepository.putInt(IntegerSetting.FILTER_BY, updatedData.filterBy);
                        store.dispatch(actionCreator.getChangeFilterState(currentState.isFiltering(), updatedData));
                    }
                });
                break;
            case ADD_SELECTION:
                view.showAddSelectionMenu(true,
                        ListSorter.sortAudioPlaylist(audioPlaylistRepository.getAll(), currentState.getSortingDefinition()),
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
                                        audioPlaylistRepository.add(playlist);
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

                                AudioPlaylist target = ListSorter.sortAudioPlaylist(audioPlaylistRepository.getAll(), currentState.getSortingDefinition()).get(index);

                                AudioPlaylist targetCopy = new AudioPlaylist(target);
                                targetCopy.getData().addAll(selectedData);

                                audioPlaylistRepository.replace(target.getMetadata().getId(), targetCopy);

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
                AudioPlaylist playlist = getPlaylistForIndicator(currentState.getUiIndicator(), audioDataRepository, audioPlaylistRepository);
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
                                    audioPlaylistRepository.remove(playlist.getMetadata().getId());
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
                        AudioPlaylist currentPlaylist = getPlaylistForIndicator(currentState.getUiIndicator(), audioDataRepository, audioPlaylistRepository);
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
                                    audioPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
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
                    AudioPlaylist playlistToDelete = getPlaylistForIndicator(currentState.getUiIndicator(), audioDataRepository, audioPlaylistRepository);

                    List<AudioPlaylist> data = new ArrayList<>();
                    data.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(data, new MainDialogFactory.DeletePlaylistsListener() {
                        @Override
                        public void onDelete() {
                            audioPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
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
                            audioPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                            store.dispatch(actionCreator.fetchAudioData());
                            view.showMessage_deletedPlaylist(playlistToDelete.getMetadata().getTitle());
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                } else {
                    AudioPlaylist currentPlaylist = getPlaylistForIndicator(currentState.getUiIndicator(), audioDataRepository, audioPlaylistRepository);
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
                            audioPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
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
                audioPlaylistRepository.remove(data.getMetadata().getId());
            } else {
                Log.v(LOG_TAG, "Replacing Playlist " + data);
                audioPlaylistRepository.replace(data.getMetadata().getId(), data);
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
}