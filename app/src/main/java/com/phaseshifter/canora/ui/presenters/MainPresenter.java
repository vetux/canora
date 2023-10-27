package com.phaseshifter.canora.ui.presenters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.application.MainApplication;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.data.settings.BooleanSetting;
import com.phaseshifter.canora.data.settings.FloatSetting;
import com.phaseshifter.canora.data.settings.IntegerSetting;
import com.phaseshifter.canora.data.settings.StringSetting;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.comparison.AudioDataComparsion;
import com.phaseshifter.canora.model.editor.AudioMetadataEditor;
import com.phaseshifter.canora.model.formatting.ListFilter;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.plugin.ytdl.AudioDataSourceYtdl;
import com.phaseshifter.canora.service.download.AutoBindDownloadService;
import com.phaseshifter.canora.service.player.state.PlayerState;
import com.phaseshifter.canora.service.player.wrapper.AutoBindMediaService;
import com.phaseshifter.canora.ui.contracts.MainContract;
import com.phaseshifter.canora.ui.data.DownloadInfo;
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
import com.phaseshifter.canora.ui.viewmodels.YoutubeDlViewModel;
import com.phaseshifter.canora.utils.Observable;
import com.phaseshifter.canora.utils.Observer;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.phaseshifter.canora.ui.selectors.MainSelector.getPlaylistForSelector;

public class MainPresenter implements MainContract.Presenter, Observer<PlayerState> {
    private final String LOG_TAG = "MainPresenter";
    private final MainContract.View view;

    private final DeviceAudioRepository deviceAudioRepository;
    private final UserPlaylistRepository userPlaylistRepository;
    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final SoundCloudAudioRepository scAudioDataRepo;
    private final YoutubeSearchRepository ytRepo;

    private final AutoBindMediaService mediaService;
    private final AutoBindDownloadService downloadService;

    private final Executor mainThread;

    private final ThreadPoolExecutor presExec;

    private final AppViewModel appViewModel;
    private final ContentViewModel contentViewModel;
    private final PlayerStateViewModel playerStateViewModel;
    private final YoutubeDlViewModel ytdlViewModel;

    private final AudioMetadataEditor metadataEditor;

    private final Observer<List<PlayerData>> ytObserver = new Observer<List<PlayerData>>() {
        @Override
        public void update(Observable<List<PlayerData>> observable, List<PlayerData> value) {
            if (uiContentSelector.getPage() == MainPage.YOUTUBE_SEARCH_VIDEOS) {
                isScrollLoading = false;
                mainThread.execute(() -> {
                    updateVisibleContent();
                });
            }
        }
    };

    private final Context context;

    private int filterBy = FilterOptions.FILTER_ANY;
    private SortingOptions sortingOptions = new SortingOptions();

    private ContentSelector uiContentSelector = new ContentSelector(MainPage.TRACKS, null);
    private ContentSelector playingContentSelector = null;

    private HashSet<UUID> selection = new HashSet<>();

    private List<PlayerData> sortedTracks = new ArrayList<>();
    private List<Playlist> sortedPlaylists = new ArrayList<>();

    private boolean downloadingAudio = false;
    private boolean downloadingVideo = false;

    private AppTheme theme = null;

    private int presenterTaskCounter = 0;

    private boolean isScrollLoading = false;

    private String contentSearch = "";
    private String scSearch = "";
    private String ytSearch = "";

    private boolean resumeSeek = false;

    public MainPresenter(MainContract.View view,
                         AutoBindMediaService service,
                         AutoBindDownloadService downloadService,
                         DeviceAudioRepository audioDataRepository,
                         UserPlaylistRepository audioPlaylistRepository,
                         SettingsRepository settingsRepository,
                         ThemeRepository themeRepository,
                         SoundCloudAudioRepository scAudioDataRepo,
                         YoutubeSearchRepository ytRepo,
                         AudioMetadataEditor metadataEditor,
                         Executor mainThread,
                         AppViewModel appViewModel,
                         ContentViewModel contentViewModel,
                         PlayerStateViewModel playerStateViewModel,
                         YoutubeDlViewModel ytdlViewModel,
                         Context context) {
        this.view = view;
        this.mediaService = service;
        this.downloadService = downloadService;
        this.deviceAudioRepository = audioDataRepository;
        this.userPlaylistRepository = audioPlaylistRepository;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.scAudioDataRepo = scAudioDataRepo;
        this.ytRepo = ytRepo;
        this.metadataEditor = metadataEditor;
        this.mainThread = mainThread;
        this.appViewModel = appViewModel;
        this.contentViewModel = contentViewModel;
        this.playerStateViewModel = playerStateViewModel;
        this.ytdlViewModel = ytdlViewModel;
        this.context = context;
        this.presExec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    private void runPresenterTask(Runnable task) {
        incrementTasks();
        presExec.submit(() -> {
            task.run();
            decrementTasks();
        });
    }

    private void incrementTasks() {
        appViewModel.isContentLoading.set(true);
        presenterTaskCounter++;
    }

    private void decrementTasks() {
        if (--presenterTaskCounter == 0) {
            mainThread.execute(() -> {
                appViewModel.isContentLoading.set(false);
            });
        }
    }

    private void updateVisibleContent() {
        // TODO: Preformat content
        List<PlayerData> formattedTracks = new ArrayList<>();
        List<Playlist> formattedPlaylists = new ArrayList<>();
        sortedTracks = new ArrayList<>();
        sortedPlaylists = new ArrayList<>();
        if (uiContentSelector.isPlaylistView()) {
            switch (uiContentSelector.getPage()) {
                case PLAYLISTS:
                    sortedPlaylists = ListSorter.sortPlaylist(userPlaylistRepository.getAll(), sortingOptions);
                    break;
                case ARTISTS:
                    sortedPlaylists = ListSorter.sortPlaylist(deviceAudioRepository.getArtists(), sortingOptions);
                    break;
                case ALBUMS:
                    sortedPlaylists = ListSorter.sortPlaylist(deviceAudioRepository.getAlbums(), sortingOptions);
                    break;
                case GENRES:
                    sortedPlaylists = ListSorter.sortPlaylist(deviceAudioRepository.getGenres(), sortingOptions);
                    break;
                case SOUNDCLOUD_CHARTS:
                    sortedPlaylists = ListSorter.sortPlaylist(scAudioDataRepo.getChartsPlaylists(), sortingOptions);
                    break;
            }
            if (appViewModel.isSearching.get()) {
                formattedPlaylists = ListFilter.filterPlaylist(sortedPlaylists, new FilterOptions(filterBy, appViewModel.searchText.get()));
            } else {
                formattedPlaylists = sortedPlaylists;
            }
            contentViewModel.visiblePlaylists.set(formattedPlaylists);
        } else {
            switch (uiContentSelector.getPage()) {
                case TRACKS:
                    sortedTracks = ListSorter.sortAudioData(deviceAudioRepository.getTracks(), sortingOptions);
                    break;
                case PLAYLISTS: {
                    Playlist playlist = userPlaylistRepository.get(uiContentSelector.getUuid());
                    if (playlist != null) {
                        sortedTracks = ListSorter.sortAudioData(playlist.getTracks(), sortingOptions);
                    }
                    break;
                }
                case ARTISTS: {
                    Playlist playlist = deviceAudioRepository.getArtist(uiContentSelector.getUuid());
                    if (playlist != null) {
                        // After a refresh the device audio repository could not contain the playlist anymore.
                        sortedTracks = ListSorter.sortAudioData(playlist.getTracks(), sortingOptions);
                    }
                    break;
                }
                case ALBUMS: {
                    Playlist playlist = deviceAudioRepository.getAlbum(uiContentSelector.getUuid());
                    if (playlist != null) {
                        sortedTracks = ListSorter.sortAudioData(playlist.getTracks(), sortingOptions);
                    }
                    break;
                }
                case GENRES: {
                    Playlist playlist = deviceAudioRepository.getGenre(uiContentSelector.getUuid());
                    if (playlist != null) {
                        sortedTracks = ListSorter.sortAudioData(playlist.getTracks(), sortingOptions);
                    }
                    break;
                }
                case SOUNDCLOUD_SEARCH:
                    sortedTracks = scAudioDataRepo.getSearchResults();
                    break;
                case SOUNDCLOUD_CHARTS:
                    sortedTracks = scAudioDataRepo.getChartsPlaylist(uiContentSelector.getUuid()).getTracks();
                    break;
                case YOUTUBE_SEARCH_VIDEOS:
                    sortedTracks = ytRepo.results.get();
                    break;
            }
            switch (uiContentSelector.getPage()) {
                case SOUNDCLOUD_SEARCH:
                case YOUTUBE_SEARCH_VIDEOS:
                    formattedTracks = sortedTracks;
                    break;
                default:
                    if (appViewModel.isSearching.get()) {
                        formattedTracks = ListFilter.filterAudioData(sortedTracks, new FilterOptions(filterBy, appViewModel.searchText.get()));
                    } else {
                        formattedTracks = sortedTracks;
                    }
                    break;
            }
            contentViewModel.visibleTracks.set(formattedTracks);
        }

        updateNotFoundText();

        updateHighlightedIndex();

        contentViewModel.contentTracksSelection.set(getSelectedTrackIndices());
        contentViewModel.contentPlaylistsSelection.set(getSelectedPlaylistIndices());
    }

    private void updateNotFoundText() {
        if (uiContentSelector.getPage() == MainPage.YOUTUBE_SEARCH_VIDEOS
                || uiContentSelector.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
            if (contentViewModel.visibleTracks.get().isEmpty()) {
                if (appViewModel.isContentLoading.get()) {
                    appViewModel.notFoundText.set(context.getString(R.string.main_notfound0loading));
                } else if (appViewModel.searchText.get() == null || appViewModel.searchText.get().isEmpty()) {
                    appViewModel.notFoundText.set(null);
                } else {
                    appViewModel.notFoundText.set(context.getString(R.string.main_notfound0stringNotFound, appViewModel.searchText.get()));
                }
            } else {
                appViewModel.notFoundText.set(null);
            }
        } else if (uiContentSelector.getPage() == MainPage.YOUTUBE_DL) {
            appViewModel.notFoundText.set(null);
        } else if (!uiContentSelector.isPlaylistView() && contentViewModel.visibleTracks.get().isEmpty()) {
            if (appViewModel.isSearching.get()) {
                appViewModel.notFoundText.set(context.getString(R.string.main_notfound0stringNotFound, appViewModel.searchText.get()));
            } else {
                appViewModel.notFoundText.set(context.getString(R.string.main_notfound0noTracks));
            }
        } else if (uiContentSelector.isPlaylistView() && contentViewModel.visiblePlaylists.get().isEmpty()) {
            if (appViewModel.isSearching.get()) {
                appViewModel.notFoundText.set(context.getString(R.string.main_notfound0stringNotFound, appViewModel.searchText.get()));
            } else {
                appViewModel.notFoundText.set(context.getString(R.string.main_notfound0noPlaylists));
            }
        } else {
            appViewModel.notFoundText.set(null);
        }
    }

    private void updateHighlightedIndex() {
        PlayerState state = mediaService.getState().get();
        if (state != null) {
            PlayerData track = state.getCurrentTrack();
            int index = -1;
            for (PlayerData t : contentViewModel.visibleTracks.get()) {
                if (AudioDataComparsion.isEqual_exclude_UUID(track, t)) {
                    index = contentViewModel.visibleTracks.get().indexOf(t);
                    break;
                }
            }
            if (track != null
                    && uiContentSelector.equals(playingContentSelector)
                    && index >= 0) {
                contentViewModel.visibleTracksHighlightedIndex.set(index);
            } else {
                contentViewModel.visibleTracksHighlightedIndex.set(null);
            }

            if (playingContentSelector != null
                    && uiContentSelector.isPlaylistView()
                    && uiContentSelector.getPage().equals(playingContentSelector.getPage())) {
                int plIndex = 0;
                switch (playingContentSelector.getPage()) {
                    case PLAYLISTS:
                        plIndex = contentViewModel.visiblePlaylists.get().indexOf(userPlaylistRepository.get(playingContentSelector.getUuid()));
                        break;
                    case ARTISTS:
                        plIndex = contentViewModel.visiblePlaylists.get().indexOf(deviceAudioRepository.getArtist(playingContentSelector.getUuid()));
                        break;
                    case ALBUMS:
                        plIndex = contentViewModel.visiblePlaylists.get().indexOf(deviceAudioRepository.getAlbum(playingContentSelector.getUuid()));
                        break;
                    case GENRES:
                        plIndex = contentViewModel.visiblePlaylists.get().indexOf(deviceAudioRepository.getGenre(playingContentSelector.getUuid()));
                        break;
                    case SOUNDCLOUD_CHARTS:
                        plIndex = contentViewModel.visiblePlaylists.get().indexOf(scAudioDataRepo.getChartsPlaylist(playingContentSelector.getUuid()));
                        break;
                }
                if (plIndex >= 0) {
                    contentViewModel.contentPlaylistHighlight.set(plIndex);
                } else {
                    contentViewModel.contentPlaylistHighlight.set(null);
                }
            } else {
                contentViewModel.contentPlaylistHighlight.set(null);
            }
        } else {
            contentViewModel.contentPlaylistHighlight.set(null);
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
                case YOUTUBE_SEARCH_VIDEOS:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0youtube));
                    break;
                case YOUTUBE_DL:
                    contentViewModel.contentName.set(view.getStringResource(R.string.main_toolbar_title0youtube_dl));
                    break;
            }
        }
        String savedSearch = appViewModel.searchText.get();
        switch (selector.getPage()) {
            default:
                appViewModel.searchText.set(contentSearch);
                break;
            case SOUNDCLOUD_SEARCH:
                appViewModel.searchText.set(scSearch);
                break;
            case YOUTUBE_SEARCH_VIDEOS:
                appViewModel.searchText.set(ytSearch);
                break;
        }
        switch (appViewModel.contentSelector.get().getPage()) {
            default:
                contentSearch = savedSearch;
                break;
            case SOUNDCLOUD_SEARCH:
                scSearch = savedSearch;
                break;
            case YOUTUBE_SEARCH_VIDEOS:
                ytSearch = savedSearch;
                break;
        }
        appViewModel.contentSelector.set(selector);
    }

    @Override
    public void update(Observable<PlayerState> observable, PlayerState value) {
        playerStateViewModel.applyPlayerState(value, getPlaylistForSelector(playingContentSelector, deviceAudioRepository, userPlaylistRepository));
        view.setShowingVideo(value.isVideo() && value.getCurrentTrack() != null);
        view.setVideoSize(value.getWidth(), value.getHeight());
        updateHighlightedIndex();
    }

    //START Presenter Interface

    @Override
    public void onCreate(Serializable savedState) {
        scAudioDataRepo.clientID.addObserver(new Observer<String>() {
            @Override
            public void update(Observable<String> observable, String value) {
                if (!Objects.equals(settingsRepository.getString(StringSetting.SC_CLIENTID), value)) {
                    settingsRepository.putString(StringSetting.SC_CLIENTID, value);
                    view.showMessage(context.getString(R.string.main_soundcloud_clientidchange, value));
                }
            }
        });

        if (savedState instanceof MainPresenterState) {
            final MainPresenterState state = (MainPresenterState) savedState;
            // Repositories are stored application wide so the saved indicator uuid should be valid.
            uiContentSelector = state.uiIndicator;
            playingContentSelector = state.contentIndicator;
            ytdlViewModel.url.set(state.url);
            ytdlViewModel.infoForUrl.set(state.info);
            downloadingVideo = state.downloadingVideo;
            downloadingAudio = state.downloadingAudio;
            contentSearch = state.contentSearch;
            scSearch = state.scSearch;
            ytSearch = state.ytSearch;
        }

        appViewModel.contentSelector.set(uiContentSelector);

        switch (uiContentSelector.getPage()) {
            default:
                appViewModel.searchText.set(contentSearch);
                break;
            case SOUNDCLOUD_SEARCH:
                appViewModel.searchText.set(scSearch);
                break;
            case YOUTUBE_SEARCH_VIDEOS:
                appViewModel.searchText.set(ytSearch);
                break;
        }

        ytRepo.setApiKey(settingsRepository.getString(StringSetting.YOUTUBE_API_KEY));
        ytRepo.results.addObserver(ytObserver);

        scAudioDataRepo.setClientID(settingsRepository.getString(StringSetting.SC_CLIENTID));

        theme = themeRepository.get(settingsRepository.getInt(IntegerSetting.THEME));

        sortingOptions.sortby = settingsRepository.getInt(IntegerSetting.SORT_BY);
        sortingOptions.sortdir = settingsRepository.getInt(IntegerSetting.SORT_DIR);
        sortingOptions.sorttech = settingsRepository.getInt(IntegerSetting.SORT_TECH);

        filterBy = settingsRepository.getInt(IntegerSetting.FILTER_BY);

        appViewModel.devMode.set(settingsRepository.getBoolean(BooleanSetting.DEVELOPERMODE));

        setViewModelContentSelector(uiContentSelector);

        mediaService.bind();

        mediaService.getState().addObserver(this);

        mediaService.setVolume(settingsRepository.getFloat(FloatSetting.VOLUME));
        mediaService.setShuffle(settingsRepository.getBoolean(BooleanSetting.SHUFFLE));
        mediaService.setRepeat(settingsRepository.getBoolean(BooleanSetting.REPEAT));
        mediaService.setEqualizerPreset(settingsRepository.getInt(IntegerSetting.EQUALIZER_PRESET_INDEX));
        mediaService.setEnableMediaSessionControls(settingsRepository.getBoolean(BooleanSetting.ENABLE_MEDIASESSION_CALLBACK));

        PlayerState playerState = mediaService.getState().get();

        if (playerState != null)
            playerStateViewModel.applyPlayerState(mediaService.getState().get(), getPlaylistForSelector(playingContentSelector, deviceAudioRepository, userPlaylistRepository));

        view.checkPermissions();

        runPresenterTask(() -> {
            deviceAudioRepository.refresh();
            mainThread.execute(() -> {
                view.setTheme(theme);
                mediaService.setVideoSurfaceView(view.getSmallVideoSurface());
                appViewModel.notifyObservers();
                contentViewModel.notifyObservers();
                playerStateViewModel.notifyObservers();
                ytdlViewModel.notifyObservers();
                updateVisibleContent();
            });
        });
    }

    @Override
    public void onDestroy() {
        scAudioDataRepo.clientID.removeAllObservers();
        ytRepo.results.removeObserver(ytObserver);

        settingsRepository.putString(StringSetting.SC_CLIENTID, scAudioDataRepo.clientID.get());

        mediaService.getState().removeObserver(this);
    }

    @Override
    public synchronized void onStart() {
        ytRepo.setApiKey(settingsRepository.getString(StringSetting.YOUTUBE_API_KEY));
        scAudioDataRepo.setClientID(settingsRepository.getString(StringSetting.SC_CLIENTID));

        sortingOptions.sortby = settingsRepository.getInt(IntegerSetting.SORT_BY);
        sortingOptions.sortdir = settingsRepository.getInt(IntegerSetting.SORT_DIR);
        sortingOptions.sorttech = settingsRepository.getInt(IntegerSetting.SORT_TECH);

        filterBy = settingsRepository.getInt(IntegerSetting.FILTER_BY);

        appViewModel.devMode.set(settingsRepository.getBoolean(BooleanSetting.DEVELOPERMODE));

        mediaService.setVolume(settingsRepository.getFloat(FloatSetting.VOLUME));
        mediaService.setShuffle(settingsRepository.getBoolean(BooleanSetting.SHUFFLE));
        mediaService.setRepeat(settingsRepository.getBoolean(BooleanSetting.REPEAT));
        mediaService.setEqualizerPreset(settingsRepository.getInt(IntegerSetting.EQUALIZER_PRESET_INDEX));
        mediaService.setEnableMediaSessionControls(settingsRepository.getBoolean(BooleanSetting.ENABLE_MEDIASESSION_CALLBACK));

        PlayerState playerState = mediaService.getState().get();

        if (playerState != null)
            playerStateViewModel.applyPlayerState(mediaService.getState().get(), getPlaylistForSelector(playingContentSelector, deviceAudioRepository, userPlaylistRepository));

        AppTheme theme1 = themeRepository.get(settingsRepository.getInt(IntegerSetting.THEME));
        if (theme != theme1) {
            theme = theme1;
            view.setTheme(theme);
            appViewModel.notifyObservers();
            contentViewModel.notifyObservers();
            playerStateViewModel.notifyObservers();
            ytdlViewModel.notifyObservers();
            updateVisibleContent();
        }
    }

    @Override
    public synchronized void onStop() {
        MainPresenterState savedState = new MainPresenterState();
        savedState.uiIndicator = uiContentSelector;
        savedState.contentIndicator = playingContentSelector;
        savedState.info = ytdlViewModel.infoForUrl.get();
        savedState.downloadingAudio = downloadingAudio;
        savedState.downloadingVideo = downloadingVideo;
        savedState.url = ytdlViewModel.url.get();
        savedState.contentSearch = contentSearch;
        savedState.scSearch = scSearch;
        savedState.ytSearch = ytSearch;
        view.saveState(savedState);
    }

    @Override
    public void onTrackSeekStart() {
        resumeSeek = mediaService.getState().get().isPlaying();
        mediaService.pause();
    }

    @Override
    public void onTrackSeek(float p) {
        mediaService.seek(p);
    }

    @Override
    public void onTrackSeekStop() {
        if (resumeSeek)
            mediaService.resume();
    }

    @Override
    public void onPrev() {
        mediaService.previous();
    }

    @Override
    public void onPlay() {
        mediaService.pauseResume();
    }

    @Override
    public void onNext() {
        mediaService.next();
    }

    @Override
    public void onShuffleSwitch() {
        boolean shuffle = !playerStateViewModel.isShuffling.get();
        settingsRepository.putBoolean(BooleanSetting.SHUFFLE, shuffle);
        mediaService.setShuffle(shuffle);
    }

    @Override
    public void onRepeatSwitch() {
        boolean repeat = !playerStateViewModel.isRepeating.get();
        settingsRepository.putBoolean(BooleanSetting.REPEAT, repeat);
        mediaService.setRepeat(repeat);
    }

    @Override
    public void onVolumeSeek(float p) {
        if (p > 1.0f || p < 0.0f)
            throw new RuntimeException("Received invalid Volume value: " + p);
        settingsRepository.putFloat(FloatSetting.VOLUME, p);
        mediaService.setVolume(p);
    }

    @Override
    public void onPresetSelected(int preset) {
        settingsRepository.putInt(IntegerSetting.EQUALIZER_PRESET_INDEX, preset);
        mediaService.setEqualizerPreset(preset);
    }

    @Override
    public void onSearchTextChange(String text) {
        assert (text != null);
        appViewModel.searchText.set(text);
        switch (uiContentSelector.getPage()) {
            case SOUNDCLOUD_SEARCH:
                scSearch = text;
                break;
            case YOUTUBE_SEARCH_VIDEOS:
                ytSearch = text;
                break;
            default:
                contentSearch = text;
                break;
        }
    }

    @Override
    public void onSearchTextEditingFinished() {
        switch (uiContentSelector.getPage()) {
            case SOUNDCLOUD_SEARCH:
            case YOUTUBE_SEARCH_VIDEOS:
                break;
            default:
                updateVisibleContent();
                break;
        }
    }

    @Override
    public void onSearchReturn() {
        if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_SEARCH) {
            String text = appViewModel.searchText.get();
            incrementTasks();
            presExec.submit(() -> {
                scAudioDataRepo.refreshSearch(text);
                decrementTasks();
                mainThread.execute(this::updateVisibleContent);
            });
            updateNotFoundText();
        } else if (uiContentSelector.getPage() == MainPage.YOUTUBE_SEARCH_VIDEOS) {
            String text = appViewModel.searchText.get();
            ytRepo.setSearchText(text);
            incrementTasks();
            ytRepo.loadNextPage(this::decrementTasks,
                    (e) -> {
                        decrementTasks();
                        view.showWarning(context.getString(R.string.main_connection_failed, e.getMessage()));
                    });
            updateNotFoundText();
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
        List<PlayerData> processedData = contentViewModel.visibleTracks.get();

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
            mediaService.setContent(sortedTracks);
            mediaService.play(processedData.get(index).getMetadata().getId());
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
        if (uiContentSelector.getPage() == MainPage.YOUTUBE_SEARCH_VIDEOS) {
            actions.add(ContextMenu.Action.DOWNLOAD_AUDIO);
            actions.add(ContextMenu.Action.DOWNLOAD_VIDEO);
        }
        view.showContentContextMenu(index, actions, (action) -> {
            onContextMenuAction(index, action);
        }, () -> {
        });
    }

    @Override
    public void onPlaylistContentClick(int index) {
        List<Playlist> processedData = contentViewModel.visiblePlaylists.get();

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
            if (!isScrollLoading && !appViewModel.searchText.get().isEmpty() && !scAudioDataRepo.isSearchLimitReached()) {
                isScrollLoading = true;
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshSearch(appViewModel.searchText.get());
                    isScrollLoading = false;
                    mainThread.execute(this::updateVisibleContent);
                });
            }
        } else if (uiContentSelector.getPage() == MainPage.SOUNDCLOUD_CHARTS && !uiContentSelector.isPlaylistView()) {
            if (!isScrollLoading && !scAudioDataRepo.isChartsLimitReached()) {
                isScrollLoading = true;
                runPresenterTask(() -> {
                    scAudioDataRepo.refreshCharts(scAudioDataRepo.getChartsIndex(uiContentSelector.getUuid()));
                    isScrollLoading = false;
                    mainThread.execute(this::updateVisibleContent);
                });
            }
        } else if (uiContentSelector.getPage() == MainPage.YOUTUBE_SEARCH_VIDEOS) {
            if (!isScrollLoading && !appViewModel.searchText.get().isEmpty() && !ytRepo.isSearchLimitReached()) {
                isScrollLoading = true;
                incrementTasks();
                ytRepo.setSearchText(appViewModel.searchText.get());
                ytRepo.loadNextPage(this::decrementTasks,
                        (e) -> {
                            decrementTasks();
                            view.showWarning(context.getString(R.string.main_connection_failed, e.getMessage()));
                        });
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
    public void onEditorResult(PlayerData data, boolean error, boolean canceled, boolean delete) {
        Log.v(LOG_TAG, "onEditorResult " + data + " " + error + " " + canceled + " " + delete);
        if (data == null)
            return;
        if (!error && !canceled) {
            if (!delete) {
                if (data.getDataSource() instanceof PlayerDataSourceUri) {
                    Log.v(LOG_TAG, "Writing Uri ...");
                    try {
                        metadataEditor.writeMetadata(((PlayerDataSourceUri) data.getDataSource()).getUri(), data.getMetadata());
                        deviceAudioRepository.refresh();
                        updateVisibleContent();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SecurityException se) {
                        Log.v(LOG_TAG, "Oh Noes!!! SecurityException: " + se);
                        view.handleSecurityException(se, () -> {
                            Log.v(LOG_TAG, "OnHandledSecurityException: " + se);
                            try {
                                metadataEditor.writeMetadata(((PlayerDataSourceUri) data.getDataSource()).getUri(), data.getMetadata());
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
    public void onEditorResult(Playlist data, boolean error, boolean canceled, boolean delete) {
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
        if (controlMax) {
            mediaService.setVideoSurfaceView(view.getLargeVideoSurface());
        } else {
            mediaService.setVideoSurfaceView(view.getSmallVideoSurface());
        }
    }

    @Override
    public void onNavigationClick(NavigationItem item) {
        switch (item) {
            case TRACKS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.TRACKS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case PLAYLISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.PLAYLISTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case ALBUMS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.ALBUMS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case ARTISTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.ARTISTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case GENRES:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.GENRES, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case SOUNDCLOUD_SEARCH:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(true);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.SOUNDCLOUD_SEARCH, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case SOUNDCLOUD_CHARTS:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.SOUNDCLOUD_CHARTS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case YOUTUBE_SEARCH:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(true);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.YOUTUBE_SEARCH_VIDEOS, null);
                setViewModelContentSelector(uiContentSelector);
                updateVisibleContent();
                break;
            case YOUTUBE_DL:
                view.setNavigationMax(false);
                view.setTransportControlMax(false);
                appViewModel.isSearching.set(false);
                appViewModel.isSelecting.set(false);
                selection.clear();
                uiContentSelector = new ContentSelector(MainPage.YOUTUBE_DL, null);
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

    @Override
    public void onUrlTextChange(String text) {
        ytdlViewModel.url.set(text);
    }

    @Override
    public void onCheckUrlClick() {
        String url = ytdlViewModel.url.get();
        runPresenterTask(() -> {
            try {
                MainApplication app = (MainApplication) context.getApplicationContext();
                YoutubeDL instance = app.getYoutubeDlInstance();

                VideoInfo info = instance.getInfo(url);

                DownloadInfo downloadInfo = new DownloadInfo();
                downloadInfo.url = info.getUrl();
                downloadInfo.title = info.getTitle();
                downloadInfo.user = info.getUploader();
                downloadInfo.size = info.getFileSize();
                if (downloadInfo.size == 0)
                    downloadInfo.size = info.getFileSizeApproximate();
                downloadInfo.duration = info.getDuration();
                downloadInfo.thumbnailUrl = info.getThumbnail();

                mainThread.execute(() -> {
                    ytdlViewModel.infoForUrl.set(downloadInfo);
                });
            } catch (Exception e) {
                e.printStackTrace();
                view.showWarning(e.getMessage());
            }
        });
    }

    @Override
    public void onDownloadVideoClick() {
        DownloadInfo info = ytdlViewModel.infoForUrl.get();
        if (info != null) {
            downloadingVideo = true;
            view.createDocument("*/*", info.title + "_" + info.user + ".mp4");
        }
    }

    @Override
    public void onDownloadAudioClick() {
        DownloadInfo info = ytdlViewModel.infoForUrl.get();
        if (info != null) {
            downloadingAudio = true;
            view.createDocument("*/*", info.title + "_" + info.user + ".mp3");
        }
    }

    @Override
    public void onAddToPlaylistClick() {
        DownloadInfo info = ytdlViewModel.infoForUrl.get();
        PlayerMetadata trackMetadata = new PlayerMetadata();
        trackMetadata.setId(UUID.randomUUID());
        trackMetadata.setTitle(info.title);
        trackMetadata.setArtist(info.user);
        trackMetadata.setDuration(info.duration * 1000L);
        PlayerDataSource trackSource = new AudioDataSourceYtdl(ytdlViewModel.url.get());
        PlayerData track = new PlayerData(trackMetadata, trackSource);
        List<PlayerData> tracks = new ArrayList<>();
        tracks.add(track);
        view.showAddSelectionMenu(userPlaylistRepository.getAll(),
                () -> {
                    view.showDialog_CreatePlaylist(tracks, (name) -> {
                        PlaylistMetadata metadata = new PlaylistMetadata(UUID.randomUUID(), name, null);
                        Playlist playlist = new Playlist(metadata, tracks);
                        userPlaylistRepository.add(playlist);
                    }, () -> {
                    });
                }, (playlist) -> {
                    playlist.getTracks().add(track);
                    userPlaylistRepository.replace(playlist.getMetadata().getId(), playlist);
                });
    }

    @Override
    public void onDocumentCreated(Uri uri) {
        OutputStream fileStream;
        try {
            fileStream = view.openDocument(uri);
        } catch (Exception e) {
            view.showError("Failed to open file: " + e.getMessage());
            return;
        }

        String url = ytdlViewModel.url.get();
        try {
            if (downloadingAudio) {
                downloadService.downloadAudio(uri,
                        fileStream,
                        url,
                        () -> {
                            if (view != null && context != null) {
                                view.showMessage(context.getString(R.string.main_download_successful));
                                view.scanDocument(uri, () -> {
                                    mainThread.execute(() -> {
                                        deviceAudioRepository.refresh();
                                        updateVisibleContent();
                                    });
                                });
                            }
                        },
                        (e) -> {
                            e.printStackTrace();
                            view.showWarning(context.getString(R.string.main_download_failed, e.getMessage()));
                        });
            } else if (downloadingVideo) {
                downloadService.downloadVideo(uri,
                        fileStream,
                        url,
                        () -> {
                            if (view != null && context != null) {
                                view.showMessage(context.getString(R.string.main_download_successful));
                                view.scanDocument(uri, () -> {
                                    mainThread.execute(() -> {
                                        deviceAudioRepository.refresh();
                                        updateVisibleContent();
                                    });
                                });
                            }
                        },
                        (e) -> {
                            e.printStackTrace();
                            view.showWarning(context.getString(R.string.main_download_failed, e.getMessage()));
                        });
            }
            downloadingVideo = false;
            downloadingAudio = false;
        } catch (Exception e) {
            e.printStackTrace();
            view.showWarning(context.getString(R.string.main_download_failed, e.getMessage()));
        }
    }

    //STOP Presenter Interface

    private HashSet<Integer> getSelectedTrackIndices() {
        HashSet<Integer> indices = new HashSet<>();
        for (UUID id : selection) {
            List<PlayerData> list = contentViewModel.visibleTracks.get();
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
            List<Playlist> list = contentViewModel.visiblePlaylists.get();
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
                    Playlist clickedPlaylist = contentViewModel.visiblePlaylists.get().get(index);
                    if (selection.contains(clickedPlaylist.getMetadata().getId()))
                        selection.remove(clickedPlaylist.getMetadata().getId());
                    else
                        selection.add(clickedPlaylist.getMetadata().getId());
                    contentViewModel.contentPlaylistsSelection.set(getSelectedPlaylistIndices());
                } else {
                    PlayerData clickedTrack = contentViewModel.visibleTracks.get().get(index);
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
                    PlayerData track = contentViewModel.visibleTracks.get().get(index);
                    view.startEditor(track, metadataEditor.getMask(track), theme);
                }
                break;
            case DELETE:
                if (uiContentSelector.isPlaylistView()) {
                    Playlist playlistToDelete = contentViewModel.visiblePlaylists.get().get(index);
                    List<Playlist> playlistsToDelete = new ArrayList<>();
                    playlistsToDelete.add(playlistToDelete);
                    view.showDialog_DeletePlaylists(playlistsToDelete, () -> {
                                userPlaylistRepository.remove(playlistToDelete.getMetadata().getId());
                                updateVisibleContent();
                                view.showMessage("Playlist deleted");
                            },
                            () -> {
                            });
                } else {
                    Playlist currentPlaylist = MainSelector.getPlaylistForSelector(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                    if (currentPlaylist == null)
                        throw new AssertionError();
                    List<PlayerData> tracks = contentViewModel.visibleTracks.get();
                    List<PlayerData> tracksToDelete = new ArrayList<>();
                    tracksToDelete.add(tracks.get(index));
                    view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, () -> {
                        List<PlayerData> cleanTracks = currentPlaylist.getTracks();
                        for (PlayerData delTrack : tracksToDelete)
                            cleanTracks.remove(delTrack);
                        Playlist clean = new Playlist(currentPlaylist.getMetadata(), cleanTracks);
                        userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                        updateVisibleContent();
                        view.showMessage("Deleted tracks from playlist");
                    }, () -> {
                    });
                }
                break;
            case DOWNLOAD_AUDIO: {
                PlayerData clickedTrack = contentViewModel.visibleTracks.get().get(index);
                downloadingAudio = true;
                downloadingVideo = false;
                ytdlViewModel.url.set(((AudioDataSourceYtdl) clickedTrack.getDataSource()).getUrl());
                view.createDocument("*/*", clickedTrack.getMetadata().getTitle() + "_" + clickedTrack.getMetadata().getArtist() + ".mp3");
            }
            break;
            case DOWNLOAD_VIDEO: {
                PlayerData clickedTrack = contentViewModel.visibleTracks.get().get(index);
                downloadingAudio = false;
                downloadingVideo = true;
                ytdlViewModel.url.set(((AudioDataSourceYtdl) clickedTrack.getDataSource()).getUrl());
                view.createDocument("*/*", clickedTrack.getMetadata().getTitle() + "_" + clickedTrack.getMetadata().getArtist() + ".mp4");
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
                final FilterOptions filterOptions = new FilterOptions(filterBy, appViewModel.searchText.get());
                view.showDialog_FilterOptions(filterOptions,
                        (options) -> {
                            boolean changed = filterOptions != options;
                            if (changed) {
                                settingsRepository.putInt(IntegerSetting.FILTER_BY, options.filterBy);
                                filterBy = options.filterBy;
                                appViewModel.searchText.set(options.filterFor);
                                updateVisibleContent();
                            }
                        });
                break;
            case ADD_SELECTION:
                view.showAddSelectionMenu(ListSorter.sortPlaylist(userPlaylistRepository.getAll(), sortingOptions),
                        () -> {
                            List<PlayerData> selectedData = new ArrayList<>();
                            if (uiContentSelector.isPlaylistView()) {
                                for (Playlist playlist : sortedPlaylists) {
                                    if (selection.contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getTracks());
                                }
                            } else {
                                for (PlayerData track : sortedTracks) {
                                    if (selection.contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }
                            view.showDialog_CreatePlaylist(selectedData, (title) -> {
                                        PlaylistMetadata metadata = new PlaylistMetadata(null,
                                                title,
                                                null
                                        );
                                        Playlist playlist = new Playlist(metadata, selectedData);
                                        userPlaylistRepository.add(playlist);
                                        appViewModel.isSelecting.set(false);
                                        updateVisibleContent();
                                        view.showMessage("Created playlist");
                                    },
                                    () -> {
                                    });
                        },
                        (targetPlaylist) -> {
                            List<PlayerData> selectedData = new ArrayList<>();
                            if (uiContentSelector.isPlaylistView()) {
                                for (Playlist playlist : sortedPlaylists) {
                                    if (selection.contains(playlist.getMetadata().getId()))
                                        selectedData.addAll(playlist.getTracks());
                                }
                            } else {
                                for (PlayerData track : sortedTracks) {
                                    if (selection.contains(track.getMetadata().getId()))
                                        selectedData.add(track);
                                }
                            }

                            targetPlaylist.getTracks().addAll(selectedData);

                            userPlaylistRepository.replace(targetPlaylist.getMetadata().getId(), targetPlaylist);

                            appViewModel.isSelecting.set(false);
                            updateVisibleContent();
                            view.showMessage("Added tracks to playlist");
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
                    for (Playlist playlist : sortedPlaylists) {
                        selection.add(playlist.getMetadata().getId());
                    }
                    contentViewModel.contentPlaylistsSelection.set(getSelectedPlaylistIndices());
                } else {
                    for (PlayerData track : sortedTracks) {
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
                Playlist playlist = getPlaylistForSelector(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                view.startEditor(playlist, theme);
                break;
            case DELETE:
                if (appViewModel.isSelecting.get()) {
                    if (uiContentSelector.isPlaylistView()) {
                        List<Playlist> playlistsToDelete = new ArrayList<>();
                        for (Playlist pl : sortedPlaylists) {
                            if (selection.contains(pl.getMetadata().getId()))
                                playlistsToDelete.add(pl);
                        }
                        appViewModel.isSelecting.set(false);
                        view.showDialog_DeletePlaylists(playlistsToDelete, () -> {
                            boolean resetUiIndicator = false;
                            for (Playlist pl : playlistsToDelete) {
                                if (playingContentSelector != null
                                        && playingContentSelector.getPage() == MainPage.PLAYLISTS
                                        && Objects.equals(playingContentSelector.getUuid(), pl.getMetadata().getId()))
                                    resetUiIndicator = true;
                                userPlaylistRepository.remove(pl.getMetadata().getId());
                            }
                            if (resetUiIndicator)
                                playingContentSelector = null;
                            else
                                updateVisibleContent();
                            view.showMessage("Deleted playlists");
                        }, () -> {
                        });
                    } else {
                        Playlist currentPlaylist = getPlaylistForSelector(uiContentSelector, deviceAudioRepository, userPlaylistRepository);
                        if (currentPlaylist != null) {
                            List<PlayerData> tracksToDelete = new ArrayList<>();
                            for (PlayerData track : currentPlaylist.getTracks()) {
                                if (selection.contains(track.getMetadata().getId())) {
                                    tracksToDelete.add(track);
                                }
                            }
                            appViewModel.isSelecting.set(false);
                            view.showDialog_DeleteTracksFromPlaylist(currentPlaylist, tracksToDelete, () -> {
                                List<PlayerData> cleanTracks = currentPlaylist.getTracks();
                                for (PlayerData delTrack : tracksToDelete)
                                    cleanTracks.remove(delTrack);
                                Playlist clean = new Playlist(currentPlaylist.getMetadata(), cleanTracks);
                                userPlaylistRepository.replace(currentPlaylist.getMetadata().getId(), clean);
                                updateVisibleContent();
                                view.showMessage("Deleted tracks from playlist");
                            }, () -> {
                            });
                        }
                    }
                } else {
                    Playlist playlistToDelete = getPlaylistForSelector(uiContentSelector, deviceAudioRepository, userPlaylistRepository);

                    List<Playlist> data = new ArrayList<>();
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
                        view.showMessage("Playlist deleted");
                    }, () -> {
                    });
                }
                break;
        }
    }
}