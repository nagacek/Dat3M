package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.solver.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.google.common.collect.BiMap;

import java.util.Collection;
import java.util.Set;

public interface GeneralExecutionGraph {
    EventDomain getDomain();

    BiMap<Relation, RelationGraph> getRelationGraphMap();

    Set<Relation> getCutRelations();

    RelationGraph getRelationGraph(Relation rel);

    void backtrackTo(int time);

}
