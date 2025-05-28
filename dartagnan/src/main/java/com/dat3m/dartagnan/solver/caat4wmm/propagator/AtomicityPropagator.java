package com.dat3m.dartagnan.solver.caat4wmm.propagator;


import com.dat3m.dartagnan.encoding.Decoder;
import com.dat3m.dartagnan.encoding.EdgeInfo;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.GenericDomain;
import com.dat3m.dartagnan.solver.caat.misc.EdgeSet;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.*;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.Joiner;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.utils.Pair;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;

public class AtomicityPropagator extends AbstractUserPropagator implements Extractor {
    private enum FUNCTIONALITY {
        TRACKING,
        JOIN,
        CONFLICT
    }
    private final FUNCTIONALITY functionality = FUNCTIONALITY.CONFLICT;

    private enum OPTIMIZATION {
        NONE,
        STATIC
    }
    private final OPTIMIZATION optimization = OPTIMIZATION.STATIC;

    private final RefinementModel refinementModel;
    private final EncodingContext encodingContext;
    private final ExecutionGraph executionGraph;
    private final PropagatorExecutionGraph propExecutionGraph;
    private final Decoder decoder;
    private CoreReasoner reasoner;
    private Refiner refiner;
    private final RelationAnalysis ra;

    private final GenericDomain<Event> domain;
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final List<BooleanFormula[]> retentionConflicts = new ArrayList<>();

    private final Set<Relation> staticRelations = new HashSet<>();
    private final Set<Relation> trackedRelations = new HashSet<>();
    private final Set<Relation> allRelations = new HashSet<>();
    private final Map<Relation, EdgeSet> staticEdges;

    private final ViolationPattern pattern;
    private final Joiner joiner;

    // ----- stats -----
    private int patternCount = 0;
    private long joinTime = 0;
    private long patternTime = 0;
    private int attempts = 0;
    /*private int numRegistered = 0;
    private int numRF = 0;
    private int numCO = 0;*/


    private boolean isFirst = true;

    private final boolean debug = false;

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


        refinementModel.getOriginalModel().getRelations().stream()
                .filter(r -> r.getNameOrTerm().equals("rf") || r.getNameOrTerm().equals("co") ||
                        r.getNameOrTerm().equals("rmw") || r.getNameOrTerm().equals("ext"))
                .forEach(allRelations::add);
        RelationAnalysis ra = analysisContext.requires(RelationAnalysis.class);
        this.ra = ra;
        if (optimization.ordinal() >= OPTIMIZATION.STATIC.ordinal()) {
            this.staticEdges = translateStaticEdges(ra);
        } else {
            trackedRelations.addAll(allRelations);
            this.staticEdges = Collections.emptyMap();
        }
        pattern = new ViolationPattern(trackedRelations, staticEdges.keySet());
        this.propExecutionGraph = new PropagatorExecutionGraph(eventDomain, allRelations, executionGraph.getCutRelations());
        this.reasoner = new CoreReasoner(analysisContext, propExecutionGraph);
        joiner = new Joiner(propExecutionGraph, staticEdges);

        // TODO: populate static graphs - perhaps listen for exec as well? => is now handled by onKnownValue
        // TODO: optional - precompute joins on them
        RelationAnalysis relAna = analysisContext.get(RelationAnalysis.class);

    }

    private Map<Relation, EdgeSet> translateStaticEdges(RelationAnalysis ra) {
        Map<Relation, EdgeSet> staticEdges = new HashMap<>();
        for (Relation rel : allRelations) {
            RelationAnalysis.Knowledge knowledge = ra.getKnowledge(rel);
            if (!knowledge.getMaySet().isEmpty() && knowledge.getMaySet().size() == knowledge.getMustSet().size()) {
                EdgeSet edgeSet = EdgeSet.from(domain, knowledge.getMustSet());
                staticEdges.put(rel, edgeSet);
                staticRelations.add(rel);
            } else {
                trackedRelations.add(rel);
            }
        }
        return staticEdges;
    }

    @Override
    public void initializeWithBackend(PropagatorBackend backend) {
        super.initializeWithBackend(backend);
        getBackend().notifyOnKnownValue();

        for (Relation rel : allRelations) {
            if (staticRelations.contains(rel)) {
                continue;
            }
            EventGraph maySet = ra.getKnowledge(rel).getMaySet();
            for (Event x : maySet.getDomain()) {
                for (Event y : maySet.getRange(x)) {
                    /*if (rel.getNameOrTerm().equals("co")) {
                        numCO++;
                    }
                    if (rel.getNameOrTerm().equals("rf")) {
                        numRF++;
                    }
                    numRegistered++;*/
                    decoder.registerEdge(refinementModel.translateToBase(rel), x, y);
                }
            }
        }
        for (BooleanFormula expr : decoder.getDecodableFormulas()) {
            getBackend().registerExpression(expr);
        }
        /*System.out.println("Rf: " + numRF);
        System.out.println("Co: " + numCO);
        System.out.println(numRegistered);*/
    }


    @Override
    public void onKnownValue(BooleanFormula expr, boolean value) {
        /*if (value) {
            System.out.println("                                  " + expr);
        }*/
        if (!isFirst) {
            return;
        }
        knownValues.push(expr);
        partialModel.put(expr, value);
        if (value) {
            Decoder.Info info = decoder.decode(expr);
            if (functionality.ordinal() >= FUNCTIONALITY.TRACKING.ordinal()) {
                Collection<Pair<Relation, Edge>> newEdges = addToRelationGraphs(info);

                if (functionality.ordinal() >= FUNCTIONALITY.JOIN.ordinal()) {
                    matchAndPropagateConflicts(newEdges);
                }
            }
        }
    }

    private Collection<Pair<Relation, Edge>> addToRelationGraphs(Decoder.Info info) {
        List<Pair<Relation, Edge>> newEdges = new ArrayList<>(info.edges().size());
        for (EdgeInfo edgeInfo : info.edges()) {
            Relation originalRelation = refinementModel.translateToOriginal(edgeInfo.relation());
            final SimpleGraph graph = (SimpleGraph)propExecutionGraph.getRelationGraph(originalRelation);
            if (graph != null) {
                int id1 = domain.getId(edgeInfo.source());
                int id2 = domain.getId(edgeInfo.target());
                Edge edge = new Edge(id1, id2, backtrackPoints.size(), 0);
                graph.add(edge);
                newEdges.add(new Pair<>(originalRelation, edge));
            }
        }
        return newEdges;
    }

    private void matchAndPropagateConflicts(Collection<Pair<Relation, Edge>> edges) {
        for (Pair<Relation, Edge> pair : edges) {
            if (trackedRelations.contains(pair.first)) {
                long curTime = System.currentTimeMillis();
                List<int[]> substitutions = joiner.join(pattern, pair.second, pair.first);
                attempts++;
                joinTime += System.currentTimeMillis() - curTime;
                if (!substitutions.isEmpty()) {
                    /*// TODO: check for match starting from edge co(3, 2) (there have not been any so far)
                    // debug
                        System.out.println("P: " + pair.first.getNameOrTerm() + pair.second + " found:");
                        for (var subs : substitutions) {
                            System.out.println("    " + Arrays.toString(subs));
                        }
                    // ^^^^^^*/

                    curTime = System.currentTimeMillis();
                    DNF<CAATLiteral> baseReasons = pattern.applySubstitutions(substitutions, propExecutionGraph);
                    if (functionality.ordinal() >= FUNCTIONALITY.CONFLICT.ordinal()) {
                        Set<Conjunction<CoreLiteral>> coreReasons = reasoner.toCoreReasons(baseReasons, false);
                        for (Conjunction<CoreLiteral> coreReason : coreReasons) {
                            BooleanFormula[] conflict = refiner.encodeVariables(coreReason, encodingContext);
                            if (isFirst) {
                                getBackend().propagateConflict(conflict);
                                //System.out.println(coreReason);
                                //System.out.println(Arrays.toString(conflict));
                                isFirst = false;
                                patternCount++;
                            } else {
                                retentionConflicts.add(conflict);
                            }
                        }
                    }
                    patternTime += System.currentTimeMillis() - curTime;
                }
            }
        }
        //progressRetention();
    }

    private void progressRetention() {
        if (!isFirst && !retentionConflicts.isEmpty()) {
            long curTime = System.currentTimeMillis();
            BooleanFormula[] conflict = retentionConflicts.remove(0);
            getBackend().propagateConflict(conflict);
            patternCount++;
            patternTime += System.currentTimeMillis() - curTime;
        }
    }

    @Override
    public void onPop(int numLevels) {
        if (functionality.ordinal() < FUNCTIONALITY.TRACKING.ordinal()) {
            return;
        }
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

        isFirst = true;

        propExecutionGraph.backtrackTo(backtrackPoints.size());

        //System.out.println("### Did pop ###");
    }

    @Override
    public void onPush() {
        if (!isFirst) {
            //System.out.println("Useless conflict");
        }
        if (functionality.ordinal() < FUNCTIONALITY.TRACKING.ordinal()) {
            return;
        }
        backtrackPoints.push(knownValues.size());
        //System.out.println("*** Did push ***");
    }


    // TODO: extract new patterns from inconsistencies
    @Override
    public void extract(DNF<CAATLiteral> inconsistencyReasons) { }

    public String printStats() {
        StringBuilder str = new StringBuilder();
        str.append("#Applied patterns: ").append(patternCount).append("\n");
        str.append("#Attempts of matching: " + attempts).append("\n");
        str.append("Pattern matching time (ms): ").append(joinTime).append("\n");
        str.append("Substitution application time (ms): ").append(patternTime);

        return str.toString();
    }

}
