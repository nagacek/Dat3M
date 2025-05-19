package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.caat.misc.EdgeSet;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;

import java.util.*;
import java.util.stream.Stream;

public class ViolationPattern {

    public class PatternNode {
        private int nodeId;

        public PatternNode() {
            this(ViolationPattern.nodeId++);
        }
        private PatternNode(int nodeId) {
            this.nodeId = nodeId;
        }
        public String toString() {
            return "" + nodeId;
        }
    }

    record PatternEdge(Relation relation, PatternNode source, PatternNode target, boolean isStatic) {
        PatternEdge(Relation relation, PatternNode source, PatternNode target) {
            this(relation, source, target, false);
        }

        public PatternNode get(PatternNode from, EdgeDirection dir) {
            return dir == EdgeDirection.INGOING ? source : target;
        }
    }

    private final List<PatternNode> nodes;
    private final Map<PatternNode, Integer> nodeMap = new HashMap<>();
    private final Map<PatternNode, Collection<PatternEdge>> outgoingEdges;
    private final Map<PatternNode, Collection<PatternEdge>> ingoingEdges;
    private final Map<Relation, Collection<PatternEdge>> relationEdges;
    private final Set<PatternEdge> negative = new HashSet<>();

    protected static int nodeId = 0;

    //private Map<Relation, Collection<Collection<PatternEdge>>> patternEdges;

    List<Joiner.JoinQuery> joinPlan = null;

    // TODO: Support for uncovered static edges
    // So far, only atomicity violations are supported
    public ViolationPattern(Set<Relation> trackedRelations, Set<Relation> staticRelations) {
        List<PatternNode> nodeList = new ArrayList<>();
        outgoingEdges = new HashMap<>();
        ingoingEdges = new HashMap<>();
        relationEdges = new HashMap<>();
        //patternEdges = new HashMap<>();

        for (int i = 0; i <= 3; i++) {
            PatternNode n = new PatternNode();
            nodeList.add(n);
            nodeMap.put(n, i);
        }

        /*          Pattern for atomicity
        * Tracked edges:  rf(0, 1)   co(0, 3)   co(3, 2)
        * Static edges:   rmw(1, 2)  ext(1, 3)  ext(3, 2)
        * */

        List<PatternEdge> edges = new ArrayList<>();
        PatternEdge newEdge;
        for (Relation rel : trackedRelations) {
            if (rel.getNameOrTerm().equals("rf")) {
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(0), nodeList.get(1)));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
            }

            if (rel.getNameOrTerm().equals("co")) {
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(0), nodeList.get(3)));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(3), nodeList.get(2)));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
            }
        }

        for (Relation rel : staticRelations) {
            boolean isStatic = staticRelations.contains(rel);
            if (rel.getNameOrTerm().equals("rmw")) {
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(1), nodeList.get(2), isStatic));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
            }

            if (rel.getNameOrTerm().equals("ext")) {
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(1), nodeList.get(3), isStatic));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
                edges.add(newEdge = new PatternEdge(rel, nodeList.get(3), nodeList.get(2), isStatic));
                relationEdges.computeIfAbsent(rel, k -> new ArrayList<>()).add(newEdge);
            }
        }

        for (PatternEdge edge : edges) {
            outgoingEdges.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge);
            ingoingEdges.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge);
        }

        nodes = nodeList;
    }

    public DNF<CAATLiteral> applySubstitutions(List<int[]> substitutions, PropagatorExecutionGraph executionGraph) {
        List<Conjunction<CAATLiteral>> cubes = new ArrayList<>();
        for (int[] substitution : substitutions) {
            cubes.add(applySubstitution(substitution, executionGraph));
        }
        return new DNF<>(cubes);
    }

    private Conjunction<CAATLiteral> applySubstitution(int[] substitution, PropagatorExecutionGraph executionGraph) {
        List<CAATLiteral> substituted = new ArrayList<>();
        for (Relation rel : relationEdges.keySet()) {
            RelationGraph graph = executionGraph.getRelationGraph(rel);
            for (PatternEdge edge : relationEdges.get(rel)) {
                int sourceId = getNodeId(edge.source());
                int targetId = getNodeId(edge.target());
                CAATLiteral lit = new EdgeLiteral(graph, new Edge(substitution[sourceId], substitution[targetId]), !negative.contains(edge));
                substituted.add(lit);
            }
        }
        return new Conjunction<>(substituted);
    }

    public Collection<PatternEdge> getRelationEdges(Relation rel) {
        return relationEdges.get(rel);
    }

    public Collection<PatternEdge> getSuccessors(PatternNode node) {
        return outgoingEdges.getOrDefault(node, Collections.emptyList());
    }

    public Collection<PatternEdge> getPredecessors(PatternNode node) {
        return ingoingEdges.getOrDefault(node, Collections.emptyList());
    }

    public Collection<PatternEdge> getEdges(PatternNode node, EdgeDirection dir) {
        if (dir == EdgeDirection.INGOING) {
            return ingoingEdges.getOrDefault(node, Collections.emptyList());
        } else if (dir == EdgeDirection.OUTGOING) {
            return outgoingEdges.getOrDefault(node, Collections.emptyList());
        } else {
            return Collections.emptyList();
        }
    }

    public int getNodeId(PatternNode n) {
        return nodeMap.getOrDefault(n, -1);
    }

    /*Stream<Joiner.JoinQuery> getJoinPlan() {
        if (joinPlan == null) {
            nodes.sort((n1, n2) -> (ingoingEdges.get(n2).size() + outgoingEdges.get(n2).size()) -
                    (ingoingEdges.get(n1).size() + ingoingEdges.get(n1).size()));

            joinPlan = new ArrayList<>();
            for (PatternNode node : nodes) {
                List<Joiner.JoinRelation> joinRelations = new ArrayList<>();
                for (PatternEdge edge : outgoingEdges.get(node)) {
                    joinRelations.add(new Joiner.JoinRelation(edge.relation(), Joiner.Argument.FIRST));
                }
                for (PatternEdge edge : ingoingEdges.get(node)) {
                    joinRelations.add(new Joiner.JoinRelation(edge.relation(), Joiner.Argument.SECOND));
                }
                joinPlan.add(new Joiner.JoinQuery(joinRelations, node));
            }
        }

        return joinPlan.stream();
    }*/

    public List<PatternNode> getNodes() { return nodes; }

}
