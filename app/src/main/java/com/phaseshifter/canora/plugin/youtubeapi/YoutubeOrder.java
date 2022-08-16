package com.phaseshifter.canora.plugin.youtubeapi;

public enum YoutubeOrder {
    YOUTUBE_DATE("date"),
    YOUTUBE_RATING("rating"),
    YOUTUBE_RELEVANCE("relevance"),
    YOUTUBE_TITLE("title"),
    YOUTUBE_VIDEO_COUNT("videoCount"),
    YOUTUBE_VIEW_COUNT("viewCount");

    public final String str;

    private YoutubeOrder(String str) {
        this.str = str;
    }
}
