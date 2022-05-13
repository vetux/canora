package com.phaseshifter.canora.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public abstract class ObjectSize {
    public static final int SIZE_REF = 8;
    public static final int SIZE_BOOL = 1; //At least one byte, depending on JVM

    private static final int MAXRECURSION = 200;

    /**
     * Recursively calculates a rough estimate of the object size based on primitives and references sizes.
     * As this is using Reflection it should only be used temporarily for testing.
     *
     * @param object The object to estimate the size of.
     * @return A rough estimate of the object size in Bytes.
     */
    public static long estimateSize(Object object) {
        if (object == null)
            return -1;
        long ret = 0;
        Class<?> objectClass = object.getClass();
        if (specialClass(objectClass))
            return estimateSpecial(object);
        Field[] fields = objectClass.getDeclaredFields();
        for (Field field : fields) {
            long size = 0;
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            } else if (field.getType().isPrimitive()) {
                if (byte.class.isAssignableFrom(field.getType())) {
                    size = Byte.BYTES;
                } else if (short.class.isAssignableFrom(field.getType())) {
                    size = Short.BYTES;
                } else if (int.class.isAssignableFrom(field.getType())) {
                    size = Integer.BYTES;
                } else if (long.class.isAssignableFrom(field.getType())) {
                    size = Long.BYTES;
                } else if (float.class.isAssignableFrom(field.getType())) {
                    size = Float.BYTES;
                } else if (double.class.isAssignableFrom(field.getType())) {
                    size = Double.BYTES;
                } else if (char.class.isAssignableFrom(field.getType())) {
                    size = Character.BYTES;
                } else if (boolean.class.isAssignableFrom(field.getType())) {
                    size = SIZE_BOOL;
                } else {
                    throw new RuntimeException("UNRECOGNIZED PRIMITIVE: " + field);
                }
            } else if (field.getType().isArray()) {
                size = SIZE_REF;
                try {
                    Object array = field.get(object);
                    if (array != null) {
                        int length = Array.getLength(array);
                        for (int i = 0; i < length; i++) {
                            Object element = Array.get(array, i);
                            if (element != null)
                                size += estimateRecursion(1, element) + SIZE_REF;
                            else
                                size += SIZE_REF;
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                ret += size;
            } else if (Object.class.isAssignableFrom(field.getType())) {
                try {
                    Object obj = field.get(object);
                    if (obj != null)
                        size = estimateRecursion(1, field.get(object)) + SIZE_REF;
                    else
                        size = SIZE_REF;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                throw new RuntimeException("Unrecognized Type: " + field);
            }
            field.setAccessible(false);
            ret += size;
        }
        return ret;
    }

    private static long estimateRecursion(int level, Object object) {
        if (level > MAXRECURSION || object == null)
            return 0;
        long ret = 0;
        Class<?> objectClass = object.getClass();
        if (specialClass(objectClass))
            return estimateSpecial(object);
        Field[] fields = objectClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType().isPrimitive()) {
                if (byte.class.isAssignableFrom(field.getType())) {
                    ret += Byte.BYTES;
                } else if (short.class.isAssignableFrom(field.getType())) {
                    ret += Short.BYTES;
                } else if (int.class.isAssignableFrom(field.getType())) {
                    ret += Integer.BYTES;
                } else if (long.class.isAssignableFrom(field.getType())) {
                    ret += Long.BYTES;
                } else if (float.class.isAssignableFrom(field.getType())) {
                    ret += Float.BYTES;
                } else if (double.class.isAssignableFrom(field.getType())) {
                    ret += Double.BYTES;
                } else if (char.class.isAssignableFrom(field.getType())) {
                    ret += Character.BYTES;
                } else if (boolean.class.isAssignableFrom(field.getType())) {
                    ret += 1;
                } else {
                    throw new RuntimeException("UNRECOGNIZED PRIMITIVE: " + field);
                }
            } else if (field.getType().isEnum()) {
                ret += SIZE_REF;
            } else if (field.getType().isArray()) {
                long size = SIZE_REF;
                try {
                    Object array = field.get(object);
                    if (array != null) {
                        int length = Array.getLength(array);
                        for (int i = 0; i < length; i++) {
                            Object element = Array.get(array, i);
                            if (element != null)
                                size += estimateRecursion(1, element) + SIZE_REF;
                            else
                                size += SIZE_REF;
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                ret += size;
            } else if (Object.class.isAssignableFrom(field.getType())) {
                try {
                    Object obj = field.get(object);
                    if (obj != null)
                        ret += estimateRecursion(level + 1, field.get(object));
                    else
                        ret += SIZE_REF;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                throw new RuntimeException("UNRECOGNIZED TYPE: " + field);
            }
            field.setAccessible(false);
        }
        return ret;
    }

    /**
     * Classes like String and Integer are modified under android and dont contain all fields.
     * Therefore we estimate the size of theses objects by a fixed number.
     *
     * @param classToCheck The class to check
     * @return True if the class represents a special class
     */
    private static boolean specialClass(Class<?> classToCheck) {
        if (classToCheck == Integer.class
                || classToCheck == String.class
                || classToCheck.isArray())
            return true;
        else
            return false;
    }

    private static long estimateSpecial(Object specialObject) {
        if (specialObject.getClass().isArray()) {
            long size = 0;
            int length = Array.getLength(Objects.requireNonNull(specialObject));
            for (int i = 0; i < length; i++) {
                Object element = Array.get(specialObject, i);
                if (element != null)
                    size += estimateRecursion(1, element) + SIZE_REF;
                else
                    size += SIZE_REF;
            }
            return size;
        } else if (specialObject instanceof Integer) {
            return Integer.BYTES;
        } else if (specialObject instanceof String) {
            return ((String) specialObject).length() * Character.BYTES;
        } else {
            throw new RuntimeException("Object not Special");
        }
    }
}