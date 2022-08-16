package com.phaseshifter.canora.net.http;

import com.phaseshifter.canora.utils.LogKeeper;

import java.util.concurrent.TimeUnit;

public class HttpClientBuilder {
    private LogKeeper logger = new LogKeeper();
    private int connectTimeout = 10;
    private TimeUnit connectTimeoutUnit = TimeUnit.SECONDS;
    private int writeTimeout = 10;
    private TimeUnit writeTimeoutUnit = TimeUnit.SECONDS;
    private int readTimeout = 30;
    private TimeUnit readTimeoutUnit = TimeUnit.SECONDS;

    public HttpClientBuilder() {
    }

    public static HttpClientBuilder create() {
        return new HttpClientBuilder();
    }

    public HttpClientBuilder setLogger(LogKeeper logger) {
        if (logger == null)
            throw new IllegalArgumentException();
        this.logger = logger;
        return this;
    }

    public HttpClientBuilder setConnectTimeout(int timeout, TimeUnit timeunit) {
        connectTimeout = timeout;
        connectTimeoutUnit = timeunit;
        return this;
    }

    public HttpClientBuilder setWriteTimeout(int timeout, TimeUnit timeunit) {
        writeTimeout = timeout;
        writeTimeoutUnit = timeunit;
        return this;
    }

    public HttpClientBuilder setReadTimeout(int timeout, TimeUnit timeunit) {
        readTimeout = timeout;
        readTimeoutUnit = timeunit;
        return this;
    }

    public HttpClient build() {
        return new OkClient(logger, connectTimeout, connectTimeoutUnit, writeTimeout, writeTimeoutUnit, readTimeout, readTimeoutUnit);
    }
}
