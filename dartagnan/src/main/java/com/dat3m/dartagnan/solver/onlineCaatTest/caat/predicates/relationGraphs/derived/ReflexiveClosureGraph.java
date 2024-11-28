package com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.derived;


import com.dat3m.dartagnan.solver.onlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.AbstractPredicate;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.RelationGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ReflexiveClosureGraph extends AbstractPredicate implements RelationGraph {

    private final RelationGraph inner;

    @Override
    public void validate (int time, Set<Derivable> activeSet, boolean active) {}

    public ReflexiveClosureGraph(RelationGraph inner) {
        this.inner = inner;
    }

    @Override
    public List<RelationGraph> getDependencies() {
        return Collections.singletonList(inner);
    }

    @Override
    public Edge get(Edge edge) {
        return edge.isLoop() ? edge.with(0, 0) : inner.get(edge);
    }

    @Override
    public Edge weakGet(Edge edge) {
        return edge.isLoop() ? edge.with(0, 0) : inner.weakGet(edge);
    }

    @Override
    public int size(int e, EdgeDirection dir) {
        return inner.size(e, dir) + (inner.containsById(e, e) ? 0 : 1);
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < domain.size(); i++) {
            size += size(i, EdgeDirection.OUTGOING);
        }
        return size;
    }

    @Override
    public boolean containsById(int id1, int id2) {
        return id1 == id2 || inner.containsById(id1, id2);
    }

    @Override
    public int getMinSize() {
        return Math.max(inner.getMinSize(), domain.size());
    }

    @Override
    public int getMinSize(int e, EdgeDirection dir) {
        return Math.max(inner.getMinSize(e, dir), 1);
    }

    @Override
    public int getMaxSize() {
        return inner.getMaxSize() + domain.size();
    }

    @Override
    public int getMaxSize(int e, EdgeDirection dir) {
        return inner.getMaxSize(e, dir) + 1;
    }

    @Override
    public Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) {
        return inner.checkBoneActivation(triggerId, time, bones);
    }

    private Edge derive(Edge e) {
        return e.withDerivationLength(e.getDerivationLength() + 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
        if (mode == PredicateHierarchy.PropagationMode.DELETE) {
            return Collections.emptyList();
        }
        if (changedSource == inner) {
            return ((Collection<Edge>) added).stream().filter(e -> !e.isLoop()).map(this::derive).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void addBones(Collection<? extends Derivable> bones) {
        inner.addBones(bones);
    }

    @Override
    public int staticDerivationLength() {
        if (maxDerivationLength < 0) {
            maxDerivationLength = inner.staticDerivationLength() + 1;
        }
        return maxDerivationLength;
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitReflexiveClosure(this, data, context);
    }

    @Override
    public void backtrackTo(int time) { }
    @Override
    public void repopulate() { }

    @Override
    public Stream<Edge> edgeStream() {
        return IntStream.range(0, domain.size())
                .mapToObj(e -> edgeStream(e, EdgeDirection.OUTGOING))
                .flatMap(s -> s);
    }

    @Override
    public Stream<Edge> edgeStream(int e, EdgeDirection dir) {
        return Stream.concat(
                Stream.of(new Edge(e, e)),
                inner.edgeStream(e, dir).filter(edge -> !edge.isLoop())
        );
    }

    @Override
    public Stream<Edge> weakEdgeStream() {
        return IntStream.range(0, domain.size())
                .mapToObj(e -> weakEdgeStream(e, EdgeDirection.OUTGOING))
                .flatMap(s -> s);
    }

    @Override
    public Stream<Edge> weakEdgeStream(int e, EdgeDirection dir) {
        return Stream.concat(
                Stream.of(new Edge(e, e)),
                inner.weakEdgeStream(e, dir).filter(edge -> !edge.isLoop())
        );
    }

}
