package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.constraints;

import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;

public abstract class AbstractConstraint implements Constraint {

    protected Domain<?> domain;

    @Override
    public void onDomainInit(CAATPredicate predicate, Domain<?> domain) {
        this.domain = domain;
    }

    @Override
    public void onPopulation(CAATPredicate predicate) {
        onChanged(predicate, predicate.setView());
    }

}
