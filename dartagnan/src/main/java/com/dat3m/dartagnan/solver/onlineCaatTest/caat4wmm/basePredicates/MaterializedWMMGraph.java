package com.dat3m.dartagnan.solver.onlineCaatTest.caat4wmm.basePredicates;

import com.dat3m.dartagnan.solver.onlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.base.SimpleGraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/*
    As opposed to a "virtualized graph", a materialized graph actually stores its nodes/edges explicitly.
    MaterializedGraph simply encapsulates a SimpleGraph and delegates all its methods
    to the underlying SimpleGraph (similar to a forwarding decorator).
 */
public abstract class MaterializedWMMGraph extends AbstractWMMGraph {

    protected final SimpleGraph simpleGraph;

    protected MaterializedWMMGraph() {
        this.simpleGraph = new SimpleGraph();
    }

    @Override
    public Edge get(Edge edge) {
        return simpleGraph.get(edge);
    }

    @Override
    public Edge weakGet(Edge edge) { return simpleGraph.weakGet(edge); }

    // TODO: is this useful? they are static edges, aren't they?
    @Override
    public void addBones(Collection<? extends Derivable> bones) {
        simpleGraph.addBones(bones);
    }

    @Override
    public boolean containsById(int a, int b) {
        return simpleGraph.containsById(a, b);
    }

    @Override
    public boolean contains(Edge edge) {
        return simpleGraph.contains(edge);
    }

    @Override
    public void backtrackTo(int time) {
        simpleGraph.backtrackTo(time);
    }

    @Override
    public Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) {
        return simpleGraph.checkBoneActivation(triggerId, time, bones);
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
    public int size(int e, EdgeDirection dir) { return simpleGraph.size(e, dir); }

    @Override
    public boolean isEmpty() {
        return simpleGraph.isEmpty();
    }

}
