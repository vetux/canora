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

public abstract class MainActionCreator {
    private static final String LOG_TAG = "MainActionCreator";

    public static Action refreshAndFetchRepoData(Store<MainStateImmutable> store,
                                                 AudioDataRepository audioDataRepository,
                                                 AudioPlaylistRepository audioPlaylistRepository,
                                                 SettingsRepository settingsRepository,
                                                 ThemeRepository themeRepository,
                                                 SCAudioDataRepo scAudioDataRepo,
                                                 MediaPlayerService service,
                                                 Executor presExec,
                                                 Executor mainExec) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
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
                        store.dispatch(fetchRepoData(store, audioDataRepository, audioPlaylistRepository, settingsRepository, themeRepository, scAudioDataRepo, service));
                        store.dispatch(new MainAction(MainActionType.CONTENT_LOAD_STOP));
                        store.dispatch(new MainAction(MainActionType.SEARCH_LOAD_STOP));
                    });
                });
                return null;
            }
        };
    }

    public static Action fetchRepoData(Store<MainStateImmutable> store,
                                       AudioDataRepository audioDataRepository,
                                       AudioPlaylistRepository audioPlaylistRepository,
                                       SettingsRepository settingsRepository,
                                       ThemeRepository themeRepository,
                                       SCAudioDataRepo scAudioDataRepo,
                                       MediaPlayerService service) {
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

                store.dispatch(fetchAudioData(store, audioDataRepository, audioPlaylistRepository, scAudioDataRepo, service));
                return new MainAction(MainActionType.CONTENT_LOAD_STOP);
            }
        };
    }

    public static Action fetchAudioData(Store<MainStateImmutable> store,
                                        AudioDataRepository audioDataRepository,
                                        AudioPlaylistRepository audioPlaylistRepository,
                                        SCAudioDataRepo scAudioDataRepo,
                                        MediaPlayerService service) {
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

                if (currentState.getContentIndicator() != null)
                    service.setContent(MainSelector.getTracksForIndicator(currentState.getContentIndicator(), currentState, audioDataRepository, audioPlaylistRepository, scAudioDataRepo));

                store.dispatch(new MainAction(MainActionType.SET_CONTENT, payload));
                store.dispatch(getReformatContent(store));

                return new MainAction(MainActionType.CONTENT_LOAD_STOP);
            }
        };
    }

    public static Action setPlaybackState(Store<MainStateImmutable> store, PlayerState state) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setPlayerState(state);
                return new MainAction(MainActionType.SET_PLAYERSTATE, payload);
            }
        };
    }

    public static Action switchFiltering(Store<MainStateImmutable> store) {
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
                return getFilterContent(store);
            }
        };
    }

    public static Action searchTextChange(Store<MainStateImmutable> store,
                                          String text) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainStateImmutable currentState = store.getState();
                MainState payload = new MainState();
                payload.setFilterDefinition(new FilterDef(currentState.getFilterDefinition().filterBy, text));
                store.dispatch(new MainAction(MainActionType.FILTER_SETDEF, payload));
                return getFilterContent(store);
            }
        };
    }

    public static Action getChangeSortingState(Store<MainStateImmutable> store,
                                               SortDef def,
                                               AudioDataRepository audioDataRepository,
                                               AudioPlaylistRepository audioPlaylistRepository,
                                               SCAudioDataRepo scAudioDataRepo,
                                               MediaPlayerService service) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setSortingDefinition(def);
                store.dispatch(new MainAction(MainActionType.SET_SORTDEF, payload));
                return fetchAudioData(store, audioDataRepository, audioPlaylistRepository, scAudioDataRepo, service);
            }
        };
    }

    public static Action getChangeFilterState(Store<MainStateImmutable> store,
                                              AudioDataRepository audioDataRepository,
                                              AudioPlaylistRepository audioPlaylistRepository,
                                              SCAudioDataRepo scAudioDataRepo,
                                              MediaPlayerService service,
                                              boolean filtering) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                return getChangeFilterState(store, audioDataRepository, audioPlaylistRepository, scAudioDataRepo, service, filtering, store.getState().getFilterDefinition());
            }
        };
    }

    public static Action getChangeFilterState(Store<MainStateImmutable> store,
                                              AudioDataRepository audioDataRepository,
                                              AudioPlaylistRepository audioPlaylistRepository,
                                              SCAudioDataRepo scAudioDataRepo,
                                              MediaPlayerService service,
                                              boolean filtering,
                                              FilterDef def) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState state = new MainState();
                state.setFiltering(filtering);
                state.setFilterDefinition(def);
                store.dispatch(new MainAction(MainActionType.SET_FILTERSTATE, state));
                return fetchAudioData(store, audioDataRepository, audioPlaylistRepository, scAudioDataRepo, service);
            }
        };
    }

    public static Action getChangeIndicators(Store<MainStateImmutable> store,
                                             AudioDataRepository audioDataRepository,
                                             AudioPlaylistRepository audioPlaylistRepository,
                                             SettingsRepository settingsRepository,
                                             ThemeRepository themeRepository,
                                             SCAudioDataRepo scAudioDataRepo,
                                             MediaPlayerService service,
                                             Executor presExec,
                                             Executor mainExec,
                                             SelectionIndicator contentIndicator,
                                             SelectionIndicator uiIndicator) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                store.dispatch(new MainAction(MainActionType.FILTER_DISABLE));
                MainState state = new MainState();
                state.setContentIndicator(contentIndicator);
                state.setUiIndicator(uiIndicator);
                store.dispatch(new MainAction(MainActionType.SET_INDICATORS, state));
                if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS
                        && !store.getState().getUiIndicator().isPlaylistView()) {
                    return refreshAndFetchRepoData(store, audioDataRepository, audioPlaylistRepository, settingsRepository, themeRepository, scAudioDataRepo, service, presExec, mainExec);
                } else {
                    return fetchAudioData(store, audioDataRepository, audioPlaylistRepository, scAudioDataRepo, service);
                }
            }
        };
    }

    public static Action getChangeSelectionMode(Store<MainStateImmutable> store, boolean isSelecting) {
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

    public static Action getChangeSelection(HashSet<UUID> selection) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState state = new MainState();
                state.setSelection(selection);
                return new MainAction(MainActionType.SET_SELECTION, state);
            }
        };
    }

    public static Action getChangeControlMax(boolean controlMax) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setControlsMaximized(controlMax);
                return new MainAction(MainActionType.CONTROL_SET, payload);
            }
        };
    }

    public static Action getChangeVolume(float volume) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setVolume(volume);
                return new MainAction(MainActionType.SET_VOLUME, payload);
            }
        };
    }

    public static Action getChangeShuffle(boolean shuffle) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setShuffling(shuffle);
                return new MainAction(MainActionType.SET_SHUFFLE, payload);
            }
        };
    }

    public static Action getChangeRepeat(boolean repeat) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setRepeating(repeat);
                return new MainAction(MainActionType.SET_REPEAT, payload);
            }
        };
    }

    public static Action getChangeDevMode(boolean devMode) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                MainState payload = new MainState();
                payload.setDevMode(devMode);
                return new MainAction(MainActionType.SET_DEVMODE, payload);
            }
        };
    }

    public static Action getReformatContent(Store<MainStateImmutable> store) {
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

    public static Action getFilterContent(Store<MainStateImmutable> store) {
        return new Thunk.ThunkAction() {
            @Override
            public Action run() {
                if (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_SEARCH
                        || (store.getState().getUiIndicator().getSelector() == AudioContentSelector.SOUNDCLOUD_CHARTS
                        && !store.getState().getUiIndicator().isPlaylistView())) {
                    return null;
                }

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
    private static SelectionIndicator patchIndicator(SelectionIndicator indicator, AudioDataRepository audioDataRepository) {
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