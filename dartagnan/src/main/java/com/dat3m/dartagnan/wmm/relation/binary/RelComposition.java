package com.dat3m.dartagnan.wmm.relation.binary;

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
import java.util.stream.Collectors;

/**
 *
 * @author Florian Furbach
 */
public class RelComposition extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + ";" + r2.getName() + ")";
    }

    public RelComposition(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet may1 = ra.getMaxTupleSet(r1);
        TupleSet must1 = ra.getMinTupleSet(r1);
        TupleSet may2 = ra.getMaxTupleSet(r2);
        TupleSet must2 = ra.getMinTupleSet(r2);
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().requires(ExecutionAnalysis.class);
        obs.listen(r1, (may, must) -> update(may, must, may2, must2, exec, buf));
        obs.listen(r2, (may, must) -> update(may1, must1, may, must, exec, buf));
    }

    private void update(TupleSet may1, TupleSet must1, TupleSet may2, TupleSet must2, ExecutionAnalysis exec, RelationAnalysis.SetBuffer buf) {
        TupleSet maxTupleSet = may1.postCompositionMay(may2, exec);
        TupleSet minTupleSet = must1.postCompositionMust(must2, exec);
        buf.send(this,maxTupleSet,minTupleSet);
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
        TupleSet max0 = ra.getMaxTupleSet(this);
        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);
        obs.listen(r1, (dis, en) -> {
            //dis0 = difference(composeMay(dis,may2),composeMay(may1,may2))
            Set<Tuple> dis0 = new HashSet<>();
            for(Tuple t1 : dis) {
                Event e1 = t1.getFirst();
                Event e2 = t1.getSecond();
                Set<Event> alternatives = max1.getByFirst(e1).stream()
                .map(Tuple::getSecond)
                .collect(Collectors.toSet());
                max2.getByFirst(e2).stream()
                .map(Tuple::getSecond)
                .filter(e3 -> alternatives.stream().noneMatch(e4 -> max2.contains(new Tuple(e4,e3))))
                .forEach(e3 -> dis0.add(new Tuple(e1,e3)));
            }
            buf.send(this, dis0, en.postCompositionMust(min2, exec));
            Set<Tuple> dis2 = new HashSet<>();
            for(Tuple t1 : en) {
                Event e1 = t1.getFirst();
                Event e2 = t1.getSecond();
                max2.getByFirst(e2).stream()
                .filter(exec.isImplied(e2,e1) ? t2 -> true : t2 -> exec.isImplied(t2.getSecond(),e1))
                .filter(t2 -> !max0.contains(new Tuple(e1, t2.getSecond())))
                .forEach(dis2::add);
            }
            buf.send(r2, dis2, Set.of());
        });
        obs.listen(r1, (dis, en) -> {
            //dis0 = difference(composeMay(may1,dis),composeMay(may1,may2))
            Set<Tuple> dis0 = new HashSet<>();
            for(Tuple t2 : dis) {
                Event e2 = t2.getFirst();
                Event e3 = t2.getSecond();
                Set<Event> alternatives = max2.getBySecond(e3).stream()
                .map(Tuple::getFirst)
                .collect(Collectors.toSet());
                max1.getBySecond(e2).stream()
                .map(Tuple::getFirst)
                .filter(e1 -> alternatives.stream().noneMatch(e4 -> max1.contains(new Tuple(e1,e4))))
                .forEach(e1 -> dis0.add(new Tuple(e1,e3)));
            }
            buf.send(this,dis0, min1.postCompositionMust(en,exec));
            Set<Tuple> dis1 = new HashSet<>();
            for(Tuple t2 : en) {
                Event e2 = t2.getFirst();
                Event e3 = t2.getSecond();
                max1.getBySecond(e2).stream()
                .filter(exec.isImplied(e2,e3) ? t1 -> true : t1 -> exec.isImplied(t1.getFirst(),e3))
                .filter(t1 -> !max0.contains(new Tuple(t1.getFirst(),e3)))
                .forEach(dis1::add);
            }
            buf.send(r1, dis1, Set.of());
        });
        obs.listen(this, (dis, en) -> {
            Set<Tuple> dis1 = new HashSet<>();
            Set<Tuple> dis2 = new HashSet<>();
            for(Tuple tuple : dis) {
                Event e1 = tuple.getFirst();
                Event e3 = tuple.getSecond();
                min1.getByFirst(e1).stream()
                .map(Tuple::getSecond)
                .filter(exec.isImplied(e3,e1) ? e2 -> true : e2 -> exec.isImplied(e2,e1))
                .map(e2 -> new Tuple(e2,e3))
                .filter(max2::contains)
                .forEach(dis2::add);
                min2.getBySecond(e3).stream()
                .map(Tuple::getFirst)
                .filter(exec.isImplied(e1,e3) ? e2 -> true : e2 -> exec.isImplied(e2,e3))
                .map(e2 -> new Tuple(e1,e2))
                .filter(max1::contains)
                .forEach(dis1::add);
            }
            buf.send(r1,dis1,Set.of());
            buf.send(r2,dis2,Set.of());
        });
    }

    @Override
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);

        TupleSet r1Max = ra.getMaxTupleSet(r1);
        TupleSet r2Max = ra.getMaxTupleSet(r2);
        TupleSet r1Min = ra.getMinTupleSet(r1);
        TupleSet r2Min = ra.getMinTupleSet(r2);
        WmmEncoder.Listener lis = news -> {
            HashSet<Tuple> r1Set = new HashSet<>();
            HashSet<Tuple> r2Set = new HashSet<>();
            for(Tuple t : news) {
                Event e1 = t.getFirst();
                Event e3 = t.getSecond();
                for(Tuple t1 : r1Max.getByFirst(e1)) {
                    Event e2 = t1.getSecond();
                    Tuple t2 = new Tuple(e2, e3);
                    if(r2Max.contains(t2)) {
                        if(!r1Min.contains(t1)) {
                            r1Set.add(t1);
                        }
                        if(!r2Min.contains(t2)) {
                            r2Set.add(t2);
                        }
                    }
                }
            }

            buf.send(r1,r1Set);
            buf.send(r2, r2Set);
        };
        obs.listen(this,lis);
        lis.notify(ra.getDisabledSet(this));
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        VerificationTask task = encoder.getTask();
        RelationAnalysis ra = encoder.getTask().getAnalysisContext().get(RelationAnalysis.class);
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();

        TupleSet max1 = ra.getMaxTupleSet(r1);
        TupleSet max2 = ra.getMaxTupleSet(r2);
        TupleSet min1 = ra.getMinTupleSet(r1);
        TupleSet min2 = ra.getMinTupleSet(r2);

        for(Tuple tuple : encodeTupleSet) {
            Event x = tuple.getFirst();
            Event z = tuple.getSecond();
            BooleanFormula expr = bmgr.makeFalse();
            for(Tuple t1 : max1.getByFirst(x)) {
                Event y = t1.getSecond();
                Tuple t2 = new Tuple(y, z);
                if(max2.contains(t2)) {
                    boolean b1 = min1.contains(t1);
                    boolean b2 = min2.contains(t2);
                    BooleanFormula f1 = b1 ? x.exec() : r1.getSMTVar(t1, task, ctx);
                    BooleanFormula f2 = b2 ? z.exec() : r2.getSMTVar(t2, task, ctx);
                    BooleanFormula f3 = b1 && b2 ? y.exec() : bmgr.makeTrue();
                    expr = bmgr.or(expr, bmgr.and(f1, f2, f3));
                }
            }

            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, task, ctx), expr));
        }
        for(Tuple tuple : ra.getDisabledSet(this)) {
            Event x = tuple.getFirst();
            Event z = tuple.getSecond();
            for(Tuple t1 : max1.getByFirst(x)) {
                Event y = t1.getSecond();
                Tuple t2 = new Tuple(y,z);
                if(max2.contains(t2)) {
                    boolean b1 = min1.contains(t1);
                    boolean b2 = min2.contains(t2);
                    BooleanFormula f1 = b1 ? x.exec() : r1.getSMTVar(t1, task, ctx);
                    BooleanFormula f2 = b2 ? z.exec() : r2.getSMTVar(t2, task, ctx);
                    BooleanFormula f3 = b1 && b2 ? y.exec() : bmgr.makeTrue();
                    enc = bmgr.and(enc, bmgr.not(bmgr.and(f1, f2, f3)));
                }
            }
        }
        return enc;
    }
}