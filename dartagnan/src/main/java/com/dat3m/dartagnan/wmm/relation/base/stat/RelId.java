package com.dat3m.dartagnan.wmm.relation.base.stat;

import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.ID;

public class RelId extends StaticRelation {

    public RelId(){
        term = ID;
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet maxTupleSet = new TupleSet();
        for(Event e : ra.getTask().getProgram().getCache().getEvents(FilterBasic.get(Tag.VISIBLE))){
            maxTupleSet.add(new Tuple(e, e));
        }
        buf.send(this, maxTupleSet, maxTupleSet);
    }
}
