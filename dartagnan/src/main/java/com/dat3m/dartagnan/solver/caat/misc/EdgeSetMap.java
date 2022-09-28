package com.dat3m.dartagnan.solver.caat.misc;

import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.*;

public class EdgeSetMap {
    private HashMap<RelationGraph, HashSet<Edge>> map;

    private EdgeSetMap() {
        map = new HashMap<>();
    }

    public static EdgeSetMap fromTupleSetMap(TupleSetMap tupleMap, ExecutionModel execModel, ExecutionGraph execGraph) {
        EdgeSetMap toReturn = new EdgeSetMap();
        for (var entry : tupleMap.getEntries()) {
            Relation rel = entry.getKey();
            TupleSet tuples = entry.getValue();
            Iterator<Tuple> tupleIterator = tuples.iterator();
            while (tupleIterator.hasNext()) {
                Tuple nextTuple = tupleIterator.next();
                if (execModel.eventExists(nextTuple.getFirst()) && execModel.eventExists(nextTuple.getSecond())) {
                    int dId1 = execModel.getData(nextTuple.getFirst()).get().getId();
                    int dId2 = execModel.getData(nextTuple.getSecond()).get().getId();
                    Edge newEdge = new Edge(dId1, dId2);
                    toReturn.initAndAdd(execGraph.getRelationGraph(rel), newEdge);
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

    protected Set<Map.Entry<RelationGraph, HashSet<Edge>>> getEntries() {
        return map.entrySet();
    }

    public boolean contains(RelationGraph rel, Edge edge) {
        HashSet<Edge> edges = map.get(rel);
        return edges == null ? false : edges.contains(edge);
    }

    public boolean contains(Relation rel) {
        return map.get(rel) != null;
    }

    private void initAndAdd(RelationGraph graph, Edge edge) {
        if (!map.containsKey(graph)) {
            map.put(graph, new HashSet<>());
        }
        HashSet<Edge> edges = map.get(graph);
        edges.add(edge);
    }
}
