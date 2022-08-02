package com.phaseshifter.canora.ui.utils.selectors;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.model.repo.SCAudioDataRepo;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;

import java.util.List;

public abstract class MainSelector {
    public static String getPlaylistTitle(SelectionIndicator indicator, AudioDataRepository audioDataRepository, AudioPlaylistRepository audioPlaylistRepository, SCAudioDataRepo scAudioDataRepo) {
        AudioPlaylist playlist = null;
        switch (indicator.getSelector()) {
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

    public static List<AudioData> getTracksForIndicator(SelectionIndicator indicator,
                                                        SortingOptions sortOpt,
                                                        AudioDataRepository audioDataRepository,
                                                        AudioPlaylistRepository audioPlaylistRepository,
                                                        SCAudioDataRepo scAudioDataRepo) {
        switch (indicator.getSelector()) {
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
                throw new RuntimeException("INVALID SELECTOR: " + indicator.getSelector());
        }
    }

    public static AudioPlaylist getPlaylistForIndicator(SelectionIndicator indicator, AudioDataRepository audioDataRepository, AudioPlaylistRepository audioPlaylistRepository) {
        switch (indicator.getSelector()) {
            case TRACKS:
                return null;
            case PLAYLISTS:
                return audioPlaylistRepository.get(indicator.getUuid());
            case ARTISTS:
                return audioDataRepository.getArtist(indicator.getUuid());
            case ALBUMS:
                return audioDataRepository.getAlbum(indicator.getUuid());
            case GENRES:
                return audioDataRepository.getGenre(indicator.getUuid());
            case SOUNDCLOUD_SEARCH:
                return null;
            case SOUNDCLOUD_CHARTS:
                return null;
            default:
                throw new RuntimeException("INVALID SELECTOR: " + indicator.getSelector());
        }
    }

    public static List<AudioPlaylist> getPlaylistsForSelector(AudioContentSelector selector,
                                                              SortingOptions sortOpt,
                                                              AudioDataRepository audioDataRepository,
                                                              AudioPlaylistRepository audioPlaylistRepository) {
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