package com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.derived;


import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.misc.SccComplexity;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.MaterializedGraph;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RecursiveGraph extends MaterializedGraph {

    private RelationGraph inner;
    private int sccComplexity = -1;

    public void injectStaticComplexity(int sccComplexity) {
        this.sccComplexity = sccComplexity;
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
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added) {
        assert(sccComplexity >= 0);
        if (changedSource == inner) {
            return ((Collection<Edge>) added).stream().map(edge -> edge.withComplexity(sccComplexity)).filter(simpleGraph::add).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitRecursiveGraph(this, data, context);
    }

    @Override
    public void repopulate() { }


}
