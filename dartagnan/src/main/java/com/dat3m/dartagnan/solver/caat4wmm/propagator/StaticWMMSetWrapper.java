package com.dat3m.dartagnan.solver.caat4wmm.propagator;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.basePredicates.AbstractWMMPredicate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class StaticWMMSetWrapper extends AbstractWMMPredicate implements SetPredicate {
    private final Filter filter;
    private final EventDomain conversionDomain;

    public StaticWMMSetWrapper(Filter filter, EventDomain conversionDomain) {
        this.filter = filter;
        this.conversionDomain = conversionDomain;
        setName(filter.toString());
    }


    @Override
    public Collection<Element> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added) {
        return List.of();
    }

    @Override
    public void backtrackTo(int time) {

    }

    @Override
    public List<SetPredicate> getDependencies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData tData, TContext tContext) {
        return null;
    }

    @Override
    public void repopulate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Element get(Element e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Element> elementStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsById(int id) {
        return filter.apply(conversionDomain.getObjectById(id).getEvent());
    }
}
