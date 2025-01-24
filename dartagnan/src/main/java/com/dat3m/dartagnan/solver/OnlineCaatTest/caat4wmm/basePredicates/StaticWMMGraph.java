package com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.basePredicates;

import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;

import java.util.stream.Stream;


public abstract class StaticWMMGraph extends AbstractWMMGraph {
    protected int size;

    @Override
    public Edge get(Edge edge) {
        return contains(edge) ? edge.with(0, 0) : null;
    }

    @Override
    public Edge weakGet(Edge edge) { return get(edge); }

    @Override
    public boolean contains(Edge edge) {
        return containsById(edge.getFirst(), edge.getSecond());
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int getEstimatedSize() { return size; }

    @Override
    public void backtrackTo(int time) { }

    @Override
    public Stream<Edge> weakEdgeStream() {
        return edgeStream();
    }

    @Override
    public Stream<Edge> weakEdgeStream(int e, EdgeDirection dir) {
        return edgeStream(e, dir);
    }

    @Override
    public void initializeToDomain(Domain<?> domain) {
        super.initializeToDomain(domain);
        size = 0;
    }

    @Override
    public void validate(int time){}
    protected final void autoComputeSize() {
        size = 0;
        for (int i = 0; i < domain.size(); i++) {
            size += size(i, EdgeDirection.OUTGOING);
        }
    }

}
