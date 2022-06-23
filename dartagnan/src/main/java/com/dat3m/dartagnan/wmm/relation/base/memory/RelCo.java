package com.dat3m.dartagnan.wmm.relation.base.memory;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.MemEvent;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.program.filter.FilterMinus;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.analysis.WmmAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.configuration.OptionNames.CO_ANTISYMMETRY;
import static com.dat3m.dartagnan.configuration.Property.LIVENESS;
import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;
import static com.dat3m.dartagnan.expression.utils.Utils.*;
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
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        logger.info("Computing knowledge for {}", getName());
        VerificationTask task = ra.getTask();
        AliasAnalysis alias = task.getAnalysisContext().get(AliasAnalysis.class);
        WmmAnalysis wmmAnalysis = task.getAnalysisContext().get(WmmAnalysis.class);

        TupleSet may = new TupleSet();
        TupleSet must = new TupleSet();
        List<Event> eventsInit = task.getProgram().getCache().getEvents(FilterBasic.get(INIT));
        List<Event> eventsStore = task.getProgram().getCache().getEvents(FilterMinus.get(FilterBasic.get(WRITE),FilterBasic.get(INIT)));

        for(Event e1 : eventsInit) {
            MemEvent w1 = (MemEvent) e1;
            for(Event e2 : eventsStore) {
                MemEvent w2 = (MemEvent) e2;
                if(alias.mayAlias(w1,w2)){
                    Tuple t = new Tuple(e1,e2);
                    may.add(t);
                    if(alias.mustAlias(w1,w2)) {
                        must.add(t);
                    }
                }
            }
        }

        boolean lc = wmmAnalysis.isLocallyConsistent();
        for(Event e1 : eventsStore) {
            MemEvent w1 = (MemEvent) e1;
            for(Event e2 : eventsStore) {
                MemEvent w2 = (MemEvent) e2;
                Tuple t = new Tuple(e1,e2);
                if(!t.isLoop() && alias.mayAlias(w1,w2) && (!lc || !t.isBackward())) {
                    may.add(t);
                    if(lc && t.isForward() && alias.mustAlias(w1,w2)) {
                        must.add(t);
                    }
                }
            }
        }

        logger.info("knowledge size for {}: {}/{}", getName(), must.size(), may.size());
        buf.send(this, may, must);
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        VerificationTask task = encoder.getTask();
        ExecutionAnalysis exec = task.getAnalysisContext().get(ExecutionAnalysis.class);
        AliasAnalysis alias = task.getAnalysisContext().get(AliasAnalysis.class);
        WmmAnalysis wmmAnalysis = task.getAnalysisContext().get(WmmAnalysis.class);
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
    	FormulaManager fmgr = ctx.getFormulaManager();
		BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        
    	BooleanFormula enc = bmgr.makeTrue();

        List<Event> eventsInit = task.getProgram().getCache().getEvents(FilterBasic.get(INIT));
        List<Event> eventsStore = task.getProgram().getCache().getEvents(FilterMinus.get(
                FilterBasic.get(WRITE),
                FilterBasic.get(INIT)
        ));

		for(Event e : eventsInit) {
            enc = bmgr.and(enc, imgr.equal(getIntVar(e, ctx), imgr.makeNumber(BigInteger.ZERO)));
        }

        List<IntegerFormula> intVars = new ArrayList<>();
        for(Event w : eventsStore) {
        	IntegerFormula coVar = getIntVar(w, ctx);
            enc = bmgr.and(enc, imgr.greaterThan(coVar, imgr.makeNumber(BigInteger.ZERO)));
            intVars.add(coVar);
        }

        BooleanFormula distinct = intVars.size() > 1 ?
        		imgr.distinct(intVars) :
                bmgr.makeTrue();

        enc = bmgr.and(enc, distinct);

        for(Event w :  task.getProgram().getCache().getEvents(FilterBasic.get(WRITE))) {
            MemEvent w1 = (MemEvent)w;
            BooleanFormula lastCo = w1.exec();

            for(Tuple t : ra.getMaxTupleSet(this).getByFirst(w1)){
                MemEvent w2 = (MemEvent)t.getSecond();
                BooleanFormula relation = getSMTVar(t, task, ctx);
                BooleanFormula execPair = execution(t.getFirst(), t.getSecond(), exec, ctx);
                lastCo = bmgr.and(lastCo, bmgr.not(relation));

                Formula a1 = w1.getMemAddressExpr();
                Formula a2 = w2.getMemAddressExpr();
                BooleanFormula sameAddress = generalEqual(a1, a2, ctx);

                enc = bmgr.and(enc, bmgr.equivalence(relation,
                        bmgr.and(execPair, sameAddress, imgr.lessThan(getIntVar(w1, ctx), getIntVar(w2, ctx))
                )));

                // ============ Local consistency optimizations ============
                if (ra.getMinTupleSet(this).contains(t)) {
                   enc = bmgr.and(enc, bmgr.equivalence(relation, execPair));
                } else if (wmmAnalysis.isLocallyConsistent()) {
                    if (w2.is(INIT) || t.isBackward()){
                        enc = bmgr.and(enc, bmgr.equivalence(relation, bmgr.makeFalse()));
                    }
                    if (w1.is(INIT) || t.isForward()) {
                        enc = bmgr.and(enc, bmgr.implication(bmgr.and(execPair, sameAddress), relation));
                    }
                }
            }

            if (task.getProgram().getFormat().equals(LITMUS) || task.getProperty().contains(LIVENESS)) {
                BooleanFormula lastCoExpr = getLastCoVar(w1, ctx);
                enc = bmgr.and(enc, bmgr.equivalence(lastCoExpr, lastCo));

                for (Event i : eventsInit) {
                    Init init = (Init) i;
                    if (!alias.mayAlias(w1, init)) {
                        continue;
                    }

                    IExpr address = init.getAddress();
                    Formula a1 = w1.getMemAddressExpr();
                    Formula a2 = address.toIntFormula(init,ctx);
                    BooleanFormula sameAddress = generalEqual(a1, a2, ctx);
                    Formula v1 = w1.getMemValueExpr();
                    Formula v2 = init.getBase().getLastMemValueExpr(ctx,init.getOffset());
                    BooleanFormula sameValue = generalEqual(v1, v2, ctx);
                    enc = bmgr.and(enc, bmgr.implication(bmgr.and(lastCoExpr, sameAddress), sameValue));
                }
            }
        }
        return enc;
    }
    
    @Override
    public BooleanFormula getSMTVar(Tuple edge, VerificationTask task, SolverContext ctx) {
        if(!antisymmetry) {
            return super.getSMTVar(edge, task, ctx);
        }

        ExecutionAnalysis exec = task.getAnalysisContext().requires(ExecutionAnalysis.class);
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
    	FormulaManager fmgr = ctx.getFormulaManager();
		BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

        MemEvent first = (MemEvent) edge.getFirst();
        MemEvent second = (MemEvent) edge.getSecond();
        // Doing the check at the java level seems to slightly improve  performance
        BooleanFormula eqAdd = first.getAddress().equals(second.getAddress()) ? bmgr.makeTrue() :
                generalEqual(first.getMemAddressExpr(), second.getMemAddressExpr(), ctx);
        return !ra.getMaxTupleSet(this).contains(edge) ? bmgr.makeFalse() :
    		first.getCId() <= second.getCId() ?
    				edge(getName(), first, second, ctx) :
    					bmgr.ifThenElse(bmgr.and(execution(edge.getFirst(), edge.getSecond(), exec, ctx), eqAdd),
    							bmgr.not(getSMTVar(edge.getInverse(), task, ctx)),
    							bmgr.makeFalse());
    }

    public IntegerFormula getIntVar(Event write, SolverContext ctx) {
    	Preconditions.checkArgument(write.is(WRITE), "Cannot get an int-var for non-writes.");
        return intVar(term, write, ctx);
    }

    public BooleanFormula getLastCoVar(Event write, SolverContext ctx) {
        return ctx.getFormulaManager().makeVariable(BooleanType, "co_last(" + write.repr() + ")");
    }
}