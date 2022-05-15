package com.phaseshifter.canora.model.repo;

import android.net.Uri;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataSimple;
import com.phaseshifter.canora.data.media.audio.source.AudioDataSourceSC;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.metadata.ImageMetadataSimple;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataSimple;
import com.phaseshifter.canora.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2ChartTrack;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2Charts;
import com.phaseshifter.canora.soundcloud.api_v2.data.SCV2Track;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class SCAudioDataRepo {
    private SCV2Client client = null;

    private String prevSearch;
    private int prevResPerPage;
    private int prevPage;

    private int prevResPerPageCharts;

    private List<AudioData> searchResults = new ArrayList<>();
    private Dictionary<UUID, AudioPlaylist> charts = new Hashtable<>();

    public void refreshSearchResults(String q, int resPerPage, int page) {
        searchResults = search(q, resPerPage, page);

        prevSearch = q;
        prevResPerPage = resPerPage;
        prevPage = page;
    }

    public void refreshCharts(int resPerPage) {
        charts = getCharts(resPerPage);

        prevResPerPageCharts = resPerPage;
    }

    public List<AudioData> getSearchResults() {
        return searchResults;
    }

    public Dictionary<UUID, AudioPlaylist> getCharts() {
        return charts;
    }

    private SCV2Client getClient() {
        if (client == null) {
            client = new SCV2Client();
            updateClientId();
        }
        return client;
    }

    private void updateClientId() {
        try {
            try {
                client.setClientID(client.getNewClientID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AudioData getAudioData(SCV2Track track) {
        if (track.getPublisher_metadata() == null) {
            return new AudioData(new AudioMetadataSimple(UUID.randomUUID(),
                    track.getTitle(),
                    track.getUser().getId(),
                    "<unknown>",
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadataSimple(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        } else {
            return new AudioData(new AudioMetadataSimple(UUID.randomUUID(),
                    track.getTitle(),
                    track.getPublisher_metadata().getArtist(),
                    track.getPublisher_metadata().getAlbum_title(),
                    new String[0],
                    Long.parseLong(track.getDuration()),
                    new ImageData(new ImageMetadataSimple(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(track.getArtwork_url())))),
                    new AudioDataSourceSC(track.getCodings()));
        }
    }

    private List<AudioData> search(String q, int resultsPerPage, int pageNumber) {
        if (prevSearch != null && prevSearch.equals(q) && prevResPerPage == resultsPerPage && prevPage == pageNumber) {
            return searchResults;
        }

        List<SCV2Track> tracks = new ArrayList<>();

        if (!q.isEmpty()) {
            SCV2Client client = getClient();
            try {
                tracks = client.getTracksContainingString(q, resultsPerPage, pageNumber);
            } catch (Exception e) {
                e.printStackTrace();
                updateClientId();
                try {
                    tracks = client.getTracksContainingString(q, resultsPerPage, pageNumber);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }

        searchResults = new ArrayList<>();
        for (SCV2Track track : tracks) {
            searchResults.add(getAudioData(track));
        }
        return searchResults;
    }

    private Dictionary<UUID, AudioPlaylist> getCharts(int resultsPerPage) {
        if (prevResPerPageCharts == resultsPerPage)
            return charts;

        SCV2Client client = getClient();

        charts = new Hashtable<>();

        for (SCGenre genre : SCGenre.values()) {
            SCV2Charts c;
            try {
                c = client.getCharts(genre, resultsPerPage, 0);
            } catch (Exception e) {
                e.printStackTrace();
                updateClientId();
                try {
                    c = client.getCharts(genre, resultsPerPage, 0);
                } catch (Exception x) {
                    x.printStackTrace();
                    c = null;
                }
            }
            if (c != null) {
                ArrayList<AudioData> t = new ArrayList<>();
                for (SCV2ChartTrack track : c.getTracks()) {
                    t.add(getAudioData(track.getTrack()));
                }

                UUID uuid = UUID.randomUUID();

                charts.put(uuid, new AudioPlaylist(new PlaylistMetadataSimple(uuid,
                        genre.parameterValue.substring("soundcloud:genres:".length()),
                        null),
                        t));
            }
        }

        return charts;
    }
}
