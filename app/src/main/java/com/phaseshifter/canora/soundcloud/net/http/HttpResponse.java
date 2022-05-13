package com.phaseshifter.canora.soundcloud.net.http;

import java.io.IOException;

public interface HttpResponse {
    /**
     * @return The returned status code.
     */
    int getStatusCode();

    /**
     * Loads the response data into memory and returns it as a byte array.
     *
     * @return The byte array representing the body bytes.
     * @throws IOException
     */
    byte[] getBodyBytes() throws IOException;

    /**
     * Loads the response data into memory and returns it as a String.
     *
     * @return The String representing the body.
     * @throws IOException
     */
    String getBodyString() throws IOException;

    /**
     * Releases held resources. The status code is unaffected.
     * Once closed getBodyBytes / getBodyString will throw a RuntimeException.
     */
    void close();
}
