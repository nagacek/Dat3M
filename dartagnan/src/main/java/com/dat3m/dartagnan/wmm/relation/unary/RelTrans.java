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

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.intersection;

/**
 *
 * @author Florian Furbach
 */
public class RelTrans extends UnaryRelation {

    public static String makeTerm(Relation r1){
        return r1.getName() + "^+";
    }

    public RelTrans(Relation r1) {
        super(r1);
        term = makeTerm(r1);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            //TODO: Make sure this is correct and efficient
            ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
            minTupleSet = new TupleSet(r1.getMinTupleSet());
            boolean changed;
            int size = minTupleSet.size();
            do {
                minTupleSet.addAll(minTupleSet.postComposition(r1.getMinTupleSet(),
                        (t1, t2) -> (exec.isImplied(t1.getFirst(), t1.getSecond())
                                || exec.isImplied(t2.getSecond(), t1.getSecond()))
                            && !exec.areMutuallyExclusive(t1.getFirst(), t2.getSecond())));
                changed = minTupleSet.size() != size;
                size = minTupleSet.size();
            } while (changed);
        }
        return minTupleSet;
    }


    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
            ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
            TupleSet child = r1.getMaxTupleSet();
            Set<Tuple> update = child;
            while(!update.isEmpty()) {
                maxTupleSet.addAll(update);
                Set<Tuple> next = new HashSet<>();
                for(Tuple t1 : update) {
                    Event e1 = t1.getFirst();
                    Event e2 = t1.getSecond();
                    for(Tuple t2 : child.getByFirst(e2)) {
                        Event e3 = t2.getSecond();
                        if(!exec.areMutuallyExclusive(e1, e3)) {
                            next.add(new Tuple(e1, e3));
                        }
                    }
                }
                next.removeAll(maxTupleSet);
                update = next;
            }
        }
        return maxTupleSet;
    }

    @Override
    public void activate(Set<Tuple> news, VerificationTask task, WmmEncoder.Buffer buf) {
        HashSet<Tuple> factors = new HashSet<>();
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet maxTupleSet = ra.getMaxTupleSet(this);
        TupleSet minTupleSet = ra.getMinTupleSet(this);
        for(Tuple t : news) {
            for(Tuple t1 : maxTupleSet.getByFirst(t.getFirst())) {
                Tuple t2 = new Tuple(t1.getSecond(), t.getSecond());
                if(maxTupleSet.contains(t2)) {
                    if(!minTupleSet.contains(t1)) {
                        factors.add(t1);
                    }
                    if(!minTupleSet.contains(t2)) {
                        factors.add(t2);
                    }
                }
            }
        }
        buf.send(this,factors);
        buf.send(r1, intersection(news, ra.getMaxTupleSet(r1)));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet maxTupleSet = ra.getMaxTupleSet(this);
        TupleSet minSet = ra.getMinTupleSet(this);
        TupleSet r1Max = ra.getMaxTupleSet(r1);
        for(Tuple tuple : encodeTupleSet){

            BooleanFormula orClause = bmgr.makeFalse();
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            if(r1Max.contains(tuple)){
                orClause = bmgr.or(orClause, r1.getSMTVar(tuple, ctx));
            }


            for(Tuple t : r1Max.getByFirst(e1)){
                Event e3 = t.getSecond();
                Tuple t2 = new Tuple(e3, e2);
                if(e3.getCId() != e1.getCId() && e3.getCId() != e2.getCId() && maxTupleSet.contains(t2)){
                    boolean b1 = minSet.contains(t);
                    boolean b2 = minSet.contains(t2);
                    BooleanFormula f1 = b1 ? e1.exec() : r1.getSMTVar(t, ctx);
                    BooleanFormula f2 = b2 ? e2.exec() : getSMTVar(t2, ctx);
                    BooleanFormula f3 = b1 && b2 ? e3.exec() : bmgr.makeTrue();
                    orClause = bmgr.or(orClause, bmgr.and(f1, f2, f3));
                }
            }

            if(Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(orClause, this.getSMTVar(tuple, ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), orClause));
            }
        }

        return enc;
    }
}