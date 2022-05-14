package com.phaseshifter.canora.net.http;

import java.io.IOException;

public interface HttpClient {
    /**
     * Executes a blocking http request.
     *
     * @param request The request to be made.
     * @return The resulting response.
     * @throws IOException              When the request could not be made.
     * @throws IllegalArgumentException When request is null or if the specified http method is not supported.
     */
    HttpResponse doRequest(HttpRequest request) throws IOException, IllegalArgumentException;

    /**
     * Releases held resources.
     */
    void shutdown();
}
