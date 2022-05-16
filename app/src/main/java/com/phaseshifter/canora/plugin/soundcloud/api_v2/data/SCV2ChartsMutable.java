package com.phaseshifter.canora.plugin.soundcloud.api_v2.data;

import com.phaseshifter.canora.plugin.soundcloud.api.data.SCGenre;

import java.util.List;

public class SCV2ChartsMutable extends SCV2Charts {
    private static final long serialVersionUID = 1;

    public void setGenre(SCGenre genre) {
        this.genre = genre;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setLast_updated(String last_updated) {
        this.last_updated = last_updated;
    }

    public void setTracks(List<SCV2ChartTrack> tracks) {
        this.tracks = tracks;
    }
}
