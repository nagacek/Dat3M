package com.dat3m.dartagnan.wmm.relation.base.stat;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

//TODO: This relation may contain non-visible events. Is this reasonable?
public class RelSetIdentity extends StaticRelation {

    protected FilterAbstract filter;

    public static String makeTerm(FilterAbstract filter){
        return "[" + filter + "]";
    }

    public RelSetIdentity(FilterAbstract filter) {
        this.filter = filter;
        term = makeTerm(filter);
    }

    public FilterAbstract getFilter() {
        return filter;
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet maxTupleSet = new TupleSet();
        for(Event e : ra.getTask().getProgram().getCache().getEvents(filter)){
            maxTupleSet.add(new Tuple(e, e));
        }
        buf.send(this,maxTupleSet,maxTupleSet);
    }
}
