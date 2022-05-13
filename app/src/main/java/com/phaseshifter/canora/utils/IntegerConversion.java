package com.phaseshifter.canora.utils;

public class IntegerConversion {
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static int safeDoubleToInt(double l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}