package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.GeneralExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.RefinementModel;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.PatternPropagator;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.*;
import java.util.stream.Collectors;

public class Extractor {
    private final boolean coveredStaticOptimization = true;
    private final boolean allowNegation = false;
    private final int maxNodeNumber = 3;

    private final PatternPropagator propagator;
    private final GeneralExecutionGraph patternExecutionGraph;
    private final GeneralExecutionGraph caatExecutionGraph;
    private final Set<Relation> staticRelations;
    private final RefinementModel refinementModel;

    public Extractor(PatternPropagator propagator, GeneralExecutionGraph patternExecutionGraph, GeneralExecutionGraph caatExecutionGraph, RefinementModel refinementModel, Set<Relation> staticRelations) {
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
                    ViolationPattern.Node from = patternNodes.get(edgeLit.getData().getFirst());
                    ViolationPattern.Node to = patternNodes.get(edgeLit.getData().getSecond());

                    if (from == null) {
                        patternNodes.put(edgeLit.getData().getFirst(), pattern.addNode());
                        from = patternNodes.get(edgeLit.getData().getFirst());
                    }
                    if (to == null) {
                        patternNodes.put(edgeLit.getData().getSecond(), pattern.addNode());
                        to = patternNodes.get(edgeLit.getData().getSecond());
                    }

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
                    pattern.addRelationGraph(baseRel, patternExecutionGraph.getRelationGraph(baseRel));
                    pattern.addEdge(baseRel, from, to, isStatic, isNegative);
                }
                if (patternNodes.size() > maxNodeNumber) {
                    rollback(violationPatterns, currentUsedRelations);
                    break;
                }
            }
            usedRelations.addAll(currentUsedRelations);
        }
        if (!violationPatterns.isEmpty()) {
            if (coveredStaticOptimization) {
                violationPatterns = violationPatterns.stream().filter(ViolationPattern::validateStaticCoverage).collect(Collectors.toList());
            }
            propagator.addPatterns(violationPatterns, usedRelations);
        }
    }

    private void rollback(List<ViolationPattern> violationPatterns, Set<Relation> usedRelations) {
        violationPatterns.remove(violationPatterns.size() - 1);
        usedRelations.clear();
    }
}
