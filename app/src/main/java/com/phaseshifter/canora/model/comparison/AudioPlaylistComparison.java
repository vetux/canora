package com.phaseshifter.canora.model.comparison;

import com.phaseshifter.canora.data.media.player.PlayerData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AudioPlaylistComparison {
    /**
     * Checks if two playlists are equal in a permissive way.
     * Allowed differences are:
     * -Ordering of the tracks.
     * -UUID changes of the playlist or tracks
     *
     * @param pl0 pl0
     * @param pl1 pl1
     * @return true if the playlists are equal by the above specified rules.
     */
    public static boolean isEqualPermissive(AudioPlaylist pl0, AudioPlaylist pl1) {
        if (!isEqual_exclude_UUID_Tracks(pl0, pl1))
            return false;
        List<PlayerData> unique = new ArrayList<>();
        List<Integer> occurences = new ArrayList<>();
        for (PlayerData track : pl0.getData()) {
            int index = unique.indexOf(track);
            if (index != -1) {
                if (occurences.size() <= index)
                    throw new RuntimeException();
                int currentOccurences = occurences.get(index);
                currentOccurences++;
                occurences.set(index, currentOccurences);
            } else {
                unique.add(track);
                occurences.add(1);
            }
        }
        List<PlayerData> uniquePl = new ArrayList<>();
        List<Integer> occurencesPl = new ArrayList<>();
        for (PlayerData track : pl1.getData()) {
            int index = uniquePl.indexOf(track);
            if (index != -1) {
                if (occurencesPl.size() <= index)
                    throw new RuntimeException();
                int currentOccurences = occurencesPl.get(index);
                currentOccurences++;
                occurencesPl.set(index, currentOccurences);
            } else {
                uniquePl.add(track);
                occurencesPl.add(1);
            }
        }
        if (unique.size() != uniquePl.size())
            return false;
        boolean matchFlag = true;
        for (PlayerData uni : unique) {
            PlayerData uniPlf = null;
            boolean uniFound = false;
            for (PlayerData uniPl : uniquePl) {
                if (AudioDataComparsion.isEqual_exclude_UUID(uniPl, uni)) {
                    uniPlf = uniPl;
                    uniFound = true;
                    break;
                }
            }
            if (!uniFound
                    || !occurences.get(unique.indexOf(uni)).equals(occurencesPl.get(uniquePl.indexOf(uniPlf)))) {
                matchFlag = false;
                break;
            }
        }
        return matchFlag;
    }

    public static boolean isEqual_exclude_UUID_Tracks(AudioPlaylist pl0, AudioPlaylist pl1) {
        if (pl0 == pl1) return true;
        if (pl0 == null || pl1 == null)
            return false;
        if (pl0.getClass() != pl1.getClass()) return false;
        return Objects.equals(pl0.getMetadata().getTitle(), pl1.getMetadata().getTitle());
    }
}