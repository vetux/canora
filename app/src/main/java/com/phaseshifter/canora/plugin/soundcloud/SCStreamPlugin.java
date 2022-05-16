package com.phaseshifter.canora.plugin.soundcloud;

import android.content.Context;
import android.net.Uri;

import com.phaseshifter.canora.R;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.audio.metadata.AudioMetadataMemory;
import com.phaseshifter.canora.data.media.image.ImageData;
import com.phaseshifter.canora.data.media.image.metadata.ImageMetadataMemory;
import com.phaseshifter.canora.data.media.image.source.ImageDataSourceUri;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.data.media.playlist.metadata.PlaylistMetadataMemory;
import com.phaseshifter.canora.plugin.StreamPlugin;
import com.phaseshifter.canora.plugin.soundcloud.api.data.SCGenre;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.client.SCV2Client;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2ChartTrack;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Charts;
import com.phaseshifter.canora.plugin.soundcloud.api_v2.data.SCV2Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SCStreamPlugin implements StreamPlugin {
    private HeaderData header;
    private List<Category> categories = new ArrayList<>();
    private SCV2Client client = new SCV2Client();

    private String soundCloudGenrePrefix = "soundcloud:genres:";

    private Set<String> getGenres() {
        Set<String> ret = new HashSet<>();
        for (SCGenre g : SCGenre.values()) {
            ret.add(g.parameterValue.substring(soundCloudGenrePrefix.length()));
        }
        return ret;
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

    public SCStreamPlugin(Context c) {
        header = new HeaderData("SoundCloud", c.getDrawable(R.drawable.soundcloud_logo_big_white));
        categories.add(new Category("Charts", getGenres(), c.getDrawable(R.drawable.ic_baseline_format_list_numbered_24)));
        try {
            client.setClientID(client.getNewClientID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public HeaderData getHeader() {
        return header;
    }

    @Override
    public List<Category> getCategories() {
        return categories;
    }

    @Override
    public List<AudioData> search(SearchArguments args) {
        List<SCV2Track> tracks = new ArrayList<>();

        if (!args.searchText.isEmpty()) {
            SCV2Client client = getClient();
            try {
                tracks = client.getTracksContainingString(args.searchText, args.resultsPerPage, args.page);
            } catch (Exception e) {
                e.printStackTrace();
                updateClientId();
                try {
                    tracks = client.getTracksContainingString(args.searchText, args.resultsPerPage, args.page);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }

        List<AudioData> ret = new ArrayList<>();
        for (SCV2Track track : tracks) {
            ret.add(getAudioData(track));
        }
        return ret;
    }

    @Override
    public AudioPlaylist getPlaylist(String category, String entry, int resultsPerPage, int page) {
        SCGenre genre = SCGenre.ALLAUDIO;
        String v = soundCloudGenrePrefix + entry;
        for (SCGenre g : SCGenre.values()) {
            if (g.parameterValue.equals(v)) {
                genre = g;
                break;
            }
        }

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
                throw new RuntimeException("Failed to retrieve genre playlists " + x.getMessage());
            }
        }

        ArrayList<AudioData> t = new ArrayList<>();
        for (SCV2ChartTrack track : c.getTracks()) {
            t.add(getAudioData(track.getTrack()));
        }

        UUID uuid = UUID.randomUUID();

        return new AudioPlaylist(new PlaylistMetadataMemory(uuid,
                genre.parameterValue.substring("soundcloud:genres:".length()),
                null),
                t);
    }
}
