package com.phaseshifter.canora.model.repo;

import android.net.Uri;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.plugin.soundcloud.AudioDataSourceSC;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.metadata.ImageMetadataMemory;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.plugin.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCConnectionException;
import com.phaseshifter.canora.plugin.soundcloud.api.exceptions.SCParsingException;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2ChartTrack;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Charts;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2ChartsMutable;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SCAudioDataRepo {
    private SCV2Client client = null;

    private final List<AudioData> searchResults = new ArrayList<>();

    private final HashMap<SCGenre, Integer> chartPages = new HashMap<>();

    private final HashMap<SCGenre, Integer> genreIndexMapping = new HashMap<>();
    private final HashMap<Integer, SCGenre> indexGenreMapping = new HashMap<>();

    private final ArrayList<AudioPlaylist> charts = new ArrayList<>();

    private String searchText;
    private int searchPage;
    private boolean searchLimitReached;

    private int chartsActiveGenre;
    private boolean chartsLimitReached;

    private String clientID;

    private static final int resPerPage = 10;

    public SCAudioDataRepo(String clientID) {
        this.clientID = clientID;
        int i = 0;
        for (SCGenre genre : SCGenre.values()) {
            indexGenreMapping.put(i, genre);
            genreIndexMapping.put(genre, i);
            i++;
            charts.add(new AudioPlaylist(new PlaylistMetadataMemory(UUID.randomUUID(),
                    genre.parameterValue.substring("soundcloud:genres:".length()),
                    null), new ArrayList<>()));
        }
    }

    public SCAudioDataRepo() {
        this(null);
    }

    public int getSearchPage() {
        return searchPage;
    }

    public String getSearchText() {
        return searchText;
    }

    public String getClientID() {
        return clientID;
    }

    public boolean isSearchLimitReached() {
        return searchLimitReached;
    }

    public boolean isChartsLimitReached() {
        return chartsLimitReached;
    }

    public void setClientID(String id) {
        clientID = id;
        if (client == null) {
            client = new SCV2Client();
        }
        client.setClientID(clientID);
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
            if (results == null) {
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

        AudioPlaylist pl = charts.get(currentGenre);

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
                pl.getData().addAll(getTracks(c));

                if (chartPages.containsKey(genre)) {
                    page++;
                }

                chartPages.put(genre, page);
            }
        }
    }

    public int getChartsIndex(UUID uuid) {
        int i = 0;
        for (AudioPlaylist pl : charts) {
            if (pl.getMetadata().getId() == uuid) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Invalid uuid");
    }

    public List<AudioData> getSearchResults() {
        return searchResults;
    }

    public List<AudioPlaylist> getChartsPlaylists() {
        return charts;
    }

    private void updateClientId() {
        try {
            clientID = client.getNewClientID();
            client.setClientID(clientID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SCV2Client getClient() throws SCConnectionException, SCParsingException, IOException {
        if (client == null) {
            client = new SCV2Client();
            if (clientID == null) {
                clientID = client.getNewClientID();
            }
            client.setClientID(clientID);
        }
        return client;
    }

    private AudioData getAudioData(SCV2Track track) {
        if (track.getPublisher_metadata() == null) {
            return new AudioData(new AudioMetadataMemory(UUID.randomUUID(),
                    track.getTitle(),
                    track.getUser().getId(),
                    "<unknown>",
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadataMemory(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        } else {
            return new AudioData(new AudioMetadataMemory(UUID.randomUUID(),
                    track.getTitle(),
                    track.getPublisher_metadata().getArtist(),
                    track.getPublisher_metadata().getAlbum_title(),
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadataMemory(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        }
    }

    private List<AudioData> getTracks(SCV2Charts c) {
        ArrayList<AudioData> t = new ArrayList<>();
        for (SCV2ChartTrack track : c.getTracks()) {
            t.add(getAudioData(track.getTrack()));
        }
        return t;
    }

    private List<AudioData> getTracks(List<SCV2Track> tracks) {
        List<AudioData> ret = new ArrayList<>();
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
