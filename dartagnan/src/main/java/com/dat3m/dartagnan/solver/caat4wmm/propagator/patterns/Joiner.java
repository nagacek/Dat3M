package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.encoding.EdgeInfo;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.utils.Pair;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern.PatternNode;
import com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns.ViolationPattern.PatternEdge;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Joiner {

    Map<Relation, SimpleGraph> relationMap;
    //Map<PatternNode, Integer> nodeMap = new HashMap<>();
    Set<PatternNode> unvisited = new HashSet<>();
    Set<PatternNode> visited = new HashSet<>();
    Set<PatternNode> hasEntries = new HashSet<>();

    public Joiner(Map<Relation, SimpleGraph> relationMap) {
        this.relationMap = relationMap;
    }

    private void reset() {
        unvisited.clear();
        visited.clear();
        hasEntries.clear();
    }

    public List<int[]> join(ViolationPattern pattern, Edge newEdge, Relation edgeRelation) {
        Collection<PatternEdge> candidateEdges = pattern.getRelationEdges(edgeRelation);
        int numNodes = pattern.getNodes().size();

        List<int[]> returnResult = new ArrayList<>();
        for (PatternEdge pEdge : candidateEdges) {
            reset();
            unvisited.addAll(pattern.getNodes());
            List<int[]> joinResult = new ArrayList<>();
            PatternNode first = pEdge.source();
            PatternNode second = pEdge.target();
            int[] initialResult = new int[numNodes];
            initialResult[pattern.getNodeId(first)] = newEdge.getFirst();
            initialResult[pattern.getNodeId(second)] = newEdge.getSecond();
            hasEntries.add(first);
            hasEntries.add(second);
            joinResult.add(initialResult);

            //TODO: start join with smallest edge
            PatternNode toVisit;
            PatternEdge smallestEdge = findSmallestIncident(pattern, pEdge, newEdge, relationMap);
            if (smallestEdge == null) { // there are no other edges to join with
                continue;
            }
            if (smallestEdge.source() == first || smallestEdge.target() == first) {
                toVisit = first;
            } else if (smallestEdge.source() == second || smallestEdge.target() == second) {
                toVisit = second;
            } else {
                throw new RuntimeException("Smallest edge not connected to added edge");
            }

            while (!unvisited.isEmpty()) {
                if (!joinOverDirection(pattern, toVisit, EdgeDirection.INGOING, joinResult) ||
                        !joinOverDirection(pattern, toVisit, EdgeDirection.OUTGOING, joinResult)) {
                    break;
                }
                unvisited.remove(toVisit);
                hasEntries.remove(toVisit);
                visited.add(toVisit);

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
            }
            returnResult.addAll(joinResult);
        }

        return returnResult;
    }

    private boolean joinOverDirection(ViolationPattern pattern, PatternNode toVisit, EdgeDirection dir, List<int[]> joinResult) {
        for (PatternEdge neighbor : pattern.getEdges(toVisit, dir)) {
            PatternNode joinNode;
            if (dir == EdgeDirection.INGOING) {
                joinNode = neighbor.source();
            } else {
                joinNode = neighbor.target();
            }
            if (unvisited.contains(joinNode)) {
                SimpleGraph joinGraph = relationMap.get(neighbor.relation());

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
                    for (int i = joinResult.size() - 1; i >= 0; i--) {
                        int[] tableRow = joinResult.get(i);
                        List<int[]> newJoinResult = new ArrayList<>();
                        for (Edge neighborEdge : joinGraph.getEdges(tableRow[pattern.getNodeId(toVisit)], dir)) {
                            int[] newRow = Arrays.copyOf(tableRow, tableRow.length);
                            newRow[pattern.getNodeId(joinNode)] = dir == EdgeDirection.INGOING ? neighborEdge.getFirst() : neighborEdge.getSecond();
                            newJoinResult.add(newRow);
                            hasEntries.add(joinNode);
                        }
                        joinResult = newJoinResult;
                    }
                }

                if (joinResult.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private PatternEdge findSmallestIncident(ViolationPattern p, PatternEdge pEdge, Edge edge, Map<Relation, SimpleGraph> relationMap) {
        Pair<PatternEdge, Integer> smallestFirstEdge = findSmallestIncident(p, pEdge, pEdge.source(), edge.getFirst(), relationMap);
        Pair<PatternEdge, Integer> smallestSecondEdge = findSmallestIncident(p, pEdge, pEdge.target(), edge.getSecond(), relationMap);

        return smallestFirstEdge.second < smallestSecondEdge.second ? smallestFirstEdge.first : smallestSecondEdge.first;
    }

    private Pair<PatternEdge, Integer> findSmallestIncident(ViolationPattern p, PatternEdge pEdge, PatternNode pNode, int edgeId, Map<Relation, SimpleGraph> relationMap) {
        int minSize = -1;
        PatternEdge minEdge = null;
        for (PatternEdge suc : p.getSuccessors(pNode)) {
            if (suc == pEdge || visited.contains(suc.target())) {
                continue;
            }
            int curSize = relationMap.get(suc.relation()).getMinSize(edgeId, EdgeDirection.OUTGOING);
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
            int curSize = relationMap.get(pre.relation()).getMinSize(edgeId, EdgeDirection.INGOING);
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
