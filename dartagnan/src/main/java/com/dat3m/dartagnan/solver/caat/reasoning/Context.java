package com.dat3m.dartagnan.solver.caat.reasoning;

import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat4wmm.WMMSolver;
import com.dat3m.dartagnan.wmm.relation.Relation;

import java.util.Set;
import java.util.function.BiFunction;

public class Context {
    private final EdgeSetMap map;
    private final Set<RelationGraph> set;
    private final BiFunction<RelationGraph, Edge, RelationGraph.Presence> hasStaticPresence;
    private final CAATSolver.StaticStatistics stats;

    public Context(Set<RelationGraph> set, EdgeSetMap map, BiFunction<RelationGraph, Edge, RelationGraph.Presence> hasStaticPresence, CAATSolver.StaticStatistics stats) {
        this.map = map;
        this.set = set;
        this.hasStaticPresence = hasStaticPresence;
        this.stats = stats;
    }

    public boolean isCovered(RelationGraph graph, Edge edge) {
        return map.contains(graph, edge) || set.contains(graph);
    }

    public void chooseRelation(RelationGraph rel) {
        set.add(rel);
    }

    public RelationGraph.Presence hasStaticPresence(RelationGraph relGraph, Edge edge) {
        return hasStaticPresence.apply(relGraph, edge);
    }

    public void increment() { stats.increment(); }
    public void incrementStatic() { stats.incrementStatic(); }
    public void incrementUnion() { stats.incrementUnion(); }

}
