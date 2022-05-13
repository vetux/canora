package com.phaseshifter.canora.ui.utils.selectors;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.model.formatting.ListSorter;
import com.phaseshifter.canora.model.repo.AudioDataRepository;
import com.phaseshifter.canora.model.repo.AudioPlaylistRepository;
import com.phaseshifter.canora.ui.data.AudioContentSelector;
import com.phaseshifter.canora.ui.data.misc.SelectionIndicator;
import com.phaseshifter.canora.ui.redux.state.MainStateImmutable;

import java.util.List;

public abstract class MainSelector {
    public static String getPlaylistTitle(SelectionIndicator indicator, AudioDataRepository audioDataRepository, AudioPlaylistRepository audioPlaylistRepository) {
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
        }
        if (playlist != null)
            return playlist.getMetadata().getTitle();
        else
            return null;
    }

    public static List<AudioData> getTracksForIndicator(SelectionIndicator indicator,
                                                        MainStateImmutable currentState,
                                                        AudioDataRepository audioDataRepository,
                                                        AudioPlaylistRepository audioPlaylistRepository) {
        switch (indicator.getSelector()) {
            case TRACKS:
                return ListSorter.sortAudioData(audioDataRepository.getTracks(), currentState.getSortingDefinition());
            case PLAYLISTS:
                return ListSorter.sortAudioData(audioPlaylistRepository.get(indicator.getUuid()).getData(), currentState.getSortingDefinition());
            case ARTISTS:
                return ListSorter.sortAudioData(audioDataRepository.getArtist(indicator.getUuid()).getData(), currentState.getSortingDefinition());
            case ALBUMS:
                return ListSorter.sortAudioData(audioDataRepository.getAlbum(indicator.getUuid()).getData(), currentState.getSortingDefinition());
            case GENRES:
                return ListSorter.sortAudioData(audioDataRepository.getGenre(indicator.getUuid()).getData(), currentState.getSortingDefinition());
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
            default:
                throw new RuntimeException("INVALID SELECTOR: " + indicator.getSelector());
        }
    }

    public static List<AudioPlaylist> getPlaylistsForSelector(AudioContentSelector selector,
                                                              MainStateImmutable currentState,
                                                              AudioDataRepository audioDataRepository,
                                                              AudioPlaylistRepository audioPlaylistRepository) {
        switch (selector) {
            case TRACKS:
                return null;
            case PLAYLISTS:
                return ListSorter.sortAudioPlaylist(audioPlaylistRepository.getAll(), currentState.getSortingDefinition());
            case ARTISTS:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getArtists(), currentState.getSortingDefinition());
            case ALBUMS:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getAlbums(), currentState.getSortingDefinition());
            case GENRES:
                return ListSorter.sortAudioPlaylist(audioDataRepository.getGenres(), currentState.getSortingDefinition());
            default:
                throw new RuntimeException("INVALID SELECTOR: " + selector);
        }
    }

    public static int getUnfilteredIndex(MainStateImmutable currentState, AudioDataRepository audioDataRepository, AudioPlaylistRepository audioPlaylistRepository, int indexFiltered) {
        if (currentState.getUiIndicator().isSubMenu()) {
            AudioPlaylist selectedPlaylist = currentState.getContentPlaylists().get(indexFiltered);
            List<AudioPlaylist> unfilteredContent = getPlaylistsForSelector(currentState.getUiIndicator().getSelector(), currentState, audioDataRepository, audioPlaylistRepository);
            if (unfilteredContent != null)
                return unfilteredContent.indexOf(selectedPlaylist);
            else
                return -1;
        } else {
            AudioData selectedTrack = currentState.getContentTracks().get(indexFiltered);
            List<AudioData> unfilteredContent = getTracksForIndicator(currentState.getUiIndicator(), currentState, audioDataRepository, audioPlaylistRepository);
            if (unfilteredContent != null)
                return unfilteredContent.indexOf(selectedTrack);
            else
                return -1;
        }
    }
}