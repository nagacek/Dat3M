package com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm;

import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.solver.OnlineCaatTest.Decoder;
import com.dat3m.dartagnan.solver.OnlineCaatTest.EdgeInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.PendingEdgeInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.CAATSolver;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.SolverDomain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.utils.collections.Pair;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.utils.BaseEdgeEncodingResult;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;
import com.dat3m.dartagnan.wmm.utils.RelationTuple;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.graph.mutable.MapEventGraph;
import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;
import java.util.stream.Collectors;


public class OnlineWMMSolver extends AbstractUserPropagator {

    private final ExecutionGraph executionGraph;
    private final CAATSolver solver;
    private final EncodingContext encodingContext;
    private final CoreReasoner reasoner;
    private final Decoder decoder;
    private final Refiner refiner;
    private final BooleanFormulaManager bmgr;
    private final RefinementModel refinementModel;
    private final WmmEncoder encoder;

    public OnlineWMMSolver(RefinementModel refinementModel, Context analysisContext, EncodingContext encCtx, WmmEncoder encoder) {
        this.refinementModel = refinementModel;
        this.encodingContext = encCtx;
        this.executionGraph = new ExecutionGraph(refinementModel, analysisContext.get(RelationAnalysis.class));
        this.reasoner = new CoreReasoner(analysisContext, executionGraph);
        this.decoder = new Decoder(encCtx, refinementModel);
        this.refiner = new Refiner(refinementModel);
        this.solver = CAATSolver.create();
        this.bmgr = encCtx.getFormulaManager().getBooleanFormulaManager();


        executionGraph.initializeToDomain(domain);


        this.encoder = encoder;
        this.relationMap = executionGraph.getRelationGraphMap().inverse();

        initTheroyPropagation();
    }

    //-------------------------------------------------------------------------------------------------------
    // Statistics
    private final Statistics totalStats = new Statistics();
    private final Statistics curStats = new Statistics();

    public Statistics getTotalStats() {
        return totalStats;
    }

    // ------------------------------------------------------------------------------------------------------
    // Online features

    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Set<BooleanFormula> trueValues = new HashSet<>();
    private final SolverDomain domain = new SolverDomain();
    private final Map<RelationGraph, List<Edge>> batchEdges = new HashMap<>();

    // ============== Theory Propagation ====================

    private final BiMap<RelationGraph, Relation> relationMap;
    Set<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> usedTheoryLemmas = new HashSet<>();
    Set<Pair<Set<BooleanFormula>, Set<CAATLiteral>>> usedTheoryPropagations = new HashSet<>();
    HashMap<Conjunction<CAATLiteral>, Set<Conjunction<CoreLiteral>>> undershootCoreReasonMap = new HashMap<>();
    HashMap<CAATLiteral, BooleanFormula> overshootRepresenterMap = new HashMap<>();

    Queue<Pair<Refiner.Conflict, Set<CAATLiteral>>> openTheoryPropagations = new ArrayDeque<>();

    boolean toggle = true;

    private void initTheroyPropagation() {
        Set<Relation> constrainedRelations = refinementModel.getOriginalModel().getAxioms()
                .stream().map(Axiom::getRelation).collect(Collectors.toSet());
        Map<Relation, EventGraph> activeSets = encoder.getEventGraphs(constrainedRelations);
        Map<RelationGraph, Set<Derivable>> activeGraphs = new HashMap<>();
        translateSets(activeSets, activeGraphs);
        activeGraphs.forEach((graph, set) -> graph.addBones(set.stream().map(Derivable::asBone).collect(Collectors.toSet())));
        executionGraph.getConstraints().forEach(c -> {
            CAATPredicate pred = c.getConstrainedPredicate();
            if (pred instanceof RelationGraph graph) {
                c.processActiveGraph(activeGraphs.get(graph));
            }
        });
    }


    private boolean progressTheoryPropagation() {
        if (!openTheoryPropagations.isEmpty()) {
            var propagation = openTheoryPropagations.peek();

            Set<BooleanFormula> assignmentList = new HashSet<>();
            assignmentList.addAll(propagation.getFirst().getVariables());
            Pair<Set<BooleanFormula>, Set<CAATLiteral>> pair = new Pair<>(assignmentList, propagation.getSecond());
            if (usedTheoryPropagations.contains(pair)) {
                openTheoryPropagations.poll();
                return progressTheoryPropagation();
            }

            for (BooleanFormula assignment : assignmentList) {
                if (!partialModel.containsKey(assignment)) {
                    openTheoryPropagations.poll();
                    return progressPropagation();
                }
            }
            BooleanFormula[] assignments = propagation.getFirst().getVariables().toArray(new BooleanFormula[0]);
            assignmentList.toArray(assignments);

            List<BooleanFormula> consequences = new ArrayList<>(propagation.getSecond().size());
            for (CAATLiteral overshoot : propagation.getSecond()) {

                if (!overshootRepresenterMap.containsKey(overshoot) && overshoot instanceof EdgeLiteral overshootEdge) {

                    RelationTuple tuple = toRelationTuple(overshootEdge);
                    BooleanFormula encoding = encoder.computeEdgeEncoding(tuple.rel(), tuple.first(), tuple.second());
                    getBackend().propagateConsequence(new BooleanFormula[0], encoding);

                    overshootRepresenterMap.put(overshootEdge, encoder.getInitialEncoding(tuple.rel(), tuple.first(), tuple.second()));
                    return true;
                }
                consequences.add(overshootRepresenterMap.get(overshoot));

            }

            openTheoryPropagations.poll();
            usedTheoryPropagations.add(pair);

            BooleanFormula propagationCons = bmgr.not(bmgr.and(consequences));
            BooleanFormula propagationPre = propagation.getFirst().toFormula(bmgr);


            // comment out for all the overhead and (almost) none of the benefits
            getBackend().propagateConsequence(new BooleanFormula[0],  bmgr.implication(propagationPre, propagationCons));


            return true;
        }
        return false;
    }

    // ======================================================



    @Override
    public void initializeWithBackend(PropagatorBackend backend) {
        super.initializeWithBackend(backend);
        getBackend().notifyOnKnownValue();
        getBackend().notifyOnFinalCheck();

        for (BooleanFormula formula : decoder.getDecodableFormulas()) {
            getBackend().registerExpression(formula);
        }
    }

    @Override
    public void onPush() {
        processBatchEdges(backtrackPoints.size());

        backtrackPoints.push(knownValues.size());
        domain.push();

        onlineCheck();

        long curTime = System.currentTimeMillis();
        executionGraph.getCAATModel().getHierarchy().onPush();
        curStats.modelExtractionTime += System.currentTimeMillis() - curTime;
        //System.out.println("PUSH " + backtrackPoints.size());

    }

    @Override
    public void onPop(int numLevels) {

        long curTime = System.currentTimeMillis();
        int oldDomainSize = domain.size();
        int oldTime = backtrackPoints.size();

        int backtrackTo = domain.resetElements(numLevels);
        if (backtrackTo < 0) {
            throw new RuntimeException("Cannot backtrack to negative time");
        }

        curStats.numBacktracks++;

        int popLevels = numLevels;
        int backtrackKnownValuesTo = knownValues.size();
        while (popLevels > 0) {
            popLevels--;
            backtrackKnownValuesTo = backtrackPoints.pop();
        }

        processBatchEdges(backtrackPoints.size());

        executionGraph.backtrackTo(backtrackPoints.size());
        backtrackEdgesTo(backtrackPoints.size());

        //System.out.println("\nPOP to time " + backtrackPoints.size() + " (event in domain: " + backtrackTo + ") from " + oldTime + " (" + oldDomainSize + ")");

        while (knownValues.size() > backtrackKnownValuesTo) {
            final BooleanFormula revertedAssignment = knownValues.pop();
            if (partialModel.remove(revertedAssignment) != null) {
                trueValues.remove(revertedAssignment);
            }
        }

        curStats.backtrackTime += System.currentTimeMillis() - curTime;

        // Validate is a debug tool only
        //executionGraph.validate(backtrackPoints.size());

        //System.out.printf("Backtracked %d levels to level %d\n", popLevels, backtrackPoints.size());
    }

    @Override
    public void onKnownValue(BooleanFormula expr, boolean value) {
        long curTime = System.currentTimeMillis();
        knownValues.push(expr);
        partialModel.put(expr, value);
        // moved to onPush/onPop for batch processing
        //progressEdges();
        if (value) {
            trueValues.add(expr);

            Decoder.Info info = decoder.decode(expr);

            //System.out.print("KNOW " + expr + ":" + knownValues.size() + "   ");

            for (Event event : info.events()) {
                if (event.hasTag(Tag.VISIBLE)) {
                    domain.addElement(event);
                }
            }

            for (EdgeInfo edge : info.edges()) {
                final Relation relInFullModel = refinementModel.translateToOriginal(edge.relation());
                final SimpleGraph graph = (SimpleGraph) executionGraph.getRelationGraph(relInFullModel);
                if (graph != null) {
                    int sourceId = domain.getId(edge.source());
                    int targetId = domain.getId(edge.target());
                    if (sourceId < 0 || targetId < 0) {
                        pendingEdges.add(new PendingEdgeInfo(edge.relation(), edge.source(), edge.target(), backtrackPoints.size(), -1));
                    } else {
                        Edge e = new Edge(sourceId, targetId).withTime(backtrackPoints.size());
                        List<Edge> graphBatch = batchEdges.computeIfAbsent(graph, g -> new ArrayList<>());
                        graphBatch.add(e);
                        // moved to onPush/onPop for batch processing
                        //executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e));
                    }
                }
            }
        }

        curStats.modelExtractionTime += System.currentTimeMillis() - curTime;
        curTime = System.currentTimeMillis();
        if(!progressPropagation()) {
            progressTheoryPropagation();
        }
        curStats.refinementTime += System.currentTimeMillis() - curTime;
    }

    private void processBatchEdges(int time) {
        for (var batch : batchEdges.entrySet()) {
            if (batch.getValue().get(0).getTime() > time) {
                break;
            }
            executionGraph.getCAATModel().getHierarchy().addAndPropagate(batch.getKey(), batch.getValue());
        }
        batchEdges.clear();
        progressEdges();
    }


    // Two-staged memory for pending edges
    // (no stage: edge is no longer in proposal for use, it is not relevant)
    // First stage: edge is not (yet) in use (inactive)
    // Second stage: edge is currently in use (active)

    private List<PendingEdgeInfo> pendingEdges = new ArrayList<>();
    private List<PendingEdgeInfo> usedEdges = new ArrayList<>();

    private void backtrackEdgesTo(int time) {
        ArrayList<PendingEdgeInfo> noRemove = new ArrayList<>(usedEdges.size());
        ArrayList<PendingEdgeInfo> toRemove = new ArrayList<>(usedEdges.size());
        for (PendingEdgeInfo info : usedEdges) {
            if (info.addTime() > time) {
                toRemove.add(info);
            } else {
                noRemove.add(info);
            }
        }
        usedEdges = noRemove;
        pendingEdges.addAll(toRemove);
        pendingEdges = pendingEdges.stream().filter(info -> info.deleteTime() <= time).collect(Collectors.toList());
    }

    private final Queue<Refiner.Conflict> openPropagations = new ArrayDeque<>();

    private void progressEdges() {
        List<PendingEdgeInfo> keep = new ArrayList<>(pendingEdges.size());
        for (PendingEdgeInfo edge : pendingEdges) {
            int sourceId = domain.getId(edge.source());
            int targetId = domain.getId(edge.target());
            if (sourceId >= 0 && targetId >= 0) {
                final Relation relInFullModel = refinementModel.translateToOriginal(edge.relation());
                final SimpleGraph graph = (SimpleGraph) executionGraph.getRelationGraph(relInFullModel);
                if (graph != null) {
                    Edge e = new Edge(sourceId, targetId).withTime(backtrackPoints.size());
                    executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e));

                    PendingEdgeInfo usedEdge = new PendingEdgeInfo(edge.relation(), edge.source(), edge.target(), edge.deleteTime(), backtrackPoints.size());
                    usedEdges.add(usedEdge);
                }
            } else {
                keep.add(edge);
            }
        }
        pendingEdges = keep;
    }
    private boolean progressPropagation() {
        if (!openPropagations.isEmpty()) {
            getBackend().propagateConsequence(new BooleanFormula[0], bmgr.not(openPropagations.poll().toFormula(bmgr)));
            return true;
        }
        return false;
    }

    private Result onlineCheck() {
        Result result = check();
        curStats.numChecks++;
        curStats.consistencyTime += result.caatStats.getConsistencyCheckTime();
        curStats.reasoningTime += result.caatStats.getReasonComputationTime();

        if (result.status == CAATSolver.Status.INCONSISTENT) {
            long curTime = System.currentTimeMillis();
            final List<Refiner.Conflict> conflicts = refiner.computeConflicts(result.coreReasons, encodingContext);
            if (conflicts.isEmpty()) {
                int i = 5;
            }
            assert !conflicts.isEmpty();
            boolean isFirst = true;
            for (Refiner.Conflict conflict : conflicts) {
                // The second part of the check is for symmetric clauses that are not yet conflicts.
                final boolean isConflict = isFirst &&
                        conflict.getVariables().stream().allMatch(partialModel::containsKey);
                if (isConflict) {
                    getBackend().propagateConflict(conflict.getVariables().toArray(new BooleanFormula[0]));
                    isFirst = false;
                } else {
                    openPropagations.add(conflict);
                }
            }
            assert !isFirst;
            curStats.refinementTime += System.currentTimeMillis() - curTime;
        } else {
            int i = 5;
        }

        // ================ Prepare Theory Propagation ======================
        if (result.nearlyViolationCoreReasons != null && !result.nearlyViolationCoreReasons.isEmpty()) {
            for (var vio : result.nearlyViolationCoreReasons) {
                Set<Conjunction<CoreLiteral>> undershoot = vio.getFirst();
                DNF<CoreLiteral> undershootDNF = new DNF<>(undershoot);
                Set<CAATLiteral> overshoots = vio.getSecond();

                List<Refiner.Conflict> undershootConflicts = refiner.computeConflicts(undershootDNF, encodingContext);

                if (undershootConflicts.size() > 1) {
                    int i = 5;
                }

                for (Refiner.Conflict singleUndershootConflict : undershootConflicts) {
                    openTheoryPropagations.add(new Pair<>(singleUndershootConflict, overshoots));
                }
            }
        }

        return result;
    }


    @Override
    public void onFinalCheck() {
        processBatchEdges(backtrackPoints.size());

        Result result = onlineCheck();

        totalStats.add(curStats);
        curStats.clear();

        if (result.status != CAATSolver.Status.INCONSISTENT) {
            System.out.println("#Overshoot edges: " + overshootRepresenterMap.size());
        }
    }


    private Result check() {
        // ============== Run the CAATSolver ==============
        CAATSolver.Result caatResult = solver.check(executionGraph.getCAATModel());
        Result result = Result.fromCAATResult(caatResult);

        if (result.getStatus() == CAATSolver.Status.INCONSISTENT) {
            // ============== Compute Core reasons ==============
            long curTime = System.currentTimeMillis();
            List<Conjunction<CoreLiteral>> coreReasons = new ArrayList<>(caatResult.getBaseReasons().getNumberOfCubes());
            for (Conjunction<CAATLiteral> baseReason : caatResult.getBaseReasons().getCubes()) {
                coreReasons.addAll(reasoner.toCoreReasons(baseReason));
            }
            if (coreReasons.isEmpty()) {
                int i = 5;
            }
            curStats.numComputedCoreReasons = coreReasons.size();
            result.coreReasons = new DNF<>(coreReasons);
            curStats.numComputedReducedCoreReasons = result.coreReasons.getNumberOfCubes();
            curStats.coreReasonComputationTime += System.currentTimeMillis() - curTime;
        }

        // ================= Prepare Theory Propagation ===================
        if (!caatResult.getNearlyViolationReasons().isEmpty()) {
            result.nearlyViolationCoreReasons = new ArrayList<>(caatResult.getNearlyViolationReasons().size());
            List<Set<Conjunction<CoreLiteral>>> nearlyCoreReasons = new ArrayList<>(caatResult.getNearlyViolationReasons().size());
            for (var vio : result.nearlyViolationReasons) {
                Set<Conjunction<CoreLiteral>> nearlyCoreReason = reasoner.toCoreReasons(vio.getFirst());

                if (!usedTheoryLemmas.contains(vio)) {
                    if (!vio.getFirst().getLiterals().isEmpty()) {
                        result.nearlyViolationCoreReasons.add(new Pair<>(nearlyCoreReason, vio.getSecond()));
                    }
                    usedTheoryLemmas.add(vio);
                }
            }
        }

        return result;
    }

    // -----------------------------------------------------------------------------------------------------
    // Helper Methods

    private DNF<CAATLiteral> getEncodingYield(EdgeLiteral from, BaseEdgeEncodingResult encodingResult) {
        RelationTuple relationTuple = toRelationTuple(from);
        return getEncodingYield(relationTuple, encodingResult, Set.of());
    }

    private DNF<CAATLiteral> getEncodingYield(RelationTuple from, BaseEdgeEncodingResult encodingResult, Set<RelationTuple> recurseSet) {
        boolean contained = false;
        DNF<CAATLiteral> encoding = null;

        if (executionGraph.getCutRelations().contains(from.rel())) {
            return new DNF<>(toCAATLiteral(from));
        }

        if (from.neg()) {
            return new DNF<>(toCAATLiteral(from));
        }

        Set<RelationTuple> and = encodingResult.and().get(from);
        if (and != null) {
            encoding = DNF.TRUE();
            for (RelationTuple tuple : and) {
                if (!recurseSet.contains(tuple)) {
                    encoding = encoding.and(getEncodingYield(tuple, encodingResult, Sets.union(recurseSet, Set.of(from))));
                    contained = true;
                }
            }
        }

        Set<RelationTuple> or = encodingResult.or().get(from);
        if (or != null) {
            encoding = DNF.FALSE();
            for (RelationTuple tuple : or) {
                if (!recurseSet.contains(tuple)) {
                    encoding = encoding.or(getEncodingYield(tuple, encodingResult, Sets.union(recurseSet, Set.of(from))));
                    contained = true;
                }
            }
        }

        Set<Set<RelationTuple>> orAnd = encodingResult.orAnd().get(from);
        if (orAnd != null) {
            encoding = DNF.FALSE();
            for (Set<RelationTuple> conjunction : orAnd) {
                DNF<CAATLiteral> inner = DNF.TRUE();
                for (RelationTuple tuple : conjunction) {
                    if (!recurseSet.contains(tuple)) {
                        inner = inner.and(getEncodingYield(tuple, encodingResult, Sets.union(recurseSet, Set.of(from))));
                    }
                }
                encoding = encoding.or(inner);
                contained = true;
            }
        }

        if (!contained) {
            return new DNF<>(toCAATLiteral(from));
        }

        return encoding;
    }

    private RelationTuple toRelationTuple(EdgeLiteral lit) {
        Relation rel = relationMap.get(lit.getPredicate());
        Edge edge = lit.getData();
        Event e1 = domain.weakGetObjectById(edge.getFirst());
        Event e2 = domain.weakGetObjectById(edge.getSecond());
        return new RelationTuple(rel, new Tuple(e1, e2));
    }

    private EdgeLiteral toCAATLiteral(RelationTuple relationTuple) {
        RelationGraph graph = executionGraph.getRelationGraph(relationTuple.rel());
        if (graph == null) {
            int i = 5;
        }
        Tuple tuple = relationTuple.tuple();
        domain.weakAddElement(tuple.first());
        domain.weakAddElement(tuple.second());
        int id1 = domain.weakGetId(tuple.first());
        int id2 = domain.weakGetId(tuple.second());
        return new EdgeLiteral(graph, new Edge(id1, id2), !relationTuple.neg());
    }


    private void translateSets(Map<Relation, EventGraph> rawSets, Map<RelationGraph, Set<Derivable>> newSets) {
        for (Relation r : rawSets.keySet()) {
            RelationGraph graph = executionGraph.getRelationGraph(r);
            if (graph == null) {
                graph = executionGraph.getRelationGraph(refinementModel.translateToOriginal(r));
            }
            newSets.putIfAbsent(graph, new HashSet<>());
            Set<Derivable> translatedEdges = newSets.get(graph);
            Map<Event, Set<Event>> containedEdges = rawSets.get(r).getOutMap();
            for (Event e1 : containedEdges.keySet()) {
                domain.weakAddElement(e1);
                for (Event e2 : containedEdges.get(e1)) {
                    domain.weakAddElement(e2);
                    int id1 = domain.weakGetId(e1);
                    int id2 = domain.weakGetId(e2);
                    Edge newEdge = new Edge(id1, id2);
                    translatedEdges.add(newEdge);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Classes

    public static class Result {
        private CAATSolver.Status status;
        private DNF<CoreLiteral> coreReasons;
        private CAATSolver.Statistics caatStats;
        private List<Pair<Conjunction<CAATLiteral>, Set<CAATLiteral>>> nearlyViolationReasons;
        private List<Pair<Set<Conjunction<CoreLiteral>>, Set<CAATLiteral>>> nearlyViolationCoreReasons;

        public CAATSolver.Status getStatus() { return status; }
        public DNF<CoreLiteral> getCoreReasons() { return coreReasons; }
        public CAATSolver.Statistics getCaatStatistics() { return caatStats; }

        Result() {
            status = CAATSolver.Status.INCONCLUSIVE;
            coreReasons = DNF.FALSE();
        }

        static Result fromCAATResult(CAATSolver.Result caatResult) {
            Result result = new Result();
            result.status = caatResult.getStatus();
            result.caatStats =  caatResult.getStatistics();
            result.nearlyViolationReasons = caatResult.getNearlyViolationReasons();

            return result;
        }

        @Override
        public String toString() {
            return status + "\n" +
                    coreReasons + "\n" +
                    caatStats;
        }
    }

    public static class Statistics {
        long modelExtractionTime;
        long coreReasonComputationTime;
        long backtrackTime;
        long refinementTime;
        long consistencyTime;
        long reasoningTime;
        long populationTime;

        int numComputedCoreReasons;
        int numComputedBaseReasons;
        int numComputedReducedCoreReasons;
        int numComputedReducedBaseReasons;
        int numBacktracks;
        int numChecks;


        public long getModelExtractionTime() { return modelExtractionTime; }
        public long getPopulationTime() { return populationTime; }
        public long getBaseReasonComputationTime() { return reasoningTime; }
        public long getCoreReasonComputationTime() { return coreReasonComputationTime; }
        public long getConsistencyCheckTime() { return consistencyTime; }
        public long getRefinementTime() { return refinementTime; }
        public long getBacktrackTime() { return backtrackTime; }
        //public int getModelSize() { return modelSize; }
        public int getNumComputedBaseReasons() { return numComputedBaseReasons; }
        public int getNumComputedReducedBaseReasons() { return numComputedReducedBaseReasons; }
        public int getNumComputedCoreReasons() { return numComputedCoreReasons; }
        public int getNumComputedReducedCoreReasons() { return numComputedReducedCoreReasons; }
        public int getNumBacktracks() { return numBacktracks; }
        public int getNumChecks() { return numChecks; }

        public void clear() {
            modelExtractionTime = 0;
            coreReasonComputationTime = 0;
            backtrackTime = 0;
            refinementTime = 0;
            consistencyTime = 0;
            reasoningTime = 0;
            populationTime = 0;

            numComputedCoreReasons = 0;
            numComputedBaseReasons = 0;
            numComputedReducedCoreReasons = 0;
            numComputedReducedBaseReasons = 0;
            numBacktracks = 0;
            numChecks = 0;
        }

        public void add (Statistics stats) {
            modelExtractionTime += stats.modelExtractionTime;
            coreReasonComputationTime += stats.coreReasonComputationTime;
            backtrackTime += stats.backtrackTime;
            refinementTime += stats.refinementTime;
            consistencyTime += stats.consistencyTime;
            reasoningTime += stats.reasoningTime;
            populationTime += stats.populationTime;

            numComputedCoreReasons += stats.numComputedCoreReasons;
            numComputedBaseReasons += stats.numComputedBaseReasons;
            numComputedReducedCoreReasons += stats.numComputedReducedCoreReasons;
            numComputedReducedBaseReasons += stats.numComputedReducedBaseReasons;
            numBacktracks += stats.numBacktracks;
            numChecks += stats.numChecks;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("Model extraction time(ms): ").append(getModelExtractionTime()).append("\n");
            str.append("Population time(ms): ").append(getPopulationTime()).append("\n");
            str.append("Consistency check time(ms): ").append(getConsistencyCheckTime()).append("\n");
            str.append("Base Reason computation time(ms): ").append(getBaseReasonComputationTime()).append("\n");
            str.append("Core Reason computation time(ms): ").append(getCoreReasonComputationTime()).append("\n");
            str.append("Refinement time(ms): ").append(getRefinementTime()).append("\n");
            str.append("Backtrack time(ms) (#Backtracks): ").append(getBacktrackTime()).append(" (").append(getNumBacktracks()).append(")\n");
            //str.append("Model size (#events): ").append(getModelSize()).append("\n");
            str.append("#Computed reasons (base/core): ").append(getNumComputedBaseReasons())
                    .append("/").append(getNumComputedCoreReasons()).append("\n");
            str.append("#Computed reduced reasons (base/core): ").append(getNumComputedReducedBaseReasons())
                    .append("/").append(getNumComputedReducedCoreReasons()).append("\n");
            str.append("#Checks: ").append(getNumChecks()).append("\n");
            return str.toString();
        }
    }


}
