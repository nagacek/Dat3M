package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Florian Furbach
 */
public abstract class BinaryRelation extends Relation {

    protected Relation r1;
    protected Relation r2;

    BinaryRelation(Relation r1, Relation r2) {
        this.r1 = r1;
        this.r2 = r2;
    }

    BinaryRelation(Relation r1, Relation r2, String name) {
        super(name);
        this.r1 = r1;
        this.r2 = r2;
    }

    public Relation getFirst() {
        return r1;
    }

    public Relation getSecond() {
        return r2;
    }

    @Override
    public List<Relation> getDependencies() {
        return Arrays.asList(r1 ,r2);
    }

    @Override
    public TupleSetMap addEncodeTupleSet(TupleSet tuples){ // Not valid for composition
        if (getName().equals("po-loc")) {
            System.out.println("Here!");
        }
        TupleSet activeSet = new TupleSet(Sets.intersection(Sets.difference(tuples, encodeTupleSet), maxTupleSet));
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        encodeTupleSet.addAll(activeSet);
        activeSet.removeAll(getMinTupleSet());

        TupleSet difference = new TupleSet(Sets.difference(encodeTupleSet, oldEncodeSet));
        TupleSetMap map = new TupleSetMap(this, difference);
        if(!activeSet.isEmpty()){
            map.merge(r1.addEncodeTupleSet(activeSet));
            map.merge(r2.addEncodeTupleSet(activeSet));
        }

        return map;
    }

}
