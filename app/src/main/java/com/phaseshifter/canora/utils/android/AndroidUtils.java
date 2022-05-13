package com.phaseshifter.canora.utils.android;

import androidx.collection.SparseArrayCompat;

import java.util.Objects;

public class AndroidUtils {
    /**
     * As the SparseArrayCompat class does not override the equals method we have to check every element manually for equality.
     *
     * @param ar1 The first sparse array to compare
     * @param ar2 The second sparse array to compare to ar1
     * @param <E> The type value of the Sparse arrays.
     * @return True if the objects inside ar1 are equal to the objects inside ar2, including order.
     */
    public static <E> boolean areSparseArraysEqual(SparseArrayCompat<E> ar1, SparseArrayCompat<E> ar2) {
        if (ar1 == null && ar2 == null)
            return true;
        if ((ar1 == null) || (ar2 == null))
            return false;
        if (ar1.size() != ar2.size())
            return false;
        for (int i = 0; i < ar1.size(); i++) {
            if (ar1.keyAt(i) != ar2.keyAt(i)
                    && !Objects.equals(ar1.valueAt(i), ar2.valueAt(i))) {
                return false;
            }
        }
        return true;
    }
}