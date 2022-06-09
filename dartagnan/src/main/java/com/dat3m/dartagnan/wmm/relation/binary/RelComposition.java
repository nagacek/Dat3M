package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Furbach
 */
public class RelComposition extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + ";" + r2.getName() + ")";
    }

    public RelComposition(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
            minTupleSet = r1.getMinTupleSet().postComposition(r2.getMinTupleSet(),
                    (t1, t2) -> (exec.isImplied(t1.getFirst(), t1.getSecond())
                            || exec.isImplied(t2.getSecond(), t1.getSecond()))
                        && !exec.areMutuallyExclusive(t1.getFirst(), t2.getSecond()));
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
            maxTupleSet = r1.getMaxTupleSet().postComposition(r2.getMaxTupleSet(),
                    (t1, t2) -> !exec.areMutuallyExclusive(t1.getFirst(), t2.getSecond()));
        }
        return maxTupleSet;
    }

    @Override
    public TupleSet getMinTupleSetRecursive(){
        if(recursiveGroupId > 0 && maxTupleSet != null){
            ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
            minTupleSet = r1.getMinTupleSetRecursive().postComposition(r2.getMinTupleSetRecursive(),
                    (t1, t2) -> (exec.isImplied(t1.getFirst(), t1.getSecond())
                            || exec.isImplied(t2.getSecond(), t1.getSecond()))
                        && !exec.areMutuallyExclusive(t1.getFirst(), t2.getFirst()));
            return minTupleSet;
        }
        return getMinTupleSet();
    }

    @Override
    public TupleSet getMaxTupleSetRecursive(){
        if(recursiveGroupId > 0 && maxTupleSet != null){
            ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
            maxTupleSet = r1.getMaxTupleSetRecursive().postComposition(r2.getMaxTupleSetRecursive(),
                    (t1, t2) -> !exec.areMutuallyExclusive(t1.getFirst(), t2.getSecond()));
            return maxTupleSet;
        }
        return getMaxTupleSet();
    }

    @Override
    public Map<Relation, Set<Tuple>> activate(Set<Tuple> activeSet) {
        HashSet<Tuple> r1Set = new HashSet<>();
        HashSet<Tuple> r2Set = new HashSet<>();

        TupleSet r1Max = r1.getMaxTupleSet();
        TupleSet r2Max = r2.getMaxTupleSet();
        TupleSet r1Min = r1.getMinTupleSet();
        TupleSet r2Min = r2.getMinTupleSet();
        for(Tuple t : activeSet) {
            Event e1 = t.getFirst();
            Event e3 = t.getSecond();
            for(Tuple t1 : r1Max.getByFirst(e1)) {
                Event e2 = t1.getSecond();
                Tuple t2 = new Tuple(e2, e3);
                if(r2Max.contains(t2)) {
                    if(!r1Min.contains(t1)) {
                        r1Set.add(t1);
                    }
                    if(!r2Min.contains(t2)) {
                        r2Set.add(t2);
                    }
                }
            }
        }

        return Map.of(r1, r1Set, r2, r2Set);
    }

    @Override
    public BooleanFormula encode(SolverContext ctx) {
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet max1 = r1.getMaxTupleSet();
        TupleSet max2 = r2.getMaxTupleSet();
        TupleSet min1 = r1.getMinTupleSet();
        TupleSet min2 = r2.getMinTupleSet();

        for(Tuple tuple : encodeTupleSet) {
            Event x = tuple.getFirst();
            Event z = tuple.getSecond();
            BooleanFormula expr = bmgr.makeFalse();
            for(Tuple t1 : max1.getByFirst(x)) {
                Event y = t1.getSecond();
                Tuple t2 = new Tuple(y, z);
                if(max2.contains(t2)) {
                    boolean b1 = min1.contains(t1);
                    boolean b2 = min2.contains(t2);
                    BooleanFormula f1 = b1 ? x.exec() : r1.getSMTVar(t1, ctx);
                    BooleanFormula f2 = b2 ? z.exec() : r2.getSMTVar(t2, ctx);
                    BooleanFormula f3 = b1 && b2 ? y.exec() : bmgr.makeTrue();
                    expr = bmgr.or(expr, bmgr.and(f1, f2, f3));
                }
            }

            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), expr));
        }
        return enc;
    }
}