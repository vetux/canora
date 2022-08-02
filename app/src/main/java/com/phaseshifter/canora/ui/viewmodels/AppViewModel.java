package com.phaseshifter.canora.ui.viewmodels;

import static com.phaseshifter.canora.ui.selectors.MainSelector.getPlaylistTitle;

import android.content.Context;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.theme.AppTheme;
import com.phaseshifter.canora.model.repo.DeviceAudioRepository;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.utils.Observable;

public class AppViewModel {
    public final Observable<Boolean> devMode = new Observable<>();

    public final Observable<SelectionIndicator> contentSelector = new Observable<>(new SelectionIndicator(AudioContentSelector.TRACKS, null));
    public final Observable<Boolean> isContentLoading = new Observable<>(false);
    public final Observable<Boolean> isSelecting = new Observable<>(false);
    public final Observable<String> notFoundText = new Observable<>();

    public final Observable<Boolean> isSearching = new Observable<>();
    public final Observable<String> searchText = new Observable<>();

    private String getContentName(SelectionIndicator uiIndicator,
                                  Context context,
                                  DeviceAudioRepository audioDataRepository,
                                  UserPlaylistRepository audioPlaylistRepository,
                                  SoundCloudAudioRepository scAudioDataRepo) {
        if (uiIndicator.isPlaylistView()) {
            return getSelectorName(uiIndicator.getSelector(), context);
        } else {
            return getIndicatorName(uiIndicator, context, audioDataRepository, audioPlaylistRepository, scAudioDataRepo);
        }
    }

    private String getSelectorName(AudioContentSelector selector, Context context) {
        switch (selector) {
            case TRACKS:
                return context.getString(R.string.main_toolbar_title0tracks);
            case PLAYLISTS:
                return context.getString(R.string.main_toolbar_title0playlists);
            case ARTISTS:
                return context.getString(R.string.main_toolbar_title0artists);
            case ALBUMS:
                return context.getString(R.string.main_toolbar_title0albums);
            case GENRES:
                return context.getString(R.string.main_toolbar_title0genres);
            case SOUNDCLOUD_SEARCH:
            case SOUNDCLOUD_CHARTS:
                return context.getString(R.string.main_toolbar_title0sc);
        }
        return "Error";
    }

    private String getIndicatorName(SelectionIndicator indicator,
                                    Context context,
                                    DeviceAudioRepository audioDataRepository,
                                    UserPlaylistRepository audioPlaylistRepository,
                                    SoundCloudAudioRepository scAudioDataRepo) {
        String text = getPlaylistTitle(indicator, audioDataRepository, audioPlaylistRepository, scAudioDataRepo);
        switch (indicator.getSelector()) {
            case TRACKS:
                return context.getString(R.string.main_toolbar_title0tracks);
            case PLAYLISTS:
                return context.getString(R.string.main_toolbar_title0subplaylist, text);
            case ARTISTS:
                return context.getString(R.string.main_toolbar_title0subartist, text);
            case ALBUMS:
                return context.getString(R.string.main_toolbar_title0subalbum, text);
            case GENRES:
                return context.getString(R.string.main_toolbar_title0subgenre, text);
            case SOUNDCLOUD_SEARCH:
                return context.getString(R.string.main_toolbar_title0sc);
            case SOUNDCLOUD_CHARTS:
                return context.getString(R.string.main_toolbar_title0subchart, text);
        }
        return "Error";
    }
}
