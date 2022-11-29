package com.phaseshifter.canora.ui.data.misc;

import com.phaseshifter.canora.ui.data.MainPage;

import java.io.Serializable;
import java.util.UUID;

public class ContentSelector implements Serializable {
    private final MainPage page;
    private final UUID uuid;

    public ContentSelector(MainPage selector, UUID uuid) {
        this.page = selector;
        this.uuid = uuid;
    }

    public ContentSelector(ContentSelector copy) {
        this(copy.page, copy.uuid);
    }

    public ContentSelector() {
        this(null, null);
    }

    public MainPage getPage() {
        return page;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isPlaylistView() {
        return page != MainPage.TRACKS
                && page != MainPage.SOUNDCLOUD_SEARCH
                && page != MainPage.YOUTUBE_SEARCH_VIDEOS
                && page != MainPage.YOUTUBE_DL
                && uuid == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ContentSelector that = (ContentSelector) obj;
        return this.page == that.page
                && this.uuid == that.uuid;
    }

    @Override
    public String toString() {
        return "SelectionIndicator{" +
                "selector=" + page +
                ", uuid=" + uuid +
                '}';
    }
}