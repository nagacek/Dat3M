package com.dat3m.dartagnan.solver.caat4wmm.propagator;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.Domain;
import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;

import java.util.*;
import java.util.stream.Stream;

public class StaticRelationGraphWrapper implements RelationGraph {

    private final Relation relation;
    private final RelationAnalysis ra;
    private Domain<?> domain;

    final Map<Integer, Set<Integer>> outEdges = new HashMap<>();
    final Map<Integer, Set<Integer>> inEdges = new HashMap<>();

    public StaticRelationGraphWrapper(Relation rel, RelationAnalysis ra) {
        this.relation = rel;
        this.ra = ra;
    }

    @Override
    public List<? extends CAATPredicate> getDependencies() {
        return List.of();
    }

    @Override
    public String getName() {
        return relation.getNameOrTerm();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Domain<?> getDomain() {
        return domain;
    }

    @Override
    public void initializeToDomain(Domain<?> domain) {
        this.domain = domain;

        inEdges.clear();
        outEdges.clear();
        EventGraph g = ra.getKnowledge(relation).getMustSet();
        for (Event e : g.getDomain()) {
            EventData eData = new EventData(e);
            int idSource = domain.getId(eData);
            final Set<Integer> outTargets = outEdges.computeIfAbsent(idSource, k -> new HashSet<>());
            for (Event e2 : g.getOutMap().get(e)) {
                EventData e2Data = new EventData(e2);
                int idTarget = domain.getId(e2Data);
                outTargets.add(idTarget);
                final Set<Integer> inTargets = inEdges.computeIfAbsent(idTarget, k -> new HashSet<>());
                inTargets.add(idSource);
            }
        }

    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData tData, TContext tContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void repopulate() {

    }

    @Override
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added) {
        return List.of();
    }

    @Override
    public void backtrackTo(int time) {

    }

    @Override
    public Edge get(Edge edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size(int e, EdgeDirection dir) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Edge> edgeStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Edge> edgeStream(int e, EdgeDirection dir) {
        Map<Integer, Set<Integer>> edges = dir == EdgeDirection.OUTGOING ? outEdges : inEdges;
        Set<Integer> targetIds = edges.getOrDefault(e, Set.of());
        return targetIds.stream().map(id -> dir == EdgeDirection.OUTGOING ? new Edge(e, id) : new Edge(id, e));
    }

    @Override
    public boolean containsById(int id1, int id2) {
        return outEdges.getOrDefault(id1, Set.of()).contains(id2);
    }
}
