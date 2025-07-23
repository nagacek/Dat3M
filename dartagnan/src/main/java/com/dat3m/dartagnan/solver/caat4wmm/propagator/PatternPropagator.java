package com.dat3m.dartagnan.solver.caat4wmm.propagator;


import com.dat3m.dartagnan.encoding.Decoder;
import com.dat3m.dartagnan.encoding.EdgeInfo;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.GenericDomain;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.*;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.Consequence;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.utils.Pair;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.utils.logic.Literal;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.difference;

public class PatternPropagator extends AbstractUserPropagator {
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

    private final EncodingContext encodingContext;
    private final PropagatorExecutionGraph propExecutionGraph;
    private final Decoder decoder;
    private CoreReasoner reasoner;
    private Refiner refiner;
    private final RelationAnalysis ra;

    private final GenericDomain<Event> domain;
    private final Map<BooleanFormula, Boolean> partialModel = new HashMap<>();
    private final Deque<BooleanFormula> knownValues = new ArrayDeque<>();
    private final Set<BooleanFormula> knownNegValuesSet = new HashSet<>();
    private final Deque<Integer> backtrackPoints = new ArrayDeque<>();
    private final List<BooleanFormula[]> retentionConflicts = new ArrayList<>();
    Collection<Pair<Relation, Edge>> newEdges = new ArrayList<>();

    private final Set<Relation> staticRelations = new HashSet<>();
    private final Set<Relation> trackedRelations = new HashSet<>();
    private final Set<Relation> allRelations = new HashSet<>();

    // Todo: find abstraction levels for ruling out patterns to match against
    private final Set<ViolationPattern> violationPatterns = new HashSet<>();
    private final HashMap<CoreLiteral, BooleanFormula> literalRepository = new HashMap<>();

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

    public PatternPropagator(Decoder decoder, EncodingContext encCtx, Context analysisContext, Refiner refiner, ExecutionModel model, Set<Relation> cutRelations, Set<Relation> relations, Set<Relation> setInducedRelations) {
        this.encodingContext = encCtx;
        this.decoder = decoder;
        this.refiner = refiner;
        this.allRelations.addAll(relations);

        Collection<Event> events = encodingContext.getTask().getProgram().getThreadEvents();
        domain = new GenericDomain<>(events);
        EventDomainWrapper eventDomain = new EventDomainWrapper(model);
        eventDomain.initializeToDomain(domain);

        RelationAnalysis ra = analysisContext.requires(RelationAnalysis.class);
        this.ra = ra;
        if (optimization.ordinal() >= OPTIMIZATION.STATIC.ordinal()) {
            findStaticRelations(ra);
        }
        this.propExecutionGraph = new PropagatorExecutionGraph(eventDomain, Sets.difference(allRelations, staticRelations), staticRelations, cutRelations, ra, setInducedRelations);
        this.reasoner = new CoreReasoner(analysisContext, propExecutionGraph);

        // TODO: populate static graphs - perhaps listen for exec as well? => is now handled by onKnownValue
        // TODO: optional - precompute joins on them
        RelationAnalysis relAna = analysisContext.get(RelationAnalysis.class);
    }

    // ----------------------------------------------------------------------
    // Initialization

    private void findStaticRelations(RelationAnalysis ra) {
        for (Relation rel : allRelations) {
            RelationAnalysis.Knowledge knowledge = ra.getKnowledge(rel);
            if (knowledge != null && !knowledge.getMaySet().isEmpty() && knowledge.getMaySet().size() == knowledge.getMustSet().size()) {
                staticRelations.add(rel);
            }
            /*if (rel.getNameOrTerm().equals("po") || rel.getNameOrTerm().equals("ext") || rel.getNameOrTerm().equals("rmw")) {
                staticRelations.add(rel);
            }*/
        }
    }

    @Override
    public void initializeWithBackend(PropagatorBackend backend) {
        super.initializeWithBackend(backend);
        getBackend().notifyOnKnownValue();

        registerRelations(Sets.difference(allRelations, staticRelations));
        decoder.extractExecutionInfo(encodingContext);
        for (BooleanFormula expr : decoder.getDecodableFormulas()) {
            getBackend().registerExpression(expr);
        }
    }

    private void registerRelations(Collection<Relation> toRegister) {
        Collection<BooleanFormula> formulasToRegister = new ArrayList<>();
        for (Relation rel : toRegister) {
            if (staticRelations.contains(rel)) {
                continue;
            }
            trackedRelations.add(rel);
            EventGraph maySet = ra.getKnowledge(rel).getMaySet();
            for (Event x : maySet.getDomain()) {
                for (Event y : maySet.getRange(x)) {
                    BooleanFormula encoding = decoder.registerEdge(rel, x, y);
                    if (encoding != null) {
                        formulasToRegister.add(encoding);
                    }
                }
            }
        }
    }


    // ----------------------------------------------------------------------
    // Solving

    @Override
    public void onKnownValue(BooleanFormula expr, boolean value) {
        if (!isFirst) {
            return;
        }
        knownValues.push(expr);
        partialModel.put(expr, value);
        if (value) {
            Decoder.Info info = decoder.decode(expr);
            if (functionality.ordinal() >= FUNCTIONALITY.TRACKING.ordinal()) {
                newEdges.addAll(addToRelationGraphs(info));
                if (functionality.ordinal() >= FUNCTIONALITY.JOIN.ordinal()) {
                    matchAndPropagateConflicts(newEdges, violationPatterns);
                }
            }
        } else {
            knownNegValuesSet.add(expr);
        }
    }

    private Collection<Pair<Relation, Edge>> addToRelationGraphs(Decoder.Info info) {
        List<Pair<Relation, Edge>> newEdges = new ArrayList<>(info.edges().size());
        for (EdgeInfo edgeInfo : info.edges()) {
            final SimpleGraph graph = (SimpleGraph)propExecutionGraph.getRelationGraph(edgeInfo.relation());
            if (graph != null) {
                int id1 = domain.getId(edgeInfo.source());
                int id2 = domain.getId(edgeInfo.target());
                Edge edge = new Edge(id1, id2, backtrackPoints.size(), 0);
                graph.add(edge);
                newEdges.add(new Pair<>(edgeInfo.relation(), edge));
            }
        }

        for (Event e : info.events()) {
            propExecutionGraph.addElement(domain.getId(e));
        }

        return newEdges;
    }

    private void matchAndPropagateConflicts(Collection<Pair<Relation, Edge>> edges, Collection<ViolationPattern> matchPatterns) {
        long curTime;

        for (Pair<Relation, Edge> pair : edges) {
            for (ViolationPattern pattern : matchPatterns) {
                Collection<Conjunction<CAATLiteral>> cubes = new ArrayList<>();
                Collection<Consequence> consequences = new ArrayList<>();
                //if (trackedRelations.contains(pair.first) && (pair.first.getNameOrTerm().contains("rf") || pair.first.getNameOrTerm().contains("co") )) {
                curTime = System.currentTimeMillis();
                final Edge edge = pair.second;
                final Relation relation = pair.first;
                final var joinCandidates = pattern.findEdgesByRelation(relation);
                final List<ViolationPattern.Match> matches = new ArrayList<>();
                for (var candidate : joinCandidates) {
                    matches.addAll(pattern.findMatches(candidate, edge.getFirst(), edge.getSecond()));
                    attempts++;
                }
                joinTime += System.currentTimeMillis() - curTime;

                for (ViolationPattern.Match match : matches) {
                    curTime = System.currentTimeMillis();
                    Consequence conseq = pattern.substituteWithMatch(match);
                    if (conseq.isConflict()) {
                        cubes.add(new Conjunction<>(conseq.getAssignments())); // Todo: think about whether one cube suffices
                    } else {
                        consequences.add(conseq);
                    }
                    patternTime += System.currentTimeMillis() - curTime;
                }

                if (cubes.isEmpty() && !consequences.isEmpty()) {
                    theoryPropagate(consequences);
                }
                //}
                curTime = System.currentTimeMillis();
                if (functionality.ordinal() >= FUNCTIONALITY.CONFLICT.ordinal()) {
                    DNF<CAATLiteral> patternConflicts = new DNF<>(cubes);
                    Set<Conjunction<CoreLiteral>> coreReasons = reasoner.toCoreReasons(patternConflicts, false);
                    for (Conjunction<CoreLiteral> coreReason : coreReasons) {
                        List<CoreLiteral> negatives = coreReason.getLiterals().stream().filter(Literal::isNegative).toList();
                        if (isFirst) {
                            if (!negatives.isEmpty()) {
                                handleNegativeConflict(coreReason, negatives);
                            } else {
                                propagateConflict(coreReason);
                            }
                            isFirst = false;
                        } /*else {
                            retentionConflicts.add(conflict);
                        }*/
                    }
                }
                patternTime += System.currentTimeMillis() - curTime;
            }
        }
        edges.clear();
        //progressRetention();
    }

    private void handleNegativeConflict(Conjunction<CoreLiteral> coreReason, List<CoreLiteral> negatives) {
        Collection<BooleanFormula> negativeFormulas = Arrays.asList(refiner.encodeVariables(new Conjunction<>(negatives), encodingContext));
        if (knownNegValuesSet.containsAll(negativeFormulas)) {
            propagateConflict(coreReason);
        } else {
            // TODO: theory propagation with remaining negative literals
        }
    }

    private void propagateConflict(Conjunction<CoreLiteral> coreReason) {
        BooleanFormula[] conflict = refiner.encodeVariables(coreReason, encodingContext);
        //System.out.println("Conf: " + Arrays.toString(conflict) + " (core reason: " + coreReason + ")");
        /*for (BooleanFormula f : conflict) {
            getBackend().registerExpression(f);
        }*/
        /* possible todo: if static coverage is not activated but tag optimization is, there are conflicts containing
                          formulas whose values have not been set yet */
        /*for (int i = 0; i < conflict.length; i++) {
            if (!knownValues.contains(conflict[i])) {
                System.out.println("Unknown conflict variable: " + conflict[i]);
                return;
            }
        }*/
        getBackend().propagateConflict(conflict);
        patternCount++;
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

    private void theoryPropagate(Consequence consequence) {
        BooleanFormulaManager bmgr = encodingContext.getBooleanFormulaManager();

        for (CAATLiteral consequenceLit : consequence.getConsequences()) {
            BooleanFormula litFormula = literalRepository.computeIfAbsent(toRelLiteral(consequenceLit, true), lit -> refiner.encode(lit, encodingContext));
            if (knownNegValuesSet.contains(litFormula) || bmgr.isFalse(litFormula)) {
                return;
            }
        }
        DNF<CAATLiteral> assignmentDnf = new DNF<>(new Conjunction<>(consequence.getAssignments()));
        Set<Conjunction<CoreLiteral>> coreAssignments = reasoner.toCoreReasons(assignmentDnf, false);
        assert coreAssignments.size() == 1;
        BooleanFormula[] assignments = refiner.encodeVariables(coreAssignments.stream().findAny().get(), encodingContext);
        List<BooleanFormula> consequenceLiterals = consequence.getConsequences().stream()
                .map(lit -> {
                    CoreLiteral coreLit = toRelLiteral(lit, true);
                    BooleanFormula literal = literalRepository.computeIfAbsent(coreLit, c -> refiner.encode(c, encodingContext));
                    if (lit.isNegative()) {
                        literal = bmgr.not(literal);
                    }
                    return literal;
                }).toList();


        BooleanFormula consequenceFormula = bmgr.not(bmgr.and(consequenceLiterals));
        if (!bmgr.isFalse(consequenceFormula)) {
            getBackend().propagateConsequence(assignments, consequenceFormula);
            //System.out.println("Propagated consequence: " + Arrays.toString(assignments) + " => " + consequenceFormula);
        }
    }

    private void theoryPropagate(Collection<Consequence> consequences) {
        for (Consequence consequence : consequences) {
            theoryPropagate(consequence);
        }
    }

    private RelLiteral toRelLiteral(CAATLiteral lit) { return toRelLiteral(lit, false); }

    private RelLiteral toRelLiteral(CAATLiteral lit, boolean onlyPositive) {
        assert lit instanceof EdgeLiteral;
        EdgeLiteral edgeLiteral = (EdgeLiteral) lit;
        Edge edge = edgeLiteral.getData();
        Event e1 = domain.getObjectById(edge.getFirst());
        Event e2 = domain.getObjectById(edge.getSecond());

        Relation rel = propExecutionGraph.getRelationGraphMap().inverse().get(edgeLiteral.getPredicate());

        if (onlyPositive) {
            return new RelLiteral(rel, e1, e2, true);
        }
        return new RelLiteral(rel, e1, e2, lit.isPositive());
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
            knownNegValuesSet.remove(expr);
        }

        isFirst = true;

        propExecutionGraph.backtrackTo(backtrackPoints.size());

        newEdges.clear();
    }

    @Override
    public void onPush() {
        if (!isFirst) {
            //System.out.println("Useless conflict");
        }
        //matchAndPropagateConflicts(newEdges, violationPatterns);
        if (functionality.ordinal() < FUNCTIONALITY.TRACKING.ordinal()) {
            return;
        }
        backtrackPoints.push(knownValues.size());
        propExecutionGraph.onPush();
    }

    // ----------------------------------------------------------------------
    // Extraction

    public void addPatterns(Collection<ViolationPattern> patterns, Set<Relation> usedRelations) {
        //System.out.println("+++++++++++++++++++++++++++++");
        for (ViolationPattern newPattern : patterns) {
            boolean hasMatch = false;
            for (ViolationPattern curPattern : violationPatterns) {
                if (curPattern.matchPattern(newPattern) != null) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                violationPatterns.add(newPattern);

                //System.out.println(newPattern.toString());
            }
        }
        List<ViolationPattern> newViolationPatterns = violationPatterns.stream().toList();
        if (newViolationPatterns.size() > 20) {
            newViolationPatterns = newViolationPatterns.subList(newViolationPatterns.size() - 11, newViolationPatterns.size() - 1);
        }
        violationPatterns.clear();
        violationPatterns.addAll(newViolationPatterns);
        violationPatterns.remove(null);
        Set<Relation> newRelations = Sets.difference(usedRelations, trackedRelations);
        registerRelations(newRelations); // it does not work to register expressions on-the-fly?
    }

    public PropagatorExecutionGraph getPropagatorExecutionGraph() {
        return propExecutionGraph;
    }

    public Set<Relation> getStaticRelations() {
        return staticRelations;
    }


    // ----------------------------------------------------------------------

    public String printStats() {
        StringBuilder str = new StringBuilder();
        str.append("#Applied patterns: ").append(patternCount).append("\n");
        str.append("#Attempts of matching: " + attempts).append("\n");
        str.append("Pattern matching time (ms): ").append(joinTime).append("\n");
        str.append("Substitution application time (ms): ").append(patternTime);

        return str.toString();
    }

}
