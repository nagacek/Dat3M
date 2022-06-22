package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Map;
import java.util.Set;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;

/**
 *
 * @author Florian Furbach
 */
public class RelMinus extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + "\\" + r2.getName() + ")";
    }

    public RelMinus(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public void initializeEncoding(SolverContext ctx){
        super.initializeEncoding(ctx);
        if(r2.getRecursiveGroupId() > 0){
            throw new RuntimeException("Relation " + r2.getName() + " cannot be recursive since it occurs in a set minus.");
        }
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = new TupleSet(difference(r1.getMinTupleSet(), r2.getMaxTupleSet()));
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet(difference(r1.getMaxTupleSet(), r2.getMinTupleSet()));
            r2.getMaxTupleSet();
        }
        return maxTupleSet;
    }

    @Override
    public TupleSet getMinTupleSetRecursive(){
        if(recursiveGroupId > 0 && minTupleSet != null){
            minTupleSet.addAll(difference(r1.getMinTupleSetRecursive(), r2.getMaxTupleSetRecursive()));
            return minTupleSet;
        }
        return getMinTupleSet();
    }

    @Override
    public TupleSet getMaxTupleSetRecursive(){
        if(recursiveGroupId > 0 && maxTupleSet != null){
            maxTupleSet.addAll(difference(r1.getMaxTupleSetRecursive(), r2.getMinTupleSetRecursive()));
            return maxTupleSet;
        }
        return getMaxTupleSet();
    }

    @Override
    public Map<Relation, Set<Tuple>> activate(Set<Tuple> news) {
        return Map.of(
            r1, difference(news, r1.getMinTupleSet()),
            r2, intersection(news, r2.getMaxTupleSet()));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
        ExecutionAnalysis exec = encoder.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet max2 = ra.getMaxTupleSet(r2);
        TupleSet min1 = ra.getMinTupleSet(r1);
        for(Tuple tuple : encodeTupleSet){
            BooleanFormula opt1 = min1.contains(tuple) ? execution(tuple.getFirst(), tuple.getSecond(), exec, ctx) : r1.getSMTVar(tuple, ctx);
            BooleanFormula opt2 = max2.contains(tuple) ? bmgr.not(r2.getSMTVar(tuple, ctx)) : bmgr.makeTrue();
            if (Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(bmgr.and(opt1, opt2), this.getSMTVar(tuple, ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), bmgr.and(opt1, opt2)));
            }
        }
        return enc;
    }
}
