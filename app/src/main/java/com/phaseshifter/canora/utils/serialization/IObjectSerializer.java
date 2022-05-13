package com.phaseshifter.canora.utils.serialization;

import java.io.IOException;
import java.io.Serializable;

public interface IObjectSerializer {
    byte[] serialize(Serializable object) throws IOException;

    Object deserialize(byte[] input) throws IOException, ClassNotFoundException;
}