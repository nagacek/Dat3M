package com.dat3m.dartagnan.encoding;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.analysis.alias.AliasAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.MemEvent;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.program.filter.FilterIntersection;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.utils.Flag;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.Utils;
import com.google.common.base.Preconditions;
import org.sosy_lab.java_smt.api.*;

import java.util.*;

import static com.dat3m.dartagnan.expression.utils.Utils.generalEqual;
import static com.dat3m.dartagnan.program.event.Tag.INIT;
import static com.dat3m.dartagnan.program.event.Tag.WRITE;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RF;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

public final class RelationEncoder implements Relation.Visitor<BooleanFormula> {
    private final ExecutionAnalysis exec;
    private final AliasAnalysis alias;
    private final SolverContext ctx;
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final Program program;
    private final boolean useSATEncoding;
    public RelationEncoder(SolverContext c, Context analysisContext, Program program, boolean useSATEncoding) {
        exec = analysisContext.get(ExecutionAnalysis.class);
        alias = analysisContext.get(AliasAnalysis.class);
        ctx = c;
        bmgr = c.getFormulaManager().getBooleanFormulaManager();
        imgr = c.getFormulaManager().getIntegerFormulaManager();
        this.program = program;
        this.useSATEncoding = useSATEncoding;
    }
    @Override
    public BooleanFormula visitDefinition(TupleSet toEncode, Relation rel, List<? extends Relation> dependencies) {
        Preconditions.checkArgument(rel instanceof StaticRelation);
        BooleanFormula enc = bmgr.makeTrue();
        for (Tuple tuple : toEncode) {
            enc = bmgr.and(enc, bmgr.equivalence(edge(rel, tuple), execution(tuple)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitUnion(TupleSet toEncode, Relation rel, Relation... r) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet min = rel.getMinTupleSet();
        for (Tuple tuple : toEncode) {
            BooleanFormula edge = edge(rel, tuple);
            if (min.contains(tuple)) {
                enc = bmgr.and(enc, bmgr.equivalence(edge, execution(tuple)));
                continue;
            }
            List<BooleanFormula> opt = new ArrayList<>(r.length);
            for (Relation relation : r) {
                opt.add(edge(relation, tuple));
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge, bmgr.or(opt)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitIntersection(TupleSet toEncode, Relation rel, Relation... r) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet min = rel.getMinTupleSet();
        for (Tuple tuple : toEncode) {
            BooleanFormula edge = edge(rel, tuple);
            if (min.contains(tuple)) {
                enc = bmgr.and(enc, bmgr.equivalence(edge, execution(tuple)));
                continue;
            }
            List<BooleanFormula> opt = new ArrayList<>(r.length);
            for (Relation relation : r) {
                opt.add(edge(relation, tuple));
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge, bmgr.and(opt)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitDifference(TupleSet toEncode, Relation rel, Relation r1, Relation r2) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet min = rel.getMinTupleSet();
        for (Tuple tuple : toEncode) {
            BooleanFormula edge = edge(rel, tuple);
            if (min.contains(tuple)) {
                enc = bmgr.and(enc, bmgr.equivalence(edge, execution(tuple)));
                continue;
            }
            BooleanFormula opt1 = edge(r1, tuple);
            BooleanFormula opt2 = bmgr.not(edge(r2, tuple));
            enc = bmgr.and(enc, bmgr.equivalence(edge, bmgr.and(opt1, opt2)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitComposition(TupleSet toEncode, Relation rel, Relation r1, Relation r2) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet r1Set = r1.getEncodeTupleSet();
        TupleSet r2Set = r2.getEncodeTupleSet();
        TupleSet minSet = rel.getMinTupleSet();
        for (Tuple tuple : toEncode) {
            BooleanFormula expr = bmgr.makeFalse();
            if (minSet.contains(tuple)) {
                expr = execution(tuple);
            } else {
                for (Tuple t1 : r1Set.getByFirst(tuple.getFirst())) {
                    Tuple t2 = new Tuple(t1.getSecond(), tuple.getSecond());
                    if (r2Set.contains(t2)) {
                        expr = bmgr.or(expr, bmgr.and(edge(r1, t1), edge(r2, t2)));
                    }
                }
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge(rel, tuple), expr));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitDomainIdentity(TupleSet toEncode, Relation rel, Relation r1) {
        BooleanFormula enc = bmgr.makeTrue();
        for (Tuple tuple : toEncode) {
            Event e = tuple.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            //TODO: Optimize using minSets (but no CAT uses this anyway)
            for (Tuple t : r1.getMaxTupleSet().getByFirst(e)) {
                opt = bmgr.or(opt, edge(r1, t));
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge(rel, tuple), opt));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitRangeIdentity(TupleSet toEncode, Relation rel, Relation r1) {
        BooleanFormula enc = bmgr.makeTrue();
        //TODO: Optimize using minSets (but no CAT uses this anyway)
        for (Tuple tuple : toEncode) {
            Event e = tuple.getFirst();
            BooleanFormula opt = bmgr.makeFalse();
            for (Tuple t : r1.getMaxTupleSet().getBySecond(e)) {
                opt = bmgr.or(opt, edge(r1, t));
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge(rel, tuple), opt));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitTransitiveClosure(TupleSet toEncode, Relation rel, Relation r1) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet maySet = rel.getMaxTupleSet();
        TupleSet minSet = rel.getMinTupleSet();
        TupleSet r1Max = r1.getMaxTupleSet();
        for (Tuple tuple : toEncode) {
            BooleanFormula edge = edge(rel, tuple);
            if (minSet.contains(tuple)) {
                enc = bmgr.and(enc, bmgr.equivalence(edge, execution(tuple)));
                continue;
            }
            BooleanFormula orClause = bmgr.makeFalse();
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();
            if (r1Max.contains(tuple)) {
                orClause = bmgr.or(orClause, edge(r1, tuple));
            }
            for (Tuple t : r1Max.getByFirst(e1)) {
                Event e3 = t.getSecond();
                if (e3.getCId() != e1.getCId() && e3.getCId() != e2.getCId() && maySet.contains(new Tuple(e3, e2))) {
                    BooleanFormula tVar = minSet.contains(t) ? edge(rel, t) : edge(r1, t);
                    orClause = bmgr.or(orClause, bmgr.and(tVar, edge(rel, new Tuple(e3, e2))));
                }
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge, orClause));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitInverse(TupleSet toEncode, Relation rel, Relation r1) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet minSet = rel.getMinTupleSet();
        for (Tuple tuple : toEncode) {
            enc = bmgr.and(enc, bmgr.equivalence(
                    edge(rel, tuple),
                    minSet.contains(tuple) ?
                            execution(tuple) :
                            edge(r1, tuple.getInverse())));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitRecursive(TupleSet toEncode, Relation rel, Relation r1) {
        return bmgr.makeTrue();
    }
    @Override
    public BooleanFormula visitFences(TupleSet toEncode, Relation rel, FilterAbstract fenceSet) {
        BooleanFormula enc = bmgr.makeTrue();
        List<Event> fences = program.getCache().getEvents(fenceSet);
        for (Tuple tuple : toEncode) {
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();
            BooleanFormula orClause;
            if (rel.getMinTupleSet().contains(tuple)) {
                orClause = bmgr.makeTrue();
            } else {
                orClause = fences.stream()
                        .filter(f -> e1.getCId() < f.getCId() && f.getCId() < e2.getCId())
                        .map(Event::exec).reduce(bmgr.makeFalse(), bmgr::or);
            }
            enc = bmgr.and(enc, bmgr.equivalence(
                    edge(rel, tuple),
                    bmgr.and(execution(tuple), orClause)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitCriticalSections(TupleSet toEncode, Relation rscs) {
        BooleanFormula enc = bmgr.makeTrue();
        TupleSet maySet = rscs.getMaxTupleSet();
        for (Tuple tuple : toEncode) {
            Event lock = tuple.getFirst();
            Event unlock = tuple.getSecond();
            BooleanFormula relation = execution(tuple);
            for (Tuple t : maySet.getBySecond(unlock)) {
                Event y = t.getFirst();
                if (lock.getCId() < y.getCId() && y.getCId() < unlock.getCId()) {
                    relation = bmgr.and(relation, bmgr.not(edge(rscs, t)));
                }
            }
            for (Tuple t : maySet.getByFirst(lock)) {
                Event y = t.getSecond();
                if (lock.getCId() < y.getCId() && y.getCId() < unlock.getCId()) {
                    relation = bmgr.and(relation, bmgr.not(edge(rscs, t)));
                }
            }
            enc = bmgr.and(enc, bmgr.equivalence(edge(rscs, tuple), relation));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitReadModifyWrites(TupleSet toEncode, Relation rmw) {
        BooleanFormula enc = bmgr.makeTrue();
        BooleanFormula unpredictable = bmgr.makeFalse();
        TupleSet maySet = rmw.getMaxTupleSet();
        TupleSet mustSet = rmw.getMinTupleSet();
        for (Event store : program.getCache().getEvents(
                FilterIntersection.get(FilterBasic.get(Tag.WRITE), FilterBasic.get(Tag.EXCL)))) {
            checkState(store instanceof MemEvent, "non-memory event participating in '" + rmw.getName() + "'");
            BooleanFormula storeExec = bmgr.makeFalse();
            for (Tuple t : maySet.getBySecond(store)) {
                MemEvent load = (MemEvent) t.getFirst();
                BooleanFormula sameAddress = generalEqual(load.getMemAddressExpr(), ((MemEvent) store).getMemAddressExpr(), ctx);
                // Encode if load and store form an exclusive pair
                BooleanFormula isPair = exclPair(load, store);
                BooleanFormula pairingCond = pairingCond(load, store, maySet);
                // For ARMv8, the store can be executed if addresses mismatch, but behaviour is "constrained unpredictable"
                // The implementation does not include all possible unpredictable cases: in case of address
                // mismatch, addresses of read and write are unknown, i.e. read and write can use any address.
                // For RISCV and Power, addresses should match.
                if (store.is(Tag.MATCHADDRESS)) {
                    pairingCond = bmgr.and(pairingCond, sameAddress);
                } else {
                    unpredictable = bmgr.or(unpredictable, bmgr.and(store.exec(), isPair, bmgr.not(sameAddress)));
                }
                enc = bmgr.and(enc, bmgr.equivalence(isPair, pairingCond));
                storeExec = bmgr.or(storeExec, isPair);
            }
            enc = bmgr.and(enc, bmgr.implication(store.exec(), storeExec));
        }
        for (Tuple tuple : toEncode) {
            MemEvent load = (MemEvent) tuple.getFirst();
            MemEvent store = (MemEvent) tuple.getSecond();
            BooleanFormula sameAddress = (alias.mustAlias(load, store) || store.is(Tag.MATCHADDRESS)) ? bmgr.makeTrue()
                    : generalEqual(load.getMemAddressExpr(), store.getMemAddressExpr(), ctx);
            enc = bmgr.and(enc, bmgr.equivalence(
                    edge(rmw, tuple),
                    mustSet.contains(tuple) ? execution(tuple) :
                            // Relation between exclusive load and store
                            bmgr.and(store.exec(), exclPair(load, store), sameAddress)));
        }
        return bmgr.and(enc, bmgr.equivalence(Flag.ARM_UNPREDICTABLE_BEHAVIOUR.repr(ctx), unpredictable));
    }
    @Override
    public BooleanFormula visitSameAddress(TupleSet toEncode, Relation loc) {
        BooleanFormula enc = bmgr.makeTrue();
        for (Tuple tuple : toEncode) {
            BooleanFormula rel = edge(loc, tuple);
            enc = bmgr.and(enc, bmgr.equivalence(rel, bmgr.and(
                    execution(tuple),
                    generalEqual(
                            ((MemEvent) tuple.getFirst()).getMemAddressExpr(),
                            ((MemEvent) tuple.getSecond()).getMemAddressExpr(), ctx)
            )));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitReadFrom(TupleSet toEncode, Relation rf) {
        BooleanFormula enc = bmgr.makeTrue();
        Map<MemEvent, List<BooleanFormula>> edgeMap = new HashMap<>();
        for (Tuple tuple : rf.getMaxTupleSet()) {
            MemEvent w = (MemEvent) tuple.getFirst();
            MemEvent r = (MemEvent) tuple.getSecond();
            BooleanFormula edge = edge(rf, tuple);
            // The boogie file might have a different type (Ints vs BVs) that the imposed by ARCH_PRECISION
            // In such cases we perform the transformation
            Formula a1 = w.getMemAddressExpr();
            Formula a2 = r.getMemAddressExpr();
            BooleanFormula sameAddress = generalEqual(a1, a2, ctx);
            Formula v1 = w.getMemValueExpr();
            Formula v2 = r.getMemValueExpr();
            BooleanFormula sameValue = generalEqual(v1, v2, ctx);
            edgeMap.computeIfAbsent(r, key -> new ArrayList<>()).add(edge);
            enc = bmgr.and(enc, bmgr.implication(edge, bmgr.and(execution(tuple), sameAddress, sameValue)));
        }
        for (MemEvent r : edgeMap.keySet()) {
            enc = bmgr.and(enc, encodeEdgeSeq(r, edgeMap.get(r)));
        }
        return enc;
    }
    @Override
    public BooleanFormula visitMemoryOrder(TupleSet toEncode, Relation co) {
        return useSATEncoding ? encodeSAT(co) : encodeIDL(co);
    }

    public BooleanFormulaManager getBmgr() { return bmgr; }

    private BooleanFormula pairingCond(Event load, Event store, TupleSet maySet) {
        BooleanFormula pairingCond = bmgr.and(load.exec(), store.cf());
        for (Tuple t : maySet.getBySecond(store)) {
            Event otherLoad = t.getFirst();
            if (otherLoad.getCId() > load.getCId()) {
                pairingCond = bmgr.and(pairingCond, bmgr.not(otherLoad.exec()));
            }
        }
        for (Tuple t : maySet.getByFirst(load)) {
            Event otherStore = t.getSecond();
            if (otherStore.getCId() < store.getCId()) {
                pairingCond = bmgr.and(pairingCond, bmgr.not(otherStore.cf()));
            }
        }
        return pairingCond;
    }
    private BooleanFormula exclPair(Event load, Event store) {
        return bmgr.makeVariable("excl(" + load.getCId() + "," + store.getCId() + ")");
    }

    private BooleanFormula encodeEdgeSeq(Event read, List<BooleanFormula> edges) {
        if (GlobalSettings.ALLOW_MULTIREADS) {
            return bmgr.implication(read.exec(), bmgr.or(edges));
        }
        int num = edges.size();
        int readId = read.getCId();
        BooleanFormula lastSeqVar = mkSeqVar(readId, 0);
        BooleanFormula newSeqVar = lastSeqVar;
        BooleanFormula atMostOne = bmgr.equivalence(lastSeqVar, edges.get(0));
        for (int i = 1; i < num; i++) {
            newSeqVar = mkSeqVar(readId, i);
            atMostOne = bmgr.and(atMostOne, bmgr.equivalence(newSeqVar, bmgr.or(lastSeqVar, edges.get(i))));
            atMostOne = bmgr.and(atMostOne, bmgr.not(bmgr.and(edges.get(i), lastSeqVar)));
            lastSeqVar = newSeqVar;
        }
        BooleanFormula atLeastOne = bmgr.or(newSeqVar, edges.get(edges.size() - 1));
        atLeastOne = bmgr.implication(read.exec(), atLeastOne);
        return bmgr.and(atMostOne, atLeastOne);
    }
    private BooleanFormula mkSeqVar(int readId, int i) {
        return bmgr.makeVariable("s(" + RF + ",E" + readId + "," + i + ")");
    }
    private BooleanFormula encodeIDL(Relation co) {
        List<MemEvent> allWrites = program.getCache().getEvents(FilterBasic.get(WRITE)).stream()
                .map(MemEvent.class::cast)
                .sorted(Comparator.comparingInt(Event::getCId))
                .collect(toList());
        TupleSet maxSet = co.getMaxTupleSet();
        Set<Tuple> transCo = RelationAnalysis.findTransitivelyImpliedCo(co, exec);
        BooleanFormula enc = bmgr.makeTrue();
        // ---- Encode clock conditions (init = 0, non-init > 0) ----
        NumeralFormula.IntegerFormula zero = imgr.makeNumber(0);
        for (MemEvent w : allWrites) {
            NumeralFormula.IntegerFormula clock = Utils.coClockVar(w, ctx);
            enc = bmgr.and(enc, w.is(INIT) ? imgr.equal(clock, zero) : imgr.greaterThan(clock, zero));
        }

        // ---- Encode coherences ----
        for (int i = 0; i < allWrites.size() - 1; i++) {
            MemEvent w1 = allWrites.get(i);
            for (MemEvent w2 : allWrites.subList(i + 1, allWrites.size())) {
                Tuple t = new Tuple(w1, w2);
                boolean forwardPossible = maxSet.contains(t);
                boolean backwardPossible = maxSet.contains(t.getInverse());
                if (!forwardPossible && !backwardPossible) {
                    continue;
                }
                BooleanFormula execPair = execution(t);
                BooleanFormula sameAddress = alias.mustAlias(w1, w2) ? bmgr.makeTrue() :
                        generalEqual(w1.getMemAddressExpr(), w2.getMemAddressExpr(), ctx);
                BooleanFormula pairingCond = bmgr.and(execPair, sameAddress);
                BooleanFormula fCond = (w1.is(INIT) || transCo.contains(t)) ? bmgr.makeTrue() :
                        imgr.lessThan(Utils.coClockVar(w1, ctx), Utils.coClockVar(w2, ctx));
                BooleanFormula bCond = (w2.is(INIT) || transCo.contains(t.getInverse())) ? bmgr.makeTrue() :
                        imgr.lessThan(Utils.coClockVar(w2, ctx), Utils.coClockVar(w1, ctx));
                BooleanFormula coF = forwardPossible ? edge(co, new Tuple(w1, w2)) : bmgr.makeFalse();
                BooleanFormula coB = backwardPossible ? edge(co, new Tuple(w2, w1)) : bmgr.makeFalse();
                enc = bmgr.and(enc,
                        bmgr.implication(coF, fCond),
                        bmgr.implication(coB, bCond),
                        bmgr.equivalence(pairingCond, bmgr.or(coF, coB))
                );
            }
        }
        return enc;
    }
    private BooleanFormula encodeSAT(Relation co) {
        List<MemEvent> allWrites = program.getCache().getEvents(FilterBasic.get(WRITE)).stream()
                .map(MemEvent.class::cast)
                .sorted(Comparator.comparingInt(Event::getCId))
                .collect(toList());
        TupleSet maxSet = co.getMaxTupleSet();
        TupleSet minSet = co.getMinTupleSet();
        BooleanFormula enc = bmgr.makeTrue();
        // ---- Encode coherences ----
        for (int i = 0; i < allWrites.size() - 1; i++) {
            MemEvent w1 = allWrites.get(i);
            for (MemEvent w2 : allWrites.subList(i + 1, allWrites.size())) {
                Tuple t = new Tuple(w1, w2);
                Tuple tInv = t.getInverse();
                boolean forwardPossible = maxSet.contains(t);
                boolean backwardPossible = maxSet.contains(tInv);
                if (!forwardPossible && !backwardPossible) {
                    continue;
                }
                BooleanFormula execPair = execution(t);
                BooleanFormula sameAddress = alias.mustAlias(w1, w2) ? bmgr.makeTrue() :
                        generalEqual(w1.getMemAddressExpr(), w2.getMemAddressExpr(), ctx);
                BooleanFormula pairingCond = bmgr.and(execPair, sameAddress);
                BooleanFormula coF = forwardPossible ? edge(co, t) : bmgr.makeFalse();
                BooleanFormula coB = backwardPossible ? edge(co, tInv) : bmgr.makeFalse();
                enc = bmgr.and(enc,
                        bmgr.equivalence(pairingCond, bmgr.or(coF, coB)),
                        bmgr.or(bmgr.not(coF), bmgr.not(coB))
                );
                if (!minSet.contains(t) && !minSet.contains(tInv)) {
                    for (MemEvent w3 : allWrites) {
                        Tuple t1 = new Tuple(w1, w3);
                        Tuple t2 = new Tuple(w3, w2);
                        if (forwardPossible && maxSet.contains(t1) && maxSet.contains(t2)) {
                            BooleanFormula co1 = edge(co, t1);
                            BooleanFormula co2 = edge(co, t2);
                            enc = bmgr.and(enc, bmgr.implication(bmgr.and(co1, co2), coF));
                        }
                        if (backwardPossible && maxSet.contains(t1.getInverse()) && maxSet.contains(t2.getInverse())) {
                            BooleanFormula co1 = edge(co, t2.getInverse());
                            BooleanFormula co2 = edge(co, t1.getInverse());
                            enc = bmgr.and(enc, bmgr.implication(bmgr.and(co1, co2), coB));
                        }
                    }
                }
            }
        }
        return enc;
    }

    private BooleanFormula edge(Relation relation, Tuple tuple) {
        return relation.getSMTVar(tuple, ctx);
    }
    private BooleanFormula execution(Tuple tuple) {
        return ProgramEncoder.execution(tuple.getFirst(), tuple.getSecond(), exec, ctx);
    }
}