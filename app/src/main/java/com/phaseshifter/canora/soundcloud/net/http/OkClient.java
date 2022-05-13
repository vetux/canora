package com.phaseshifter.canora.soundcloud.net.http;

import com.phaseshifter.canora.soundcloud.util.LogKeeper;
import com.phaseshifter.canora.soundcloud.util.Pair;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class OkClient implements HttpClient {
    private final String LOG_TAG = "HTTPCLIENT/" + hashCode();

    private final OkHttpClient client;
    private final LogKeeper logger;

    OkClient(LogKeeper logger, int connectTimeout, TimeUnit connectTimeoutUnit, int writeTimeout, TimeUnit writeTimeoutUnit, int readTimeout, TimeUnit readTimeoutUnit) {
        client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, connectTimeoutUnit)
                .writeTimeout(writeTimeout, writeTimeoutUnit)
                .readTimeout(readTimeout, readTimeoutUnit)
                .build();
        this.logger = logger;
    }

    OkClient(LogKeeper logger) {
        this(logger, 10, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, 30, TimeUnit.SECONDS);
    }

    OkClient() {
        this(new LogKeeper(), 10, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, 30, TimeUnit.SECONDS);
    }

    @Override
    public HttpResponse doRequest(HttpRequest request) throws IOException {
        if (request == null || request.getUrl() == null)
            throw new IllegalArgumentException("Invalid URL: " + (request == null ? null : request.getUrl()));

        HttpUrl url = HttpUrl.parse(request.getUrl());
        if (url == null)
            throw new IllegalArgumentException("Failed parsing URL: " + request.getUrl());

        HttpUrl.Builder urlBuilder = url.newBuilder();

        Request okRequest;
        switch (request.getMethod()) {
            case GET:
                if (request.getParameters() != null) {
                    for (Pair<String, String> param : request.getParameters()) {
                        urlBuilder.addQueryParameter(param.getFirst(), param.getSecond());
                    }
                }
                okRequest = new Request.Builder()
                        .url(urlBuilder.build())
                        .method(HttpMethod.GET.method, null)
                        .addHeader("Accept", "*/*")
                        .build();
                break;
            case POST:
                if (request.getParameters() != null) {
                    MultipartBody.Builder builder = new MultipartBody.Builder();
                    builder.setType(MultipartBody.FORM);
                    for (Pair<String, String> param : request.getParameters()) {
                        builder.addFormDataPart(param.getFirst(), param.getSecond());
                    }
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .post(builder.build())
                            .addHeader("Accept", "*/*")
                            .build();
                } else {
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .method(HttpMethod.POST.method, RequestBody.create("", null))
                            .addHeader("Accept", "*/*")
                            .header("Content-Length", "0")
                            .build();
                }
                break;
            case PUT:
                if (request.getParameters() != null) {
                    MultipartBody.Builder builder = new MultipartBody.Builder();
                    builder.setType(MultipartBody.FORM);
                    for (Pair<String, String> param : request.getParameters()) {
                        builder.addFormDataPart(param.getFirst(), param.getSecond());
                    }
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .put(builder.build())
                            .addHeader("Accept", "*/*")
                            .build();
                } else {
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .method(HttpMethod.PUT.method, RequestBody.create("", null))
                            .addHeader("Accept", "*/*")
                            .header("Content-Length", "0")
                            .build();
                }
                break;
            case DELETE:
                if (request.getParameters() != null) {
                    MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
                    bodyBuilder.setType(MultipartBody.FORM);
                    for (Pair<String, String> param : request.getParameters()) {
                        bodyBuilder.addFormDataPart(param.getFirst(), param.getSecond());
                    }
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .delete(bodyBuilder.build())
                            .addHeader("Accept", "*/*")
                            .build();
                } else {
                    okRequest = new Request.Builder()
                            .url(urlBuilder.build())
                            .method(HttpMethod.DELETE.method, RequestBody.create("", null))
                            .addHeader("Accept", "*/*")
                            .header("Content-Length", "0")
                            .build();
                }
                break;
            case HEAD:
                okRequest = new Request.Builder()
                        .url(urlBuilder.build())
                        .head()
                        .addHeader("Accept", "*/*")
                        .build();
                break;
            default:
                throw new IllegalArgumentException("Http method not supported: " + request.getMethod().method);
        }
        logger.log(LOG_TAG, "Sending http request: " + okRequest.url().toString());
        return new OkResponse(client.newCall(okRequest).execute());
    }

    @Override
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
