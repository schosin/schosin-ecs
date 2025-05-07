package de.schosin.ecs.api.utils;

import java.lang.reflect.Array;

public class ArrayUtils {

    /**
     * Concatenates the array and varargs and returns a new array
     * with the all elements of first and others in order.
     * 
     * @return new array of length first.length + others.length
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concat(Class<T> arrayClazz, T[] first, T... others) {
        var array = (T[]) Array.newInstance(arrayClazz, first.length + others.length);
        System.arraycopy(first, 0, array, 0, first.length);
        System.arraycopy(others, 0, array, first.length, others.length);

        return array;
    }
    
}
