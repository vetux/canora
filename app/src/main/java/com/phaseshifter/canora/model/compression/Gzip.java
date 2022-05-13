package com.phaseshifter.canora.model.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip {
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(data);
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static byte[] decompress(byte[] data) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        List<Byte> uncompressedData = new ArrayList<>();
        int bytesRead;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = gis.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                uncompressedData.add(buffer[i]);
            }
        }
        gis.close();
        is.close();
        byte[] ret = new byte[uncompressedData.size()];
        for (int i = 0; i < uncompressedData.size(); i++) {
            ret[i] = uncompressedData.get(i);
        }
        return ret;
    }
}