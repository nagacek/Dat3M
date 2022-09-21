package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

public class EdgeManager {
    private final TupleSetMap edges;
    //private EdgeSetMap caatEdges;
    private ExecutionModel model;

    public EdgeManager() {
        edges = new TupleSetMap();
    }

    public void setExecutionModel(ExecutionModel model) {
        this.model = model;
    }

    public TupleSetMap addEagerlyEncodedEdges(TupleSetMap newEdges) {
        if (newEdges == null) {
            return new TupleSetMap();
        }
        TupleSetMap distinctEdges = newEdges.difference(edges);
        edges.merge(newEdges);
        return distinctEdges;
    }

    public TupleSetMap substractFrom(TupleSetMap other) {
        return other.difference(edges);
    }

    public EdgeSetMap initCAATView() {
        return EdgeSetMap.fromTupleSetMap(edges, model);
    }

    public boolean isEagerlyEncoded(String name, Tuple edge) {
        return edges.contains(name, edge);
    }

    public boolean isEagerlyEncoded(String name) {
        return edges.contains(name);
    }
}
