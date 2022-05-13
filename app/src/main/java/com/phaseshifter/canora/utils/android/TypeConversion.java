package com.phaseshifter.canora.utils.android;

import androidx.collection.SparseArrayCompat;

import java.util.ArrayList;
import java.util.List;

public class TypeConversion {
    /**
     * Loses key information as sparse arrays can have keys starting above 0, ordering will be retained
     *
     * @param input The sparse array to convert to a list.
     * @param <E>   The sparse array element type.
     * @return A list representation of the supplied sparse array.
     */
    public static <E> List<E> sparseArrayAsList(SparseArrayCompat<E> input) {
        List<E> ret = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            int key = input.keyAt(i);
            E value = input.get(key);
            ret.add(value);
        }
        return ret;
    }

    public static <E> SparseArrayCompat<E> listAsSparseArray(List<E> input) {
        SparseArrayCompat<E> ret = new SparseArrayCompat<E>();
        for (int i = 0; i < input.size(); i++) {
            E value = input.get(i);
            ret.append(i, value);
        }
        return ret;
    }
}