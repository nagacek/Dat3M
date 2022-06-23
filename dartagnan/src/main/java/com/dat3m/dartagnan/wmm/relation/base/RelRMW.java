package com.dat3m.dartagnan.wmm.relation.base;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.ExclusiveAccesses;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.MemEvent;
import com.dat3m.dartagnan.program.event.core.rmw.RMWStore;
import com.dat3m.dartagnan.program.event.lang.svcomp.EndAtomic;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.program.filter.FilterIntersection;
import com.dat3m.dartagnan.program.filter.FilterUnion;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.exclusivePairVariable;
import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;
import static com.dat3m.dartagnan.expression.utils.Utils.generalEqual;
import static com.dat3m.dartagnan.program.event.Tag.SVCOMP.SVCOMPATOMIC;
import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.RMW;

/*
    NOTE: Changes to the semantics of this class may need to be reflected
    in RMWGraph for Refinement!
 */
public class RelRMW extends StaticRelation {

	private static final Logger logger = LogManager.getLogger(RelRMW.class);

    public RelRMW(){
        term = RMW;
        forceDoEncode = true;
    }

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().requires(ExecutionAnalysis.class);
        logger.info("Computing maxTupleSet for " + getName());
        TupleSet minTupleSet = new TupleSet();

        // RMWLoad -> RMWStore
        FilterAbstract filter = FilterIntersection.get(FilterBasic.get(Tag.RMW), FilterBasic.get(Tag.WRITE));
        for(Event store : ra.getTask().getProgram().getCache().getEvents(filter)){
            if(store instanceof RMWStore) {
                minTupleSet.add(new Tuple(((RMWStore)store).getLoadEvent(), store));
            }
        }

        // Locks: Load -> Assume/CondJump -> Store
        FilterAbstract locks = FilterUnion.get(FilterBasic.get(Tag.C11.LOCK),
                FilterBasic.get(Tag.Linux.LOCK_READ));
        filter = FilterIntersection.get(FilterBasic.get(Tag.RMW), locks);
        for(Event e : ra.getTask().getProgram().getCache().getEvents(filter)){

            // Connect Load to Store
            minTupleSet.add(new Tuple(e, e.getSuccessor().getSuccessor()));
        }

        // Atomics blocks: BeginAtomic -> EndAtomic
        filter = FilterIntersection.get(FilterBasic.get(Tag.RMW), FilterBasic.get(SVCOMPATOMIC));
        for(Event end : ra.getTask().getProgram().getCache().getEvents(filter)){
            List<Event> block = ((EndAtomic)end).getBlock().stream().filter(x -> x.is(Tag.VISIBLE)).collect(Collectors.toList());
            for (int i = 0; i < block.size(); i++) {
                Event e1 = block.get(i);
                for(Event e2 : block.subList(i + 1, block.size())) {
                    if(!exec.areMutuallyExclusive(e1, e2)) {
                        minTupleSet.add(new Tuple(e1, e2));
                    }
                }
            }
        }

        TupleSet maxTupleSet = new TupleSet();
        maxTupleSet.addAll(minTupleSet);

        // LoadExcl -> StoreExcl
        ExclusiveAccesses excl = ra.getTask().getAnalysisContext().requires(ExclusiveAccesses.class);
        AliasAnalysis alias = ra.getTask().getAnalysisContext().requires(AliasAnalysis.class);
        for(MemEvent store : excl.getStores()) {
            for(ExclusiveAccesses.LoadInfo info : excl.getLoads(store)) {
                Tuple tuple = new Tuple(info.load,store);
                maxTupleSet.add(tuple);
                if(info.intermediates.isEmpty() && alias.mustAlias(info.load,store)) {
                    minTupleSet.add(tuple);
                }
            }
        }
        logger.info("maxTupleSet size for " + getName() + ": " + maxTupleSet.size());
        buf.send(this,maxTupleSet,minTupleSet);
    }

    @Override
    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        FormulaManager fmgr = ctx.getFormulaManager();
		BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

        BooleanFormula enc = bmgr.makeTrue();

        // Encode RMW for exclusive pairs
        ExecutionAnalysis exec = encoder.getTask().getAnalysisContext().requires(ExecutionAnalysis.class);
        ExclusiveAccesses excl = encoder.getTask().getAnalysisContext().requires(ExclusiveAccesses.class);
        AliasAnalysis alias = encoder.getTask().getAnalysisContext().requires(AliasAnalysis.class);
        for(MemEvent store : excl.getStores()) {
            for(ExclusiveAccesses.LoadInfo info : excl.getLoads(store)) {
                // Encode if load and store form an exclusive pair
                BooleanFormula isPair = info.intermediates.isEmpty()
                    ? bmgr.makeTrue()
                    : exclusivePairVariable(info.load,store,ctx);
                // If load and store have the same address
                BooleanFormula sameAddress = alias.mustAlias(info.load,store)
                    ? bmgr.makeTrue()
                    : generalEqual(info.load.getMemAddressExpr(),store.getMemAddressExpr(),ctx);
                // Relation between exclusive load and store
                enc = bmgr.and(enc,bmgr.equivalence(
                    getSMTVar(info.load,store,encoder.getTask(),ctx),
                    bmgr.and(execution(info.load, store, exec, ctx),isPair,sameAddress)));

                // Can be executed if addresses mismatch, but behaviour is "constrained unpredictable"
                // The implementation does not include all possible unpredictable cases: in case of address
                // mismatch, addresses of read and write are unknown, i.e. read and write can use any address
            }
        }
        return enc;
    }
}