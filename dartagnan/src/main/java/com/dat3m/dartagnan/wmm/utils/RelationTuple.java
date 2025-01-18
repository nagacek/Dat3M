package com.dat3m.dartagnan.wmm.utils;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.wmm.Relation;

public record RelationTuple(Relation rel, Tuple tuple, boolean neg) {
    public RelationTuple(Relation rel, Tuple tuple) { this(rel, tuple, false); }
    public RelationTuple(Relation rel, Event e1, Event e2) { this (rel, new Tuple(e1, e2)); }

    public Event first() { return tuple.first(); }
    public Event second() { return tuple.second(); }
}
