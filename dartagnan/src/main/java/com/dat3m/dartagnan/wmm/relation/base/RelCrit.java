package com.dat3m.dartagnan.wmm.relation.base;

import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static com.dat3m.dartagnan.program.event.Tag.Linux.RCU_LOCK;
import static com.dat3m.dartagnan.program.event.Tag.Linux.RCU_UNLOCK;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CRIT;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.collect.Sets.intersection;
import static java.util.stream.Stream.concat;

public class RelCrit extends StaticRelation {

    public RelCrit(){
        term = CRIT;
    }

    @Override
    public <T> T accept(Visitor<? extends T> v) {
        return v.visitCriticalSections(this);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            computeTupleSets();
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null) {
            computeTupleSets();
        }
        return maxTupleSet;
    }

    private void computeTupleSets() {
        final ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
        maxTupleSet = new TupleSet();
        minTupleSet = new TupleSet();
        for (Thread thread : task.getProgram().getThreads()) {
            // assume order by cId
            // assume cId describes a topological sorting over the control flow
            List<Event> locks = reverse(thread.getCache().getEvents(FilterBasic.get(RCU_LOCK)));
            for (Event unlock : thread.getCache().getEvents(FilterBasic.get(RCU_UNLOCK))) {
                // iteration order assures that all intermediaries were already iterated
                for (Event lock : locks) {
                    if (unlock.getCId() < lock.getCId() ||
                            exec.areMutuallyExclusive(lock, unlock) ||
                            concat(minTupleSet.getByFirst(lock).stream().map(Tuple::getSecond),
                                            minTupleSet.getBySecond(unlock).stream().map(Tuple::getFirst))
                                    .anyMatch(e -> exec.isImplied(lock, e) || exec.isImplied(unlock, e))) {
                        continue;
                    }
                    boolean noIntermediary = maxTupleSet.getBySecond(unlock).stream()
                                    .allMatch(t -> exec.areMutuallyExclusive(lock, t.getFirst())) &&
                            maxTupleSet.getByFirst(lock).stream()
                                    .allMatch(t -> exec.areMutuallyExclusive(t.getSecond(), unlock));
                    Tuple tuple = new Tuple(lock, unlock);
                    maxTupleSet.add(tuple);
                    if (noIntermediary) {
                        minTupleSet.add(tuple);
                    }
                }
            }
        }
    }

    @Override
    public TupleSetMap addEncodeTupleSet(TupleSet tuples) {
        Queue<Tuple> queue = new ArrayDeque<>(intersection(tuples, maxTupleSet));
        TupleSet newEntries = new TupleSet();
        while (!queue.isEmpty()) {
            Tuple tuple = queue.remove();
            if (!encodeTupleSet.add(tuple)) {
                continue;
            } else {
                newEntries.add(tuple);
            }
            Event lock = tuple.getFirst();
            Event unlock = tuple.getSecond();
            for (Tuple t : maxTupleSet.getBySecond(unlock)) {
                if (isOrdered(lock, t.getFirst(), unlock)) {
                    queue.add(t);
                }
            }
            for (Tuple t : maxTupleSet.getByFirst(lock)) {
                if (isOrdered(lock, t.getSecond(), unlock)) {
                    queue.add(t);
                }
            }
        }
        return new TupleSetMap(this, newEntries);
    }

    @Override
    protected BooleanFormula encodeApprox(SolverContext ctx) {
        return encodeApprox(ctx, encodeTupleSet);
    }

    @Override
    public BooleanFormula encodeApprox(SolverContext ctx, TupleSet toEncode) {
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        for (Tuple tuple : toEncode) {
            Event lock = tuple.getFirst();
            Event unlock = tuple.getSecond();
            BooleanFormula relation = getExecPair(tuple, ctx);
            for (Tuple t : maxTupleSet.getBySecond(unlock)) {
                if (isOrdered(lock, t.getFirst(), unlock)) {
                    relation = bmgr.and(relation, bmgr.not(getSMTVar(t, ctx)));
                }
            }
            for (Tuple t : maxTupleSet.getByFirst(lock)) {
                if (isOrdered(lock, t.getSecond(), unlock)) {
                    relation = bmgr.and(relation, bmgr.not(getSMTVar(t, ctx)));
                }
            }
            enc = bmgr.and(enc, bmgr.equivalence(getSMTVar(tuple, ctx), relation));
        }
        return enc;
    }

    private boolean isOrdered(Event x, Event y, Event z) {
        return x.getCId() < y.getCId() && y.getCId() < z.getCId();
    }
}