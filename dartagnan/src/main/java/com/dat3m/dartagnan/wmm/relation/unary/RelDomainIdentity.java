package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Set;

public class RelDomainIdentity extends UnaryRelation {

    public static String makeTerm(Relation r1){
        return "[domain(" + r1.getName() + ")]";
    }

    public RelDomainIdentity(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    public RelDomainIdentity(Relation r1, String name) {
        super(r1, name);
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
    public void activate(Set<Tuple> activeSet, WmmEncoder.Buffer buf) {
        //TODO: Optimize using minSets (but no CAT uses this anyway)
        TupleSet r1Set = new TupleSet();
        RelationAnalysis ra = buf.analysisContext().get(RelationAnalysis.class);
        TupleSet may1 = ra.may(r1);
        for(Tuple tuple : activeSet){
            r1Set.addAll(may1.getByFirst(tuple.getFirst()));

        }
        buf.send(r1, r1Set);
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.solverContext();
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        RelationAnalysis ra = encoder.analysisContext().get(RelationAnalysis.class);
        TupleSet may1 = ra.may(r1);
        for(Tuple tuple1 : encodeTupleSet){
            Event e = tuple1.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            //TODO: Optimize using minSets (but no CAT uses this anyway)
            for(Tuple tuple2 : may1.getByFirst(e)){
                opt = bmgr.or(encoder.edge(r1, tuple2));
            }
            enc = bmgr.and(enc, bmgr.equivalence(encoder.edge(this, e, e), opt));
        }
        return enc;
    }
}
