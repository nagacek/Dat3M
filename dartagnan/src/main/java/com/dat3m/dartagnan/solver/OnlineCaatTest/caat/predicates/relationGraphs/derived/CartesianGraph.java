package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.derived;

import com.dat3m.dartagnan.solver.OnlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.AbstractPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.sets.SetPredicate;

import java.util.*;
import java.util.stream.Stream;

public class CartesianGraph extends AbstractPredicate implements RelationGraph {

    private final SetPredicate first;
    private final SetPredicate second;

    public CartesianGraph(SetPredicate first, SetPredicate second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void validate (int time, Set<Derivable> activeSet, boolean active) {}

    @Override
    public List<SetPredicate> getDependencies() {
        return Arrays.asList(first, second);
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData tData, TContext context) {
        return visitor.visitCartesian(this, tData, context);
    }

    @Override
    public void repopulate() { }
    @Override
    public void backtrackTo(int time) { }

    @Override
    public Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) { return new HashSet<>(); }

    private Edge derive(Element a, Element b) {
        return new Edge(a.getId(), b.getId(), Math.max(a.getTime(), b.getTime()),
                Math.max(a.getDerivationLength(), b.getDerivationLength()) + 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
        if (mode == PredicateHierarchy.PropagationMode.DELETE) {
            return Collections.emptyList();
        }
        List<Edge> addedEdges = new ArrayList<>();
        Collection<Element> addedElems = (Collection<Element>) added;
        if (changedSource == first) {
            for (Element a : addedElems) {
                for (Element b : second.elements()) {
                    addedEdges.add(derive(a, b));
                }
            }
        } else if (changedSource == second) {
            for (Element b : addedElems) {
                for (Element a : first.elements()) {
                    addedEdges.add(derive(a, b));
                }
            }
        }
        return addedEdges;
    }

    @Override
    public int staticDerivationLength() {
        if (maxDerivationLength < 0) {
            maxDerivationLength = Math.max(first.staticDerivationLength(), second.staticDerivationLength()) + 1;
        }
        return maxDerivationLength;
    }

    @Override
    public void addBones(Collection<? extends Derivable> bones) { }

    @Override
    public Edge get(Edge edge) {
        Element a = first.getById(edge.getFirst());
        Element b = second.getById(edge.getSecond());
        return (a != null && b != null) ? derive(a, b) : null;
    }

    @Override
    public Edge weakGet(Edge edge) { return get(edge); }

    @Override
    public boolean contains(Edge edge) {
        return containsById(edge.getFirst(), edge.getSecond());
    }

    @Override
    public boolean containsById(int id1, int id2) {
        return first.containsById(id1) && second.containsById(id2);
    }

    @Override
    public int size() { return first.size() * second.size(); }
    @Override
    public int getMinSize() { return first.getMinSize() * second.getMinSize(); }
    @Override
    public int getMaxSize() { return first.getMaxSize() * second.getMaxSize(); }

    @Override
    public int size(int e, EdgeDirection dir) {
        if (dir == EdgeDirection.INGOING) {
            return first.containsById(e) ? second.size() : 0;
        } else {
            return second.containsById(e) ? first.size() : 0;
        }
    }

    @Override
    public int getMinSize(int e, EdgeDirection dir) {
        if (dir == EdgeDirection.INGOING) {
            return first.containsById(e) ? second.getMinSize() : 0;
        } else {
            return second.containsById(e) ? first.getMinSize() : 0;
        }
    }

    @Override
    public int getMaxSize(int e, EdgeDirection dir) {
        if (dir == EdgeDirection.INGOING) {
            return first.containsById(e) ? second.getMaxSize() : 0;
        } else {
            return second.containsById(e) ? first.getMaxSize() : 0;
        }
    }


    @Override
    public Stream<Edge> edgeStream() {
        return first.elementStream().flatMap(a -> second.elementStream().map(b -> derive(a, b)));
    }

    @Override
    public Stream<Edge> weakEdgeStream() {
        return edgeStream();
    }

    @Override
    public Stream<Edge> edgeStream(int e, EdgeDirection dir) {
        if (dir == EdgeDirection.INGOING) {
            Element a = first.getById(e);
            return a != null ? second.elementStream().map(b -> derive(a, b)) : Stream.empty();
        } else {
            Element b = second.getById(e);
            return b != null ? first.elementStream().map(a -> derive(a, b)) : Stream.empty();
        }
    }

    @Override
    public Stream<Edge> weakEdgeStream(int e, EdgeDirection dir) {
        return edgeStream(e, dir);
    }
}
