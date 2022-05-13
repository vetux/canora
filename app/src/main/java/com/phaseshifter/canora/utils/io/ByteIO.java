package com.phaseshifter.canora.utils.io;

import java.io.*;

public class ByteIO implements IByteIO {
    public void write(byte[] data, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(data);
        bos.flush();
        bos.close();
    }

    public byte[] read(File file) throws IOException {
        byte[] bFile = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        if (fileInputStream.read(bFile) == -1)
            throw new EOFException("READ RETURNED -1");
        fileInputStream.close();
        return bFile;
    }
}