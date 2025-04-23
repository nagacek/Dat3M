package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.Domain;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;


public class EventDomainWrapper extends EventDomain {
    private Domain<Event> domain;

    public EventDomainWrapper(ExecutionModel model) {
        super(model);
    }

    public void initializeToDomain(Domain<Event> domain) {
        this.domain = domain;
    }

    @Override
    public int size() {
        return domain.size();
    }

    @Override
    public int getId(Object obj) {
        if (obj instanceof EventData evData) {
            return domain.getId(evData.getEvent());
        } else {
            throw new IllegalArgumentException(obj + " is not of type EventData");
        }
    }

    @Override
    public EventData getObjectById(int id) {
        return new EventData(domain.getObjectById(id));
    }
}
