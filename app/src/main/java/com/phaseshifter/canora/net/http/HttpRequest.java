package com.phaseshifter.canora.net.http;

import com.phaseshifter.canora.utils.Pair;

import java.util.List;

public class HttpRequest {
    private final HttpMethod method;
    private final String url;
    private final List<Pair<String, String>> parameters;

    /**
     * Constructs a immutable http request from the supplied url and parameters.
     *
     * @param method     The Http Method to use for this request.
     * @param url        The request url, may contain GET parameters.
     * @param parameters A list of data to send with the request. May be null.
     */
    public HttpRequest(HttpMethod method, String url, List<Pair<String, String>> parameters) {
        this.url = url;
        this.method = method;
        this.parameters = parameters;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public List<Pair<String, String>> getParameters() {
        return parameters;
    }
}