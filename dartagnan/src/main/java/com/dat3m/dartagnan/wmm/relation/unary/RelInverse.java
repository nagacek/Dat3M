package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

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

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        obs.listen(r1, (may, must) -> buf.send(this, may.inverse(), must.inverse()));
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        obs.listen(this, news -> buf.send(r1, news.stream().map(Tuple::getInverse).collect(toSet())));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        for(Tuple tuple : encodeTupleSet){
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, encoder.getTask(), ctx), r1.getSMTVar(tuple.getInverse(), encoder.getTask(), ctx)));
        }
        return enc;
    }
}