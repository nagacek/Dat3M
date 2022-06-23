package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

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
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet max2 = ra.getMaxTupleSet(r2);
        TupleSet min2 = ra.getMinTupleSet(r2);
        obs.listen(r1, (may, must) -> buf.send(this, difference(may, min2), difference(must, max2)));
    }

    @Override
    public void activate(Set<Tuple> news, VerificationTask task, WmmEncoder.Buffer buf) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        buf.send(r1, difference(news, ra.getMinTupleSet(r1)));
        buf.send(r2, intersection(news, ra.getMaxTupleSet(r2)));
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
            BooleanFormula opt1 = min1.contains(tuple) ? execution(tuple.getFirst(), tuple.getSecond(), exec, ctx) : r1.getSMTVar(tuple, encoder.getTask(), ctx);
            BooleanFormula opt2 = max2.contains(tuple) ? bmgr.not(r2.getSMTVar(tuple, encoder.getTask(), ctx)) : bmgr.makeTrue();
            if (Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(bmgr.and(opt1, opt2), this.getSMTVar(tuple, encoder.getTask(), ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, encoder.getTask(), ctx), bmgr.and(opt1, opt2)));
            }
        }
        return enc;
    }
}
