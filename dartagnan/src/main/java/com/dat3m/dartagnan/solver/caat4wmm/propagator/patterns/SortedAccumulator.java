package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import java.util.*;
import java.util.stream.Stream;

public class SortedAccumulator<T extends Regulator>{
    private final List<T> list;
    private final Comparator<T> comparator;
    private final int capacity;

    private final double reward;
    private final double punishment;
    private double scale = 1;

    public SortedAccumulator(Comparator<T> comparator, int capacity, double reward, double punishment) {
        this.reward = reward;
        this.punishment = punishment;
        this.capacity = capacity;
        this.comparator = comparator;
        list = new ArrayList<>();
    }

    public SortedAccumulator(Collection<T> initialSet, Comparator<T> comparator, int capacity, double reward, double punishment) {
        this.reward = reward;
        this.punishment = punishment;
        this.capacity = capacity;
        this.comparator = comparator;
        list = new ArrayList<>();
        addAll(initialSet);
    }

    public void reward(T value) {
        changeWith(value, reward);
    }

    public void punishAll() {
        if (!list.isEmpty()) {
            scale *= punishment;
        }
        restoreScale();
    }

    public void punishExcept(T value) {
        punishAll();
        changeWith(value, 1/punishment);
        restoreScale();
    }

    private void changeWith(T value, double factor) {
        if (value.getScore() > 1.0E150d && factor > 0.0d) { return; }
        if (list.remove(value)) {
            value.reward(factor);
            add(value);
        }
    }

    private void changeAllWith(double factor) {
        list.forEach(value -> value.reward(factor));
        list.sort(comparator);
    }

    private void restoreScale() {
        if (scale < 0.0000000001) {
            changeAllWith(scale*10000);
            scale = 1;
        }
    }

    public boolean hasCapacityFor(T toInsert) {
        if (toInsert == null) {
            return false;
        }
        if (list.size() < capacity){
            return true;
        }
        return comparator.compare((T)toInsert.adaptToScale(scale), last()) < 0;
    }

    public T last() { return list.get(list.size() - 1); }
    public T first() { return list.get(0); }
    public Iterator<T> iterator() {
        return list.iterator();
    }

    public Stream<T> stream() {
        return list.stream();
    }

    public List<T> asList() {
        return list;
    }

    public boolean addAll(Collection<T> toInsert) {
        if (toInsert.isEmpty()) {
            return false;
        }
        Collection<T> toScale = new ArrayList<>(toInsert.size());
        toInsert.forEach(ele -> toScale.add((T)ele.adaptToScale(scale)));
        T any = toScale.stream().findAny().orElse(null);
        if (hasCapacityFor(any)){
            for (T insert : toScale) {
                addInOrder(insert);
            }
            restrictToCapacity();
            return true;
        }
        return false;
    }

    public boolean add(T toInsert) {
        T toScale = (T)toInsert.adaptToScale(scale);
        if (hasCapacityFor(toScale)){
           addInOrder(toScale);
           restrictToCapacity();
           return true;
        }
        restrictToCapacity();
        return false;
    }

    private void addInOrder(T toInsert) {
        int index = Collections.binarySearch(list, toInsert, comparator);
        if (index < 0) {
            list.add((-1 * index) - 1, toInsert);
        } else {
            list.add(index, toInsert);
        }
    }

    private void restrictToCapacity() {
        if (list.size() > capacity) {
            list.subList(capacity, list.size()).clear();
        }
    }

    public String print(T entry) {
        return entry.toString() + ": " + entry.getScore() * scale;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (T entry : list) {
            sb.append(entry.toString()).append(": ").append(entry.getScore() * scale).append("\n");
        }
        return sb.toString();
    }

}
