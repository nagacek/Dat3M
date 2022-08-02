package com.dat3m.dartagnan.wmm.relation.base.memory;

import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.analysis.alias.AliasAnalysis;
import com.dat3m.dartagnan.program.event.EventCache;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.MemEvent;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.program.filter.FilterMinus;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.analysis.WmmAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dat3m.dartagnan.configuration.OptionNames.CO_ANTISYMMETRY;
import static com.dat3m.dartagnan.configuration.Property.LIVENESS;
import static com.dat3m.dartagnan.expression.utils.Utils.generalEqual;
import static com.dat3m.dartagnan.program.Program.SourceLanguage.LITMUS;
import static com.dat3m.dartagnan.program.event.Tag.INIT;
import static com.dat3m.dartagnan.program.event.Tag.WRITE;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.CO;
import static com.dat3m.dartagnan.wmm.utils.Utils.edge;
import static com.dat3m.dartagnan.wmm.utils.Utils.intVar;
import static org.sosy_lab.java_smt.api.FormulaType.BooleanType;

@Options
public class RelCo extends Relation {

	private static final Logger logger = LogManager.getLogger(RelCo.class);

    // =========================== Configurables ===========================

	@Option(
		name=CO_ANTISYMMETRY,
		description="Encodes the antisymmetry of coherences explicitly.",
		secure=true)
	private boolean antisymmetry = false;

	// =====================================================================

    public RelCo(){
        term = CO;
        forceDoEncode = true;
    }

    @Override
    public void initializeEncoding(SolverContext ctx) {
        super.initializeEncoding(ctx);
        try {
            task.getConfig().inject(this);
            logger.info("{}: {}", CO_ANTISYMMETRY, antisymmetry);
        } catch(InvalidConfigurationException e) {
            logger.warn(e.getMessage());
        }
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            final WmmAnalysis wmmAnalysis = analysisContext.get(WmmAnalysis.class);
            minTupleSet = new TupleSet();
            if (wmmAnalysis.isLocallyConsistent()) {
                applyLocalConsistencyMinSet();
            }
        }
        return minTupleSet;
    }

    private void applyLocalConsistencyMinSet() {
        final AliasAnalysis alias = analysisContext.get(AliasAnalysis.class);
        for (Tuple t : getMaxTupleSet()) {
            MemEvent w1 = (MemEvent) t.getFirst();
            MemEvent w2 = (MemEvent) t.getSecond();
            if (!w2.is(INIT) && alias.mustAlias(w1, w2) && (w1.is(INIT) || t.isForward())) {
                minTupleSet.add(t);
            }
        }
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
        	logger.info("Computing maxTupleSet for " + getName());
        	final AliasAnalysis alias = analysisContext.get(AliasAnalysis.class);
            final WmmAnalysis wmmAnalysis = analysisContext.get(WmmAnalysis.class);
            final List<Event> eventsStores = task.getProgram().getCache().getEvents(FilterBasic.get(WRITE));
            final List<Event> eventsNonInitStores= task.getProgram().getCache().getEvents(FilterMinus.get(
                    FilterBasic.get(WRITE),
                    FilterBasic.get(INIT)
            ));

            maxTupleSet = new TupleSet();
            for (Event w1 : eventsStores) {
                for (Event w2 : eventsNonInitStores) {
                    if(w1.getCId() != w2.getCId() && alias.mayAlias((MemEvent) w1, (MemEvent)w2)){
                        maxTupleSet.add(new Tuple(w1, w2));
                    }
                }
            }

            removeMutuallyExclusiveTuples(maxTupleSet);
            if (wmmAnalysis.isLocallyConsistent()) {
                maxTupleSet.removeIf(Tuple::isBackward);
            }

            logger.info("maxTupleSet size for " + getName() + ": " + maxTupleSet.size());
        }
        return maxTupleSet;
    }

    @Override
    protected BooleanFormula encodeApprox(SolverContext ctx) {
		final BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        final IntegerFormulaManager imgr = ctx.getFormulaManager().getIntegerFormulaManager();
        final EventCache cache = task.getProgram().getCache();
        final List<MemEvent> writes = Lists.transform(cache.getEvents(FilterBasic.get(WRITE)), MemEvent.class::cast);
        final TupleSet maxSet = getMaxTupleSet();
        final TupleSet minSet = getMinTupleSet();

        BooleanFormula enc = bmgr.makeTrue();
        // Encode clock conditions (init = 0, non-init > 0).
        for (MemEvent w : writes) {
            enc = bmgr.and(enc, w.is(INIT)
                            ? imgr.equal(getClockVar(w, ctx), imgr.makeNumber(BigDecimal.ZERO))
                            : imgr.greaterThan(getClockVar(w, ctx), imgr.makeNumber(BigInteger.ZERO)));
        }

        // We find classes of events such that events in different classes
        // may never alias.
        // It suffices to generate distinctness constraints per class.
        final List<Event> eventsStore = cache.getEvents(FilterMinus.get(FilterBasic.get(WRITE), FilterBasic.get(INIT)));
        final DependencyGraph<Event> aliasGraph = DependencyGraph.from(eventsStore, e ->
                Stream.concat(
                        maxSet.getByFirst(e).stream().map(Tuple::getSecond),
                        maxSet.getBySecond(e).stream().map(Tuple::getFirst)
                )::iterator
        );
        for (Set<DependencyGraph<Event>.Node> aliasClass : aliasGraph.getSCCs()) {
            List<IntegerFormula> clockVars = aliasClass.stream().map(DependencyGraph.Node::getContent)
                    .filter(e -> !e.is(INIT)).map(e -> getClockVar(e, ctx)).collect(Collectors.toList());
            enc = bmgr.and(enc, imgr.distinct(clockVars));
        }

        final Set<Tuple> transCo = findTransitivelyImpliedCo();
        for(MemEvent w1 : writes) {
            for(Tuple t : maxSet.getByFirst(w1)){
                MemEvent w2 = (MemEvent)t.getSecond();
                BooleanFormula relation = getSMTVar(t, ctx);
                BooleanFormula execPair = getExecPair(t, ctx);
                BooleanFormula sameAddress = generalEqual(w1.getMemAddressExpr(), w2.getMemAddressExpr(), ctx);
                BooleanFormula clockConstr = (w1.is(INIT) || transCo.contains(t)) ? bmgr.makeTrue()
                        : imgr.lessThan(getClockVar(w1, ctx), getClockVar(w2, ctx));

                if (minSet.contains(t)) {
                    enc = bmgr.and(enc, clockConstr, bmgr.equivalence(relation, execPair));
                } else if (!maxSet.contains(t.getInverse())) {
                    enc = bmgr.and(enc,
                            bmgr.equivalence(relation, bmgr.and(execPair, sameAddress)),
                            bmgr.implication(sameAddress, clockConstr));
                } else {
                    enc = bmgr.and(enc, bmgr.equivalence(relation, bmgr.and(execPair, sameAddress, clockConstr)));
                }
            }
        }

        final boolean doEncodeLastCo = task.getProgram().getFormat().equals(LITMUS) || task.getProperty().contains(LIVENESS);
        if (doEncodeLastCo) {
            enc = bmgr.and(enc, encodeLastCoConstraints(ctx));
        }
        return enc;
    }

    private BooleanFormula encodeLastCoConstraints(SolverContext ctx) {
        final AliasAnalysis alias = analysisContext.requires(AliasAnalysis.class);
        final ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
        final BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        final TupleSet minSet = getMinTupleSet();
        final TupleSet maxSet = getMaxTupleSet();
        final List<Event> initEvents = task.getProgram().getCache().getEvents(FilterBasic.get(INIT));
        final List<MemEvent> writes = Lists.transform(task.getProgram().getCache().getEvents(FilterBasic.get(WRITE)), MemEvent.class::cast);
        final boolean doEncodeFinalAddressValues = task.getProgram().getFormat() == LITMUS;

        BooleanFormula enc = bmgr.makeTrue();
        // Find all writes that can potentially be final writes (i.e. are not guaranteed to get overwritten)
        Set<MemEvent> possiblyLastWrites = new HashSet<>(writes);
        minSet.stream().filter(t -> t.getFirst() != t.getSecond() && exec.isImplied(t.getFirst(), t.getSecond()))
                .map(Tuple::getFirst).forEach(possiblyLastWrites::remove);

        for (MemEvent w1 : writes) {
            if (!possiblyLastWrites.contains(w1)) {
                enc = bmgr.and(enc, bmgr.equivalence(getLastCoVar(w1, ctx), bmgr.makeFalse()));
                continue;
            }

            BooleanFormula lastCo = w1.exec();
            // ---- Find all possibly overwriting writes ----
            for (Tuple t : maxSet.getByFirst(w1)) {
                MemEvent w2 = (MemEvent) t.getSecond();
                if (possiblyLastWrites.contains(w2)) {
                    if (minSet.getByFirst(w1).stream().map(Tuple::getSecond).filter(possiblyLastWrites::contains)
                            .anyMatch(w3 -> w2 != w3 && exec.isImplied(w2, w3))) {
                        // ... if w2 was executed then so would t2.getSecond. But t2.getSecond already witnesses
                        // that w1 is not the last write, so we never need to encode w2 as a witness.
                        continue;
                    }

                    BooleanFormula isAfter = minSet.contains(t) ? bmgr.not(w2.exec()) : bmgr.not(getSMTVar(t, ctx));
                    lastCo = bmgr.and(lastCo, isAfter);
                }
            }

            BooleanFormula lastCoExpr = getLastCoVar(w1, ctx);
            enc = bmgr.and(enc, bmgr.equivalence(lastCoExpr, lastCo));

            if (doEncodeFinalAddressValues) {
                for (Event i : initEvents) {
                    Init init = (Init) i;
                    if (!alias.mayAlias(w1, init)) {
                        continue;
                    }
                    IExpr address = init.getAddress();
                    Formula a1 = w1.getMemAddressExpr();
                    Formula a2 = address.toIntFormula(init, ctx);
                    BooleanFormula sameAddress = alias.mustAlias(init, w1) ? bmgr.makeTrue() : generalEqual(a1, a2, ctx);
                    Formula v1 = w1.getMemValueExpr();
                    Formula v2 = init.getBase().getLastMemValueExpr(ctx, init.getOffset());
                    BooleanFormula sameValue = generalEqual(v1, v2, ctx);
                    enc = bmgr.and(enc, bmgr.implication(bmgr.and(lastCoExpr, sameAddress), sameValue));
                }
            }
        }
        return enc;
    }

    /*
        Returns a set of co-edges (w1, w2) (subset of maxTupleSet) whose clock-constraints
        do not need to get encoded explicitly.
        The reason is that whenever we have co(w1,w2) then there exists an intermediary
        w3 s.t. co(w1, w3) /\ co(w3, w2). As a result we have c(w1) < c(w3) < c(w2) transitively.
        Reasoning: Let (w1, w2) be a potential co-edge. Suppose there exists a w3 different to w1 and w2,
        whose execution is either implied by either w1 or w2.
        Now, if co(w1, w3) is a must-edge and co(w2, w3) is impossible, then we can reason as follows.
            - Suppose w1 and w2 get executed and their addresses match, then w3 must also get executed.
            - Since co(w1, w3) is a must-edge, we have that w3 accesses the same address as w1 and w2,
              and c(w1) < c(w3).
            - Because addr(w2)==addr(w3), we must also have either co(w2, e3) or co(w3, w2).
              The former is disallowed by assumption, so we have co(w3, w2) and hence c(w3) < c(w2).
            - By transitivity, we have c(w1) < c(w3) < c(w2) as desired.
            - Note that this reasoning has to be done inductively, because co(w1, w3) or co(w3, w2) may
              not involve encoding a clock constraint (due to this optimization).
        There is also a symmetric case where co(w3, w1) is impossible and co(w3, w2) is a must-edge.

     */
    private Set<Tuple> findTransitivelyImpliedCo() {
        final ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
        final TupleSet min = getMinTupleSet();
        final TupleSet max = getMaxTupleSet();

        Set<Tuple> transCo = new HashSet<>();
        for (final Tuple t : max) {
            final MemEvent e1 = (MemEvent) t.getFirst();
            final MemEvent e2 = (MemEvent) t.getSecond();
            final Predicate<Event> execPred = (e3 -> e3 != e1 && e3 != e2 && (exec.isImplied(e1, e3) || exec.isImplied(e2, e3)));
            final boolean hasIntermediary = min.getByFirst(e1).stream().map(tuple -> (MemEvent)tuple.getSecond())
                                    .anyMatch(e3 -> execPred.apply(e3) && !max.contains(new Tuple(e2, e3))) ||
                                min.getBySecond(e2).stream().map(tuple -> (MemEvent)tuple.getFirst())
                                    .anyMatch(e3 -> execPred.apply(e3) && !max.contains(new Tuple(e3, e1)));
            if (hasIntermediary) {
                transCo.add(t);
            }
        }
        return transCo;
    }

    @Override
    public BooleanFormula getSMTVar(Tuple edge, SolverContext ctx) {
        if(!antisymmetry) {
            return super.getSMTVar(edge, ctx);
        }
		final BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();

        final MemEvent first = (MemEvent) edge.getFirst();
        final MemEvent second = (MemEvent) edge.getSecond();
        // Doing the check at the java level seems to slightly improve  performance
        final BooleanFormula eqAdd = first.getAddress().equals(second.getAddress()) ? bmgr.makeTrue() :
                generalEqual(first.getMemAddressExpr(), second.getMemAddressExpr(), ctx);
        return !getMaxTupleSet().contains(edge) ? bmgr.makeFalse() :
    		first.getCId() <= second.getCId() ?
    				edge(getName(), first, second, ctx) :
    					bmgr.ifThenElse(bmgr.and(getExecPair(edge, ctx), eqAdd),
    							bmgr.not(getSMTVar(edge.getInverse(), ctx)),
    							bmgr.makeFalse());
    }

    public IntegerFormula getClockVar(Event write, SolverContext ctx) {
    	Preconditions.checkArgument(write.is(WRITE), "Cannot get a clock-var for non-writes.");
        if (write.is(INIT)) {
            return ctx.getFormulaManager().getIntegerFormulaManager().makeNumber(0);
        }
        return intVar(term, write, ctx);
    }

    public BooleanFormula getLastCoVar(Event write, SolverContext ctx) {
        return ctx.getFormulaManager().makeVariable(BooleanType, "co_last(" + write.repr() + ")");
    }
}