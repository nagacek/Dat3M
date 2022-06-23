package com.dat3m.dartagnan.wmm.relation.base.stat;

import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;

public class RelEmpty extends StaticRelation {

    public RelEmpty(String name) {
        setName(name);
        term = name;
    }

    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
    }
}
