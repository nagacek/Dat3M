package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
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
import static java.util.stream.Collectors.toSet;

public class RelRangeIdentity extends UnaryRelation {

    public static String makeTerm(Relation r1){
        return "[range(" + r1.getName() + ")]";
    }

    public RelRangeIdentity(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
        obs.listen(r1, (may, must) -> buf.send(this,
            may.stream().map(Tuple::getSecond).map(e -> new Tuple(e,e)).collect(toSet()),
            must.stream().filter(t -> exec.isImplied(t.getSecond(),t.getFirst())).map(Tuple::getSecond).map(e -> new Tuple(e,e)).collect(toSet())));
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet max = ra.getMaxTupleSet(r1);
        TupleSet min = ra.getMinTupleSet(r1);
        obs.listen(this, news -> buf.send(r1, news.stream()
            .flatMap(t -> max.getBySecond(t.getSecond()).stream())
            .filter(t -> !min.contains(t))
            .collect(toSet())));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        ExecutionAnalysis exec = encoder.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet min1 = ra.getMinTupleSet(r1);
        for(Tuple tuple1 : encodeTupleSet){
            Event e = tuple1.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            for(Tuple tuple2 : max1.getBySecond(e)){
                opt = bmgr.or(min1.contains(tuple2) ? execution(tuple2.getFirst(), e, exec, ctx) : r1.getSMTVar(tuple2.getFirst(), e, encoder.getTask(), ctx));
            }
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(e, e, encoder.getTask(), ctx), opt));
        }
        return enc;
    }
}