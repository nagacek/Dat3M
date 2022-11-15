package com.dat3m.dartagnan.solver.caat;


import com.dat3m.dartagnan.solver.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.misc.PathAlgorithm;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.Context;
import com.dat3m.dartagnan.solver.caat.reasoning.Reasoner;
import com.dat3m.dartagnan.solver.caat4wmm.EdgeManager;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.solver.caat.CAATSolver.Status.CONSISTENT;
import static com.dat3m.dartagnan.solver.caat.CAATSolver.Status.INCONSISTENT;


public class CAATSolver {

    // ======================================== Fields  ==============================================

    private final Reasoner reasoner;
    private final EdgeManager manager;

    // The statistics of the last call
    private Statistics stats;

    // ======================================== Construction ==============================================

    private CAATSolver(EdgeManager manager, Set<RelationGraph> relations) {
        this.reasoner = new Reasoner(relations);
        this.manager = manager;
    }

    public static CAATSolver create(EdgeManager manager, Set<RelationGraph> relations) {
        return new CAATSolver(manager, relations);
    }

    // ======================================== Accessors ==============================================

    public Reasoner getReasoner() { return reasoner; }

    public Statistics getStatistics() { return stats; }

    // ======================================== Solving ==============================================

    /*
        <check> assumes the following:
            - The CAATModel <model> has been initialized to some domain (<model.initializeToDomain>)
            - All base predicates are populated or will populate themselves.

        <check> will:
            - Populate the derived predicates in <model>
            - Check consistency of <model>
            - If applicable, compute base reasons of consistency violations
            - Return results about the computation
     */
    public Result check(CAATModel model) {
        Result result = new Result();
        stats = result.getStatistics();

        PathAlgorithm.ensureCapacity(model.getDomain().size());
        // ============== Populate derived predicates ===============
        long curTime = System.currentTimeMillis();
        model.populate();
        stats.populationTime = System.currentTimeMillis() - curTime;

        // ============== Check for inconsistencies ===============
        curTime = System.currentTimeMillis();
        List<Constraint> violatedConstraints = model.getViolatedConstraints();
        Status status = violatedConstraints.isEmpty() ? CONSISTENT : INCONSISTENT;
        result.setStatus(status);
        stats.consistencyCheckTime = System.currentTimeMillis() - curTime;

        if (status == INCONSISTENT) {
            // ============== Compute reasons ===============
            curTime = System.currentTimeMillis();
            Set<RelationGraph> assumedAsBaseRelations = new HashSet<>();
            result.setBaseReasons(computeInconsistencyReasons(violatedConstraints, assumedAsBaseRelations));
            result.assumeAsBaseRelations = assumedAsBaseRelations;
            stats.reasonComputationTime += (System.currentTimeMillis() - curTime);
            stats.complexityComputationTime = reasoner.getComplexityComputationTime();
        }

        return result;
    }

    // ======================================== Reason computation ==============================================

    private DNF<CAATLiteral> computeInconsistencyReasons(List<Constraint> violatedConstraints, Set<RelationGraph> toCut) {
        EdgeSetMap caatView = manager.initCAATView();
        List<Conjunction<CAATLiteral>> reasons = new ArrayList<>();
        for (Constraint constraint : violatedConstraints) {
            reasons.addAll(reasoner.computeViolationReasons(constraint, new Context(toCut, caatView)).getCubes());
        }
        stats.numComputedReasons += reasons.size();
        DNF<CAATLiteral> result = new DNF<>(reasons); // The conversion to DNF removes duplicates and dominated clauses
        stats.numComputedReducedReasons += result.getNumberOfCubes();

        return result;
    }

    // ======================================== Inner Classes ==============================================

    public static class Result {
        private Status status;
        private DNF<CAATLiteral> baseReasons;
        private final Statistics stats;
        private Set<RelationGraph> assumeAsBaseRelations;

        public Status getStatus() { return status; }
        public DNF<CAATLiteral> getBaseReasons() { return baseReasons; }
        public Statistics getStatistics() { return stats; }
        public Set<RelationGraph> getAssumedRelations() { return assumeAsBaseRelations; }

        void setStatus(Status status) { this.status = status; }
        void setBaseReasons(DNF<CAATLiteral> reasons) {
            this.baseReasons = reasons;
        }

        public Result() {
            stats = new Statistics();
            status = Status.INCONCLUSIVE;
            baseReasons = DNF.FALSE();
        }

        @Override
        public String toString() {
            return status + "\n" +
                    baseReasons + "\n" +
                    stats;
        }
    }

    public static class Statistics {
        long populationTime;
        long consistencyCheckTime;
        long reasonComputationTime;
        long complexityComputationTime;
        int numComputedReasons;
        int numComputedReducedReasons;

        public long getPopulationTime() { return populationTime; }
        public long getReasonComputationTime() { return reasonComputationTime; }
        public long getConsistencyCheckTime() { return consistencyCheckTime; }
        public long getComplexityComputationTime() { return complexityComputationTime; }
        public int getNumComputedReasons() { return numComputedReasons; }
        public int getNumComputedReducedReasons() { return numComputedReducedReasons; }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("Model construction time(ms): ").append(populationTime).append("\n");
            str.append("Consistency check time(ms): ").append(consistencyCheckTime).append("\n");
            str.append("Reason computation time(ms): ").append(reasonComputationTime).append("\n");
            str.append("Complexity computation time(ms): ").append(complexityComputationTime).append("\n");
            str.append("#Computed reasons: ").append(numComputedReasons).append("\n");
            str.append("#Computed reduced reasons: ").append(numComputedReducedReasons).append("\n");

            return str.toString();
        }
    }

    public enum Status {
        CONSISTENT, INCONSISTENT, INCONCLUSIVE;

        @Override
        public String toString() {
            switch (this) {
                case CONSISTENT:
                    return "Consistent";
                case INCONSISTENT:
                    return "Inconsistent";
                case INCONCLUSIVE:
                    return "Inconclusive";
                default:
                    throw new UnsupportedOperationException("The enum value " + this.name() + "is not known.");
            }
        }
    }

}
