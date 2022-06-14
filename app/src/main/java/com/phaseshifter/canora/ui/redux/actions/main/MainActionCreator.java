package com.phaseshifter.canora.ui.redux.actions.main;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.model.formatting.ListFilter;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.model.repo.SettingsRepository;
import com.phaseshifter.canora.model.repo.ThemeRepository;
import com.phaseshifter.canora.service.MediaPlayerService;
import com.phaseshifter.canora.service.state.PlayerState;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;
import com.phaseshifter.canora.ui.data.formatting.SortDef;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.ui.redux.core.Action;
import com.phaseshifter.canora.ui.redux.core.ActionChain;
import com.phaseshifter.canora.ui.redux.core.Store;
import com.phaseshifter.canora.ui.redux.middlewares.Thunk;
import com.phaseshifter.canora.ui.redux.state.MainState;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;
import com.phaseshifter.canora.ui.utils.selectors.MainSelector;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

public class MainActionCreator {
    private static final String LOG_TAG = "MainActionCreator";

    private final Store<MainStateImmutable> store;
    private final AudioDataRepository audioDataRepository;
    private final AudioPlaylistRepository audioPlaylistRepository;
    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final SCAudioDataRepo scAudioDataRepo;
    private final MediaPlayerService service;
    private final Executor presExec;
    private final Executor mainExec;

    public MainActionCreator(Store<MainStateImmutable> store,
                             AudioDataRepository audioDataRepository,
                             AudioPlaylistRepository audioPlaylistRepository,
                             SettingsRepository settingsRepository,
                             ThemeRepository themeRepository,
                             SCAudioDataRepo scAudioDataRepo,
                             MediaPlayerService service,
                             Executor presExec,
                             Executor mainExec) {
        this.store = store;
        this.audioDataRepository = audioDataRepository;
        this.audioPlaylistRepository = audioPlaylistRepository;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.scAudioDataRepo = scAudioDataRepo;
        this.service = service;
        this.presExec = presExec;
        this.mainExec = mainExec;
    }

    public Action refreshAndFetchRepoData() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                if (store.getState().getContentLoadSemaphore() > 0) {
                    return null;
                }

                store.dispatch(new MainAction(MainActionType.CONTENT_LOAD_START));
                store.dispatch(new MainAction(MainActionType.SEARCH_LOAD_START));

                presExec.execute(() -> {
                    audioDataRepository.refresh();

                    if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH) {
                        String q = store.getState().getFilterDefinition().filterFor;
                        int page = store.getState().getSearchPage();
                        if (scAudioDataRepo.getSearchText() == null || !scAudioDataRepo.getSearchText().equals(q) || scAudioDataRepo.getSearchPage() != page) {
                            scAudioDataRepo.refreshSearch(q);
                        }
                    } else if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS) {
                        if (!store.getState().getUiIndicator().isPlaylistView()) {
                            // Get the index of the playlist into the contentPlaylists array list based on the uuid.
                            scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(store.getState().getUiIndicator().getUuid()));
                        }
                    }

                    mainExec.execute(() -> {
                        store.dispatch(fetchRepoData());
                        store.dispatch(new MainAction(MainActionType.CONTENT_LOAD_STOP));
                        store.dispatch(new MainAction(MainActionType.SEARCH_LOAD_STOP));
                    });
                });

                return null;
            }
        };
    }

    public Action fetchRepoData() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                store.dispatch(new MainAction(MainActionType.CONTENT_LOAD_START));

                ActionChain chain = new ActionChain();

                MainState payload = new MainState();
                payload.setTheme(themeRepository.get(settingsRepository.getInt(IntegerSetting.THEME)));
                chain.chain(new MainAction(MainActionType.SET_THEME, payload));

                payload = new MainState();
                payload.setSortingDefinition(new SortDef(
                        settingsRepository.getInt(IntegerSetting.SORT_BY),
                        settingsRepository.getInt(IntegerSetting.SORT_DIR),
                        settingsRepository.getInt(IntegerSetting.SORT_TECH))
                );
                chain.chain(new MainAction(MainActionType.SET_SORTDEF, payload));

                MainStateImmutable currentState = store.getState();

                payload = new MainState();
                payload.setFiltering(currentState.isFiltering());
                payload.setFilterDefinition(new FilterDef(settingsRepository.getInt(IntegerSetting.FILTER_BY), currentState.getFilterDefinition().filterFor));
                chain.chain(new MainAction(MainActionType.SET_FILTERSTATE, payload));

                payload = new MainState();
                payload.setVolume(settingsRepository.getFloat(FloatSetting.VOLUME));
                chain.chain(new MainAction(MainActionType.SET_VOLUME, payload));

                payload = new MainState();
                payload.setRepeating(settingsRepository.getBoolean(BooleanSetting.REPEAT));
                chain.chain(new MainAction(MainActionType.SET_REPEAT, payload));

                payload = new MainState();
                payload.setShuffling(settingsRepository.getBoolean(BooleanSetting.SHUFFLE));
                chain.chain(new MainAction(MainActionType.SET_SHUFFLE, payload));

                payload = new MainState();
                payload.setDevMode(settingsRepository.getBoolean(BooleanSetting.DEVELOPERMODE));
                chain.chain(new MainAction(MainActionType.SET_DEVMODE, payload));

                store.dispatch(chain);

                currentState = store.getState();

                service.setVolume(currentState.getVolume());
                service.setRepeat(currentState.isRepeating());
                service.setShuffle(currentState.isShuffling());

                store.dispatch(fetchAudioData());
                return new MainAction(MainActionType.CONTENT_LOAD_STOP);
            }
        };
    }

    public Action fetchAudioData() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                store.dispatch(new MainAction(MainActionType.CONTENT_LOAD_START));

                MainStateImmutable currentState = store.getState();

                SelectionIndicator uiPatch = patchIndicator(currentState.getUiIndicator(), audioDataRepository);
                SelectionIndicator contentPatch = patchIndicator(currentState.getContentIndicator(), audioDataRepository);
                if (!uiPatch.equals(currentState.getUiIndicator())
                        || !Objects.equals(contentPatch, currentState.getContentIndicator())) {
                    MainState payload = new MainState();
                    payload.setContentIndicator(contentPatch);
                    payload.setUiIndicator(uiPatch);
                    store.dispatch(new MainAction(MainActionType.SET_INDICATORS, payload));
                }

                MainState payload = new MainState();
                payload.setVisibleTracks(currentState.getVisibleTracks());
                payload.setVisiblePlaylists(currentState.getVisiblePlaylists());

                List<AudioData> trackData;
                List<AudioPlaylist> playlistData;
                switch (currentState.getUiIndicator().getSelector()) {
                    case TRACKS:
                        trackData = audioDataRepository.getTracks();
                        payload.setContentTracks(trackData);
                        payload.setContentPlaylists(currentState.getContentPlaylists());
                        break;
                    case PLAYLISTS:
                        if (currentState.getUiIndicator().isPlaylistView()) {
                            playlistData = audioPlaylistRepository.getAll();
                            payload.setContentTracks(currentState.getContentTracks());
                            payload.setContentPlaylists(playlistData);
                        } else {
                            trackData = audioPlaylistRepository.get(currentState.getUiIndicator().getUuid()).getData();
                            payload.setContentTracks(trackData);
                            payload.setContentPlaylists(currentState.getContentPlaylists());
                        }
                        break;
                    case ALBUMS:
                        if (currentState.getUiIndicator().isPlaylistView()) {
                            playlistData = audioDataRepository.getAlbums();
                            payload.setContentTracks(currentState.getContentTracks());
                            payload.setContentPlaylists(playlistData);
                        } else {
                            trackData = audioDataRepository.getAlbum(currentState.getUiIndicator().getUuid()).getData();
                            payload.setContentTracks(trackData);
                            payload.setContentPlaylists(currentState.getContentPlaylists());
                        }
                        break;
                    case ARTISTS:
                        if (currentState.getUiIndicator().isPlaylistView()) {
                            playlistData = audioDataRepository.getArtists();
                            payload.setContentTracks(currentState.getContentTracks());
                            payload.setContentPlaylists(playlistData);
                        } else {
                            trackData = audioDataRepository.getArtist(currentState.getUiIndicator().getUuid()).getData();
                            payload.setContentTracks(trackData);
                            payload.setContentPlaylists(currentState.getContentPlaylists());
                        }
                        break;
                    case GENRES:
                        if (currentState.getUiIndicator().isPlaylistView()) {
                            playlistData = audioDataRepository.getGenres();
                            payload.setContentTracks(currentState.getContentTracks());
                            payload.setContentPlaylists(playlistData);
                        } else {
                            trackData = audioDataRepository.getGenre(currentState.getUiIndicator().getUuid()).getData();
                            payload.setContentTracks(trackData);
                            payload.setContentPlaylists(currentState.getContentPlaylists());
                        }
                        break;
                    case SOUNDCLOUD_SEARCH:
                        List<AudioData> t = scAudioDataRepo.getSearchResults();
                        payload.setContentTracks(t);
                        payload.setContentPlaylists(currentState.getContentPlaylists());
                        break;
                    case SOUNDCLOUD_CHARTS:
                        if (currentState.getUiIndicator().isPlaylistView()) {
                            List<AudioPlaylist> pl = scAudioDataRepo.getChartsPlaylists();
                            payload.setContentTracks(currentState.getContentTracks());
                            payload.setContentPlaylists(pl);
                        } else {
                            int index = scAudioDataRepo.getChartsIndex(currentState.getUiIndicator().getUuid());
                            payload.setContentTracks(scAudioDataRepo.getChartsPlaylists().get(index).getData());
                            payload.setContentPlaylists(currentState.getContentPlaylists());
                        }
                        break;
                    case YOUTUBE_SEARCH:
                        break;
                }

                store.dispatch(new MainAction(MainActionType.SET_CONTENT, payload));
                store.dispatch(getReformatContent());

                return new MainAction(MainActionType.CONTENT_LOAD_STOP);
            }
        };
    }

    public Action setPlaybackState(PlayerState state) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setPlayerState(state);
                return new MainAction(MainActionType.SET_PLAYERSTATE, payload);
            }
        };
    }

    public Action switchFiltering() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                if (store.getState().isFiltering()) {
                    store.dispatch(new MainAction(MainActionType.FILTER_DISABLE));
                } else {
                    MainStateImmutable currentState = store.getState();
                    if (currentState.isControlsMaximized())
                        store.dispatch(new MainAction(MainActionType.CONTROL_MIN));
                    store.dispatch(new MainAction(MainActionType.FILTER_ENABLE));
                }
                return getFilterContent();
            }
        };
    }

    public Action searchTextChange(String text) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainStateImmutable currentState = store.getState();
                MainState payload = new MainState();
                payload.setFilterDefinition(new FilterDef(currentState.getFilterDefinition().filterBy, text));
                store.dispatch(new MainAction(MainActionType.FILTER_SETDEF, payload));
                return getFilterContent();
            }
        };
    }

    public Action getChangeSortingState(SortDef def) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setSortingDefinition(def);
                store.dispatch(new MainAction(MainActionType.SET_SORTDEF, payload));
                return fetchAudioData();
            }
        };
    }

    public Action getChangeFilterState(boolean filtering) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                return getChangeFilterState(filtering, store.getState().getFilterDefinition());
            }
        };
    }

    public Action getChangeFilterState(boolean filtering, FilterDef def) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState state = new MainState();
                state.setFiltering(filtering);
                state.setFilterDefinition(def);
                store.dispatch(new MainAction(MainActionType.SET_FILTERSTATE, state));
                return fetchAudioData();
            }
        };
    }

    public Action getChangeIndicators(SelectionIndicator contentIndicator,
                                      SelectionIndicator uiIndicator) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                store.dispatch(new MainAction(MainActionType.FILTER_DISABLE));
                MainState state = new MainState();
                state.setContentIndicator(contentIndicator);
                state.setUiIndicator(uiIndicator);
                store.dispatch(new MainAction(MainActionType.SET_INDICATORS, state));
                return fetchAudioData();
            }
        };
    }

    public Action getChangeSelectionMode(boolean isSelecting) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                if (isSelecting) {
                    return new MainAction(MainActionType.SELECTION_ENABLE);
                } else {
                    store.dispatch(getChangeSelection(new HashSet<>()));
                    return new MainAction(MainActionType.SELECTION_DISABLE);
                }
            }
        };
    }

    public Action getChangeSelection(HashSet<UUID> selection) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState state = new MainState();
                state.setSelection(selection);
                return new MainAction(MainActionType.SET_SELECTION, state);
            }
        };
    }

    public Action getChangeControlMax(boolean controlMax) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setControlsMaximized(controlMax);
                return new MainAction(MainActionType.CONTROL_SET, payload);
            }
        };
    }

    public Action getChangeVolume(float volume) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setVolume(volume);
                return new MainAction(MainActionType.SET_VOLUME, payload);
            }
        };
    }

    public Action getChangeShuffle(boolean shuffle) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setShuffling(shuffle);
                return new MainAction(MainActionType.SET_SHUFFLE, payload);
            }
        };
    }

    public Action getChangeRepeat(boolean repeat) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setRepeating(repeat);
                return new MainAction(MainActionType.SET_REPEAT, payload);
            }
        };
    }

    public Action getChangeDevMode(boolean devMode) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setDevMode(devMode);
                return new MainAction(MainActionType.SET_DEVMODE, payload);
            }
        };
    }

    public Action getReformatContent() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainStateImmutable currentState = store.getState();
                List<AudioData> processedTracks = currentState.getVisibleTracks();
                List<AudioPlaylist> processedPlaylists = currentState.getVisiblePlaylists();

                MainState payload = new MainState();
                if (currentState.getUiIndicator().isPlaylistView()) {
                    processedPlaylists = ListSorter.sortAudioPlaylist(currentState.getContentPlaylists(), currentState.getSortingDefinition());
                    payload.setSortedPlaylists(processedPlaylists);
                    if (currentState.isFiltering())
                        processedPlaylists = ListFilter.filterAudioPlaylist(processedPlaylists, currentState.getFilterDefinition());
                } else {
                    if (currentState.getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH
                            || currentState.getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS) {
                        // Do not sort the content of chart contents and search results because they are infinite scrolls.
                        processedTracks = currentState.getContentTracks();
                        payload.setSortedTracks(processedTracks);
                        if (currentState.getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS) {
                            if (currentState.isFiltering())
                                processedTracks = ListFilter.filterAudioData(processedTracks, currentState.getFilterDefinition());
                        }
                    } else {
                        processedTracks = ListSorter.sortAudioData(currentState.getContentTracks(), currentState.getSortingDefinition());
                        payload.setSortedTracks(processedTracks);
                        if (currentState.isFiltering())
                            processedTracks = ListFilter.filterAudioData(processedTracks, currentState.getFilterDefinition());
                    }
                }
                payload.setContentTracks(currentState.getContentTracks());
                payload.setContentPlaylists(currentState.getContentPlaylists());
                payload.setVisibleTracks(processedTracks);
                payload.setVisiblePlaylists(processedPlaylists);
                return new MainAction(MainActionType.SET_CONTENT, payload);
            }
        };
    }

    public Action getFilterContent() {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainStateImmutable currentState = store.getState();
                MainState payload = new MainState();
                payload.setContentTracks(currentState.getContentTracks());
                payload.setContentPlaylists(currentState.getContentPlaylists());
                payload.setSortedTracks(currentState.getSortedTracks());
                payload.setSortedPlaylists(currentState.getSortedPlaylists());
                payload.setVisibleTracks(currentState.getVisibleTracks());
                payload.setVisiblePlaylists(currentState.getVisiblePlaylists());

                if (currentState.getUiIndicator().isPlaylistView()) {
                    if (currentState.isFiltering()) {
                        payload.setVisiblePlaylists(ListFilter.filterAudioPlaylist(payload.getSortedPlaylists(), currentState.getFilterDefinition()));
                    } else {
                        payload.setVisiblePlaylists(payload.getSortedPlaylists());
                    }
                } else {
                    if (currentState.isFiltering()) {
                        if (currentState.getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH)
                            payload.setVisibleTracks(payload.getSortedTracks());
                        else
                            payload.setVisibleTracks(ListFilter.filterAudioData(payload.getSortedTracks(), currentState.getFilterDefinition()));
                    } else {
                        payload.setVisibleTracks(payload.getSortedTracks());
                    }
                }
                return new MainAction(MainActionType.SET_CONTENT, payload);
            }
        };
    }

    /**
     * Verify that the supplied indicator points to valid data, if not the indicator is reset to submenu.
     *
     * @param indicator           The indicator to verify
     * @param audioDataRepository The AudioDataRepository to run the checks on
     * @return The passed indicator if data is available for it, or the passed indicator reset to submenu if not, or null if the supplied indicator was null.
     */
    private SelectionIndicator patchIndicator(SelectionIndicator indicator, AudioDataRepository audioDataRepository) {
        if (indicator == null)
            return null;
        switch (indicator.getSelector()) {
            case TRACKS:
            case PLAYLISTS:
                //No change
                break;
            case ARTISTS:
                if (!indicator.isPlaylistView()) {
                    if (audioDataRepository.getArtist(indicator.getUuid()) == null) {
                        return new SelectionIndicator(indicator.getSelector(), null);
                    }
                }
                break;
            case ALBUMS:
                if (!indicator.isPlaylistView()) {
                    if (audioDataRepository.getAlbum(indicator.getUuid()) == null) {
                        return new SelectionIndicator(indicator.getSelector(), null);
                    }
                }
                break;
            case GENRES:
                if (!indicator.isPlaylistView()) {
                    if (audioDataRepository.getGenre(indicator.getUuid()) == null) {
                        return new SelectionIndicator(indicator.getSelector(), null);
                    }
                }
                break;
        }
        return indicator;
    }
}