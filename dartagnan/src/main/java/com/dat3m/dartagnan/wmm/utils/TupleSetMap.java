package com.dat3m.dartagnan.wmm.utils;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TupleSetMap {
    private final HashMap<Relation, TupleSet> map;

    public TupleSetMap(TupleSetMap other) {
        map = new HashMap<>(other.getMap());
    }

    public TupleSetMap(Relation rel, TupleSet set) {
        map = new HashMap<>();
        map.put(rel, set);
    }

    public TupleSetMap(Map<Relation, Set<List<Event>>> other) {
        map = new HashMap<>();
        for (var entry : other.entrySet()) {
            TupleSet newSet = new TupleSet();
            entry.getValue().forEach(e -> {
                if (e.size() == 2)
                    newSet.add(new Tuple(e.get(0), e.get(1)));
            });
            if (newSet.size() > 0)
                map.put(entry.getKey(), newSet);
        }
    }

    public TupleSetMap() {
        map = new HashMap<>();
    }

    public void merge(TupleSetMap other) {
        other.getMap().forEach((name, set) -> map.merge(name, set, (set1, set2) -> new TupleSet(Sets.union(set1, set2))));
    }

    public TupleSetMap difference(TupleSetMap other) {
        TupleSetMap newMap = new TupleSetMap();
        for (var entry : map.entrySet()) {
            TupleSet difference = new TupleSet(entry.getValue());
            TupleSet overlap = other.get(entry.getKey());
            if (overlap != null) {
                difference.removeAll(overlap);
            }
            newMap.merge(new TupleSetMap(entry.getKey(), difference));
        }
        return newMap;
    }

    public TupleSet get(Relation rel) {
        return map.get(rel);
    }


    public boolean contains(Relation rel, Tuple tuple) {
        TupleSet contained = map.get(rel);
        return contained != null && contained.contains(tuple);
    }

    public boolean contains(Relation rel) {
        return map.containsKey(rel);
    }

    public Set<Map.Entry<Relation, TupleSet>> getEntries() { return map.entrySet(); }

    public Set<Relation> getRelations() { return map.keySet(); }

    public long getCount() {
        long count = 0;
        for (var entry : map.values()) {
            count += entry.size();
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (var entry : map.entrySet()) {
            str.append(entry.getKey().getName()).append(": ").append(entry.getValue()).append("\n");
        }
        return str.toString();
    }

    protected HashMap<Relation, TupleSet> getMap() {
        return map;
    }

}
