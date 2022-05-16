package com.phaseshifter.canora.plugin;

import android.graphics.drawable.Drawable;

import com.phaseshifter.canora.data.media.audio.AudioData;
import com.phaseshifter.canora.data.media.playlist.AudioPlaylist;

import java.util.List;
import java.util.Set;

public interface StreamPlugin {
    class SearchArguments {
        public String searchText;

        public int resultsPerPage;
        public int page;

        public String artist;
        public String album;
        public String genre;

        public Long durationMin;
        public Long durationMax;

        public SearchArguments(String searchText, int resultsPerPage, int page) {
            this.searchText = searchText;
            this.resultsPerPage = resultsPerPage;
            this.page = page;
        }

        public SearchArguments(String searchText, int resultsPerPage, int page, String artist, String album, String genre, Long durationMin, Long durationMax) {
            this.searchText = searchText;
            this.resultsPerPage = resultsPerPage;
            this.page = page;
            this.artist = artist;
            this.album = album;
            this.genre = genre;
            this.durationMin = durationMin;
            this.durationMax = durationMax;
        }
    }

    class Category {
        public String name;
        public Set<String> entries;
        public Drawable icon;

        public Category(String name, Set<String> entries, Drawable icon) {
            this.name = name;
            this.entries = entries;
            this.icon = icon;
        }
    }

    class HeaderData {
        public String title;
        public Drawable icon;

        public HeaderData(String title, Drawable icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    HeaderData getHeader();

    List<Category> getCategories();

    List<AudioData> search(SearchArguments args);

    AudioPlaylist getPlaylist(String category, String entry, int resultsPerPage, int page);
}
