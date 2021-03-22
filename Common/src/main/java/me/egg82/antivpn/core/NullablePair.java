package me.egg82.antivpn.core;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class should be used sparingly, only when needed.
 * It's far too easy to abuse it and have it become an anti-pattern.
 * Also, hashCode is calculated on Pair creation, so any changes made to objects
 * in this Pair won't be reflected in the hashCode.
 */
public class NullablePair<T1, T2> {
    private final @Nullable T1 t1;
    private final @Nullable T2 t2;

    private final int hc;

    public NullablePair(@Nullable T1 t1, @Nullable T2 t2) {
        this.t1 = t1;
        this.t2 = t2;

        this.hc = Objects.hash(t1, t2);
    }

    public @Nullable T1 getT1() { return t1; }

    public @Nullable T2 getT2() { return t2; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NullablePair)) {
            return false;
        }
        NullablePair<?, ?> that = (NullablePair<?, ?>) o;
        return Objects.equals(t1, that.t1) && Objects.equals(t2, that.t2);
    }

    @Override
    public int hashCode() { return hc; }

    @Override
    public String toString() {
        return "NullablePair{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }
}
