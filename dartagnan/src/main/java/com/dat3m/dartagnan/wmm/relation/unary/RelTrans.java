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
import java.util.function.Function;

import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.union;
import static java.util.stream.Stream.concat;

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
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().requires(ExecutionAnalysis.class);
        TupleSet maySet = ra.getMaxTupleSet(this);
        TupleSet mustSet = ra.getMinTupleSet(this);
        obs.listen(this, (may, must) -> {
            TupleSet maxTupleSet = may.postCompositionMay(maySet,exec);
            TupleSet minTupleSet = must.postCompositionMust(mustSet,exec);
            buf.send(this,maxTupleSet,minTupleSet);
        });
        obs.listen(r1, (may, must) -> buf.send(this, may, must));
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
        TupleSet max0 = ra.getMaxTupleSet(this);
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet min0 = ra.getMinTupleSet(this);
        obs.listen(r1, (dis, en) -> buf.send(this,Set.of(),en));
        obs.listen(this, (dis, en) -> {
            Set<Tuple> dis0 = new HashSet<>();
            Set<Tuple> dis1 = new HashSet<>();
            for(Tuple tuple : dis) {
                Event e1 = tuple.getFirst();
                Event e3 = tuple.getSecond();
                concat(
                    min0.getByFirst(e1).stream()
                    .map(Tuple::getSecond)
                    .filter(exec.isImplied(e3,e1) ? e2 -> true : e2 -> exec.isImplied(e2,e1))
                    .map(e2 -> new Tuple(e2,e3)),
                    min0.getBySecond(e3).stream()
                    .map(Tuple::getFirst)
                    .filter(exec.isImplied(e1,e3) ? e2 -> true : e2 -> exec.isImplied(e2,e3))
                    .map(e2 -> new Tuple(e1,e2)))
                .filter(max0::contains)
                .forEach(dis0::add);
            }
            for(Tuple t2 : en) {
                Event e2 = t2.getFirst();
                Event e3 = t2.getSecond();
                concat(
                    max0.getBySecond(e2).stream()
                    .filter(exec.isImplied(e2,e3) ? t1 -> true : t1 -> exec.isImplied(t1.getFirst(),e3))
                    .filter(t1 -> !max0.contains(new Tuple(t1.getFirst(),e3))),
                    max0.getByFirst(e3).stream()
                    .filter(exec.isImplied(e3,e2) ? t3 -> true : t3 -> exec.isImplied(t3.getSecond(),e2))
                    .filter(t3 -> !max0.contains(new Tuple(e2,t3.getSecond()))))
                .forEach(dis0::add);
            }
            buf.send(r1,intersection(dis1,max1),Set.of());
            buf.send(this,dis0,union(min0.postCompositionMust(en,exec),en.postCompositionMust(min0,exec)));
        });
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        TupleSet maxTupleSet = ra.getMaxTupleSet(this);
        TupleSet minTupleSet = ra.getMinTupleSet(this);
        WmmEncoder.Listener lis = news -> {
            Set<Tuple> factors = new HashSet<>();
            for(Tuple t : news) {
                Event e1 = t.getFirst();
                Event e3 = t.getSecond();
                for(Tuple t1 : maxTupleSet.getByFirst(e1)) {
                    Event e2 = t1.getSecond();
                    if(e1.equals(e2) || e2.equals(e3)) {
                        continue;
                    }
                    Tuple t2 = new Tuple(e2,e3);
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
        };
        obs.listen(this,lis);
        lis.notify(ra.getDisabledSet(this));
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
        Function<Tuple,BooleanFormula> clause = tuple ->{

            BooleanFormula orClause = bmgr.makeFalse();
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            if(r1Max.contains(tuple)) {
                orClause = bmgr.or(orClause, r1.getSMTVar(tuple, encoder.getTask(), ctx));
            }


            for(Tuple t : r1Max.getByFirst(e1)) {
                Event e3 = t.getSecond();
                Tuple t2 = new Tuple(e3, e2);
                if(e3.getCId() != e1.getCId() && e3.getCId() != e2.getCId() && maxTupleSet.contains(t2)) {
                    boolean b1 = minSet.contains(t);
                    boolean b2 = minSet.contains(t2);
                    BooleanFormula f1 = b1 ? e1.exec() : r1.getSMTVar(t, encoder.getTask(), ctx);
                    BooleanFormula f2 = b2 ? e2.exec() : getSMTVar(t2, encoder.getTask(), ctx);
                    BooleanFormula f3 = b1 && b2 ? e3.exec() : bmgr.makeTrue();
                    orClause = bmgr.or(orClause, bmgr.and(f1, f2, f3));
                }
            }

            return orClause;
        };
        for(Tuple tuple : encodeTupleSet) {
            BooleanFormula orClause = clause.apply(tuple);
            if(Relation.PostFixApprox) {
                enc = bmgr.and(enc, bmgr.implication(orClause, this.getSMTVar(tuple, encoder.getTask(), ctx)));
            } else {
                enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, encoder.getTask(), ctx), orClause));
            }
        }
        for(Tuple tuple : ra.getDisabledSet(this)) {
            enc = bmgr.and(enc, bmgr.not(clause.apply(tuple)));
        }

        return enc;
    }
}