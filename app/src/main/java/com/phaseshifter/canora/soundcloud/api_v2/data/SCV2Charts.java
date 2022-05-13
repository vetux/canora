package com.phaseshifter.canora.soundcloud.api_v2.data;

import com.phaseshifter.canora.soundcloud.api.data.SCGenre;

import java.io.Serializable;
import java.util.List;

/**
 * A Charts representation of the given genre.
 */
public abstract class SCV2Charts implements Serializable {
    private static final long serialVersionUID = 1;

    protected SCGenre genre;
    protected String kind;
    protected String last_updated;
    protected List<SCV2ChartTrack> tracks;

    public SCGenre getGenre() {
        return genre;
    }

    public String getKind() {
        return kind;
    }

    public String getLast_updated() {
        return last_updated;
    }

    public List<SCV2ChartTrack> getTracks() {
        return tracks;
    }
}
