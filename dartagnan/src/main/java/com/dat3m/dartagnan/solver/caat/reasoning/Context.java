package com.dat3m.dartagnan.solver.caat.reasoning;

import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;

import java.util.Set;

public class Context {
    private final EdgeSetMap map;
    private final Set<RelationGraph> set;

    public Context(Set<RelationGraph> set, EdgeSetMap map) {
        this.map = map;
        this.set = set;
    }

    public boolean isCovered(RelationGraph graph, Edge edge) {
        if (map.contains(graph, edge) || set.contains(graph)) {
            return true;
        }
        return false;
    }

    public void chooseRelation(RelationGraph rel) {
        set.add(rel);
    }
}
