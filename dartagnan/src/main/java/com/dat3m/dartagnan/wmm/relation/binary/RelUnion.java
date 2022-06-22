package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.intersection;

/**
 *
 * @author Florian Furbach
 */
public class RelUnion extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + "+" + r2.getName() + ")";
    }

    public RelUnion(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = new TupleSet(Sets.union(r1.getMinTupleSet(), r2.getMinTupleSet()));
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet(Sets.union(r1.getMaxTupleSet(), r2.getMaxTupleSet()));
        }
        return maxTupleSet;
    }

    @Override
    public TupleSet getMinTupleSetRecursive(){
        if(recursiveGroupId > 0 && minTupleSet != null){
            minTupleSet.addAll(Sets.union(r1.getMinTupleSetRecursive(), r2.getMinTupleSetRecursive()));
            return minTupleSet;
        }
        return getMinTupleSet();
    }

    @Override
    public TupleSet getMaxTupleSetRecursive(){
        if(recursiveGroupId > 0 && maxTupleSet != null){
            maxTupleSet.addAll(Sets.union(r1.getMaxTupleSetRecursive(), r2.getMaxTupleSetRecursive()));
            return maxTupleSet;
        }
        return getMaxTupleSet();
    }

    @Override
    public Map<Relation, Set<Tuple>> activate(Set<Tuple> news) {
        return Map.of(
            r1, intersection(news, r1.getMaxTupleSet()),
            r2, intersection(news, r2.getMaxTupleSet()));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        for(Tuple tuple : encodeTupleSet){
            BooleanFormula opt1 = max1.contains(tuple) ? r1.getSMTVar(tuple, ctx) : bmgr.makeFalse();
            BooleanFormula opt2 = max2.contains(tuple) ? r2.getSMTVar(tuple, ctx) : bmgr.makeFalse();
            if (Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(bmgr.or(opt1, opt2), this.getSMTVar(tuple, ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), bmgr.or(opt1, opt2)));
            }
        }
        return enc;
    }
}