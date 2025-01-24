package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain;

import java.util.Collection;

public interface Domain<T> {

    //int fullSize();
    int size();
    Collection<T> getElements();
    int getId(Object obj);
    T getObjectById(int id);
    default T weakGetObjectById(int id) { return getObjectById(id); }
}

