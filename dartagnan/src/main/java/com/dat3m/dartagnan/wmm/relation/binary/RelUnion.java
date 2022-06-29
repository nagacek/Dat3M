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
public class RelUnion extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + "+" + r2.getName() + ")";
    }

    public RelUnion(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        obs.listen(r1, (may, must) -> buf.send(this,may,must));
        obs.listen(r2, (may, must) -> buf.send(this,may,must));
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        obs.listen(r1, (dis, en) -> buf.send(this,difference(dis,max2),en));
        obs.listen(r2, (dis, en) -> buf.send(this,difference(dis,max1),en));
        obs.listen(this, (dis, en) -> {
            buf.send(r1,intersection(dis,max1),Set.of());
            buf.send(r2,intersection(dis,max2),Set.of());
        });
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        obs.listen(this, news -> {
            buf.send(r1, intersection(news, max1));
            buf.send(r2, intersection(news, max2));
        });
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
            BooleanFormula opt1 = max1.contains(tuple) ? r1.getSMTVar(tuple, encoder.getTask(), ctx) : bmgr.makeFalse();
            BooleanFormula opt2 = max2.contains(tuple) ? r2.getSMTVar(tuple, encoder.getTask(), ctx) : bmgr.makeFalse();
            if (Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(bmgr.or(opt1, opt2), this.getSMTVar(tuple, encoder.getTask(), ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, encoder.getTask(), ctx), bmgr.or(opt1, opt2)));
            }
        }
        return enc;
    }
}