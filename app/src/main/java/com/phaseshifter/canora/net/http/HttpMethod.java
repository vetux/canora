package com.phaseshifter.canora.net.http;

public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS");

    public final String method;

    private HttpMethod(String method) {
        this.method = method;
    }
}