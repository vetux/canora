package com.phaseshifter.canora.plugin.soundcloud.api_v2.data;

public class SCV2TrackStreamData {
    public final String url;
    public final SCV2StreamProtocol protocol;

    public SCV2TrackStreamData(String url, SCV2StreamProtocol protocol) {
        this.url = url;
        this.protocol = protocol;
    }
}
