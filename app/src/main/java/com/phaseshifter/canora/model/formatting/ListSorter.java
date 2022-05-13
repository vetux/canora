package com.phaseshifter.canora.model.formatting;

import android.util.Log;
import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;
import com.phaseshifter.canora.ui.data.formatting.SortDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.phaseshifter.canora.ui.data.formatting.SortDef.*;

public class ListSorter {
    private static final String LOG_TAG = "ListSorter";

    public static List<AudioData> sortAudioData(List<AudioData> input, SortDef def) {
        if (input == null)
            return null;
        Log.v(LOG_TAG, "sortAudioData");
        List<AudioData> ret = new ArrayList<>(input);
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

    public static List<AudioPlaylist> sortAudioPlaylist(List<AudioPlaylist> input, SortDef def) {
        if (input == null)
            return null;
        Log.v(LOG_TAG, "sortAudioPlaylist");
        List<AudioPlaylist> ret = new ArrayList<>(input);
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

    private static class PLTitleComparatorABC implements Comparator<AudioPlaylist> {
        @Override
        public int compare(AudioPlaylist o1, AudioPlaylist o2) {
            return o1.getMetadata().getTitle().toLowerCase().compareTo(o2.getMetadata().getTitle().toLowerCase());
        }
    }

    private static class PLTitleComparator012 implements Comparator<AudioPlaylist> {
        @Override
        public int compare(AudioPlaylist o1, AudioPlaylist o2) {
            return extractIntegerFromString(o1.getMetadata().getTitle()) - extractIntegerFromString(o2.getMetadata().getTitle());
        }
    }

    private static class TitleComparatorABC implements Comparator<AudioData> {
        @Override
        public int compare(AudioData o1, AudioData o2) {
            return o1.getMetadata().getTitle().toLowerCase().compareTo(o2.getMetadata().getTitle().toLowerCase());
        }
    }

    private static class TitleComparator012 implements Comparator<AudioData> {
        @Override
        public int compare(AudioData o1, AudioData o2) {
            return extractIntegerFromString(o1.getMetadata().getTitle()) - extractIntegerFromString(o2.getMetadata().getTitle());
        }
    }

    private static class ArtistComparatorABC implements Comparator<AudioData> {
        @Override
        public int compare(AudioData o1, AudioData o2) {
            return o1.getMetadata().getArtist().toLowerCase().compareTo(o2.getMetadata().getArtist().toLowerCase());
        }
    }

    private static class ArtistComparator012 implements Comparator<AudioData> {
        @Override
        public int compare(AudioData o1, AudioData o2) {
            return extractIntegerFromString(o1.getMetadata().getArtist()) - extractIntegerFromString(o2.getMetadata().getArtist());
        }
    }
}