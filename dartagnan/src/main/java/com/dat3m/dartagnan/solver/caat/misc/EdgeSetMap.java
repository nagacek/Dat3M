package com.dat3m.dartagnan.solver.caat.misc;

import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.*;

public class EdgeSetMap {
    private HashMap<String, HashSet<Edge>> map;

    private EdgeSetMap() {
        map = new HashMap<>();
    }

    public static EdgeSetMap fromTupleSetMap(TupleSetMap tupleMap, ExecutionModel execModel) {
        EdgeSetMap toReturn = new EdgeSetMap();
        for (var entry : tupleMap.getEntries()) {
            String name = entry.getKey();
            TupleSet tuples = entry.getValue();
            Iterator<Tuple> tupleIterator = tuples.iterator();
            while (tupleIterator.hasNext()) {
                Tuple nextTuple = tupleIterator.next();
                if (execModel.eventExists(nextTuple.getFirst()) && execModel.eventExists(nextTuple.getSecond())) {
                    int dId1 = execModel.getData(nextTuple.getFirst()).get().getId();
                    int dId2 = execModel.getData(nextTuple.getSecond()).get().getId();
                    Edge newEdge = new Edge(dId1, dId2);
                    toReturn.initAndAdd(name, newEdge);
                }
            }
        }
        return toReturn;
    }

    public void merge(EdgeSetMap other) {
        for (var entry : other.getEntries()) {
            Set<Edge> current = map.get(entry.getKey());
            if (current == null) {
                map.put(entry.getKey(), entry.getValue());
            } else {
                current.addAll(entry.getValue());
            }
        }
    }

    protected Set<Map.Entry<String, HashSet<Edge>>> getEntries() {
        return map.entrySet();
    }

    public boolean contains(String name, Edge edge) {
        HashSet<Edge> edges = map.get(name);
        return edges == null ? false : edges.contains(edge);
    }

    public boolean contains(String name) {
        return map.get(name) != null;
    }

    private void initAndAdd(String name, Edge edge) {
        if (!map.containsKey(name)) {
            map.put(name, new HashSet<>());
        }
        HashSet<Edge> edges = map.get(name);
        edges.add(edge);
    }
}
