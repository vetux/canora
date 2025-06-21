package com.phaseshifter.canora.model.repo;

import android.net.Uri;
import android.provider.MediaStore;

import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.ImageMetadata;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.player.PlayerMetadata;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSource;
import com.phaseshifter.canora.data.media.player.source.PlayerDataSourceUri;
import com.phaseshifter.canora.plugin.webradio.AudioDataSourceWebRadio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.EndpointDiscovery;
import de.sfuhrm.radiobrowser4j.FieldName;
import de.sfuhrm.radiobrowser4j.ListParameter;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.SearchMode;
import de.sfuhrm.radiobrowser4j.Station;

public class WebRadioRepository {
    private static final String userAgent = "Canora";

    private RadioBrowser radioBrowser;

    private List<PlayerData> charts = new ArrayList<>();
    private List<PlayerData> results = new ArrayList<>();

    public void updateCharts() {
        setupBrowser();
        List<PlayerData> ret = new ArrayList<>();
        if (radioBrowser != null) {
            radioBrowser
                    .listStations(ListParameter.create().order(FieldName.CLICKCOUNT))
                    .limit(64)
                    .forEach(s -> ret.add(convertStation(s)));
        }
        this.charts = ret;
    }

    public List<PlayerData> getCharts() {
        return charts;
    }

    public void updateSearch(String searchText) {
        setupBrowser();
        List<PlayerData> ret = new ArrayList<>();
        if (radioBrowser != null) {
            radioBrowser.listStationsBy(SearchMode.BYNAME, searchText)
                    .limit(64)
                    .forEach(s -> ret.add(convertStation(s)));
        }
        this.results = ret;
    }

    public List<PlayerData> getResults() {
        return results;
    }

    PlayerData convertStation(Station s) {
        PlayerMetadata metadata = new PlayerMetadata();
        metadata.id = UUID.randomUUID();
        metadata.title = s.getName();
        metadata.artist = s.getName();
        metadata.album = s.getName();
        metadata.genres = new String[0];
        if (!s.getFavicon().isEmpty()) {
            metadata.artwork = new ImageData(new ImageMetadata(UUID.randomUUID()), new ImageDataSourceUri(Uri.parse(s.getFavicon())));
        } else {
            metadata.artwork = null;
        }

        PlayerDataSource source = new AudioDataSourceWebRadio(s);

        return new PlayerData(metadata, source);
    }

    void setupBrowser() {
        if (this.radioBrowser == null) {
            try {
                Optional<String> endpoint = new EndpointDiscovery(userAgent).discover();
                radioBrowser = new RadioBrowser(ConnectionParams.builder()
                        .apiUrl(endpoint.get())
                        .userAgent(userAgent)
                        .timeout(5000)
                        .build());
            } catch (IOException e) {
                radioBrowser = null;
            }
        }
    }
}
