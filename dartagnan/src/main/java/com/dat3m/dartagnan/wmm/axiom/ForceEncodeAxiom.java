package com.dat3m.dartagnan.wmm.axiom;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import static com.google.common.collect.Sets.difference;

/*
    This is a fake axiom that forces a relation to get encoded!
 */
public class ForceEncodeAxiom extends Axiom {
    public ForceEncodeAxiom(Relation rel, boolean negated, boolean flag) {
        super(rel, negated, flag);
    }

    public ForceEncodeAxiom(Relation rel) {
        super(rel, false, false);
    }

    @Override
    public TupleSet getEncodeTupleSet(VerificationTask task) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        return new TupleSet(difference(ra.getMaxTupleSet(rel), ra.getMinTupleSet(rel)));
    }

    @Override
    public BooleanFormula consistent(WmmEncoder encoder) {
        BooleanFormulaManager bmgr = encoder.getSolverContext().getFormulaManager().getBooleanFormulaManager();
		return negated ? bmgr.makeFalse() : bmgr.makeTrue();
    }

    @Override
    public String toString() {
        return "forceEncode " + (negated ? "~" : "") + rel.getName();
    }
}
