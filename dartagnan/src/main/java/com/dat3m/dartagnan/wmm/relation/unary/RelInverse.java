package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.collect.Sets;

/**
 *
 * @author Florian Furbach
 */
public class RelInverse extends UnaryRelation {
    //TODO/Note: We can forward getSMTVar calls
    // to avoid encoding this completely!

    public static String makeTerm(Relation r1){
        return r1.getName() + "^-1";
    }

    public RelInverse(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    public RelInverse(Relation r1, String name) {
        super(r1, name);
        term = makeTerm(r1);
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitInverse(this, r1);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = r1.getMinTupleSet().inverse();
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = r1.getMaxTupleSet().inverse();
        }
        return maxTupleSet;
    }

    @Override
    public TupleSetMap addEncodeTupleSet(TupleSet tuples){
        TupleSet activeSet = new TupleSet(Sets.intersection(Sets.difference(tuples, encodeTupleSet), maxTupleSet));
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        encodeTupleSet.addAll(activeSet);
        TupleSet difference = new TupleSet(Sets.difference(encodeTupleSet, oldEncodeSet));
        TupleSetMap map = new TupleSetMap(this, difference);
        activeSet.removeAll(getMinTupleSet());

        if(!activeSet.isEmpty()){
            map.merge(r1.addEncodeTupleSet(activeSet.inverse()));
        }

        return map;
    }

    @Override
    protected BooleanFormula encodeApprox(SolverContext ctx) {
        return encodeApprox(ctx, encodeTupleSet);
    }

    @Override
    public BooleanFormula encodeApprox(SolverContext ctx, TupleSet toEncode) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = bmgr.makeTrue();


        TupleSet minSet = getMinTupleSet();
        for(Tuple tuple : toEncode){
            BooleanFormula opt = minSet.contains(tuple) ? getExecPair(tuple, ctx) : r1.getSMTVar(tuple.getInverse(), ctx);
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), opt));
        }
        return enc;
    }
}