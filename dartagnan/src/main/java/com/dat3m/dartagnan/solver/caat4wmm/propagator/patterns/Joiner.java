package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.encoding.EdgeInfo;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
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
import java.util.stream.Stream;

public class Joiner {

    private final GeneralExecutionGraph executionGraph;
    //private Map<PatternNode, Integer> nodeMap = new HashMap<>();
    private final Set<PatternNode> unvisited = new HashSet<>();
    private final Set<PatternNode> visited = new HashSet<>();
    private final Set<PatternNode> hasEntries = new HashSet<>();
    private List<int[]> joinResult = new ArrayList<>();

    public Joiner(GeneralExecutionGraph executionGraph) {
        this.executionGraph = executionGraph;
    }

    private void reset() {
        unvisited.clear();
        visited.clear();
        hasEntries.clear();
        joinResult.clear();
    }

    public List<int[]> join(ViolationPattern pattern, Edge newEdge, Relation edgeRelation) {
        Collection<PatternEdge> candidateEdges = pattern.getRelationEdges(edgeRelation);
        int numNodes = pattern.getNodes().size();

        List<int[]> returnResult = new ArrayList<>();
        for (PatternEdge pEdge : candidateEdges) {
            reset();
            unvisited.addAll(pattern.getNodes());
            PatternNode first = pEdge.source();
            PatternNode second = pEdge.target();
            int[] initialResult = new int[numNodes];
            initialResult[pattern.getNodeId(first)] = newEdge.getFirst();
            initialResult[pattern.getNodeId(second)] = newEdge.getSecond();
            hasEntries.add(first);
            hasEntries.add(second);
            //TODO: start join with smallest edge
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
            unvisited.remove(toVisit);
            hasEntries.remove(toVisit);
            visited.add(toVisit);

            while (!unvisited.isEmpty()) {
                if (!joinOverDirection(pattern, toVisit, EdgeDirection.INGOING) ||
                        !joinOverDirection(pattern, toVisit, EdgeDirection.OUTGOING)) {
                    break;
                }

                Set<PatternNode> successors = pattern.getSuccessors(toVisit).stream().map(PatternEdge::target).collect(Collectors.toSet());
                Set<PatternNode> unvisitedSucs = Sets.intersection(successors, unvisited);
                Set<PatternNode> predecessors = pattern.getPredecessors(toVisit).stream().map(PatternEdge::source).collect(Collectors.toSet());
                Set<PatternNode> unvisitedPreds = Sets.intersection(predecessors, unvisited);
                if (!unvisitedSucs.isEmpty()) {
                    toVisit = unvisitedSucs.iterator().next();
                } else if (!unvisitedPreds.isEmpty()) {
                    toVisit = unvisitedPreds.iterator().next();
                } else {
                    toVisit = hasEntries.iterator().next();
                }

                unvisited.remove(toVisit);
                hasEntries.remove(toVisit);
                visited.add(toVisit);
            }
            returnResult.addAll(joinResult);
        }

        return returnResult;
    }

    private boolean joinOverDirection(ViolationPattern pattern, PatternNode toVisit, EdgeDirection dir) {
        for (PatternEdge neighbor : pattern.getEdges(toVisit, dir)) {
            PatternNode joinNode;
            if (dir == EdgeDirection.INGOING) {
                joinNode = neighbor.source();
            } else {
                joinNode = neighbor.target();
            }
            if (unvisited.contains(joinNode)) {
                SimpleGraph joinGraph = (SimpleGraph)executionGraph.getRelationGraph(neighbor.relation());

                if (hasEntries.contains(joinNode)) {
                    List<Integer> substitutionsToRemove = new ArrayList<>();
                    for (int i = joinResult.size() - 1; i >= 0; i--) {
                        int[] tableRow = joinResult.get(i);
                        Collection<Edge> targetEdges = joinGraph.getEdges(tableRow[pattern.getNodeId(toVisit)], dir);
                        Set<Integer> targetIds = targetEdges.stream().map(e -> dir == EdgeDirection.INGOING ? e.getFirst() : e.getSecond()).collect(Collectors.toSet());
                        if (!targetIds.contains(tableRow[pattern.getNodeId(joinNode)])) {
                            substitutionsToRemove.add(i);
                        }
                    }
                    for (int i : substitutionsToRemove) {
                        joinResult.remove(i);
                    }

                } else {
                    List<int[]> newJoinResult = new ArrayList<>();
                    for (int i = joinResult.size() - 1; i >= 0; i--) {
                        int[] tableRow = joinResult.get(i);
                        for (Edge neighborEdge : joinGraph.getEdges(tableRow[pattern.getNodeId(toVisit)], dir)) {
                            int[] newRow = Arrays.copyOf(tableRow, tableRow.length);
                            newRow[pattern.getNodeId(joinNode)] = dir == EdgeDirection.INGOING ? neighborEdge.getFirst() : neighborEdge.getSecond();
                            newJoinResult.add(newRow);
                            hasEntries.add(joinNode);
                        }
                    }
                    joinResult = newJoinResult;
                }

                if (joinResult.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private PatternEdge findSmallestIncident(ViolationPattern p, PatternEdge pEdge, Edge edge) {
        Pair<PatternEdge, Integer> smallestFirstEdge = findSmallestIncident(p, pEdge, pEdge.source(), edge.getFirst());
        Pair<PatternEdge, Integer> smallestSecondEdge = findSmallestIncident(p, pEdge, pEdge.target(), edge.getSecond());

        return smallestFirstEdge.second < smallestSecondEdge.second ? smallestFirstEdge.first : smallestSecondEdge.first;
    }

    private Pair<PatternEdge, Integer> findSmallestIncident(ViolationPattern p, PatternEdge pEdge, PatternNode pNode, int edgeId) {
        int minSize = -1;
        PatternEdge minEdge = null;
        for (PatternEdge suc : p.getSuccessors(pNode)) {
            if (suc == pEdge || visited.contains(suc.target())) {
                continue;
            }
            int curSize = executionGraph.getRelationGraph(suc.relation()).getMinSize(edgeId, EdgeDirection.OUTGOING);
            if (minEdge == null && curSize > 0) {
                minEdge = suc;
                minSize = curSize;
            }
            if (curSize < minSize) {
                minEdge = suc;
                minSize = curSize;
            }
        }

        for (PatternEdge pre : p.getPredecessors(pNode)) {
            if (pre == pEdge || visited.contains(pre.source())) {
                continue;
            }
            int curSize = executionGraph.getRelationGraph(pre.relation()).getMinSize(edgeId, EdgeDirection.INGOING);
            if (minEdge == null && curSize > 0) {
                minEdge = pre;
                minSize = curSize;
            }
            if (curSize < minSize) {
                minEdge = pre;
                minSize = curSize;
            }
        }
        return new Pair<>(minEdge, minSize);
    }

    public record JoinQuery(Collection<JoinRelation> joinRelation, PatternNode node) {}

    public record JoinRelation(Relation relation, Argument argument) {}

    public enum Argument {
        FIRST,
        SECOND
    }
}
