package com.dat3m.dartagnan.solver.propagator;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.GeneralExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.ExecutedSetWrapper;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.StaticRelationGraphWrapper;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.StaticWMMSetWrapper;
import com.dat3m.dartagnan.wmm.Definition;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.definition.CartesianProduct;
import com.dat3m.dartagnan.wmm.definition.SetIdentity;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Set;

public class PropagatorExecutionGraph implements GeneralExecutionGraph {

    private final EventDomain domain;
    private final ExecutedSetWrapper executedEvents;
    private final BiMap<Relation, RelationGraph> relationGraphMap;
    private final BiMap<Filter, SetPredicate> setMap;
    private final Set<Relation> cutRelations;

    public PropagatorExecutionGraph(EventDomain domain, Collection<Relation> trackedRelations, Set<Relation> staticRelations, Set<Relation> cutRelations, RelationAnalysis ra, Set<Relation> setInducedRelations) {
        this.domain = domain;
        this.relationGraphMap = HashBiMap.create();
        this.setMap = HashBiMap.create();
        this.cutRelations = cutRelations;
        this.executedEvents = new ExecutedSetWrapper(domain.size());
        executedEvents.setName("exec");

        initializeTracked(trackedRelations);
        initializeStatic(staticRelations, ra);
    }

    private void initializeTracked(Collection<Relation> relations) {
        for (Relation rel : relations) {
            SimpleGraph graph = new SimpleGraph();
            graph.setName(rel.getNameOrTerm());
            graph.initializeToDomain(domain);
            relationGraphMap.put(rel, graph);
        }
    }

    private void initializeStatic(Collection<Relation> relations, RelationAnalysis ra) {
        for (Relation rel : relations) {
            StaticRelationGraphWrapper graph = new StaticRelationGraphWrapper(rel, ra);
            graph.initializeToDomain(domain);
            relationGraphMap.put(rel, graph);
        }
    }

    public void triggerDomainInitialization() {
       relationGraphMap.forEach((rel, graph) -> graph.initializeToDomain(domain));
    }

    public SetPredicate getOrCreateSetPredicate(Filter filter) {
        if (!setMap.containsKey(filter)) {
            setMap.put(filter, new StaticWMMSetWrapper(filter, domain));
        }
        return setMap.get(filter);
    }

    public SetPredicate getExecutedSet() {
        return executedEvents;
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
        executedEvents.backtrackTo(time);
    }

    // TODO: do!
    public void onPush() {
        executedEvents.onPush();
    }

    public void addElement(int elementId) {
        executedEvents.addElement(elementId);
    }
}
