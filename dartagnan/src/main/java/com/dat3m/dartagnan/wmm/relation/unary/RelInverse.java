package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Map;
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
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = r1.getMinTupleSet().inverse();
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = r1.getMaxTupleSet().inverse();
        }
        return maxTupleSet;
    }

    @Override
    public Map<Relation, Set<Tuple>> activate(Set<Tuple> news) {
        return Map.of(r1, news.stream().map(Tuple::getInverse).collect(toSet()));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        for(Tuple tuple : encodeTupleSet){
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), r1.getSMTVar(tuple.getInverse(), ctx)));
        }
        return enc;
    }
}