package com.phaseshifter.canora.model.formatting;

import android.util.Log;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.data.formatting.FilterOptions;

import java.util.ArrayList;
import java.util.List;

import static com.phaseshifter.canora.ui.data.formatting.FilterOptions.*;

public class ListFilter {
    private static final String LOG_TAG = "ListFilter";

    public static List<PlayerData> filterAudioData(List<PlayerData> content, FilterOptions def) {
        if (content == null)
            return null;
        Log.v(LOG_TAG, "filterAudioData");
        if (def.filterFor.length() > 0) {
            Log.v(LOG_TAG, "CONTENT SIZE BEFORE FILTER: " + content.size());
            List<PlayerData> ret = new ArrayList<>();
            switch (def.filterBy) {
                case FILTER_TITLE:
                    for (PlayerData f : content) {
                        if (f.getMetadata().getTitle() != null && f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ALBUM:
                    for (PlayerData f : content) {
                        if (f.getMetadata().getAlbum() != null && f.getMetadata().getAlbum().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ARTIST:
                    for (PlayerData f : content) {
                        if (f.getMetadata().getArtist() != null && f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_GENRE:
                    for (PlayerData f : content) {
                        String genreString = concatStringArray(f.getMetadata().getGenres());
                        if (genreString.toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_TITLE_ARTIST:
                    for (PlayerData f : content) {
                        if (f.getMetadata().getTitle() != null
                                && (f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())
                                || f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase()))) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ANY:
                    for (PlayerData f : content) {
                        String genreString = concatStringArray(f.getMetadata().getGenres());
                        if ((f.getMetadata().getTitle() != null && f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())) ||
                                (f.getMetadata().getAlbum() != null && f.getMetadata().getAlbum().toLowerCase().contains(def.filterFor.toLowerCase())) ||
                                (f.getMetadata().getArtist() != null && f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase())) ||
                                genreString.toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                default:
                    ret = content;
                    break;
            }
            Log.v(LOG_TAG, "CONTENT SIZE AFTER FILTER " + ret.size());
            return ret;
        } else {
            Log.v(LOG_TAG, "EMPTY FILTER STRING");
            return content;
        }
    }

    public static List<AudioPlaylist> filterAudioPlaylist(List<AudioPlaylist> content, FilterOptions def) {
        if (content == null)
            return null;
        Log.v(LOG_TAG, "filterAudioPlaylist");
        List<AudioPlaylist> ret = new ArrayList<>();
        if (def.filterFor.length() > 0) {
            Log.v(LOG_TAG, "CONTENT SIZE BEFORE FILTER " + content.size());
            for (AudioPlaylist f : content) {
                if (f.getMetadata().getTitle() != null && f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())) {
                    ret.add(f);
                }
            }
            Log.v(LOG_TAG, "CONTENT SIZE AFTER FILTER: " + ret.size());
            return ret;
        } else {
            Log.v(LOG_TAG, "EMPTY FILTER STRING");
            return content;
        }
    }

    private static String concatStringArray(String[] array) {
        if (array == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s);
        }
        return sb.toString();
    }
}