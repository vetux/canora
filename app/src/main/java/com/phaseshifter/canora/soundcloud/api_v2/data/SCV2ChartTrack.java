package com.phaseshifter.canora.soundcloud.api_v2.data;

import java.io.Serializable;

public class SCV2ChartTrack implements Serializable {
    private static final long serialVersionUID = 1;

    private final int score;
    private final SCV2Track track;

    public SCV2ChartTrack(int score, SCV2Track track) {
        this.score = score;
        this.track = track;
    }

    public int getScore() {
        return score;
    }

    public SCV2Track getTrack() {
        return track;
    }
}
