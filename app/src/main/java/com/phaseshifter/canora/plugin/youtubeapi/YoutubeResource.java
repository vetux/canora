package com.phaseshifter.canora.plugin.youtubeapi;

public enum YoutubeResource {
    YOUTUBE_CHANNEL("channel"),
    YOUTUBE_PLAYLIST("playlist"),
    YOUTUBE_VIDEO("video");

    public final String str;

    private YoutubeResource(String str) {
        this.str = str;
    }
}
