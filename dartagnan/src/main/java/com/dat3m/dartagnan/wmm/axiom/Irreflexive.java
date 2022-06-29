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

import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Florian Furbach
 */
public class Irreflexive extends Axiom {

    public Irreflexive(Relation rel, boolean negated, boolean flag) {
        super(rel, negated, flag);
    }

    public Irreflexive(Relation rel) {
        super(rel, false, false);
    }

    @Override
    public TupleSet getEncodeTupleSet(VerificationTask task) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet set = new TupleSet();
        TupleSet max = ra.getMaxTupleSet(rel);
        TupleSet min = ra.getMinTupleSet(rel);
        max.stream().filter(Tuple::isLoop).filter(t -> !min.contains(t)).forEach(set::add);
        return set;
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        if(!flag && !negated) {
            buf.send(rel, ra.getMaxTupleSet(rel).stream().filter(Tuple::isLoop).collect(toSet()), Set.of());
        }
    }

    @Override
    public BooleanFormula consistent(WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        VerificationTask task = encoder.getTask();
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        for(Tuple tuple : encoder.getActiveSet(rel)){
            if(tuple.isLoop()){
                enc = bmgr.and(enc, bmgr.not(rel.getSMTVar(tuple, task, ctx)));
            }
        }
        return negated ? bmgr.not(enc) : enc;
    }

    @Override
    public String toString() {
        return (negated ? "~" : "") + "irreflexive " + rel.getName();
    }
}