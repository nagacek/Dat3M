package com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs;

import com.dat3m.dartagnan.solver.onlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.misc.AbstractPredicateSetView;
import com.dat3m.dartagnan.utils.collections.OneTimeIterable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RelationGraph extends CAATPredicate {

    @Override
    Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode);

    void addBones(Collection<? extends Derivable> bones);

    Edge get(Edge edge);

    Edge weakGet(Edge edge);

    int size(int e, EdgeDirection dir);

    Stream<Edge> edgeStream();
    Stream<Edge> edgeStream(int e, EdgeDirection dir);

    Stream<Edge> weakEdgeStream();
    Stream<Edge> weakEdgeStream(int e, EdgeDirection dir);


    // ================= Default methods ==================


    default boolean contains(Edge edge) { return get(edge) != null; }
    default boolean containsById(int id1, int id2) { return contains(getById(id1, id2)); }
    default Edge getById(int id1, int id2) { return get(new Edge(id1, id2)); }

    default int getMinSize(int e, EdgeDirection dir) { return size(e, dir); }
    default int getMaxSize(int e, EdgeDirection dir) { return size(e, dir); }
    default int getEstimatedSize(int e, EdgeDirection dir) {
        return (getMinSize(e, dir) + getMaxSize(e, dir)) >> 1;
    }

    @Override
    default Edge get(Derivable value) {
        return (value instanceof Edge edge) ? get(edge) : null;
    }
    @Override
    default Stream<Edge> valueStream() { return edgeStream(); }
    @Override
    default Iterator<Edge> valueIterator() { return edgeIterator(); }
    @Override
    default Iterable<Edge> values() { return edges(); }


    default Stream<Edge> outEdgeStream(int e) {
        return edgeStream(e, EdgeDirection.OUTGOING);
    }
    default Stream<Edge> inEdgeStream(int e) {
        return edgeStream(e, EdgeDirection.INGOING);
    }

    default Iterator<Edge> edgeIterator() {
        return edgeStream().iterator();
    }
    default Iterator<Edge> weakEdgeIterator() {
        return weakEdgeStream().iterator();
    }
    default Iterator<Edge> edgeIterator(int e, EdgeDirection dir) {
        return edgeStream(e, dir).iterator();
    }
    default Iterator<Edge> weakEdgeIterator(int e, EdgeDirection dir) {
        return weakEdgeStream(e, dir).iterator();
    }

    default Iterable<Edge> edges() { return OneTimeIterable.create(edgeIterator()); }
    default Iterable<Edge> weakEdges() { return OneTimeIterable.create(weakEdgeIterator()); }
    default Iterable<Edge> edges(int e, EdgeDirection dir)
    { return OneTimeIterable.create(edgeIterator(e, dir)); }
    default Iterable<Edge> weakEdges(int e, EdgeDirection dir)
    { return OneTimeIterable.create(weakEdgeIterator(e, dir)); }

    default Iterator<Edge> inEdgeIterator(int e) { return edgeIterator(e, EdgeDirection.INGOING); }
    default Iterator<Edge> outEdgeIterator(int e) { return edgeIterator(e, EdgeDirection.OUTGOING); }

    default Iterable<Edge> inEdges(int e) { return edges(e, EdgeDirection.INGOING); }
    default Iterable<Edge> outEdges(int e) {  return edges(e, EdgeDirection.OUTGOING); }

    @Override
    default Set<Edge> setView() { return new SetView(this); }

    default Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) {
        return bones.stream().map(b -> b.edge().withTime(time)).collect(Collectors.toSet());
    }

    class SetView extends AbstractPredicateSetView<Edge> {

        private final RelationGraph graph;
        private SetView(RelationGraph graph) {
            this.graph = graph;
        }

        @Override
        public RelationGraph getPredicate() { return graph; }

        @Override
        public Stream<Edge> stream() { return graph.edgeStream(); }

        @Override
        public Iterator<Edge> iterator() { return graph.edgeIterator(); }
    }

}
