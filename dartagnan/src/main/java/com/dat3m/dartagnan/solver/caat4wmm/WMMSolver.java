package com.dat3m.dartagnan.solver.caat4wmm;


import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    This is our domain-specific bridging component that specializes the CAATSolver to the WMM setting.
*/
public class WMMSolver {

    private final ExecutionGraph executionGraph;
    private final ExecutionModel executionModel;
    private final CAATSolver solver;
    private final CoreReasoner reasoner;

    private WMMSolver(VerificationTask task, Context analysisContext, EdgeManager manager, ExecutionModel m) {
        analysisContext.requires(RelationAnalysis.class);
        this.executionGraph = new ExecutionGraph(task, true);
        this.executionModel = m;
        manager.init(executionModel, executionGraph);
        this.reasoner = new CoreReasoner(task, analysisContext, executionGraph, manager);
        this.solver = CAATSolver.create(manager, manager.transformCAATRelations(executionGraph));
    }

    public static WMMSolver fromConfig(VerificationTask task, Context analysisContext, EdgeManager manager, Configuration config) throws InvalidConfigurationException {
        return new WMMSolver(task, analysisContext, manager, ExecutionModel.fromConfig(task, config));
    }

    public ExecutionModel getExecution() {
        return executionModel;
    }

    public ExecutionGraph getExecutionGraph() {
        return executionGraph;
    }

    public Result check(Model model, SolverContext ctx) {
        // ============ Extract ExecutionModel ==============
        long curTime = System.currentTimeMillis();
        executionModel.initialize(model, ctx);
        executionGraph.initializeFromModel(executionModel);
        long extractTime = System.currentTimeMillis() - curTime;

        // ============== Run the CAATSolver ==============
        CAATSolver.Result caatResult = solver.check(executionGraph.getCAATModel(), this::hasStaticPresence);
        Result result = Result.fromCAATResult(caatResult);
        Statistics stats = result.stats;
        stats.modelExtractionTime = extractTime;
        stats.modelSize = executionGraph.getDomain().size();

        if (result.getStatus() == CAATSolver.Status.INCONSISTENT) {
            // ============== Compute Core reasons ==============
            curTime = System.currentTimeMillis();
            List<Conjunction<CoreLiteral>> coreReasons = new ArrayList<>(caatResult.getBaseReasons().getNumberOfCubes());
            Set<RelationGraph> assumedCAATRelations = caatResult.getAssumedRelations();
            TupleSetMap edgesToBeEncoded = new TupleSetMap();
            for (Conjunction<CAATLiteral> baseReason : caatResult.getBaseReasons().getCubes()) {
                // TODO: handle edge cases (?)
                coreReasons.add(reasoner.toCoreReason(baseReason, assumedCAATRelations, edgesToBeEncoded));
            }
            result.dynamicallyCut = edgesToBeEncoded;
            stats.numComputedCoreReasons = coreReasons.size();
            result.coreReasons = new DNF<>(coreReasons);
            stats.numComputedReducedCoreReasons = result.coreReasons.getNumberOfCubes();
            stats.coreReasonComputationTime = System.currentTimeMillis() - curTime;
        }

        return result;
    }

    // ============= Callback =============

    private RelationGraph.Presence hasStaticPresence(RelationGraph relGraph, Edge edge) {
        Relation rel = executionGraph.getRelation(relGraph);
        Event e1 = executionGraph.getDomain().getObjectById(edge.getFirst()).getEvent();
        Event e2 = executionGraph.getDomain().getObjectById(edge.getSecond()).getEvent();
        Tuple tuple = new Tuple(e1, e2);
        TupleSet minSet = rel.getMinTupleSet();
        TupleSet maxSet = rel.getMaxTupleSet();
        if (minSet.contains(tuple)) {
            return RelationGraph.Presence.PRESENT;
        } else if (!maxSet.contains(tuple)) {
            return RelationGraph.Presence.ABSENT;
        } else {
            return RelationGraph.Presence.UNKNOWN;
        }
    }

    // ===================== Classes ======================

    public static class Result {
        private CAATSolver.Status status;
        private DNF<CoreLiteral> coreReasons;
        private Statistics stats;
        private TupleSetMap dynamicallyCut;

        public CAATSolver.Status getStatus() {
            return status;
        }

        public DNF<CoreLiteral> getCoreReasons() {
            return coreReasons;
        }

        public Statistics getStatistics() {
            return stats;
        }

        public TupleSetMap getDynamicallyCut() {
            return dynamicallyCut;
        }

        Result() {
            status = CAATSolver.Status.INCONCLUSIVE;
            coreReasons = DNF.FALSE();
        }

        static Result fromCAATResult(CAATSolver.Result caatResult) {
            Result result = new Result();
            result.status = caatResult.getStatus();
            result.stats = new Statistics();
            result.stats.caatStats = caatResult.getStatistics();

            return result;
        }

        @Override
        public String toString() {
            return status + "\n" +
                    coreReasons + "\n" +
                    stats;
        }
    }

    public static class Statistics {
        CAATSolver.Statistics caatStats;
        long modelExtractionTime;
        long coreReasonComputationTime;
        int modelSize;
        int numComputedCoreReasons;
        int numComputedReducedCoreReasons;

        public long getModelExtractionTime() {
            return modelExtractionTime;
        }

        public long getPopulationTime() {
            return caatStats.getPopulationTime();
        }

        public long getBaseReasonComputationTime() {
            return caatStats.getReasonComputationTime();
        }

        public long getCoreReasonComputationTime() {
            return coreReasonComputationTime;
        }

        public long getConsistencyCheckTime() {
            return caatStats.getConsistencyCheckTime();
        }

        public int getModelSize() {
            return modelSize;
        }

        public int getNumComputedBaseReasons() {
            return caatStats.getNumComputedReasons();
        }

        public int getNumComputedReducedBaseReasons() {
            return caatStats.getNumComputedReducedReasons();
        }

        public int getNumComputedCoreReasons() {
            return numComputedCoreReasons;
        }

        public int getNumComputedReducedCoreReasons() {
            return numComputedReducedCoreReasons;
        }

        public int getNumSkippedStaticEdges() { return caatStats.getSkippedEdges().getNumStaticEdges(); }
        public int getNumEdges() { return caatStats.getSkippedEdges().getNumEdges(); }
        public int getNumSkippedUnionEdges() { return caatStats.getSkippedEdges().getNumStaticUnions(); }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("Model extraction time(ms): ").append(getModelExtractionTime()).append("\n");
            str.append("Population time(ms): ").append(getPopulationTime()).append("\n");
            str.append("Consistency check time(ms): ").append(getConsistencyCheckTime()).append("\n");
            str.append("Base Reason computation time(ms): ").append(getBaseReasonComputationTime()).append("\n");
            str.append("Core Reason computation time(ms): ").append(getCoreReasonComputationTime()).append("\n");
            str.append("Model size (#events): ").append(getModelSize()).append("\n");
            str.append("#Computed reasons (base/core): ").append(getNumComputedBaseReasons())
                    .append("/").append(getNumComputedCoreReasons()).append("\n");
            str.append("#Computed reduced reasons (base/core): ").append(getNumComputedReducedBaseReasons())
                    .append("/").append(getNumComputedReducedCoreReasons()).append("\n");
            str.append("Number of skipped edges due to static presence: ").append(getNumSkippedStaticEdges()).append("/").append(getNumEdges()).append("\n");
            str.append("    of which coming from an union choice: ").append(getNumSkippedUnionEdges()).append("\n");
            return str.toString();
        }
    }

}
