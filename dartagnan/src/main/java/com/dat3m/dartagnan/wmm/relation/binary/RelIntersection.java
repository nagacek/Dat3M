package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Set;

import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;

/**
 *
 * @author Florian Furbach
 */
public class RelIntersection extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + "&" + r2.getName() + ")";
    }

    public RelIntersection(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        obs.listen(r1, (may, must) -> buf.send(this, intersection(may, max2), intersection(must, min2)));
        obs.listen(r2, (may, must) -> buf.send(this, intersection(may, max1), intersection(must, min1)));
    }

    @Override
    public void activate(Set<Tuple> news, VerificationTask task, WmmEncoder.Buffer buf) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        buf.send(r1, difference(news, ra.getMinTupleSet(r1)));
        buf.send(r2, difference(news, ra.getMinTupleSet(r2)));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        for(Tuple tuple : encodeTupleSet){
            BooleanFormula opt1 = min1.contains(tuple) ? bmgr.makeTrue() : r1.getSMTVar(tuple, encoder.getTask(), ctx);
            BooleanFormula opt2 = min2.contains(tuple) ? bmgr.makeTrue() : r2.getSMTVar(tuple, encoder.getTask(), ctx);
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, encoder.getTask(), ctx), bmgr.and(opt1, opt2)));
        }
        return enc;
    }
}