package com.dat3m.dartagnan.wmm.axiom;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Set;

import static com.google.common.collect.Sets.difference;

public class Empty extends Axiom {

    public Empty(Relation rel, boolean negated, boolean flag) {
        super(rel, negated, flag);
    }

    public Empty(Relation rel) {
        super(rel, false, false);
    }

    @Override
    public TupleSet getEncodeTupleSet(VerificationTask task) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        return new TupleSet(difference(ra.getMaxTupleSet(rel), ra.getMinTupleSet(rel)));
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        if(!flag && !negated) {
            buf.send(rel, ra.getMaxTupleSet(rel), Set.of());
        }
    }

    @Override
    public BooleanFormula consistent(WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        VerificationTask task = encoder.getTask();
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        for(Tuple tuple : encoder.getActiveSet(rel)) {
            enc = bmgr.and(enc, bmgr.not(rel.getSMTVar(tuple, task, ctx)));
        }
        return negated ? bmgr.not(enc) : enc;
    }

    @Override
    public String toString() {
        return (negated ? "~" : "") + "empty " + rel.getName();
    }
}