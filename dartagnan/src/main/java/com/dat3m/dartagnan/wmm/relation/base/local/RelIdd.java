package com.dat3m.dartagnan.wmm.relation.base.local;

import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegReaderData;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import java.util.Collection;

import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.IDD;

public class RelIdd extends BasicRegRelation {

    public RelIdd(){
        term = IDD;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitInternalDataDependency(encodeTupleSet, this);
    }
    @Override
    public <T> T accept(Visitor<? extends T> v, TupleSet toEncode) {
        return v.visitInternalDataDependency(toEncode, this);
    }

    @Override
    protected Collection<Event> getEvents() {
        return task.getProgram().getCache().getEvents(FilterBasic.get(Tag.REG_READER));
    }

    @Override
    Collection<Register> getRegisters(Event regReader){
        return ((RegReaderData) regReader).getDataRegs();
    }
}
