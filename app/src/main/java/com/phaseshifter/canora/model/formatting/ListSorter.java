package com.phaseshifter.canora.model.formatting;

import android.util.Log;
import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.Playlist;
import com.phaseshifter.canora.ui.data.formatting.SortingOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.phaseshifter.canora.ui.data.formatting.SortingOptions.*;

public class ListSorter {
    private static final String LOG_TAG = "ListSorter";

    public static List<PlayerData> sortAudioData(List<PlayerData> input, SortingOptions def) {
        if (input == null)
            return null;
        Log.v(LOG_TAG, "sortAudioData");
        List<PlayerData> ret = new ArrayList<>(input);
        switch (def.sorttech) {
            case SORT_TECH_ALPHA:
                switch (def.sortby) {
                    case SORT_TITLE:
                        switch (def.sortdir) {
                            case SORT_DIR_DOWN:
                                Collections.sort(ret, new TitleComparatorABC());
                                break;
                            case SORT_DIR_UP:
                                Collections.sort(ret, new TitleComparatorABC());
                                Collections.reverse(ret);
                                break;
                        }
                        break;
                    case SORT_ARTIST:
                        switch (def.sortdir) {
                            case SORT_DIR_DOWN:
                                Collections.sort(ret, new ArtistComparatorABC());
                                break;
                            case SORT_DIR_UP:
                                Collections.sort(ret, new ArtistComparatorABC());
                                Collections.reverse(ret);
                                break;
                        }
                        break;
                    case SORT_FILENAME:
                        throw new RuntimeException("Not Supported.");
                }
                break;
            case SORT_TECH_NUM:
                switch (def.sortby) {
                    case SORT_TITLE:
                        switch (def.sortdir) {
                            case SORT_DIR_DOWN:
                                Collections.sort(ret, new TitleComparator012());
                                break;
                            case SORT_DIR_UP:
                                Collections.sort(ret, new TitleComparator012());
                                Collections.reverse(ret);
                                break;
                        }
                        break;
                    case SORT_ARTIST:
                        switch (def.sortdir) {
                            case SORT_DIR_DOWN:
                                Collections.sort(ret, new ArtistComparator012());
                                break;
                            case SORT_DIR_UP:
                                Collections.sort(ret, new ArtistComparator012());
                                Collections.reverse(ret);
                                break;
                        }
                        break;
                    case SORT_FILENAME:
                        throw new RuntimeException("Not Supported.");
                }
                break;
        }
        return ret;
    }

    public static List<Playlist> sortPlaylist(List<Playlist> input, SortingOptions def) {
        if (input == null)
            return null;
        Log.v(LOG_TAG, "sortPlaylist");
        List<Playlist> ret = new ArrayList<>(input);
        switch (def.sortby) {
            default:
                switch (def.sortdir) {
                    case SORT_DIR_DOWN:
                        //Sort Normally A-Z
                        Collections.sort(ret, new PLTitleComparatorABC());
                        break;
                    case SORT_DIR_UP:
                        //Sort Upwards Z-A
                        Collections.sort(ret, new PLTitleComparatorABC());
                        Collections.reverse(ret);
                        break;
                }
                break;
        }
        return ret;
    }

    private static int extractIntegerFromString(String s) {
        String num = getIntegersInString(s);
        try {
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getIntegersInString(String s) {
        StringBuilder ret = new StringBuilder();
        int index = 0;
        while (index < s.length()) {
            char c = s.charAt(index++);
            if (Character.isDigit(c))
                ret.append(c);
        }
        return ret.toString();
    }

    private static class PLTitleComparatorABC implements Comparator<Playlist> {
        @Override
        public int compare(Playlist o1, Playlist o2) {
            return o1.getMetadata().getTitle().toLowerCase().compareTo(o2.getMetadata().getTitle().toLowerCase());
        }
    }

    private static class PLTitleComparator012 implements Comparator<Playlist> {
        @Override
        public int compare(Playlist o1, Playlist o2) {
            return extractIntegerFromString(o1.getMetadata().getTitle()) - extractIntegerFromString(o2.getMetadata().getTitle());
        }
    }

    private static class TitleComparatorABC implements Comparator<PlayerData> {
        @Override
        public int compare(PlayerData o1, PlayerData o2) {
            String v1 = o1.getMetadata().getTitle() == null ? "" : o1.getMetadata().getTitle();
            String v2 = o2.getMetadata().getTitle() == null ? "" : o2.getMetadata().getTitle();
            return v1.toLowerCase().compareTo(v2.toLowerCase());
        }
    }

    private static class TitleComparator012 implements Comparator<PlayerData> {
        @Override
        public int compare(PlayerData o1, PlayerData o2) {
            String v1 = o1.getMetadata().getTitle() == null ? "" : o1.getMetadata().getTitle();
            String v2 = o2.getMetadata().getTitle() == null ? "" : o2.getMetadata().getTitle();
            return extractIntegerFromString(v1) - extractIntegerFromString(v2);
        }
    }

    private static class ArtistComparatorABC implements Comparator<PlayerData> {
        @Override
        public int compare(PlayerData o1, PlayerData o2) {
            String v1 = o1.getMetadata().getTitle() == null ? "" : o1.getMetadata().getTitle();
            String v2 = o2.getMetadata().getTitle() == null ? "" : o2.getMetadata().getTitle();
            return v1.toLowerCase().compareTo(v2.toLowerCase());
        }
    }

    private static class ArtistComparator012 implements Comparator<PlayerData> {
        @Override
        public int compare(PlayerData o1, PlayerData o2) {
            String v1 = o1.getMetadata().getTitle() == null ? "" : o1.getMetadata().getTitle();
            String v2 = o2.getMetadata().getTitle() == null ? "" : o2.getMetadata().getTitle();
            return extractIntegerFromString(v1) - extractIntegerFromString(v2);
        }
    }
}