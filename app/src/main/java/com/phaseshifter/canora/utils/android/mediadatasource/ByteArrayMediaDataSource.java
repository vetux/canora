package com.phaseshifter.canora.utils.android.mediadatasource;

import android.annotation.TargetApi;
import android.media.MediaDataSource;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

@TargetApi(23)
public class ByteArrayMediaDataSource extends MediaDataSource implements Serializable {
    private byte[] bytes;

    public ByteArrayMediaDataSource(byte[] data) {
        bytes = data;
    }

    public ByteArrayMediaDataSource() {
        this(null);
    }

    public final void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public final byte[] getBytes() {
        if (bytes == null) {
            return null;
        }
        return bytes.clone();
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (bytes == null)
            return -1;
        if (position >= bytes.length)
            return -1;

        if (position + size > bytes.length)
            size -= (position + size) - bytes.length; //Reduces the size to the maximum available bytes. The Caller gets notified of the change by the return value.

        int bytesRed = 0;
        for (int i = offset; i <= size; i++) {
            buffer[bytesRed] = bytes[i];
            bytesRed++;
        }
        return bytesRed;
    }

    @Override
    public long getSize() throws IOException {
        if (bytes == null)
            return 0;
        return bytes.length;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ByteArrayMediaDataSource)) return false;
        return Arrays.equals(this.bytes, ((ByteArrayMediaDataSource) obj).bytes);
    }
}