package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.derived;


import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.MaterializedGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;

// A materialized Intersection Graph.
// This seems to be more efficient than the virtualized IntersectionGraph we used before.
public class IntersectionGraph extends MaterializedGraph {

    private final RelationGraph[] operands;

    @Override
    protected Set<Edge> computeFromInnerEdges() {
        HashSet<Edge> innerEdges = new HashSet<>();
        RelationGraph smallest = operands[0];
        for (RelationGraph gr : operands) {
            if (gr.size() < smallest.size()) {
                smallest = gr;
            }
        }
        for (Edge e : smallest.edges()) {
            Edge derived = derive(e, Arrays.stream(operands).toList());
            if (derived != null) {
                innerEdges.add(derived);
            }
        }
        return innerEdges;
    }

    @Override
    public List<RelationGraph> getDependencies() {
        return Arrays.asList(operands);
    }

    public IntersectionGraph(RelationGraph... o) {
        operands = o;
    }

    @Override
    public void repopulate() {
        RelationGraph first = Stream.of(operands).min(comparingInt(RelationGraph::getEstimatedSize)).orElseThrow();
        List<RelationGraph> others = Stream.of(operands).filter(x -> first != x).toList();
        for (Edge e1 : first.edges()) {
            Edge e = derive(e1, others);
            if (e != null) {
                simpleGraph.add(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
        Collection<Edge> addedEdges = (Collection<Edge>)added;
        List<Edge> newlyAdded = new ArrayList<>();
        if (changedSource == null && (mode == PredicateHierarchy.PropagationMode.DEFER | mode == PredicateHierarchy.PropagationMode.DELETE)) {
            for (Edge e : addedEdges) {
                if (simpleGraph.add(e)) {
                    newlyAdded.add(e);
                }
            }
        } else if (Stream.of(operands).anyMatch(g -> changedSource == g)) {
            List<RelationGraph> others = Stream.of(operands).filter(g -> g != changedSource).toList();

            for (Edge e1 : addedEdges) {
                Edge e = derive(e1, others);
                if (e != null) {
                    simpleGraph.add(e);
                    newlyAdded.add(e);
                }
            }
        } else {
            return Collections.emptyList();
        }
        return newlyAdded;
    }

    @Override
    public int staticDerivationLength() {
        if (maxDerivationLength < 0) {
            for (RelationGraph o : operands) {
                maxDerivationLength = Math.max(o.staticDerivationLength(), maxDerivationLength);
            }
        }
        return maxDerivationLength;
    }



    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitGraphIntersection(this, data, context);
    }

    // Note: The derived edge has the timestamp of edge
    private Edge derive(Edge edge, List<RelationGraph> operands) {
        int time = edge.getTime();
        int length = edge.getDerivationLength();
        for (RelationGraph g : operands) {
            Edge e;
            if (edge.isBone() && !edge.isActive()) {
                e = g.weakGet(edge);
            } else {
                e = g.get(edge);
            }
            if (e == null) {
                return null;
            }
            time = Math.max(time, e.getTime());
            length = Math.max(length, e.getDerivationLength());
        }
        return edge.with(time, length);
    }
}