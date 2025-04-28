package com.dat3m.dartagnan.solver.propagator;

import com.dat3m.dartagnan.solver.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.GeneralExecutionGraph;
import com.dat3m.dartagnan.wmm.Relation;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Set;

public class PropagatorExecutionGraph implements GeneralExecutionGraph {

    private final EventDomain domain;
    private final BiMap<Relation, RelationGraph> relationGraphMap;
    private final Set<Relation> cutRelations;

    public PropagatorExecutionGraph(EventDomain domain, Collection<Relation> relations, Set<Relation> cutRelations) {
        this.domain = domain;
        this.relationGraphMap = HashBiMap.create();
        this.cutRelations = cutRelations;

        initializeGraphs(relations);
    }

    private void initializeGraphs(Collection<Relation> relations) {
        for (Relation rel : relations) {
            SimpleGraph graph = new SimpleGraph();
            graph.setName(rel.getNameOrTerm());
            graph.initializeToDomain(domain);
            relationGraphMap.put(rel, graph);
        }
    }

    public void triggerDomainInitialization() {
       relationGraphMap.forEach((rel, graph) -> graph.initializeToDomain(domain));
    }

    @Override
    public EventDomain getDomain() {
        return domain;
    }

    @Override
    public BiMap<Relation, RelationGraph> getRelationGraphMap() {
        return Maps.unmodifiableBiMap(relationGraphMap);
    }

    @Override
    public Set<Relation> getCutRelations() {
        return cutRelations;
    }

    @Override
    public RelationGraph getRelationGraph(Relation rel) {
        return relationGraphMap.get(rel);
    }

    @Override
    public void backtrackTo(int time) {
        for (RelationGraph graph : relationGraphMap.values()) {
            graph.backtrackTo(time);
        }
    }
}
