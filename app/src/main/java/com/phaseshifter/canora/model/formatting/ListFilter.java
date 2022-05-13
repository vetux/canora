package com.phaseshifter.canora.model.formatting;

import android.util.Log;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.data.formatting.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static com.phaseshifter.canora.ui.data.formatting.FilterDef.*;

public class ListFilter {
    private static final String LOG_TAG = "ListFilter";

    public static List<AudioData> filterAudioData(List<AudioData> content, FilterDef def) {
        if (content == null)
            return null;
        Log.v(LOG_TAG, "filterAudioData");
        if (def.filterFor.length() > 0) {
            Log.v(LOG_TAG, "CONTENT SIZE BEFORE FILTER: " + content.size());
            List<AudioData> ret = new ArrayList<>();
            switch (def.filterBy) {
                case FILTER_TITLE:
                    for (AudioData f : content) {
                        if (f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ALBUM:
                    for (AudioData f : content) {
                        if (f.getMetadata().getAlbum().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ARTIST:
                    for (AudioData f : content) {
                        if (f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_GENRE:
                    for (AudioData f : content) {
                        String genreString = concatStringArray(f.getMetadata().getGenres());
                        if (genreString.toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_FILENAME:
                    throw new RuntimeException("Not supported");
                case FILTER_TITLE_ARTIST:
                    for (AudioData f : content) {
                        if (f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())
                                || f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                case FILTER_ANY:
                    for (AudioData f : content) {
                        String genreString = concatStringArray(f.getMetadata().getGenres());
                        if (f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase()) ||
                                f.getMetadata().getAlbum().toLowerCase().contains(def.filterFor.toLowerCase()) ||
                                f.getMetadata().getArtist().toLowerCase().contains(def.filterFor.toLowerCase()) ||
                                genreString.toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("FILTER NOT RECOGNIZED: " + def.filterBy);
            }
            Log.v(LOG_TAG, "CONTENT SIZE AFTER FILTER " + ret.size());
            return ret;
        } else {
            Log.v(LOG_TAG, "EMPTY FILTER STRING");
            return content;
        }
    }

    public static List<AudioPlaylist> filterAudioPlaylist(List<AudioPlaylist> content, FilterDef def) {
        if (content == null)
            return null;
        Log.v(LOG_TAG, "filterAudioPlaylist");
        List<AudioPlaylist> ret = new ArrayList<>();
        if (def.filterFor.length() > 0) {
            Log.v(LOG_TAG, "CONTENT SIZE BEFORE FILTER " + content.size());
            switch (def.filterBy) {
                case FILTER_TITLE:
                default:
                    for (AudioPlaylist f : content) {
                        if (f.getMetadata().getTitle().toLowerCase().contains(def.filterFor.toLowerCase())) {
                            ret.add(f);
                        }
                    }
                    break;
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