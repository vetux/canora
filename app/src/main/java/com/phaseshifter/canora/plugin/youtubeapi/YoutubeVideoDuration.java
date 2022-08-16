package com.phaseshifter.canora.plugin.youtubeapi;

public enum YoutubeVideoDuration {
    ANY("any"),
    LONG("long"),
    MEDIUM("medium"),
    SHORT("short");

    public final String str;

    private YoutubeVideoDuration(String str) {
        this.str = str;
    }
}
