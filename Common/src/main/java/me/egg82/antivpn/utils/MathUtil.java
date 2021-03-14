package me.egg82.antivpn.utils;

import it.unimi.dsi.fastutil.ints.IntArrays;
import org.jetbrains.annotations.NotNull;

public class MathUtil {
    private MathUtil() {
    }

    public static int percentile(int @NotNull [] list, double percentile) {
        IntArrays.quickSort(list);
        int index = (int) Math.ceil(percentile / (100.0d * list.length));
        return list[index - 1];
    }
}
