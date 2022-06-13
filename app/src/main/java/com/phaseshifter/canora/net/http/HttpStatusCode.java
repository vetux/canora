package com.phaseshifter.canora.net.http;

public enum HttpStatusCode {
    OK(200),
    NOT_FOUND(404)
    ;

    public final int code;

    HttpStatusCode(final int code) {
        this.code = code;
    }
}
