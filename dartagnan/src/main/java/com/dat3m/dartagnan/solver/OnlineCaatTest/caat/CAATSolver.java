package com.dat3m.dartagnan.solver.OnlineCaatTest.caat;


import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.PathAlgorithm;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.reasoning.Reasoner;
import com.dat3m.dartagnan.utils.collections.Pair;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.solver.OnlineCaatTest.caat.CAATSolver.Status.CONSISTENT;
import static com.dat3m.dartagnan.solver.OnlineCaatTest.caat.CAATSolver.Status.INCONSISTENT;
import static com.dat3m.dartagnan.solver.OnlineCaatTest.caat.CAATSolver.Status.*;


public class CAATSolver {

    // ======================================== Fields  ==============================================

    private final Reasoner reasoner;

    // The statistics of the last call
    private Statistics stats;

    // ======================================== Construction ==============================================

    private CAATSolver() {
        this.reasoner = new Reasoner();
    }

    public static CAATSolver create() {
        return new CAATSolver();
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
        return check(model, false);
    }

    public Result check(CAATModel model, boolean populate) {
        Result result = new Result();
        stats = result.getStatistics();

        PathAlgorithm.ensureCapacity(model.getDomain().size());
        // ============== Populate derived predicates ===============
        long curTime = System.currentTimeMillis();
        if (populate) {
            model.populate();
            stats.populationTime = System.currentTimeMillis() - curTime;
        }

        // ============== Check for inconsistencies ===============
        curTime = System.currentTimeMillis();
        List<Constraint> violatedConstraints = model.getViolatedConstraints();
        Status status = violatedConstraints.isEmpty() ? CONSISTENT : POSSLIBLY_INCONSISTENT;
        stats.consistencyCheckTime = System.currentTimeMillis() - curTime;

        if (status == POSSLIBLY_INCONSISTENT) {
            // ============== Compute reasons ===============
            curTime = System.currentTimeMillis();
            result.setBaseReasons(computeInconsistencyReasons(violatedConstraints));
            result.setStatus(result.baseReasons.getCubes().isEmpty() ? CONSISTENT : INCONSISTENT);
            stats.reasonComputationTime += (System.currentTimeMillis() - curTime);

            // ============= Prepare Theory Propagation ==================
            List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> nearlyViolated = computeNearInconsistencyReasons(model.getConstraints());
            result.nearlyViolationReasons = nearlyViolated;
        } else {
            result.setStatus(CONSISTENT);
        }


        return result;
    }

    // ======================================== Reason computation ==============================================

    private DNF<CAATLiteral> computeInconsistencyReasons(List<Constraint> violatedConstraints) {
        List<Conjunction<CAATLiteral>> reasons = new ArrayList<>();
        //System.out.println("*********************");
        for (Constraint constraint : violatedConstraints) {
            var cubes = reasoner.computeViolationReasons(constraint).getCubes();
            reasons.addAll(cubes);
            if (cubes.isEmpty()) {
                int i = 5;
            }
            //System.out.println("WAS IN LOOP");
        }
        stats.numComputedReasons += reasons.size();
        if (reasons.isEmpty()) {
            int i = 5;
        }
        DNF<CAATLiteral> result = new DNF<>(reasons); // The conversion to DNF removes duplicates and dominated clauses
        stats.numComputedReducedReasons += result.getNumberOfCubes();

        return result;
    }

    private List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> computeNearInconsistencyReasons(Set<Constraint> constraints) {
        List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> reasons = new ArrayList<>();
        for (Constraint constraint : constraints) {
             reasons.addAll(reasoner.computeNearlyViolationResaons(constraint));
        }
        return reasons;
    }

    // ======================================== Inner Classes ==============================================

    public static class Result {
        private Status status;
        private DNF<CAATLiteral> baseReasons;
        private List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> nearlyViolationReasons;
        private final Statistics stats;

        public Status getStatus() { return status; }
        public DNF<CAATLiteral> getBaseReasons() { return baseReasons; }
        public List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> getNearlyViolationReasons() { return nearlyViolationReasons; }
        public Statistics getStatistics() { return stats; }

        void setStatus(Status status) { this.status = status; }
        void setBaseReasons(DNF<CAATLiteral> reasons) {
            this.baseReasons = reasons;
        }

        public Result() {
            stats = new Statistics();
            status = Status.INCONCLUSIVE;
            baseReasons = DNF.FALSE();
            nearlyViolationReasons = Collections.emptyList();
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
        int numComputedReasons;
        int numComputedReducedReasons;

        public long getPopulationTime() { return populationTime; }
        public long getReasonComputationTime() { return reasonComputationTime; }
        public long getConsistencyCheckTime() { return consistencyCheckTime; }
        public int getNumComputedReasons() { return numComputedReasons; }
        public int getNumComputedReducedReasons() { return numComputedReducedReasons; }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("Model construction time(ms): ").append(populationTime).append("\n");
            str.append("Consistency check time(ms): ").append(consistencyCheckTime).append("\n");
            str.append("Reason computation time(ms): ").append(reasonComputationTime).append("\n");
            str.append("#Computed reasons: ").append(numComputedReasons).append("\n");
            str.append("#Computed reduced reasons: ").append(numComputedReducedReasons).append("\n");

            return str.toString();
        }
    }

    public enum Status {
        CONSISTENT, POSSLIBLY_INCONSISTENT, INCONSISTENT, INCONCLUSIVE;

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
