package me.egg82.antivpn.utils;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

public class MathUtil {
    private MathUtil() { }

    public static int percentile(@NotNull IntList list, double percentile) {
        int[] sorted = list.toIntArray();
        IntArrays.quickSort(sorted);
        int index = (int) Math.ceil(percentile / (100.0d * sorted.length));
        return sorted[index - 1];
    }
}
