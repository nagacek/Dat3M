package com.dat3m.dartagnan.solver.caat4wmm.propagator;


import com.dat3m.dartagnan.encoding.Decoder;
import com.dat3m.dartagnan.encoding.EdgeInfo;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.GenericDomain;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.Reasoner;
import com.dat3m.dartagnan.solver.caat4wmm.*;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.Joiner;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;

public class AtomicityPropagator extends AbstractUserPropagator {
    private final RefinementModel refinementModel;
    private final EncodingContext encodingContext;
    private final ExecutionGraph executionGraph;
    private final PropagatorExecutionGraph propExecutionGraph;
    private final Decoder decoder;
    private CoreReasoner reasoner;
    private Refiner refiner;

    private final GenericDomain<Event> domain;
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final List<BooleanFormula[]> retentionConflicts = new ArrayList<>();

    private final Set<Relation> staticRelations = new HashSet<>();
    private final Set<Relation> trackedRelations = new HashSet<>();

    private final ViolationPattern pattern;
    private final Joiner joiner;
    private int patternCount = 0;
    private long joinTime = 0;
    private long patternTime = 0;

    public AtomicityPropagator(RefinementModel refinementModel, EncodingContext encCtx, Context analysisContext, Refiner refiner, ExecutionModel model, ExecutionGraph executionGraph) {
        this.refinementModel = refinementModel;
        this.encodingContext = encCtx;
        this.decoder = new Decoder(encCtx, refinementModel);
        this.refiner = refiner;
        this.executionGraph = executionGraph;

        Collection<Event> events = encodingContext.getTask().getProgram().getThreadEvents();
        domain = new GenericDomain<>(events);
        EventDomainWrapper eventDomain = new EventDomainWrapper(model);
        eventDomain.initializeToDomain(domain);

        Set<Relation> allRelations = new HashSet<>();


        BiMap<Relation, SimpleGraph> relationMap = HashBiMap.create();
        refinementModel.getOriginalModel().getRelations().stream()
                .filter(r -> r.getNameOrTerm().equals("rf") || r.getNameOrTerm().equals("co"))
                .forEach(r -> {
                    //Relation origR = refinementModel.translateToOriginal(r);
                    trackedRelations.add(r);
                    allRelations.add(r);
                    //SimpleGraph graph = new SimpleGraph();
                    //graph.initializeToDomain(domain);
                    //relationMap.put(r, graph);
                });

        refinementModel.getOriginalModel().getRelations().stream()
                .filter(r -> r.getNameOrTerm().equals("rmw") || r.getNameOrTerm().equals("ext"))
                .forEach(r -> {
                    //Relation origR = refinementModel.translateToOriginal(r);
                    //staticRelations.add(r);
                    trackedRelations.add(r);
                    allRelations.add(r);
                    //SimpleGraph graph = new SimpleGraph();
                    //graph.initializeToDomain(domain);
                    //relationMap.put(r, graph);
                });

        //trackedRelations.forEach(refinementModel::translateToOriginal);
        //staticRelations.forEach(refinementModel::translateToOriginal);
        pattern = new ViolationPattern(trackedRelations, staticRelations);
        this.propExecutionGraph = new PropagatorExecutionGraph(eventDomain, allRelations, executionGraph.getCutRelations());
        this.reasoner = new CoreReasoner(analysisContext, propExecutionGraph);
        joiner = new Joiner(propExecutionGraph);

        // TODO: populate static graphs - perhaps listen for exec as well? => is now handled by onKnownValue
        // TODO: optional - precompute joins on them
        RelationAnalysis relAna = analysisContext.get(RelationAnalysis.class);

    }

    @Override
    public void initializeWithBackend(PropagatorBackend backend) {
        super.initializeWithBackend(backend);
        getBackend().notifyOnKnownValue();

        for (BooleanFormula tLiteral : decoder.getDecodableFormulas()) {
            Decoder.Info info = decoder.decode(tLiteral);
            if (info.edges().stream().anyMatch(i -> trackedRelations.contains(refinementModel.translateToOriginal(i.relation()))
                    || staticRelations.contains(refinementModel.translateToOriginal(i.relation())))) {
                getBackend().registerExpression(tLiteral);
            }
        }
    }

    @Override
    public void onKnownValue(BooleanFormula expr, boolean value) {
        knownValues.push(expr);
        partialModel.put(expr, value);
        if (value) {
            Decoder.Info info = decoder.decode(expr);


            boolean isFirst = true;
            for (EdgeInfo edgeInfo : info.edges()) {
                Relation originalRelation = refinementModel.translateToOriginal(edgeInfo.relation());
                final SimpleGraph graph = (SimpleGraph)propExecutionGraph.getRelationGraph(originalRelation);
                if (graph != null) {
                    int id1 = domain.getId(edgeInfo.source());
                    int id2 = domain.getId(edgeInfo.target());
                    Edge edge = new Edge(id1, id2, backtrackPoints.size(), 0);
                    graph.add(edge);

                    if (trackedRelations.contains(originalRelation)) {
                        long curTime = System.currentTimeMillis();
                        List<int[]> substitutions = joiner.join(pattern, edge, originalRelation);
                        joinTime += System.currentTimeMillis() - curTime;
                        if (!substitutions.isEmpty()) {
                            curTime = System.currentTimeMillis();
                            DNF<CAATLiteral> baseReasons = pattern.applySubstitutions(substitutions, propExecutionGraph);
                            Set<Conjunction<CoreLiteral>> coreReasons = reasoner.toCoreReasons(baseReasons, false);
                            for (Conjunction<CoreLiteral> coreReason : coreReasons) {
                                BooleanFormula[] conflict = refiner.encodeVariables(coreReason, encodingContext);
                                if (isFirst) {
                                    getBackend().propagateConflict(conflict);
                                    isFirst = false;
                                    patternCount++;
                                } else {
                                    retentionConflicts.add(conflict);
                                }
                            }
                            patternTime += System.currentTimeMillis() - curTime;
                        }
                    }
                }
            }
            if (!isFirst) {
                long curTime = System.currentTimeMillis();
                progressRetention();
                patternTime += System.currentTimeMillis() - curTime;
            }
        }
    }

    private void progressRetention() {
        if (!retentionConflicts.isEmpty()) {
            BooleanFormula[] conflict = retentionConflicts.remove(0);
            getBackend().propagateConflict(conflict);
            patternCount++;
        }
    }

    @Override
    public void onPop(int numLevels) {
        int popLevels = numLevels;
        int backtrackKnownValues = knownValues.size();
        while (popLevels > 0) {
            backtrackKnownValues = backtrackPoints.pop();
            popLevels--;
        }

        while (knownValues.size() > backtrackKnownValues) {
            BooleanFormula expr = knownValues.pop();
            partialModel.remove(expr);
        }

        propExecutionGraph.backtrackTo(backtrackPoints.size());
    }

    @Override
    public void onPush() {
        backtrackPoints.push(knownValues.size());
    }

    public String printStats() {
        StringBuilder str = new StringBuilder();
        str.append("#Applied patterns: ").append(patternCount).append("\n");
        str.append("Pattern matching time (ms): ").append(joinTime).append("\n");
        str.append("Substitution application time (ms): ").append(patternTime).append("\n");

        return str.toString();
    }
}
