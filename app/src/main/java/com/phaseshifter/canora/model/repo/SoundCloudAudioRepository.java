package com.phaseshifter.canora.model.repo;

import android.net.Uri;

import com.phaseshifter.canora.data.media.image.ImageMetadata;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.data.media.playlist.PlaylistMetadata;
import com.phaseshifter.canora.plugin.soundcloud.AudioDataSourceSC;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.plugin.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCConnectionException;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2ChartTrack;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Charts;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2ChartsMutable;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;
import com.phaseshifter.canora.utils.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * SoundCloud
 */
public class SoundCloudAudioRepository {
    private SCV2Client client = null;

    private final List<PlayerData> searchResults = new ArrayList<>();

    private final HashMap<SCGenre, Integer> chartPages = new HashMap<>();

    private final HashMap<SCGenre, Integer> genreIndexMapping = new HashMap<>();
    private final HashMap<Integer, SCGenre> indexGenreMapping = new HashMap<>();

    private final ArrayList<Playlist> charts = new ArrayList<>();

    private String searchText;
    private int searchPage;
    private boolean searchLimitReached;

    private int chartsActiveGenre;
    private boolean chartsLimitReached;

    public Observable<String> clientID = new Observable(null);

    private static final int resPerPage = 10;

    public SoundCloudAudioRepository(String clientID) {
        this.clientID.set(clientID);
        int i = 0;
        for (SCGenre genre : SCGenre.values()) {
            indexGenreMapping.put(i, genre);
            genreIndexMapping.put(genre, i);
            i++;
            charts.add(new Playlist(new PlaylistMetadata(UUID.randomUUID(),
                    genre.parameterValue.substring("soundcloud:genres:".length()),
                    null), new ArrayList<>()));
        }
    }

    public SoundCloudAudioRepository() {
        this(null);
    }

    public int getSearchPage() {
        return searchPage;
    }

    public String getSearchText() {
        return searchText;
    }

    public boolean isSearchLimitReached() {
        return searchLimitReached;
    }

    public boolean isChartsLimitReached() {
        return chartsLimitReached;
    }

    public void setClientID(String id) {
        clientID.set(id);
        if (client == null) {
            client = new SCV2Client();
        }
        client.setClientID(id);
    }

    public void refreshSearch(String q) {
        if (searchText != null && searchText.equals(q)) {
            if (searchLimitReached) {
                return;
            }
            searchPage++;
        } else {
            searchLimitReached = false;
            searchPage = 0;
            searchResults.clear();
        }
        searchText = q;
        List<SCV2Track> results;
        boolean succeeded = true;
        try {
            results = search(q, searchPage);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                updateClientId();
                results = search(q, searchPage);
            } catch (Exception ex) {
                ex.printStackTrace();
                succeeded = false;
                results = new ArrayList<>();
            }
        }

        if (succeeded) {
            if (results == null || results.isEmpty()) {
                // Last page reached
                searchLimitReached = true;
            } else {
                searchResults.addAll(getTracks(results));
            }
        }
    }

    public void refreshCharts(int currentGenre) {
        if (chartsActiveGenre == currentGenre && chartsLimitReached) {
            return;
        }
        chartsActiveGenre = currentGenre;

        SCGenre genre = indexGenreMapping.get(currentGenre);
        assert genre != null;

        Playlist pl = charts.get(currentGenre);

        int page = 0;
        if (chartPages.containsKey(genre)) {
            page = chartPages.get(genre);
        }

        boolean succeeded = true;
        SCV2Charts c;
        try {
            c = receiveCharts(genre, page);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                updateClientId();
                c = receiveCharts(genre, page);
            } catch (Exception ex) {
                ex.printStackTrace();
                succeeded = false;
                c = new SCV2ChartsMutable();
            }
        }

        if (succeeded) {
            if (c == null) {
                chartsLimitReached = true;
            } else {
                pl.getTracks().addAll(getTracks(c));

                if (chartPages.containsKey(genre)) {
                    page++;
                }

                chartPages.put(genre, page);
            }
        }
    }

    public int getChartsIndex(UUID uuid) {
        int i = 0;
        for (Playlist pl : charts) {
            if (pl.getMetadata().getId().equals(uuid)) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Invalid uuid " + uuid);
    }

    public List<PlayerData> getSearchResults() {
        return searchResults;
    }

    public List<Playlist> getChartsPlaylists() {
        return charts;
    }

    public Playlist getChartsPlaylist(UUID uuid) {
        return charts.get(getChartsIndex(uuid));
    }

    private void updateClientId() {
        try {
            clientID.set(client.getNewClientID());
            client.setClientID(clientID.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SCV2Client getClient() throws SCConnectionException, SCParsingException, IOException {
        if (client == null) {
            client = new SCV2Client();
            if (clientID.get() == null) {
                clientID.set(client.getNewClientID());
            }
            client.setClientID(clientID.get());
        }
        return client;
    }

    private PlayerData getAudioData(SCV2Track track) {
        if (track.getPublisher_metadata() == null) {
            return new PlayerData(new PlayerMetadata(UUID.randomUUID(),
                    track.getTitle(),
                    track.getUser().getId(),
                    "<unknown>",
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadata(UUID.randomUUID(), 0 ,0), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        } else {
            return new PlayerData(new PlayerMetadata(UUID.randomUUID(),
                    track.getTitle(),
                    track.getPublisher_metadata().getArtist(),
                    track.getPublisher_metadata().getAlbum_title(),
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadata(UUID.randomUUID(), 0, 0), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        }
    }

    private List<PlayerData> getTracks(SCV2Charts c) {
        ArrayList<PlayerData> t = new ArrayList<>();
        for (SCV2ChartTrack track : c.getTracks()) {
            t.add(getAudioData(track.getTrack()));
        }
        return t;
    }

    private List<PlayerData> getTracks(List<SCV2Track> tracks) {
        List<PlayerData> ret = new ArrayList<>();
        for (SCV2Track track : tracks) {
            ret.add(getAudioData(track));
        }
        return ret;
    }

    private List<SCV2Track> search(String q, int pageNumber) throws SCConnectionException, SCParsingException, IOException {
        if (!q.isEmpty()) {
            SCV2Client client = getClient();
            return client.getTracksContainingString(q, resPerPage, pageNumber);
        } else {
            return null;
        }
    }

    private SCV2Charts receiveCharts(SCGenre genre, int pageNumber) throws SCConnectionException, SCParsingException, IOException {
        SCV2Client client = getClient();
        return client.getCharts(genre, resPerPage, pageNumber);
    }
}
