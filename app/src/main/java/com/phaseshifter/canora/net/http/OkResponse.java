package com.phaseshifter.canora.net.http;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;

class OkResponse implements HttpResponse {
    private final Response response;
    private final ResponseBody body;

    OkResponse(Response response) {
        this.response = response;
        this.body = response.body();
    }

    @Override
    public int getStatusCode() {
        return response.code();
    }

    @Override
    public byte[] getBodyBytes() throws IOException {
        if (body == null)
            return null;
        return streamToByteArray(body.byteStream());
    }

    @Override
    public String getBodyString() throws IOException {
        if (body == null)
            return null;
        return streamToString(body.byteStream());
    }

    @Override
    public void close() {
        body.close();
    }

    private byte[] streamToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        while (stream.read(buffer) > 0) {
            bout.write(buffer);
        }
        return bout.toByteArray();
    }

    private String streamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String str;
        while ((str = reader.readLine()) != null) {
            sb.append(str);
        }
        return sb.toString();
    }
}