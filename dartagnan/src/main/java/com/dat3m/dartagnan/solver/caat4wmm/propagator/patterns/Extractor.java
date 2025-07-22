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

import java.util.*;
import java.util.stream.Collectors;

public class Extractor {
    private final boolean tagOptimization = true;
    private final boolean coveredStaticOptimization = true;
    private final boolean locationApproximation = false;
    private final boolean allowNegation = true;
    private final int maxNodeNumber = 20;

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
        List<ViolationPattern> violationPatterns = new ArrayList<>();
        Set<Relation> usedRelations = new HashSet<>();
        for (Conjunction<CAATLiteral> cube : inconsistencyReasons.getCubes()) {
            ViolationPattern pattern = new ViolationPattern();
            violationPatterns.add(pattern);
            Map<Integer, ViolationPattern.Node> patternNodes = new HashMap<>();
            Set<Relation> currentUsedRelations = new HashSet<>();
            for (CAATLiteral lit : cube.getLiterals()) {
                if (lit instanceof EdgeLiteral edgeLit) {
                    Relation rel = caatExecutionGraph.getRelationGraphMap().inverse().get(edgeLit.getPredicate());
                    Relation baseRel = refinementModel.translateToBase(rel);

                    ViolationPattern.Node from = initAndGet(patternNodes, edgeLit.getData().getFirst(), pattern);
                    ViolationPattern.Node to = initAndGet(patternNodes, edgeLit.getData().getSecond(), pattern);

                    // additional compatibility checks
                    boolean isNegative = lit.isNegative();
                    if (!allowNegation && isNegative) {
                        rollback(violationPatterns, currentUsedRelations);
                        break;
                    }
                    boolean isStatic = false;
                    if (coveredStaticOptimization) {
                        isStatic = staticRelations.contains(baseRel);
                    }
                    if (!isStatic) {
                        currentUsedRelations.add(baseRel);
                    }

                    // TODO ????
                    if (patternExecutionGraph.getRelationGraph(baseRel) == null) {
                        rollback(violationPatterns, currentUsedRelations);
                        break;
                    }
                    // -----------------------

                    pattern.addRelationGraph(baseRel, patternExecutionGraph.getRelationGraph(baseRel));
                    pattern.addEdge(baseRel, from, to, isStatic, isNegative);
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

                if (patternNodes.size() > maxNodeNumber) {
                    rollback(violationPatterns, currentUsedRelations);
                    break;
                }


                // under-approximates the location relation by omitting the edge from a pattern if
                // (i) both events are writes (then they have to be coherence ordered)
                // (ii) the events are also connected by an fr-edge (by definition, they have to refer to the same location)
                // otherwise, the pattern is not used
                if (locationApproximation) { // not useful as patterns only contain loc if no other information is available
                    if (!approximateLocationEdges(pattern)) {
                        rollback(violationPatterns, currentUsedRelations);
                        break;
                    }
                } else { // do not allow location edges in settings where the relation is not cut from the mm
                    boolean locCut = patternExecutionGraph.getCutRelations().stream().anyMatch(rel -> rel.getNameOrTerm().contains("loc"));
                    boolean containsLoc = pattern.rel2Graph.keySet().stream().anyMatch(rel -> rel.getNameOrTerm().contains("loc"));
                    if (!locCut && containsLoc) {
                        rollback(violationPatterns, currentUsedRelations);
                        break;
                    }
                }
            }
            for (int i = 0; i < violationPatterns.size() - 1; i++) {
                if (violationPatterns.get(violationPatterns.size() - 1).matchPattern(violationPatterns.get(i)) != null) {
                    rollback(violationPatterns, currentUsedRelations);
                }
            }
            usedRelations.addAll(currentUsedRelations);
        }
        if (!violationPatterns.isEmpty()) {
            if (coveredStaticOptimization && !tagOptimization) {
                violationPatterns = violationPatterns.stream().filter(ViolationPattern::newValidateStaticCoverage).collect(Collectors.toList());
            }
            if (tagOptimization) {
                violationPatterns.forEach(p -> p.findShortcutEdges(patternExecutionGraph.getExecutedSet()));
            }
            propagator.addPatterns(violationPatterns, usedRelations);
        }
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
