package com.dat3m.dartagnan.solver.caat4wmm.propagator;

import com.dat3m.dartagnan.solver.caat.misc.MediumDenseIntegerSet;
import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.solver.caat4wmm.basePredicates.AbstractWMMPredicate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class ExecutedSetWrapper extends AbstractWMMPredicate implements SetPredicate {
    private final MediumDenseIntegerSet executedIds = new MediumDenseIntegerSet();

    public ExecutedSetWrapper(int elementNo) {
        executedIds.ensureCapacity(elementNo);
    }

    public void onPush() {
        executedIds.increaseLevel();
    }

    public void addElement(int id) {
        executedIds.add(id);
    }

    @Override
    public Collection<Element> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added) {
        return List.of();
    }

    @Override
    public void backtrackTo(int time) {
        executedIds.resetToLevel((short) time);
    }

    @Override
    public List<SetPredicate> getDependencies() {
        return List.of();
    }

    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData tData, TContext tContext) {
        return null;
    }

    @Override
    public void repopulate() {

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
        return executedIds.contains(id);
    }
}
