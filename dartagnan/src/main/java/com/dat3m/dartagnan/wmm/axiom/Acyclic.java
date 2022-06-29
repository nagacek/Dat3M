package com.dat3m.dartagnan.wmm.axiom;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.java_smt.api.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.wmm.utils.Utils.cycleVar;
import static com.dat3m.dartagnan.wmm.utils.Utils.edge;
import static com.google.common.collect.Iterables.concat;

/**
 *
 * @author Florian Furbach
 */
public class Acyclic extends Axiom {

	private static final Logger logger = LogManager.getLogger(Acyclic.class);

    public Acyclic(Relation rel, boolean negated, boolean flag) {
        super(rel, negated, flag);
    }

    public Acyclic(Relation rel) {
        super(rel, false, false);
    }

    @Override
    public TupleSet getEncodeTupleSet(VerificationTask task){
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        logger.info("Computing encodeTupleSet for " + this);
        // ====== Construct [Event -> Successor] mapping ======
        Map<Event, Collection<Event>> succMap = new HashMap<>();
        TupleSet relMaxTuple = ra.getMaxTupleSet(rel);
        TupleSet min = ra.getMinTupleSet(rel);
        for (Tuple t : relMaxTuple) {
            succMap.computeIfAbsent(t.getFirst(), key -> new ArrayList<>()).add(t.getSecond());
        }

        // ====== Compute SCCs ======
        DependencyGraph<Event> depGraph = DependencyGraph.from(succMap.keySet(), succMap);
        TupleSet result = new TupleSet();
        for (Set<DependencyGraph<Event>.Node> scc : depGraph.getSCCs()) {
            for (DependencyGraph<Event>.Node node1 : scc) {
                for (DependencyGraph<Event>.Node node2 : scc) {
                    Tuple t = new Tuple(node1.getContent(), node2.getContent());
                    if (relMaxTuple.contains(t) && !min.contains(t)) {
                        result.add(t);
                    }
                }
            }
        }

        logger.info("encodeTupleSet size " + result.size());
        if (GlobalSettings.REDUCE_ACYCLICITY_ENCODE_SETS) {
            reduceWithMinSets(result,min,task);
            logger.info("reduced encodeTupleSet size " + result.size());
        }
        return result;
    }

    private void reduceWithMinSets(TupleSet encodeSet, TupleSet minSet, VerificationTask task) {
        /*
            ASSUMPTION: MinSet is acyclic!
            IDEA:
                Edges that are (must-)transitively implied do not need to get encoded.
                For this, we compute a (must-)transitive closure and a (must-)transitive reduction of must(rel).
                The difference "must(rel)+ \ red(must(rel))" does not net to be encoded.
                Note that it this is sound if the closure gets underapproximated and/or the reduction
                gets over approximated.
            COMPUTATION:
                (1) We compute an approximate (must-)transitive closure of must(rel)
                    - must(rel) is likely to be already transitive per thread (due to mostly coming from po)
                      Hence, we get a reasonable approximation by closing transitively over thread-crossing edges only.
                (2) We compute a (must) transitive reduction of the transitively closed must(rel)+.
                    - Since must(rel)+ is transitive, it suffice to check for each edge (a, c) if there
                      is an intermediate event b such that (a, b) and (b, c) are in must(rel)+
                      and b is implied by either a or c.
                    - It is possible to reduce must(rel) but that may give a less precise result.
         */
        ExecutionAnalysis exec = task.getAnalysisContext().get(ExecutionAnalysis.class);

        // (1) Approximate transitive closure of minSet (only gets computed when crossEdges are available)
        List<Tuple> crossEdges = minSet.stream()
                .filter(t -> t.isCrossThread() && !t.getFirst().is(Tag.INIT))
                .collect(Collectors.toList());
        TupleSet transMinSet = crossEdges.isEmpty() ? minSet : new TupleSet(minSet);
        for (Tuple crossEdge : crossEdges) {
            Event e1 = crossEdge.getFirst();
            Event e2 = crossEdge.getSecond();

            List<Event> ingoing = new ArrayList<>();
            ingoing.add(e1); // ingoing events + self
            minSet.getBySecond(e1).stream().map(Tuple::getFirst)
                    .filter(e -> exec.isImplied(e, e1))
                    .forEach(ingoing::add);


            List<Event> outgoing = new ArrayList<>();
            outgoing.add(e2); // outgoing edges + self
            minSet.getByFirst(e2).stream().map(Tuple::getSecond)
                    .filter(e -> exec.isImplied(e, e2))
                    .forEach(outgoing::add);

            for (Event in : ingoing) {
                for (Event out : outgoing) {
                    transMinSet.add(new Tuple(in, out));
                }
            }
        }

        // (2) Approximate reduction of transitive must-set: red(must(r)+).
        // Note: We reduce the transitive closure which may have more edges
        // that can be used to perform reduction
        TupleSet reduct = TupleSet.approximateTransitiveMustReduction(exec, transMinSet);

        // Remove (must(r)+ \ red(must(r)+)
        encodeSet.removeIf(t -> transMinSet.contains(t) && !reduct.contains(t));
    }

    @Override
    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
        if(flag || negated) {
            return;
        }
        ExecutionAnalysis exec = ra.getTask().getAnalysisContext().get(ExecutionAnalysis.class);
        Map<Event,Set<Event>> byFirst = new HashMap<>();
        Map<Event,Set<Event>> bySecond = new HashMap<>();
        Queue<Tuple> queue = new LinkedList<>();
        Predicate<Tuple> processTuple = tuple -> {
            Event x = tuple.getFirst();
            Event y = tuple.getSecond();
            if(!byFirst.computeIfAbsent(x, k -> new HashSet<>()).add(y)) {
                return false;
            }
            bySecond.computeIfAbsent(y, k -> new HashSet<>()).add(x);
            byFirst.getOrDefault(y, Set.of()).stream()
            .filter(exec.isImplied(x,y) ? z -> true : z -> exec.isImplied(z,y))
            .forEach(z -> new Tuple(x,z));
            bySecond.getOrDefault(x, Set.of()).stream()
            .filter(exec.isImplied(y,x) ? w -> true : w -> exec.isImplied(w,x))
            .forEach(w -> new Tuple(w,y));
            return true;
        };
        Function<Set<Tuple>,Set<Tuple>> processAll = set -> {
            Set<Tuple> result = new HashSet<>();
            set.stream()
            .filter(processTuple)
            .map(Tuple::getInverse)
            .forEach(result::add);
            while(!queue.isEmpty()) {
                Tuple t = queue.remove();
                if(processTuple.test(t)) {
                    result.add(t.getInverse());
                }
            }
            return result;
        };
        buf.send(rel, processAll.apply(ra.getMinTupleSet(rel)), Set.of());
        obs.listen(rel, (dis, en) -> buf.send(rel, processAll.apply(en), Set.of()));
    }

    @Override
	public BooleanFormula consistent(WmmEncoder encoder) {
        SolverContext ctx = encoder.getSolverContext();
        VerificationTask task = encoder.getTask();
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
    	FormulaManager fmgr = ctx.getFormulaManager();
		BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();

        BooleanFormula enc = bmgr.makeTrue();
        BooleanFormula eventsInCycle = bmgr.makeFalse();
        //TODO only those that add to a cycle; consult the dependency graph from above
        TupleSet min = ra.getMinTupleSet(rel);
        TupleSet tuples = encoder.getActiveSet(rel);
        if(negated) {
        	// We use Boolean variables which guess the edges and nodes constituting the cycle. 
            for(Event e : tuples.stream().map(Tuple::getFirst).collect(Collectors.toSet())){
            	
            	eventsInCycle = bmgr.or(eventsInCycle, cycleVar(rel.getName(), e, ctx));
            	
            	BooleanFormula in = bmgr.makeFalse();
            	for(Tuple pre : concat(tuples.getBySecond(e), min.getBySecond(e))) {
            		in = bmgr.or(in, getSMTCycleVar(pre, task, ctx));
            	}
            	BooleanFormula out = bmgr.makeFalse();
            	for(Tuple post : concat(tuples.getByFirst(e), min.getByFirst(e))) {
            		out = bmgr.or(out, getSMTCycleVar(post, task, ctx));
            	}
            	// We ensure that for every event in the cycle, there should be at least one incoming 
            	// edge and at least one outgoing edge that are also in the cycle.
            	enc = bmgr.and(enc, bmgr.implication(cycleVar(rel.getName(), e, ctx), bmgr.and(in , out)));
            	
                for(Tuple tuple : concat(tuples,min)){
                    Event e1 = tuple.getFirst();
                    Event e2 = tuple.getSecond();
                    // If an edge is guessed to be in a cycle, the edge must belong to relation, 
                    // and both events must also be guessed to be on the cycle.
                    enc = bmgr.and(enc, bmgr.implication(getSMTCycleVar(tuple, task, ctx),
                    		bmgr.and(rel.getSMTVar(tuple, task, ctx), cycleVar(rel.getName(), e1, ctx), cycleVar(rel.getName(), e2, ctx))));
                }
            }
            // A cycle exists if there is an event in the cycle.
            enc = bmgr.and(enc, eventsInCycle);
        } else {
            for(Tuple tuple : concat(tuples,min)){
                Event e1 = tuple.getFirst();
                Event e2 = tuple.getSecond();
    			enc = bmgr.and(enc, bmgr.implication(rel.getSMTVar(tuple, task, ctx),
    									imgr.lessThan(
    											Utils.intVar(rel.getName(), e1, ctx), 
    											Utils.intVar(rel.getName(), e2, ctx))));
            }        	
        }        
        return enc;
    }

    @Override
    public String toString() {
        return (negated ? "~" : "") + "acyclic " + rel.getName();
    }

    private BooleanFormula getSMTCycleVar(Tuple edge, VerificationTask task, SolverContext ctx) {
        return !task.getAnalysisContext().get(RelationAnalysis.class).getMaxTupleSet(rel).contains(edge) ?
                ctx.getFormulaManager().getBooleanFormulaManager().makeFalse() :
                edge(getName() + "-cycle", edge.getFirst(), edge.getSecond(), ctx);
    }
}