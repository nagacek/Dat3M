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
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        Set<Tuple> dis0 = ra.getDisabledSet(this);
        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        obs.listen(r1, (dis, en) -> {
            buf.send(this, dis, intersection(en, min2));
            buf.send(r2, intersection(en, dis0), Set.of());
        });
        obs.listen(r2, (dis, en) ->{
            buf.send(this, dis, intersection(en, min1));
            buf.send(r1, intersection(en, dis0), Set.of());
        });
        obs.listen(this, (dis, en) -> {
            buf.send(r1,intersection(dis,min2),en);
            buf.send(r2,intersection(dis,min1),en);
        });
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        obs.listen(this, news -> {
            buf.send(r1, difference(news, min1));
            buf.send(r2, difference(news, min2));
        });
        Set<Tuple> dis = ra.getDisabledSet(this);
        buf.send(r1, intersection(difference(dis,min1), ra.getMaxTupleSet(r1)));
        buf.send(r2, intersection(difference(dis,min2), ra.getMaxTupleSet(r2)));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        VerificationTask task = encoder.getTask();
        SolverContext ctx = encoder.getSolverContext();
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        for(Tuple tuple : encodeTupleSet){
            BooleanFormula opt1 = min1.contains(tuple) ? bmgr.makeTrue() : r1.getSMTVar(tuple, task, ctx);
            BooleanFormula opt2 = min2.contains(tuple) ? bmgr.makeTrue() : r2.getSMTVar(tuple, task, ctx);
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, task, ctx), bmgr.and(opt1, opt2)));
        }
        for(Tuple tuple : ra.getDisabledSet(this)) {
            if(!min1.contains(tuple) && !min2.contains(tuple)) {
                enc = bmgr.and(enc, bmgr.not(bmgr.and(r1.getSMTVar(tuple,task,ctx), r2.getSMTVar(tuple,task,ctx))));
            }
        }
        return enc;
    }
}