package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;

public class RelDomainIdentity extends UnaryRelation {

    public static String makeTerm(Relation r1){
        return "[domain(" + r1.getName() + ")]";
    }

    public RelDomainIdentity(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
            minTupleSet = new TupleSet();
            r1.getMinTupleSet().stream()
                    .filter(t -> exec.isImplied(t.getFirst(), t.getSecond()))
                    .map(t -> new Tuple(t.getFirst(), t.getFirst()))
                    .forEach(minTupleSet::add);
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = r1.getMaxTupleSet().mapped(t -> new Tuple(t.getFirst(), t.getFirst()));
        }
        return maxTupleSet;
    }

    @Override
    public void addEncodeTupleSet(TupleSet tuples){
        TupleSet activeSet = truncated(tuples);
        encodeTupleSet.addAll(activeSet);

        TupleSet max = r1.getMaxTupleSet();
        TupleSet min = r1.getMinTupleSet();
        TupleSet r1Set = new TupleSet();
        for(Tuple tuple : activeSet){
            r1Set.addAll(max.getByFirst(tuple.getFirst()));
        }
        r1Set.removeAll(min);
        if(!r1Set.isEmpty()){
            r1.addEncodeTupleSet(r1Set);
        }
    }

    @Override
    public BooleanFormula encode(SolverContext ctx) {
        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet max1 = r1.getMaxTupleSet();
        TupleSet min1 = r1.getMinTupleSet();
        for(Tuple tuple1 : encodeTupleSet){
            Event e = tuple1.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            for(Tuple tuple2 : max1.getByFirst(e)){
                opt = bmgr.or(min1.contains(tuple2) ? execution(e, tuple2.getSecond(), exec, ctx) : r1.getSMTVar(e, tuple2.getSecond(), ctx));
            }
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(e, e, ctx), opt));
        }
        return enc;
    }
}
