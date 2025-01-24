package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.constraints;

import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.misc.PredicateListener;
import com.dat3m.dartagnan.utils.dependable.Dependent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface Constraint extends PredicateListener, Dependent<CAATPredicate> {
    CAATPredicate getConstrainedPredicate();

    boolean checkForViolations();

    Collection<? extends Collection<? extends Derivable>> getViolations();

    boolean checkForNearlyViolations();

    // call only after getViolations()
    List<? extends Collection<? extends Derivable>> getUndershootViolations();

    // call only after getViolations()
    List<? extends Collection<? extends Derivable>> getOvershootEdges();

    @Override
    default Collection<CAATPredicate> getDependencies() { return Collections.singletonList(getConstrainedPredicate()); }

    default void processActiveGraph(Set<Derivable> derivables) {}
}
