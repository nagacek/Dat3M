package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.ElementLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.GeneralExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.RefinementModel;
import com.dat3m.dartagnan.solver.caat4wmm.basePredicates.StaticWMMSet;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.PatternPropagator;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.wmm.Relation;

import javax.swing.text.html.parser.Parser;
import java.util.*;
import java.util.stream.Collectors;

public class Extractor {
    private final boolean tagOptimization = true;
    private final boolean coveredStaticOptimization = true;
    private final boolean locationApproximation = false;
    private final boolean allowNegation = true;
    private final int maxNodeNumber = 10;

    private final PatternPropagator propagator;
    private final PropagatorExecutionGraph patternExecutionGraph;
    private final GeneralExecutionGraph caatExecutionGraph;
    private final Set<Relation> staticRelations;
    private final RefinementModel refinementModel;

    public Extractor(PatternPropagator propagator, PropagatorExecutionGraph patternExecutionGraph, GeneralExecutionGraph caatExecutionGraph, RefinementModel refinementModel, Set<Relation> staticRelations) {
        this.propagator = propagator;
        this.patternExecutionGraph = patternExecutionGraph;
        this.caatExecutionGraph = caatExecutionGraph;
        this.staticRelations = staticRelations;
        this.refinementModel = refinementModel;
    }

    public void extract(DNF<CAATLiteral> inconsistencyReasons) {
        if (!propagator.hasPatternCapacityFor(new ViolationPattern())) {
            return;
        }
        List<ViolationPattern> violationPatterns = new ArrayList<>();
        Set<Relation> usedRelations = new HashSet<>();
        for (Conjunction<CAATLiteral> cube : inconsistencyReasons.getCubes()) {
            ViolationPattern pattern = new ViolationPattern();
            Map<Integer, ViolationPattern.Node> patternNodes = new HashMap<>();
            Set<Relation> currentUsedRelations = new HashSet<>();

            violationPatterns.add(pattern);
            for (CAATLiteral lit : cube.getLiterals()) {
                if (lit instanceof EdgeLiteral edgeLit) {
                    Relation rel = caatExecutionGraph.getRelationGraphMap().inverse().get(edgeLit.getPredicate());
                    Relation baseRel = refinementModel.translateToBase(rel);
                    boolean isNegative = lit.isNegative();

                    ViolationPattern.Node from = initAndGet(patternNodes, edgeLit.getData().getFirst(), pattern);
                    ViolationPattern.Node to = initAndGet(patternNodes, edgeLit.getData().getSecond(), pattern);

                    if (!createAndAddEdge(baseRel, from, to, isNegative, currentUsedRelations, violationPatterns)) {
                        rollback(violationPatterns, currentUsedRelations);
                        break;
                    }
                } else if (tagOptimization && lit instanceof ElementLiteral elementLit) {
                    if (elementLit.getPredicate() instanceof StaticWMMSet staticSetPred) {
                        ViolationPattern.Node node = initAndGet(patternNodes, elementLit.getData().getId(), pattern);

                        Filter filter = staticSetPred.getFilter();
                        SetPredicate set = patternExecutionGraph.getOrCreateSetPredicate(filter);
                        node.integrateSet(set);
                    } else {
                        final String errorMsg = String.format("Literals of type %s are not supported.", elementLit.getPredicate().getClass().getSimpleName());
                        throw new UnsupportedOperationException(errorMsg);
                    }
                }
            }
            if (!checkPatternConformity(patternNodes, violationPatterns, currentUsedRelations)) {
                rollback(violationPatterns, currentUsedRelations);
                continue;
            }
            removeDuplicate(violationPatterns, currentUsedRelations);
            usedRelations.addAll(currentUsedRelations);
        }
        if (handleStaticEdges(violationPatterns)) {
            propagator.addPatterns(violationPatterns, usedRelations);
        }
    }

    public void extract(Collection<ParserPattern> patterns) {
        List<ViolationPattern> violationPatterns = new ArrayList<>();
        Set<Relation> usedRelations = new HashSet<>();
        for (ParserPattern parserPattern : patterns) {
            ViolationPattern pattern = new ViolationPattern();
            Map<Integer, ViolationPattern.Node> patternNodes = new HashMap<>();
            Set<Relation> currentUsedRelations = new HashSet<>();

            violationPatterns.add(pattern);
            for (ParserPattern.Edge parserEdge : parserPattern.edges()) {
                Relation baseRel = refinementModel.translateToBase(parserEdge.r());
                boolean isNegative = parserEdge.isNegative();

                ViolationPattern.Node from = initAndGet(patternNodes, parserEdge.n1().id(), pattern);
                ViolationPattern.Node to = initAndGet(patternNodes, parserEdge.n2().id(), pattern);

                if (!createAndAddEdge(baseRel, from, to, isNegative, currentUsedRelations, violationPatterns)) {
                    rollback(violationPatterns, currentUsedRelations);
                    break;
                }

                integrateSets(from, parserEdge.n1().filters());
                integrateSets(to, parserEdge.n2().filters());
            }
            if (!checkPatternConformity(patternNodes, violationPatterns, currentUsedRelations)) {
                rollback(violationPatterns, currentUsedRelations);
                continue;
            }
            removeDuplicate(violationPatterns, currentUsedRelations);
            usedRelations.addAll(currentUsedRelations);
        }
        if (handleStaticEdges(violationPatterns)) {
            propagator.addPatterns(violationPatterns, usedRelations);
        }
    }

    private void integrateSets(ViolationPattern.Node node, Collection<Filter> filters) {
        for (Filter filter : filters) {
            SetPredicate set;
            if (filter.getName() != null && filter.getName().equals("exec")) {
                set = patternExecutionGraph.getExecutedSet();
            } else {
                set = patternExecutionGraph.getOrCreateSetPredicate(filter);
            }
            node.integrateSet(set);
        }
    }

    private boolean createAndAddEdge(Relation rel, ViolationPattern.Node from, ViolationPattern.Node to, boolean isNegative, Set<Relation> usedRelations, List<ViolationPattern> violationPatterns) {
        // additional compatibility checks
        if (!allowNegation && isNegative) {
            return false;
        }
        boolean isStatic = false;
        if (coveredStaticOptimization) {
            isStatic = staticRelations.contains(rel);
        }
        if (!isStatic) {
            usedRelations.add(rel);
        }

        // TODO ????
        if (patternExecutionGraph.getRelationGraph(rel) == null) {
            return false;
        }
        // -----------------------

        ViolationPattern pattern = violationPatterns.get(violationPatterns.size() - 1);
        pattern.addRelationGraph(rel, patternExecutionGraph.getRelationGraph(rel));
        pattern.addEdge(rel, from, to, isStatic, isNegative);
        return true;
    }

    private boolean checkPatternConformity(Map<Integer, ViolationPattern.Node> patternNodes, List<ViolationPattern> violationPatterns, Set<Relation> usedRelations) {
        if (patternNodes.size() > maxNodeNumber) {
            return false;
        }

        ViolationPattern pattern = violationPatterns.get(violationPatterns.size() - 1);
        // under-approximates the location relation by omitting the edge from a pattern if
        // (i) both events are writes (then they have to be coherence ordered)
        // (ii) the events are also connected by an fr-edge (by definition, they have to refer to the same location)
        // otherwise, the pattern is not used
        if (locationApproximation) { // not useful as patterns only contain loc if no other information is available
            if (!approximateLocationEdges(pattern)) {
                return false;
            }
        } else { // do not allow location edges in settings where the relation is not cut from the mm
            boolean locCut = patternExecutionGraph.getCutRelations().stream().anyMatch(rel -> rel.getNameOrTerm().contains("loc"));
            boolean containsLoc = pattern.rel2Graph.keySet().stream().anyMatch(rel -> rel.getNameOrTerm().contains("loc"));
            if (!locCut && containsLoc) {
                return false;
            }
        }
        return true;
    }

    private void removeDuplicate(List<ViolationPattern> violationPatterns, Set<Relation> usedRelations) {
        for (int i = 0; i < violationPatterns.size() - 1; i++) {
            if (violationPatterns.get(violationPatterns.size() - 1).matchPattern(violationPatterns.get(i)) != null) {
                rollback(violationPatterns, usedRelations);
            }
        }
    }

    private boolean handleStaticEdges(List<ViolationPattern> violationPatterns) {
        if (!violationPatterns.isEmpty()) {
            if (coveredStaticOptimization && !tagOptimization) {
                violationPatterns = violationPatterns.stream().filter(ViolationPattern::newValidateStaticCoverage).collect(Collectors.toList());
            }
            if (tagOptimization) {
                violationPatterns.forEach(p -> p.findShortcutEdges(patternExecutionGraph.getExecutedSet()));
            }
        }
        return !violationPatterns.isEmpty();
    }

    private ViolationPattern.Node initAndGet(Map<Integer, ViolationPattern.Node> patternNodes, int eventId, ViolationPattern pattern) {
        if (!patternNodes.containsKey(eventId)) {
            patternNodes.put(eventId, pattern.addNode());
        }
        return patternNodes.get(eventId);
    }

    private void rollback(List<ViolationPattern> violationPatterns, Set<Relation> usedRelations) {
        violationPatterns.remove(violationPatterns.size() - 1);
        usedRelations.clear();
    }

    private boolean approximateLocationEdges(ViolationPattern pattern) {
        Relation locRel = patternExecutionGraph.getRelationGraphMap().keySet().stream().filter(rel -> rel.getNameOrTerm().contains("loc")).findAny().orElse(null);
        if (locRel == null) {
            return true;
        }

        List<ViolationPattern.Edge> locEdges = pattern.findEdgesByRelation(locRel);
        if (locEdges.isEmpty()) {
            return true;
        }

        // TODO: is that even useful?
        for (ViolationPattern.Edge edge : locEdges) {

        }
        return true;
    }
}
