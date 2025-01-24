package com.dat3m.dartagnan.solver.OnlineCaatTest;

import java.util.HashMap;
import java.util.Map;

public class Stats<T> {
    private final HashMap<T, Integer> stats = new HashMap<>();

    public void track(T toTrack) {
        Integer value = stats.putIfAbsent(toTrack, 1);
        if (value != null) {
            stats.put(toTrack, ++value);
        }
    }

    public int check(T toCheck) {
        Integer value = stats.get(toCheck);
        return value != null ? value : 0;
    }

    public String toString(int threshold) {
        StringBuilder sb = new StringBuilder();
        stats.entrySet().stream().filter(e -> e.getValue() >= threshold).sorted(Map.Entry.comparingByValue()).forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        return sb.toString();
    }
}
