package com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.basePredicates;

import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;

import java.util.Set;

// Used for static relations that are not yet implemented explicitly
public class StaticDefaultWMMGraph extends MaterializedWMMGraph {
    private final Relation relation;
    private final RelationAnalysis ra;

    public StaticDefaultWMMGraph(Relation rel, RelationAnalysis relationAnalysis) {
        this.relation = rel;
        this.ra = relationAnalysis;
    }

    @Override
    public void validate (int time, Set<Derivable> activeSet, boolean active) {}

    @Override
    public void repopulate() {
        ra.getKnowledge(relation).getMaySet().apply((e1, e2) -> {
            EventData d1 = model.getData(e1).orElse(null);
            EventData d2 = model.getData(e2).orElse(null);
            if (d1 != null && d2 != null) {
                simpleGraph.add(new Edge(d1.getId(), d2.getId()));
            }
        });
    }

    @Override
    public int staticDerivationLength() {
        return 0;
    }
}
