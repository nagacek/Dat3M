package com.dat3m.dartagnan.solver.caat.reasoning;

import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;

import java.util.Set;

public class Context {
    private final EdgeSetMap map;
    private final Set<String> set;

    public Context(Set<String> set, EdgeSetMap map) {
        this.map = map;
        this.set = set;
    }

    public boolean isCovered(String name, Edge edge) {
        if (map.contains(name, edge) || set.contains(name)) {
            return true;
        }
        return false;
    }

    public void chooseRelation(String name) {
        set.add(name);
    }
}
