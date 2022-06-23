package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

/**
 *
 * @author Florian Furbach
 */
public class RelTransRef extends RelTrans {

    public static String makeTerm(Relation r1){
        return r1.getName() + "^*";
    }

    public RelTransRef(Relation r1) {
        super(r1);
        term = makeTerm(r1);
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        super.initialize(ra,buf,obs);
        TupleSet maxTupleSet = new TupleSet();
        for(Event e : ra.getTask().getProgram().getCache().getEvents(FilterBasic.get(Tag.VISIBLE))){
            maxTupleSet.add(new Tuple(e, e));
        }
        buf.send(this,maxTupleSet,maxTupleSet);
    }
}