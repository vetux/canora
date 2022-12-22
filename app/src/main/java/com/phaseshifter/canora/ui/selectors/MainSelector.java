package com.phaseshifter.canora.ui.selectors;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.DeviceAudioRepository;
import com.phaseshifter.canora.model.repo.SoundCloudAudioRepository;
import com.phaseshifter.canora.model.repo.UserPlaylistRepository;
import com.phaseshifter.canora.ui.data.MainPage;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.ContentSelector;

import java.util.List;

public abstract class MainSelector {
    public static String getPlaylistTitle(ContentSelector indicator, DeviceAudioRepository audioDataRepository, UserPlaylistRepository audioPlaylistRepository, SoundCloudAudioRepository scAudioDataRepo) {
        AudioPlaylist playlist = null;
        switch (indicator.getPage()) {
            case PLAYLISTS:
                playlist = audioPlaylistRepository.get(indicator.getUuid());
                break;
            case ARTISTS:
                playlist = audioDataRepository.getArtist(indicator.getUuid());
                break;
            case ALBUMS:
                playlist = audioDataRepository.getAlbum(indicator.getUuid());
                break;
            case GENRES:
                playlist = audioDataRepository.getGenre(indicator.getUuid());
                break;
            case SOUNDCLOUD_CHARTS:
                playlist = scAudioDataRepo.getChartsPlaylists().get(scAudioDataRepo.getChartsIndex(indicator.getUuid()));
                break;
        }
        if (playlist != null)
            return playlist.getMetadata().getTitle();
        else
            return null;
    }

    public static List<PlayerData> getTracksForSelector(ContentSelector indicator,
                                                        SortingOptions sortOpt,
                                                        DeviceAudioRepository audioDataRepository,
                                                        UserPlaylistRepository audioPlaylistRepository,
                                                        SoundCloudAudioRepository scAudioDataRepo) {
        switch (indicator.getPage()) {
            case TRACKS:
                return ListSorter.sortAudioData(audioDataRepository.getTracks(), sortOpt);
            case PLAYLISTS:
                return ListSorter.sortAudioData(audioPlaylistRepository.get(indicator.getUuid()).getData(), sortOpt);
            case ARTISTS:
                return ListSorter.sortAudioData(audioDataRepository.getArtist(indicator.getUuid()).getData(), sortOpt);
            case ALBUMS:
                return ListSorter.sortAudioData(audioDataRepository.getAlbum(indicator.getUuid()).getData(), sortOpt);
            case GENRES:
                return ListSorter.sortAudioData(audioDataRepository.getGenre(indicator.getUuid()).getData(), sortOpt);
            case SOUNDCLOUD_SEARCH:
                return scAudioDataRepo.getSearchResults();
            case SOUNDCLOUD_CHARTS:
                return scAudioDataRepo.getChartsPlaylists().get(scAudioDataRepo.getChartsIndex(indicator.getUuid())).getData();
            default:
                throw new RuntimeException("INVALID SELECTOR: " + indicator.getPage());
        }
    }

    public static AudioPlaylist getPlaylistForSelector(ContentSelector indicator, DeviceAudioRepository audioDataRepository, UserPlaylistRepository audioPlaylistRepository) {
        if (indicator == null)
            return null;
        switch (indicator.getPage()) {
            case PLAYLISTS:
                return audioPlaylistRepository.get(indicator.getUuid());
            case ARTISTS:
                return audioDataRepository.getArtist(indicator.getUuid());
            case ALBUMS:
                return audioDataRepository.getAlbum(indicator.getUuid());
            case GENRES:
                return audioDataRepository.getGenre(indicator.getUuid());
            default:
                return null;
        }
    }

    public static List<AudioPlaylist> getPlaylistsForSelector(MainPage selector,
                                                              SortingOptions sortOpt,
                                                              DeviceAudioRepository audioDataRepository,
                                                              UserPlaylistRepository audioPlaylistRepository) {
        switch (selector) {
            case TRACKS:
                return null;
            case PLAYLISTS:
                return ListSorter.sortAudioPlaylist(audioPlaylistRepository.getAll(), sortOpt);
            case ARTISTS:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getArtists(), sortOpt);
            case ALBUMS:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getAlbums(), sortOpt);
            case GENRES:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getGenres(), sortOpt);
            default:
                throw new RuntimeException("INVALID SELECTOR: " + selector);
        }
    }
}