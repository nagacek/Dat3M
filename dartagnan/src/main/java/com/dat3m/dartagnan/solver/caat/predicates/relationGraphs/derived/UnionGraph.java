package com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.derived;

import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.MaterializedGraph;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;

import java.util.*;
import java.util.function.Function;

// A materialized Union Graph.
// This seems to be more efficient than the virtualized UnionGraph we used before.
public class UnionGraph extends MaterializedGraph {

    private final RelationGraph first;
    private final RelationGraph second;

    @Override
    public List<RelationGraph> getDependencies() {
        return Arrays.asList(first, second);
    }

    public RelationGraph getFirst() { return first; }
    public RelationGraph getSecond() { return second; }

    public UnionGraph(RelationGraph first, RelationGraph second) {
        this.first = first;
        this.second = second;
    }

    private Edge derive(Edge e) {
        return e.withDerivationLength(e.getDerivationLength() + 1);
    }

    @Override
    public void repopulate() {
        HashMap<Edge, Integer> smallestComplexity = new HashMap<>();
        for (Edge e : first.edges()) {
            insertSmallestUniquely(derive(e), smallestComplexity);
        }
        for (Edge e : second.edges()) {
            insertSmallestUniquely(derive(e), smallestComplexity);
        }
        simpleGraph.addAll(smallestComplexity.keySet());
    }


    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added) {
        if (changedSource == first || changedSource == second) {
            HashMap<Edge, Integer> smallestComplexityNew = new HashMap<>();
            Collection<Edge> addedEdges = (Collection<Edge>)added;
            for (Edge e : addedEdges) {
                Edge edge = derive(e);
                if (!simpleGraph.contains(edge)) {
                    insertSmallestUniquely(edge, smallestComplexityNew);
                }
            }
            return smallestComplexityNew.keySet();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitGraphUnion(this, data, context);
    }

    private void insertSmallestUniquely(Edge e, Map<Edge, Integer> measure) {
        if (measure.putIfAbsent(e, e.getComplexity()) != null) {
            int inserted = measure.get(e);
            int current = e.getComplexity();
            if (current < inserted) {
                // equality on edges is only defined by their ids
                measure.remove(e);
                measure.put(e, current);
            }
        }
    }


}