package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.caat.misc.EdgeSet;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.caat4wmm.GeneralExecutionGraph;
import com.dat3m.dartagnan.utils.Pair;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern.PatternNode;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern.PatternEdge;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

public class Joiner {

    private final GeneralExecutionGraph executionGraph;
    private final Map<Relation, EdgeSet> staticEdges;
    private final Set<PatternNode> unvisited = new HashSet<>();
    private final Set<PatternNode> visited = new HashSet<>();
    private final Set<PatternNode> hasEntries = new HashSet<>();
    private List<int[]> joinResult = new ArrayList<>();

    public Joiner(GeneralExecutionGraph executionGraph, Map<Relation, EdgeSet> staticEdges) {
        this.executionGraph = executionGraph;
        this.staticEdges = staticEdges;
    }

    private void reset() {
        unvisited.clear();
        visited.clear();
        hasEntries.clear();
        joinResult.clear();
    }

    private int[] initPatternJoin(ViolationPattern pattern, PatternEdge pEdge, Edge newEdge) {
        int numNodes = pattern.getNodes().size();
        reset();
        unvisited.addAll(pattern.getNodes());
        int[] initialResult = new int[numNodes];
        initialResult[pattern.getNodeId(pEdge.source())] = newEdge.getFirst();
        initialResult[pattern.getNodeId(pEdge.target())] = newEdge.getSecond();
        hasEntries.add(pEdge.source());
        hasEntries.add(pEdge.target());
        return initialResult;
    }

    public List<int[]> join(ViolationPattern pattern, Edge newEdge, Relation edgeRelation) {
        Collection<PatternEdge> candidateEdges = pattern.getEdges(edgeRelation);

        List<int[]> returnResult = new ArrayList<>();
        for (PatternEdge pEdge : candidateEdges) {
            int[] initialResult = initPatternJoin(pattern, pEdge, newEdge);
            PatternNode first = pEdge.source();
            PatternNode second = pEdge.target();

            /*
             *  TODO: allow for a static edge to be the smallest incident edge and check for execution
             *   of the associated events later
             */
            PatternNode toVisit;
            PatternEdge smallestEdge = findSmallestIncident(pattern, pEdge, newEdge);
            if (smallestEdge == null) { // there are no other edges to join with
                continue;
            }
            joinResult.add(initialResult);
            if (smallestEdge.source() == first || smallestEdge.target() == first) {
                toVisit = first;
            } else if (smallestEdge.source() == second || smallestEdge.target() == second) {
                toVisit = second;
            } else {
                throw new RuntimeException("Smallest edge not connected to added edge");
            }

            do {
                unvisited.remove(toVisit);
                hasEntries.remove(toVisit);
                visited.add(toVisit);

                if (!joinOverDirection(pattern, toVisit, EdgeDirection.INGOING) ||
                        !joinOverDirection(pattern, toVisit, EdgeDirection.OUTGOING)) {
                    break;
                }

                toVisit = getNextUnvisited(pattern, toVisit);

            } while (!unvisited.isEmpty());

            returnResult.addAll(joinResult);
        }
        /*if (!validate(returnResult, pattern).isEmpty()) {
            int i = 5;
        }*/
        return returnResult;
    }

    private PatternNode getNextUnvisited(ViolationPattern pattern, PatternNode start) {
        Set<PatternNode> successors = pattern.getSuccessors(start).stream().map(PatternEdge::target).collect(Collectors.toSet());
        Set<PatternNode> unvisitedSucs = Sets.intersection(successors, unvisited);
        Set<PatternNode> predecessors = pattern.getPredecessors(start).stream().map(PatternEdge::source).collect(Collectors.toSet());
        Set<PatternNode> unvisitedPreds = Sets.intersection(predecessors, unvisited);

        PatternNode unvisitedNode = null;
        if (!unvisitedSucs.isEmpty()) {
            PatternNode node = unvisitedSucs.iterator().next();
            if (hasEntries.contains(node)) {
                unvisitedNode = node;
            }
        }
        if (unvisitedNode == null && !unvisitedPreds.isEmpty()) {
            PatternNode node = unvisitedPreds.iterator().next();
            if (hasEntries.contains(node)) {
                unvisitedNode = node;
            }
        }
        if (unvisitedNode == null && !unvisited.isEmpty()) {
            return hasEntries.iterator().next();
        }
        return null;
    }
    // cannot handle unconnected pattern nodes. Will take the initial result as final result
    private boolean joinOverDirection(ViolationPattern pattern, PatternNode toVisit, EdgeDirection dir) {
        for (PatternEdge neighbor : pattern.getEdges(toVisit, dir)) {
            PatternNode joinNode = neighbor.get(toVisit, dir);

            if (neighbor.isStatic()) {
                handleStaticEdge(pattern, neighbor, joinNode, dir);
            } else if (unvisited.contains(joinNode)) {
                if (hasEntries.contains(joinNode)) {
                    pruneResults(pattern, neighbor, toVisit, dir);
                } else {
                    joinNewResults(pattern, neighbor, toVisit, dir);
                }
            }
            if (joinResult.isEmpty()) {
                return false;
            }
        }

        return !joinResult.isEmpty();
    }

    private void handleStaticEdge(ViolationPattern pattern, PatternEdge joinEdge, PatternNode visitedNode, EdgeDirection dir) {
        if (!unvisited.contains(joinEdge.get(visitedNode, dir))) {
            List<Integer> substitutionsToRemove = new ArrayList<>();
            for (int i = joinResult.size() - 1; i >= 0; i--) {
                int[] tableRow = joinResult.get(i);
                Set<Integer> targetIds = staticEdges.get(joinEdge.relation()).getOutgoingEdges(tableRow[pattern.getNodeId(joinEdge.source())]);
                if (!targetIds.contains(tableRow[pattern.getNodeId(joinEdge.target())])) {
                    substitutionsToRemove.add(i);
                }
            }
            for (int i : substitutionsToRemove) {
                joinResult.remove(i);
            }
        }
    }

    private void pruneResults(ViolationPattern pattern, PatternEdge joinEdge, PatternNode visitedNode, EdgeDirection dir) {
        SimpleGraph joinGraph = (SimpleGraph)executionGraph.getRelationGraph(joinEdge.relation());
        List<Integer> substitutionsToRemove = new ArrayList<>();
        for (int i = joinResult.size() - 1; i >= 0; i--) {
            int[] tableRow = joinResult.get(i);
            Collection<Edge> targetEdges = joinGraph.getEdges(tableRow[pattern.getNodeId(visitedNode)], dir);
            Set<Integer> targetIds = targetEdges.stream().map(e -> dir == EdgeDirection.INGOING ? e.getFirst() : e.getSecond()).collect(Collectors.toSet());
            if (!targetIds.contains(tableRow[pattern.getNodeId(joinEdge.get(visitedNode, dir))])) {
                substitutionsToRemove.add(i);
            }
        }
        for (int i : substitutionsToRemove) {
            joinResult.remove(i);
        }
    }

    private void joinNewResults(ViolationPattern pattern, PatternEdge joinEdge, PatternNode visitedNode, EdgeDirection dir) {
        SimpleGraph joinGraph = (SimpleGraph)executionGraph.getRelationGraph(joinEdge.relation());
        List<int[]> newJoinResult = new ArrayList<>();
        for (int i = joinResult.size() - 1; i >= 0; i--) {
            int[] tableRow = joinResult.get(i);
            for (Edge neighborEdge : joinGraph.getEdges(tableRow[pattern.getNodeId(visitedNode)], dir)) {
                int[] newRow = Arrays.copyOf(tableRow, tableRow.length);
                PatternNode joinNode = joinEdge.get(visitedNode, dir);
                newRow[pattern.getNodeId(joinNode)] = dir == EdgeDirection.INGOING ? neighborEdge.getFirst() : neighborEdge.getSecond();
                newJoinResult.add(newRow);
                hasEntries.add(joinNode);
            }
        }
        joinResult = newJoinResult;
    }

    // TODO: Can be optimized if handling of non-covered static edges is implemented
    private PatternEdge findSmallestIncident(ViolationPattern p, PatternEdge pEdge, Edge edge) {
        Pair<PatternEdge, Integer> smallestFirstEdge = findSmallestIncident(p, pEdge, pEdge.source(), edge.getFirst());
        Pair<PatternEdge, Integer> smallestSecondEdge = findSmallestIncident(p, pEdge, pEdge.target(), edge.getSecond());

        if (smallestFirstEdge.first == null) {
            return smallestSecondEdge.first;
        } else if (smallestSecondEdge.first == null) {
            return smallestFirstEdge.first;
        } else {
            return smallestFirstEdge.second < smallestSecondEdge.second ? smallestFirstEdge.first : smallestSecondEdge.first;
        }
    }

    private Pair<PatternEdge, Integer> findSmallestIncident(ViolationPattern p, PatternEdge pEdge, PatternNode pNode, int edgeId) {
        Pair<PatternEdge, Integer> outgoing = findSmallestIncident(p, pEdge, pNode, edgeId, EdgeDirection.OUTGOING);
        Pair<PatternEdge, Integer> ingoing = findSmallestIncident(p, pEdge, pNode, edgeId, EdgeDirection.INGOING);

        if (outgoing.first == null) {
            return ingoing;
        } else if (ingoing.first == null) {
            return outgoing;
        } else {
            return outgoing.second < ingoing.second ? outgoing : ingoing;
        }
    }
    // can the join be optimized by aborting when curSize is 0 at some point?
    private Pair<PatternEdge, Integer> findSmallestIncident(ViolationPattern p, PatternEdge pEdge, PatternNode pNode, int edgeId, EdgeDirection dir) {
        boolean forward = dir == EdgeDirection.OUTGOING;
        int minSize = -1;
        PatternEdge minEdge = null;
        Collection<PatternEdge> edges = forward ? p.getSuccessors(pNode) : p.getPredecessors(pNode);
        for (PatternEdge edge : edges) {
            if (edge == pEdge || edge.isStatic() || visited.contains(forward ? edge.target() : edge.source())) {
                continue;
            }
            int curSize = executionGraph.getRelationGraph(edge.relation()).getMinSize(edgeId, dir);
            if (minEdge == null && curSize > 0) {
                minEdge = edge;
                minSize = curSize;
            }
            if (curSize < minSize) {
                minEdge = edge;
                minSize = curSize;
            }
        }

        return new Pair<>(minEdge, minSize);
    }

    private Collection<int[]> validate(Collection<int[]> toValidate, ViolationPattern pattern) {
        Collection<int[]> incorrect = new ArrayList<>();
        for (int[] substitution : toValidate) {
            Set<Integer> executed = new HashSet<>();
            Set<Integer> checkExecution = new HashSet<>();
            for (PatternEdge pEdge : pattern.getEdges()) {
                int firstId = substitution[pattern.getNodeId(pEdge.source())];
                int secondId = substitution[pattern.getNodeId(pEdge.target())];

                if (pEdge.isStatic()) {
                    Set<Integer> possible = staticEdges.get(pEdge.relation()).getOutgoingEdges(firstId);
                    if (!possible.contains(secondId)) {
                        incorrect.add(substitution);
                        System.err.println("Substitution " + Arrays.toString(substitution) + " does not respect static edges.");
                        break;
                    } else if (!executed.contains(firstId) || !executed.contains(secondId)) {
                        if (!executed.contains(firstId)) {
                            checkExecution.add(firstId);
                        }
                        if (!executed.contains(secondId)) {
                            checkExecution.add(secondId);
                        }
                    }
                } else {
                    if (executionGraph.getRelationGraph(pEdge.relation()).contains(new Edge(firstId, secondId))) {
                        executed.add(firstId);
                        executed.add(secondId);
                        checkExecution.remove(firstId);
                        checkExecution.remove(secondId);
                    } else {
                        System.err.println("Substitution " + Arrays.toString(substitution) + " does not respect dynamic edges.");
                        break;
                    }
                }
            }
            if (!checkExecution.isEmpty()) {
                System.err.println("Substitution " + Arrays.toString(substitution) + " does not check for execution of event(s) with id(s) " + checkExecution + " used in static edge(s).");
            }
        }
        return incorrect;
    }

    public record JoinQuery(Collection<JoinRelation> joinRelation, PatternNode node) {}

    public record JoinRelation(Relation relation, Argument argument) {}

    public enum Argument {
        FIRST,
        SECOND
    }
}
