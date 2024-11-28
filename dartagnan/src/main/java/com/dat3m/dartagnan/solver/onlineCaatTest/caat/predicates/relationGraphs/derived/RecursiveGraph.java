package com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.derived;


import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.MaterializedGraph;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.RelationGraph;

import java.util.*;
import java.util.stream.Collectors;

public class RecursiveGraph extends MaterializedGraph {

    private RelationGraph inner;

    @Override
    protected Set<Edge> computeFromInnerEdges() {
        HashSet<Edge> innerEdges = new HashSet<>();
        for (Edge e : inner.edges()) {
            innerEdges.add(e);
        }
        return innerEdges;
    }

    @Override
    public List<RelationGraph> getDependencies() {
        return Collections.singletonList(inner);
    }

    public void setConcreteGraph(RelationGraph concreteGraph) {
        this.inner = concreteGraph;
    }

    public RelationGraph getInner() { return inner; }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
         if (changedSource == inner
            || changedSource == null && (mode == PredicateHierarchy.PropagationMode.DEFER | mode == PredicateHierarchy.PropagationMode.DELETE)) {
            return ((Collection<Edge>) added).stream().filter(simpleGraph::add).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public int staticDerivationLength() {
        if (maxDerivationLength < 0) {
            maxDerivationLength = inner.staticDerivationLength();
        }
        return maxDerivationLength;
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitRecursiveGraph(this, data, context);
    }

    @Override
    public void repopulate() { }


}
