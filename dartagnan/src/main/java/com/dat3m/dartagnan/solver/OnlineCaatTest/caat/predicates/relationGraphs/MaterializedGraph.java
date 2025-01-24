package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs;

import com.dat3m.dartagnan.solver.OnlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.AbstractPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.base.SimpleGraph;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/*
    As opposed to a "virtualized graph", a materialized graph actually stores its nodes/edges explicitly.
    MaterializedGraph simply encapsulates a SimpleGraph and delegates all its methods
    to the underlying SimpleGraph (similar to a forwarding decorator).
 */
public abstract class MaterializedGraph extends AbstractPredicate implements RelationGraph {

    protected final SimpleGraph simpleGraph;

    protected MaterializedGraph() {
        this.simpleGraph = new SimpleGraph();
    }

    // ----------------- Debug only ------------------

    @Override
    public void validate(int time, Set<Derivable> activeSet, boolean active) {
        AtomicLong activeCount = new AtomicLong();
        AtomicLong inactiveCount = new AtomicLong();
        Set<Edge> innerEdges = computeFromInnerEdges();
        Set<Edge> wrongEdges = new HashSet<>();
        Set<Edge> rightEdges = new HashSet<>();
        edgeStream().forEach(e -> {
            if (!innerEdges.contains(e)) {
                wrongEdges.add(e);
            } else {
                rightEdges.add(e);
            }
            if (activeSet.contains(e)) {
                activeCount.getAndIncrement();
            } else {
                inactiveCount.getAndIncrement();
            }
        });
        if (active && (activeCount.get() != 0 || inactiveCount.get() != 0)) {
            System.out.println(this.getName() + "(active / inactive): " + activeCount.get() + " / " + inactiveCount.get());
        }
        Set<Edge> missingEdges = new HashSet<>(innerEdges);
        missingEdges.removeAll(rightEdges);

        if (!wrongEdges.isEmpty()) {
            // Flaw in must analysis?
            // Edge (x, y) is static in relation rel := [a | (b;c)] and comes from (b;c) where it is conditionally
            // static when event z is executed (e.g. (x, z) and (z, y) are static in b and c, respectively)
            //
            // Now, when x and y are executed, (x, y) is set as active in rel even though z is not executed yet
            //
            // Are conditions for static edges not respected in must analysis?
            int i = 5;
        }

        if (!missingEdges.isEmpty() && !Sets.intersection(missingEdges, activeSet).isEmpty()) {
            int i = 5;
        }

        //assert wrongEdges.isEmpty() && missingEdges.isEmpty();
    }

    protected abstract Set<Edge> computeFromInnerEdges();

    // -----------------------------------------------

    @Override
    public Edge get(Edge edge) {
        return simpleGraph.get(edge);
    }

    @Override
    public Edge weakGet(Edge edge) { return simpleGraph.weakGet(edge); }

    @Override
    public boolean containsById(int a, int b) {
        return simpleGraph.containsById(a, b);
    }

    @Override
    public boolean contains(Edge edge) {
        return simpleGraph.contains(edge);
    }

    @Override
    public void addBones(Collection<? extends Derivable> bones) {
        simpleGraph.addBones(bones);
    }

    @Override
    public void backtrackTo(int time) {
        simpleGraph.backtrackTo(time);
    }

    @Override
    public void initializeToDomain(Domain<?> domain) {
        super.initializeToDomain(domain);
        simpleGraph.initializeToDomain(domain);
    }

    @Override
    public int getEstimatedSize() {
        return simpleGraph.getEstimatedSize();
    }

    @Override
    public int getEstimatedSize(int e, EdgeDirection dir) {
        return simpleGraph.getEstimatedSize(e, dir);
    }

    @Override
    public int getMinSize() {
        return simpleGraph.getMinSize();
    }

    @Override
    public int getMinSize(int e, EdgeDirection dir) {
        return simpleGraph.getMinSize(e, dir);
    }

    @Override
    public int getMaxSize() {
        return simpleGraph.getMaxSize();
    }

    @Override
    public int getMaxSize(int e, EdgeDirection dir) {
        return simpleGraph.getMaxSize(e, dir);
    }

    @Override
    public Stream<Edge> edgeStream() {
        return simpleGraph.edgeStream();
    }

    @Override
    public Stream<Edge> weakEdgeStream() {
        return simpleGraph.weakEdgeStream();
    }

    @Override
    public Stream<Edge> edgeStream(int e, EdgeDirection dir) {
        return simpleGraph.edgeStream(e, dir);
    }

    @Override
    public Stream<Edge> weakEdgeStream(int e, EdgeDirection dir) {
        return simpleGraph.weakEdgeStream(e, dir);
    }

    @Override
    public Iterator<Edge> edgeIterator() {
        return simpleGraph.edgeIterator();
    }

    @Override
    public Iterator<Edge> edgeIterator(int e, EdgeDirection dir) {
        return simpleGraph.edgeIterator(e, dir);
    }

    @Override
    public int size() {
        return simpleGraph.size();
    }

    @Override
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
        return simpleGraph.forwardPropagate(changedSource, added, mode);
    }

    public Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) {
        return simpleGraph.checkBoneActivation(triggerId, time, bones);
    }

    @Override
    public int size(int e, EdgeDirection dir) { return simpleGraph.size(e, dir); }

    @Override
    public boolean isEmpty() {
        return simpleGraph.isEmpty();
    }

}
