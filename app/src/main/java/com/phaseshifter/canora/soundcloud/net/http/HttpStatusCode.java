package com.phaseshifter.canora.soundcloud.net.http;

public enum HttpStatusCode {
    OK(200),
    ;

    public final int code;

    HttpStatusCode(final int code) {
        this.code = code;
    }
}
