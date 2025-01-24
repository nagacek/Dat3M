package com.dat3m.dartagnan.utils.collections;

import java.util.Collection;
import java.util.List;

public class Pair<T, K> {
    private final T first;
    private final K second;

    public Pair(T first, K second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public K getSecond() {
        return second;
    }

    @Override
    public int hashCode() { return  first.hashCode() ^ second.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair p)) {
            return false;
        }
        boolean firstEqual = false;
        boolean secondEqual = false;

        if (p.getFirst() == null || first == null) {
            if (p.getFirst() == null && first == null) {
                firstEqual = true;
            }
        } else if (p.getFirst().equals(first)) {
            firstEqual = true;
        }
        if (p.getSecond() == null || second == null) {
            if (p.getSecond() == null && second == null) {
                secondEqual = true;
            }
        } else if (p.getSecond().equals(second)) {
            secondEqual = true;
        }
        return firstEqual && secondEqual;
    }
}
