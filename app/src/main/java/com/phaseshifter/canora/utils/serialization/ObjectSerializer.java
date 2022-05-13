package com.phaseshifter.canora.utils.serialization;

import java.io.*;

public class ObjectSerializer implements IObjectSerializer {
    @Override
    public byte[] serialize(Serializable object) throws IOException {
        if (object == null)
            throw new IllegalArgumentException();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream obOut = new ObjectOutputStream(byteOut);
        obOut.writeObject(object);
        obOut.close();
        byteOut.close();
        return byteOut.toByteArray();
    }

    @Override
    public Object deserialize(byte[] input) throws IOException, ClassNotFoundException {
        if (input == null)
            throw new IllegalArgumentException();
        Object ret;
        ByteArrayInputStream byteIn = new ByteArrayInputStream(input);
        ObjectInputStream in = new ObjectInputStream(byteIn);
        ret = in.readObject();
        in.close();
        byteIn.close();
        return ret;
    }
}