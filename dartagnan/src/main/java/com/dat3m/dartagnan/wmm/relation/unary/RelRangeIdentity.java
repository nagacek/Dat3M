package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.collect.Sets;

public class RelRangeIdentity extends UnaryRelation {

    public static String makeTerm(Relation r1){
        return "[range(" + r1.getName() + ")]";
    }

    public RelRangeIdentity(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    public RelRangeIdentity(Relation r1, String name) {
        super(r1, name);
        term = makeTerm(r1);
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitRangeIdentity(this, r1);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
            minTupleSet = new TupleSet();
            r1.getMinTupleSet().stream()
                    .filter(t -> exec.isImplied(t.getSecond(), t.getFirst()))
                    .map(t -> new Tuple(t.getSecond(), t.getSecond()))
                    .forEach(minTupleSet::add);
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = r1.getMaxTupleSet().mapped(t -> new Tuple(t.getSecond(), t.getSecond()));
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

        //TODO: Optimize using minSets (but no CAT uses this anyway)
        if(!activeSet.isEmpty()){
            TupleSet r1Set = new TupleSet();
            for(Tuple tuple : activeSet){
                r1Set.addAll(r1.getMaxTupleSet().getBySecond(tuple.getFirst()));
            }
            map.merge(r1.addEncodeTupleSet(r1Set));
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

        //TODO: Optimize using minSets (but no CAT uses this anyway)
        for(Tuple tuple1 : toEncode){
            Event e = tuple1.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            for(Tuple tuple2 : r1.getMaxTupleSet().getBySecond(e)){
                opt = bmgr.or(r1.getSMTVar(tuple2.getFirst(), e, ctx));
            }
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(e, e, ctx), opt));
        }
        return enc;
    }
}