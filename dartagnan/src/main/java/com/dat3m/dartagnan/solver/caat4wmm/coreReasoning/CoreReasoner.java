package com.dat3m.dartagnan.solver.caat4wmm.coreReasoning;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.ElementLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.EdgeManager;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.basePredicates.FenceGraph;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.base.stat.RelFencerel;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.wmm.relation.RelationNameRepository.*;

// The CoreReasoner transforms base reasons of the CAATSolver to core reason of the WMMSolver
public class CoreReasoner {

    private final ExecutionGraph executionGraph;
    private final Wmm memoryModel;
    private final ExecutionAnalysis exec;
    private final EdgeManager manager;

    public CoreReasoner(VerificationTask task, Context analysisContext, ExecutionGraph executionGraph, EdgeManager manager) {
        this.executionGraph = executionGraph;
        this.memoryModel = task.getMemoryModel();
        this.exec = analysisContext.requires(ExecutionAnalysis.class);
        this.manager = manager;
    }


    public Conjunction<CoreLiteral> toCoreReason(Conjunction<CAATLiteral> baseReason, Set<RelationGraph> dynamicallyCut, TupleSetMap notBase) {

        EventDomain domain = executionGraph.getDomain();

        List<CoreLiteral> coreReason = new ArrayList<>(baseReason.getSize());
        for (CAATLiteral lit : baseReason.getLiterals()) {
            if (lit instanceof ElementLiteral) {
                Event e = domain.getObjectById(((ElementLiteral) lit).getElement().getId()).getEvent();
                // We only have static tags, so all of them reduce to execution literals
                coreReason.add(new ExecLiteral(e, lit.isNegative())); if (lit.getName().equals("loc")) {
                    System.out.println(1);
                }
            } else {

                EdgeLiteral edgeLit = (EdgeLiteral) lit;
                Edge edge = edgeLit.getEdge();
                Event e1 = domain.getObjectById(edge.getFirst()).getEvent();
                Event e2 = domain.getObjectById(edge.getSecond()).getEvent();
                Tuple tuple = new Tuple(e1, e2);

                int uIndex = lit.getName().indexOf("_");
                String relName = uIndex < 0 ? lit.getName() : lit.getName().substring(0,uIndex);
                Relation rel = memoryModel.getRelation(relName);
                if (rel == null) {
                    System.out.println("DEBUG: " + relName);

                    System.out.println("DEBUG: " + lit.getName());
                }

                TupleSet minSet = rel.getMinTupleSet();
                TupleSet maxSet = rel.getMaxTupleSet();

                if (lit.isPositive() && minSet != null && minSet.contains(tuple)) {
                    // Statically present edges
                    addExecReason(tuple, coreReason);
                } else if (lit.isNegative() && maxSet != null && !maxSet.contains(tuple)) {
                    // Statically absent edges
                } else {
                    if (rel.getName().equals(RF) || rel.getName().equals(CO)) {
                        coreReason.add(new RelLiteral(rel.getName(), tuple, lit.isNegative()));
                    } else if (dynamicallyCut != null && dynamicallyCut.contains(executionGraph.getRelationGraph(rel))
                            && !manager.isEagerlyEncoded(rel, tuple)) { // new dynamically cut edge
                        coreReason.add(new RelLiteral(rel.getName(), tuple, lit.isNegative()));
                        TupleSet set = new TupleSet();
                        set.add(tuple);
                        notBase.merge(new TupleSetMap(rel, set));
                    } else if (manager.isEagerlyEncoded(rel, tuple)) { // old dynamically cut edge or intermediate edge
                        coreReason.add(new RelLiteral(rel.getName(), tuple, lit.isNegative()));
                    } else if (rel.getName().equals(LOC)) {
                        coreReason.add(new AddressLiteral(tuple, lit.isNegative()));
                    } else if (rel instanceof RelFencerel) {
                        // This is a special case since "fencerel(F) = po;[F];po".
                        // We should do this transformation directly on the Wmm to avoid this special reasoning
                        if (lit.isNegative()) {
                            throw new UnsupportedOperationException(String.format("FenceRel %s is not allowed on the rhs of differences.", rel));
                        }
                        addFenceReason(rel, edge, coreReason);
                    } else {
                        //TODO: Right now, we assume many relations like Data, Ctrl and Addr to be
                        // static.
                        if (lit.isNegative()) {
                            throw new UnsupportedOperationException(String.format("Negated literals of type %s are not supported.", rel));
                        }
                        addExecReason(tuple, coreReason);
                    }
                }
            }
        }

        minimize(coreReason);
        return new Conjunction<>(coreReason);
    }

    private void minimize(List<CoreLiteral> reason) {
        //TODO: Make sure that his is correct for exclusive events
        // Their execution variable can only be removed if it is contained in some
        // RelLiteral but not if it gets cf-implied!
        reason.removeIf( lit -> {
            if (!(lit instanceof ExecLiteral) || lit.isNegative()) {
                return false;
            }
            Event ev = ((ExecLiteral) lit).getData();
            return reason.stream().filter(e -> e instanceof RelLiteral && e.isPositive())
                    .map(RelLiteral.class::cast)
                    .anyMatch(e -> exec.isImplied(e.getData().getFirst(), ev)
                            || exec.isImplied(e.getData().getSecond(), ev));

        });
    }

    private void addExecReason(Tuple edge, List<CoreLiteral> coreReasons) {
        Event e1 = edge.getFirst();
        Event e2 = edge.getSecond();

        if (e1.getCId() > e2.getCId()) {
            // Normalize edge direction
            Event temp = e1;
            e1 = e2;
            e2 = temp;
        }

        if (exec.isImplied(e1, e2)) {
            coreReasons.add(new ExecLiteral(e1));
        } else if (exec.isImplied(e2, e1)) {
            coreReasons.add(new ExecLiteral(e2));
        } else {
            coreReasons.add(new ExecLiteral(e1));
            coreReasons.add(new ExecLiteral(e2));
        }
    }

    private void addFenceReason(Relation rel, Edge edge, List<CoreLiteral> coreReasons) {
        FenceGraph fenceGraph = (FenceGraph) executionGraph.getRelationGraph(rel);
        EventDomain domain = executionGraph.getDomain();
        EventData e1 = domain.getObjectById(edge.getFirst());
        EventData e2 = domain.getObjectById(edge.getSecond());
        EventData f = fenceGraph.getNextFence(e1);

        coreReasons.add(new ExecLiteral(f.getEvent()));
        if (!exec.isImplied(f.getEvent(), e1.getEvent())) {
            coreReasons.add(new ExecLiteral(e1.getEvent()));
        }
        if (!exec.isImplied(f.getEvent(), e2.getEvent())) {
            coreReasons.add(new ExecLiteral(e2.getEvent()));
        }
    }
}
