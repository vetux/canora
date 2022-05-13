package com.phaseshifter.canora.utils.io;

import java.io.File;
import java.io.IOException;

public interface IByteIO {
    void write(byte[] data, File file) throws IOException;

    byte[] read(File file) throws IOException;
}