package com.phaseshifter.canora.plugin;

import android.graphics.drawable.Drawable;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;
import java.util.Set;

public interface StreamPlugin {
    class Category {
        String name;
        Set<String> entries;
        Drawable icon;
    }

    class SearchArguments {
        String searchText;

        String artist;
        String album;
        String genre;

        long durationMin;
        long durationMax;

        int resultsPerPage;
        int page;
    }

    List<AudioData> search(SearchArguments args);

    List<Category> getCategories();

    AudioPlaylist getPlaylist(String category, String entry, int resultsPerPage, int page);
}
