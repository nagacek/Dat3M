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
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;

public class AtomicityPropagator extends AbstractUserPropagator {
    private final RefinementModel refinementModel;
    private final EncodingContext encodingContext;
    private final ExecutionGraph executionGraph;
    private final Decoder decoder;
    private CoreReasoner reasoner;
    private Refiner refiner;

    private final GenericDomain<Event> domain;
    private EventDomainWrapper eventDomain;
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final List<BooleanFormula[]> retentionConflicts = new ArrayList<>();

    private final Set<Relation> staticRelations = new HashSet<>();
    private final Set<Relation> trackedRelations = new HashSet<>();
    private final Map<Relation, SimpleGraph> relationMap = new HashMap<>();

    private final ViolationPattern pattern;
    private final Joiner joiner;


    public AtomicityPropagator(RefinementModel refinementModel, EncodingContext encCtx, Context analysisContext, Refiner refiner, ExecutionModel model, ExecutionGraph executionGraph) {
        this.refinementModel = refinementModel;
        this.encodingContext = encCtx;
        this.decoder = new Decoder(encCtx, refinementModel);
        this.eventDomain = new EventDomainWrapper(model);
        this.reasoner = new CoreReasoner(analysisContext, executionGraph);
        this.refiner = refiner;
        this.executionGraph = executionGraph;

        Collection<Event> events = encodingContext.getTask().getProgram().getThreadEvents();
        domain = new GenericDomain<>(events);
        eventDomain.initializeToDomain(domain);

        refinementModel.computeBoundaryRelations().stream()
                .filter(r -> r.getNameOrTerm().equals("rf") || r.getNameOrTerm().equals("co"))
                .forEach(r -> {
                    trackedRelations.add(r);
                    SimpleGraph graph = new SimpleGraph();
                    graph.initializeToDomain(domain);
                    relationMap.put(r, graph);
                });

        refinementModel.computeBoundaryRelations().stream()
                .filter(r -> r.getNameOrTerm().equals("rmw") || r.getNameOrTerm().equals("ext"))
                .forEach(r -> {
                    staticRelations.add(r);
                    SimpleGraph graph = new SimpleGraph();
                    graph.initializeToDomain(domain);
                    relationMap.put(r, graph);
                });

        trackedRelations.forEach(refinementModel::translateToOriginal);
        staticRelations.forEach(refinementModel::translateToOriginal);
        joiner = new Joiner(relationMap);
        pattern = new ViolationPattern(trackedRelations, staticRelations);

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
            if (info.edges().stream().anyMatch(i -> trackedRelations.contains(i.relation())
                    || staticRelations.contains(i.relation()))) {
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
                final SimpleGraph graph = relationMap.get(edgeInfo.relation());
                if (graph != null) {
                    int id1 = domain.getId(edgeInfo.source());
                    int id2 = domain.getId(edgeInfo.target());
                    Edge edge = new Edge(id1, id2, backtrackPoints.size(), 0);
                    graph.add(edge);

                    if (trackedRelations.contains(edgeInfo.relation())) {
                        List<int[]> substitutions = joiner.join(pattern, edge, edgeInfo.relation());
                        if (!substitutions.isEmpty()) {
                            DNF<CAATLiteral> baseReasons = pattern.applySubstitutions(substitutions, executionGraph);
                            Set<Conjunction<CoreLiteral>> coreReasons = reasoner.toCoreReasons(baseReasons, eventDomain);
                            for (Conjunction<CoreLiteral> coreReason : coreReasons) {
                                BooleanFormula[] conflict = refiner.encodeVariables(coreReason, encodingContext);
                                if (isFirst) {
                                    getBackend().propagateConflict(conflict);
                                    isFirst = false;
                                } else {
                                    retentionConflicts.add(conflict);
                                }
                            }
                        }
                    }
                }
            }
            if (!isFirst) {
                progressRetention();
            }
        }
    }

    private void progressRetention() {
        if (!retentionConflicts.isEmpty()) {
            BooleanFormula[] conflict = retentionConflicts.remove(0);
            getBackend().propagateConflict(conflict);
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
    }

    @Override
    public void onPush() {
        backtrackPoints.push(knownValues.size());
    }
}
