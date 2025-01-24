package com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm;

import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.solver.OnlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.Decoder;
import com.dat3m.dartagnan.solver.OnlineCaatTest.EdgeInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.PendingEdgeInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.CAATSolver;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.SolverDomain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.derived.CompositionGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.derived.RecursiveGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;
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

    // used for (semi-) offline solving
    /*private final ExecutionGraph offlineExecutionGraph;
    private final CoreReasoner offlineReasoner;
    private final CAATSolver offlineSolver;*/

    public OnlineWMMSolver(RefinementModel refinementModel, Context analysisContext, EncodingContext encCtx, WmmEncoder encoder) {
        this.refinementModel = refinementModel;
        this.encodingContext = encCtx;
        this.executionGraph = new ExecutionGraph(refinementModel, analysisContext.get(RelationAnalysis.class));
        this.reasoner = new CoreReasoner(analysisContext, executionGraph);
        this.decoder = new Decoder(encCtx, refinementModel);
        this.refiner = new Refiner(refinementModel);
        this.solver = CAATSolver.create();
        this.bmgr = encCtx.getFormulaManager().getBooleanFormulaManager();
        this.analysisContext = analysisContext;

        this.activeSets = new HashMap<>();
        this.boundarySets = new HashMap<>();
        this.boundaryBones = new ArrayList<>();
        this.inactiveBones = new ArrayList<>();
        this.boundaryConditionalBones = new ArrayList<>();
        this.inactiveConditionalBones = new ArrayList<>();

        executionGraph.initializeToDomain(domain);
        translateSets(encoder.getActiveSets(), activeSets);
        translateSets(encoder.getInactiveBoundarySets(), boundarySets);
        makeSkeleton(analysisContext);
        executionGraph.initializeActiveSets(activeSets);

        // used for (semi-) offline solving
        /*this.offlineExecutionGraph = new ExecutionGraph(refinementModel);
        this.offlineReasoner = new CoreReasoner(analysisContext, offlineExecutionGraph);
        this.offlineSolver = CAATSolver.create();*/
    }

    //-------------------------------------------------------------------------------------------------------
    // Statistics
    private final Statistics totalStats = new Statistics();
    private final Statistics curStats = new Statistics();

    public Statistics getTotalStats() {
        return totalStats;
    }

    // ------------------------------------------------------------------------------------------------------
    // Static Skeleton

    private final Context analysisContext;
    private final Map<RelationGraph, Set<Derivable>> activeSets;
    private final Map<RelationGraph, Set<Derivable>> boundarySets;

    /*private final List<HashMap<RelationGraph, Set<BoneInfo>>> boundaryBones;
    private final List<HashMap<RelationGraph, Set<BoneInfo>>> inactiveBones;*/
    private final List<HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>>> boundaryBones;
    private final List<HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>>> inactiveBones;
    private final List<HashMap<RelationGraph, Set<BoneInfo>>> boundaryConditionalBones;
    private final List<HashMap<RelationGraph, Set<BoneInfo>>> inactiveConditionalBones;

    private void makeSkeleton(Context analysisContext) {
        RelationAnalysis ra = analysisContext.get(RelationAnalysis.class);
        final Set<Relation> relations = refinementModel.getOriginalModel().getRelations();
        for (Relation rel : relations) {
            RelationGraph graph = executionGraph.getRelationGraph(rel);

            if (graph == null) {
                int i = 5;
                continue;
                //Relation origRel = refinementModel.translateToOriginal(rel);
                //graph = executionGraph.getRelationGraph(origRel);
            }


            final EventGraph mustSet = ra.getKnowledge(rel).getMustSet();
            Map<Event, Set<Event>> outMap = mustSet.getOutMap();

            Set<Edge> bones = new HashSet<>();
            for (Event e1 : outMap.keySet()) {
                domain.weakAddElement(e1);
                int id1 = domain.weakGetId(e1);

                for (Event e2 : outMap.get(e1)) {
                    domain.weakAddElement(e2);
                    int id2 = domain.weakGetId(e2);

                    Edge edge = new Edge(id1, id2).withDerivationLength(-1);
                    Edge bone = edge.asBone();
                    bones.add(bone);
                    buildListeners(graph, edge);
                }
            }

            if (graph instanceof RecursiveGraph) {
                ((RelationGraph)graph.getDependencies().get(0)).addBones(bones);
            }
            graph.addBones(bones);
        }

        //BiMap<RelationGraph, Relation> relationMap = executionGraph.getRelationGraphMap().inverse();
        /*for (CAATPredicate pred : executionGraph.getCAATModel().getHierarchy().getPredicateList()) {
            if (pred instanceof RelationGraph) {
                RelationGraph graph = ((RelationGraph) pred);
                if (graph == null) {
                    int i = 5;
                }
                Relation rel = relationMap.get(graph);
                if (rel == null) {
                    int i = 5;
                }
                for (Edge edge : graph.weakEdges()) {
                    if (edge == null) {
                        int i = 5;
                    }
                    buildListeners(new GraphEdgeInfo(graph, edge.getFirst(), edge.getSecond()));
                }
            }
        }*/
        int i = 5;
    }

    private void buildListeners(RelationGraph graph, Edge edge) {
        int id1 = edge.getFirst();
        int id2 = edge.getSecond();

        Set<Derivable> boundarySet = boundarySets.get(graph);
        boolean isBoundary = boundarySet != null && boundarySet.contains(edge);
        BoneInfo boneInfo;
        BoneInfo compositionBoneInfo = handleCompositionListeners(graph, edge, isBoundary);
        if (compositionBoneInfo != null) {
            boneInfo = compositionBoneInfo;
        } else {
            boneInfo = new BoneInfo(edge, false, true);
        }

        //List<HashMap<RelationGraph, Set<BoneInfo>>> boneMaps;
        List<HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>>> boneMaps;
        if (isBoundary) {
            boneMaps = boundaryBones;
        } else {
            boneMaps = inactiveBones;
        }

        initListener(id1, boneMaps);
        //HashMap<RelationGraph, Set<BoneInfo>> boneMap = boneMaps.get(id1);
        HashMap<RelationGraph, Set<BoneInfo>> boneMap = boneMaps.get(id1).computeIfAbsent(id2, key -> new HashMap<>());
        boneMap.putIfAbsent(graph, new HashSet<>());
        Set<BoneInfo> boneSet = boneMap.get(graph);
        boneSet.add(boneInfo);

        initListener(id2, boneMaps);
        //boneMap = boneMaps.get(id2);
        boneMap = boneMaps.get(id2).computeIfAbsent(id1, key -> new HashMap<>());
        boneMap.putIfAbsent(graph, new HashSet<>());
        boneSet = boneMap.get(graph);
        boneSet.add(boneInfo);
    }


    private void initConditionalListener(int id, List<HashMap<RelationGraph, Set<BoneInfo>>> bones) {
        if (id < 0) {
            int i = 5;
        }
        if (id >= bones.size() || bones.get(id) == null) {
            while(bones.size() <= id) {
                bones.add(new HashMap<>());
            }
        }
    }
    private void initListener(int id, List<HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>>> bones) {
        if (id < 0) {
            int i = 5;
        }
        if (id >= bones.size() || bones.get(id) == null) {
            while (bones.size() <= id) {
                bones.add(new HashMap<>());
            }
        }
    }
    private BoneInfo handleCompositionListeners(RelationGraph graph, Edge edge, boolean isActive) {
        //Relation origRel = refinementModel.translateToOriginal(boneInfo.relation());
        if (graph instanceof CompositionGraph) {
            CompositionGraph compGraph = (CompositionGraph) graph;

            BiMap<RelationGraph, Relation> relationMap = executionGraph.getRelationGraphMap().inverse();

            Relation firstRelation = relationMap.get(compGraph.getFirst());
            Relation secondRelation = relationMap.get(compGraph.getSecond());

            /*Set<Integer> firstIntersect = compGraph.getFirst().weakEdgeStream(domain.getId(boneInfo.source()), EdgeDirection.OUTGOING)
                    .map(e -> e.getSecond()).collect(Collectors.toSet());
            Set<Integer> secondIntersect = compGraph.getSecond().weakEdgeStream(domain.getId(boneInfo.target()), EdgeDirection.INGOING)
                    .map(e -> e.getFirst()).collect(Collectors.toSet());*/

            Event source = domain.weakGetObjectById(edge.getFirst());
            Event target = domain.weakGetObjectById(edge.getSecond());

            RelationAnalysis ra = analysisContext.requires(RelationAnalysis.class);
            Set<Event> firstIntersect = ra.getKnowledge(firstRelation).getMustSet().getOutMap().get(source);
            Set<Event> secondIntersect = ra.getKnowledge(secondRelation).getMustSet().getInMap().get(target);
            Set<Event> intersect = Sets.intersection(firstIntersect, secondIntersect);

            if (intersect.contains(source) || intersect.contains(target)) {
                return new BoneInfo(edge, true, true);
            }

            List<HashMap<RelationGraph, Set<BoneInfo>>> boneMaps;
            if (isActive) {
                boneMaps = boundaryConditionalBones;
            } else {
                boneMaps = inactiveConditionalBones;
            }


            BoneInfo boneInfo = new BoneInfo(edge, false, true);
            Relation rel = relationMap.get(graph);
            for (Event e3 : intersect) {
                domain.weakAddElement(e3);
                int id = domain.weakGetId(e3);
                initConditionalListener(id, boneMaps);
                //HashMap<RelationGraph, Set<BoneInfo>> boneMap = boneMaps.get(id);
                HashMap<RelationGraph, Set<BoneInfo>> boneMap = boneMaps.get(id);
                boneMap.putIfAbsent(graph, new HashSet<>());
                Set<BoneInfo> boneSet = boneMap.get(graph);
                boneSet.add(boneInfo);
            }
            return boneInfo;
        }
        return null;
    }

    private void updateSkeleton(Event event) {
        int id = domain.getId(event);
        if (id >= boundaryBones.size() && id >= inactiveBones.size()) {
            return;
        }

        //HashMap<RelationGraph, Set<BoneInfo>> active = null;
        HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>> active = null;
        if (id < boundaryBones.size()) {
            active = boundaryBones.get(id);
        }
        //HashMap<RelationGraph, Set<BoneInfo>> inactive = null;
        HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>> inactive = null;
        if (id < inactiveBones.size()) {
            inactive = inactiveBones.get(id);
        }
        HashMap<RelationGraph, Set<BoneInfo>> activeConditional = null;
        if (id < boundaryConditionalBones.size()) {
            activeConditional = boundaryConditionalBones.get(id);
        }
        //HashMap<RelationGraph, Set<BoneInfo>> inactive = null;
        HashMap<RelationGraph, Set<BoneInfo>> inactiveConditional = null;
        if (id < inactiveConditionalBones.size()) {
            inactiveConditional = inactiveConditionalBones.get(id);
        }

        PredicateHierarchy hierarchy = executionGraph.getCAATModel().getHierarchy();
        propagateBoneInfos(active, hierarchy, id, PredicateHierarchy.PropagationMode.DEFER);
        propagateBoneInfos(inactive, hierarchy, id, PredicateHierarchy.PropagationMode.DELETE);
        propagateConditionalBoneInfos(activeConditional, id, PredicateHierarchy.PropagationMode.DEFER);
        propagateConditionalBoneInfos(inactiveConditional, id, PredicateHierarchy.PropagationMode.DELETE);

        /*for (CAATPredicate pred : hierarchy.getPredicateList()) {

            if (inactive != null && !inactive.isEmpty()) {
                Set<BoneInfo> nonPropagateBoneInfos = inactive.get(pred);
                if (nonPropagateBoneInfos != null) {
                    propagateBoneInfos(nonPropagateBoneInfos, pred, id, PredicateHierarchy.PropagationMode.DELETE);
                }
            }

            if (active != null && !active.isEmpty()) {
                Set<BoneInfo> propagateBoneInfos = active.get(pred);
                if (propagateBoneInfos != null) {
                    propagateBoneInfos(propagateBoneInfos, pred, id, PredicateHierarchy.PropagationMode.DEFER);
                }
            }
        }*/
        hierarchy.triggerDeferredPropagation();

        /*for (EdgeInfo info : boneInfo) {
            Relation rel = info.relation();
            RelationGraph graph = executionGraph.getRelationGraph(rel);

            if (graph.getName().equals("rfe ; fence")) {
                int i = 5;
            }

            if (graph instanceof UnionGraph || graph instanceof IntersectionGraph) {
                int i = 5;
            }

            if (graph == null) {
                rel = refinementModel.translateToOriginal(rel);
                graph = executionGraph.getRelationGraph(rel);
            }

            int id1 = domain.getId(info.source());
            int id2 = domain.getId(info.target());

            if (graph != null) {
                Edge e = new Edge(domain.weakGetId(info.source()), domain.weakGetId(info.target()));
                if (graph.checkBoneActivation(domain.getId(event), backtrackPoints.size(), , id1 >= 0 && id2 >= 0)) {
                    executionGraph.getCAATModel().getHierarchy().informListeners(graph, Collections.singleton(e));
                    if (graph instanceof RecursiveGraph) {
                        ((RelationGraph)graph.getDependencies().get(0)).checkBoneActivation(domain.getId(event), e, backtrackPoints.size(), id1 >= 0 && id2 >= 0, info.condition(), );
                        executionGraph.getCAATModel().getHierarchy().informListeners(((RelationGraph)graph.getDependencies().get(0)), Collections.singleton(e));
                    }
                    //System.out.println(info.relation() + "   (" + domain.getId(info.source()) + ", " + domain.getId(info.target()) + ")");
                } else if (id1 >= 0 && id2 >= 0) {
                    int i = 5;
                }
            } else {
                int i = 5;
            }
        }*/
    }

    /*private void propagateBoneInfos(Set<BoneInfo> infos, CAATPredicate pred, int id, PredicateHierarchy.PropagationMode mode) {
        if (infos != null) {
            RelationGraph graph = (RelationGraph) pred;
            Set<BoneInfo> activeEventInfos = infos.stream().map(info -> {
                Edge timeEdge = info.edge().withTime(backtrackPoints.size());
                return new BoneInfo(timeEdge, info.condition(),
                    domain.getObjectById(timeEdge.getFirst()) != null && domain.getObjectById(timeEdge.getSecond()) != null);
            })
                    .collect(Collectors.toSet());
            Set<Edge> propagationBones = graph.checkBoneActivation(id, backtrackPoints.size(), activeEventInfos);
            executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, propagationBones, mode);
        }
    }*/
    private void propagateBoneInfos(HashMap<Integer, HashMap<RelationGraph, Set<BoneInfo>>> bones, PredicateHierarchy hierarchy, int id, PredicateHierarchy.PropagationMode mode) {
        if (bones != null) {
            HashMap<RelationGraph, Set<BoneInfo>> activatedBones = new HashMap<>();
            for (var b : bones.entrySet()) {
                if (domain.getObjectById(b.getKey()) != null) {
                    b.getValue().forEach((graph, boneInfo) -> activatedBones.merge(graph, boneInfo, Sets::union));
                }
            }
            for (var b : activatedBones.entrySet()) {
                RelationGraph graph = b.getKey();
                Set<BoneInfo> boneInfos = b.getValue();
                Set<Edge> propagationBones = graph.checkBoneActivation(id, backtrackPoints.size(), boneInfos);
                executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, propagationBones, mode);
            }
        }
    }

    private void propagateConditionalBoneInfos(HashMap<RelationGraph, Set<BoneInfo>> bones, int id, PredicateHierarchy.PropagationMode mode) {
        if (bones != null) {
            for (var b : bones.entrySet()) {
                RelationGraph graph = b.getKey();
                Set<BoneInfo> boneInfos = b.getValue();
                Set<BoneInfo> compositionInfos = boneInfos.stream()
                        .map(bone -> new BoneInfo(bone.edge(), bone.condition(),
                                domain.getObjectById(bone.edge().getFirst()) != null && domain.getObjectById(bone.edge().getSecond()) != null))
                        .collect(Collectors.toSet());
                Set<Edge> propagationBones = graph.checkBoneActivation(id, backtrackPoints.size(), compositionInfos);
                executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, propagationBones, mode);
            }
        }
    }

    private void translateSets(Map<Relation, EventGraph> rawSets, Map<RelationGraph, Set<Derivable>> newSets) {
        for (Relation r : rawSets.keySet()) {
            RelationGraph graph = executionGraph.getRelationGraph(r);
            if (graph == null) {
                continue;
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
    // Online features

    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Set<BooleanFormula> trueValues = new HashSet<>();
    private final Map<RelationGraph, List<Edge>> batchEdges = new HashMap<>();
    private final SolverDomain domain = new SolverDomain();



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

        // Validate is a debug tool only
        //executionGraph.validate(backtrackPoints.size(), true);

        long curTime = System.currentTimeMillis();
        int oldDomainSize = domain.size();
        int oldTime = backtrackPoints.size();


        //System.out.println("BACKTRACK");
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
        //executionGraph.validate(backtrackPoints.size(), false);

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
                    //System.out.println(domain.getId(event));
                    updateSkeleton(event);
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
                        if (!activeSets.get(graph).contains(e)) {
                            PredicateHierarchy.PropagationMode mode = PredicateHierarchy.PropagationMode.DELETE;
                            executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e), mode);
                        }
                        //executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e));
                    }

                }
            }
        }
        // Validate is a debug tool only
        //executionGraph.validate(backtrackPoints.size(), false);

        curStats.modelExtractionTime += System.currentTimeMillis() - curTime;
        curTime = System.currentTimeMillis();
        progressPropagation();
        curStats.refinementTime += System.currentTimeMillis() - curTime;
    }

    private void processBatchEdges(int time) {
        for (var batch : batchEdges.entrySet()) {
            if (batch.getValue().get(0).getTime() > time) {
                break;
            }
            PredicateHierarchy.PropagationMode mode = PredicateHierarchy.PropagationMode.NORMAL;
            executionGraph.getCAATModel().getHierarchy().addAndPropagate(batch.getKey(), batch.getValue(), mode);
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
                    PredicateHierarchy.PropagationMode mode;
                    if (activeSets.get(graph).contains(e)) {
                        mode = PredicateHierarchy.PropagationMode.NORMAL;
                    } else {
                        mode = PredicateHierarchy.PropagationMode.DELETE;
                    }
                    executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e), mode);
                    //executionGraph.getCAATModel().getHierarchy().addAndPropagate(graph, Collections.singleton(e));

                    PendingEdgeInfo usedEdge = new PendingEdgeInfo(edge.relation(), edge.source(), edge.target(), edge.deleteTime(), backtrackPoints.size());
                    usedEdges.add(usedEdge);
                }
            } else {
                keep.add(edge);
            }
        }
        pendingEdges = keep;
    }
    private void progressPropagation() {
        if (!openPropagations.isEmpty()) {
            getBackend().propagateConsequence(new BooleanFormula[0], bmgr.not(openPropagations.poll().toFormula(bmgr)));
        }
    }

    private Result onlineCheck() {
        Result result = check();
        //Result offlineResult = checkOffline();
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
        }

        // compares online graph to (semi-) offline graph
        /*
        if (offlineResult.status != result.status) {
            System.out.println("\nRESULT SHOULD BE " + offlineResult.status + " BUT IS " + result.status);
        }
        HashMap<Relation, List<Edge>> wrongEdges = new HashMap<>();
        HashMap<Relation, List<Edge>> missingEdges = new HashMap<>();

        BiMap<Relation, RelationGraph> offlineGraphs = offlineExecutionGraph.getRelationGraphMap();
        BiMap<Relation, RelationGraph> graphs = executionGraph.getRelationGraphMap();

        for (Map.Entry<Relation, RelationGraph> relationTuple : graphs.entrySet()) {
            List<Edge> wrong = relationTuple.getValue().edgeStream().filter(e -> offlineGraphs.get(relationTuple.getKey()).edgeStream().noneMatch(offlineE -> e.equals(offlineE))).collect(Collectors.toList());
            if (!wrong.isEmpty()) {
                wrongEdges.put(relationTuple.getKey(), wrong);
            }
        }

        for (Map.Entry<Relation, RelationGraph> relationTuple : offlineGraphs.entrySet()) {
            List<Edge> missing = relationTuple.getValue().edgeStream().filter(offlineE -> graphs.get(relationTuple.getKey()).edgeStream().noneMatch(e -> offlineE.equals(e))).collect(Collectors.toList());
            if (!missing.isEmpty()) {
                missingEdges.put(relationTuple.getKey(), missing);
            }
        }

        List<Map.Entry> nonEmpty = graphs.entrySet().stream().filter(tuple -> !tuple.getValue().isEmpty()).collect(Collectors.toList());

        if (!wrongEdges.isEmpty() || !missingEdges.isEmpty()) {
            int j = 5;

            RelationAnalysis relAna = analysisContext.requires(RelationAnalysis.class);
            for (Relation rel : wrongEdges.keySet()) {
                RelationAnalysis.Knowledge knowledge = relAna.getKnowledge(rel);
                EventGraph mustGraph = knowledge.getMustSet();
                Map<Event, Set<Event>> outMap = mustGraph.getOutMap();
                for (Edge e : wrongEdges.get(rel)) {
                    if (!outMap.get(e.getFirst()).contains(e.getSecond())) {
                        int i = 5;
                    }
                }
            }
        }

        PredicateHierarchy offlinePredicateHierarchy = offlineExecutionGraph.getCAATModel().getHierarchy();
        List<CAATPredicate> offlinePredicates = offlinePredicateHierarchy.getPredicateList();
        PredicateHierarchy predicateHierarchy = executionGraph.getCAATModel().getHierarchy();
        List<CAATPredicate> predicates = predicateHierarchy.getPredicateList();
        */


        return result;
    }


    @Override
    public void onFinalCheck() {
        processBatchEdges(backtrackPoints.size());

        Result result = onlineCheck();

        totalStats.add(curStats);
        curStats.clear();

        if (result.status != CAATSolver.Status.INCONSISTENT) {
        }

    }

    // used for (semi-) offline solving only
    /*private void  initModel() {
        List<EdgeInfo> edges = new ArrayList<>();
        for (BooleanFormula assigned : trueValues) {
            Decoder.Info info = decoder.decode(assigned);
            edges.addAll(info.edges());
        }

        // Init domain
        offlineExecutionGraph.initializeToDomain(domain);

        // Setup base relation graphs
        for (EdgeInfo edge : edges) {
            final Relation relInFullModel = refinementModel.translateToOriginal(edge.relation());
            final SimpleGraph graph = (SimpleGraph) offlineExecutionGraph.getRelationGraph(relInFullModel);
            if (graph != null) {
                int sourceId = domain.getId(edge.source());
                int targetId = domain.getId(edge.target());
                int edgeTime = Math.max(sourceId, targetId);
                Edge e = (new Edge(sourceId, targetId)).withTime(edgeTime);
                graph.add(e);
            }
        }
    }*/

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

        return result;
    }
    // used for (semi-) offline solving only
    /*private Result checkOffline() {
        // ============ Extract CAAT base model ==============
        long curTime = System.currentTimeMillis();
        initModel();
        long extractTime = System.currentTimeMillis() - curTime;

        // ============== Run the CAATSolver ==============
        CAATSolver.Result caatResult = offlineSolver.check(offlineExecutionGraph.getCAATModel(), true);
        Result result = Result.fromCAATResult(caatResult);
        Statistics stats = result.stats;
        stats.modelExtractionTime = extractTime;
        stats.modelSize = offlineExecutionGraph.getDomain().size();

        if (result.getStatus() == CAATSolver.Status.INCONSISTENT) {
            // ============== Compute Core reasons ==============
            curTime = System.currentTimeMillis();
            List<Conjunction<CoreLiteral>> coreReasons = new ArrayList<>(caatResult.getBaseReasons().getNumberOfCubes());
            for (Conjunction<CAATLiteral> baseReason : caatResult.getBaseReasons().getCubes()) {
                coreReasons.addAll(offlineReasoner.toCoreReasons(baseReason));
            }
            stats.numComputedCoreReasons = coreReasons.size();
            result.coreReasons = new DNF<>(coreReasons);
            stats.numComputedReducedCoreReasons = result.coreReasons.getNumberOfCubes();
            stats.coreReasonComputationTime = System.currentTimeMillis() - curTime;
        }

        return result;
    }*/

    // ------------------------------------------------------------------------------------------------------
    // Classes

    public static class Result {
        private CAATSolver.Status status;
        private DNF<CoreLiteral> coreReasons;
        private CAATSolver.Statistics caatStats;

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
