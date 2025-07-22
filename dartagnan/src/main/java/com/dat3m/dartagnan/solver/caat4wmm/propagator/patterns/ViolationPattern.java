package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.program.event.TagSet;
import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.program.filter.TagFilter;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.*;
import java.util.stream.Collectors;

public class ViolationPattern {
    
    final Map<Relation, RelationGraph> rel2Graph = new HashMap<>();
    final List<Node> nodes = new ArrayList<>();
    final List<Edge> edges = new ArrayList<>();

    // ------------------------------------------------------------------------------------------
    // Construction

    public Node addNode() {
        final Node node = new Node(nodes.size());
        nodes.add(node);
        return node;
    }

    public Edge addEdge(Relation relation, Node from, Node to, boolean isStatic) {
        final RelationGraph graph = rel2Graph.get(relation);
        assert graph != null;
        final Edge edge = new Edge(relation, graph, from, to, false, isStatic, false);
        this.edges.add(edge);
        return edge;
    }

    public Edge addEdge(Relation relation, Node from, Node to, boolean isStatic, boolean isNegated) {
        final RelationGraph graph = rel2Graph.get(relation);
        assert graph != null;
        final Edge edge = new Edge(relation, graph, from, to, isNegated, isStatic, false);
        this.edges.add(edge);
        return edge;
    }

    public void addRelationGraph(Relation rel, RelationGraph graph) {
        rel2Graph.putIfAbsent(rel, graph);
    }

    // ------------------------------------------------------------------------------------------
    // Matching

    public List<Edge> findEdgesByRelation(Relation relation) {
        List<Edge> matches = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.relation == relation) {
                matches.add(edge);
            }
        }
        return matches;
    }

    public List<Match> findMatches(Edge edge, int from, int to) {
        if (edge.isNegated) {
            return List.of();
        }
        final NodeSet visited = new NodeSet();
        visited.add(edge.from);
        visited.add(edge.to);

        if (!(checkTags(edge.from, from) && checkTags(edge.to, to))) {
            return List.of();
        }

        final Match firstMatch = new Match(this);
        firstMatch.setMatch(edge.from, from);
        firstMatch.setMatch(edge.to, to);

        List<Match> curMatches = new ArrayList<>();
        curMatches.add(firstMatch);

        final EdgeQueue queue = new EdgeQueue(this);
        queue.remove(edge);

        Edge nextEdgeToJoin;
        while ((nextEdgeToJoin = queue.pop(visited)) != null) {
            final boolean joinAtSource = visited.add(nextEdgeToJoin.to);
            final boolean joinAtTarget = visited.add(nextEdgeToJoin.from);
            final boolean doPrune = !(joinAtSource || joinAtTarget);

            if (doPrune) {
                prune(curMatches, nextEdgeToJoin);
            } else {
                final Node matchedNode = joinAtSource ? nextEdgeToJoin.from : nextEdgeToJoin.to;
                final Node unmatchedNode = joinAtSource ? nextEdgeToJoin.to : nextEdgeToJoin.from;
                List<Match> nextMatches = new ArrayList<>();
                for (Match match : curMatches) {
                    final List<Integer> joinedNodeIds = joinAlong(match, matchedNode, nextEdgeToJoin);
                    for (Integer nodeId : joinedNodeIds) {
                        nextMatches.add(match.with(unmatchedNode, nodeId));
                    }
                }
                curMatches = nextMatches;
            }

            if (curMatches.isEmpty()) {
                return curMatches;
            }
        }


        return curMatches;
    }

    private List<Integer> joinAlong(Match partialMatch, Node node, Edge edge) {
        final boolean outgoing = edge.from == node;
        final Node target = outgoing ? edge.to : edge.from;
        final int nodeMatch = partialMatch.atNode(node);

        assert nodeMatch != -1;
        assert partialMatch.atNode(target) == -1;

        final RelationGraph relationGraph = edge.graph;
        final var edges = outgoing ? relationGraph.outEdges(nodeMatch) : relationGraph.inEdges(nodeMatch);
        final List<Integer> matches = new ArrayList<>();
        for (var e : edges) {
            int targetId = outgoing ? e.getSecond() : e.getFirst();

            if (checkTags(target, targetId)) {
                matches.add(targetId);
            }
        }

        return matches;
    }

    private void prune(List<Match> matches, Edge edge) {
        final RelationGraph relationGraph = edge.graph;
        matches.removeIf(m -> {
            final int source = m.atNode(edge.from);
            final int target = m.atNode(edge.to);
            return edge.isNegated == relationGraph.containsById(source, target);
        });
    }

    private boolean checkTags(Node node, int id) {
        boolean rightTags = true;
        for (SetPredicate set : node.sets) {
            if (!set.containsById(id)) {
                rightTags = false;
                break;
            }
        }
        return rightTags;
    }


    public Match matchPattern(ViolationPattern pattern2) {
        NodeSet visited = new NodeSet();
        EdgeQueue queue = new EdgeQueue(this);

        ViolationPattern.Edge ownEdge = edges.get(0);
        List<Edge> matchPartners = pattern2.findEdgesByRelation(ownEdge.relation);
        queue.remove(ownEdge);
        if (matchPartners.isEmpty()) {
            return null;
        }

        List<Match> matches = new ArrayList<>();
        for (ViolationPattern.Edge matchEdge : matchPartners) {
            Match match = new Match(this);
            match.setMatch(ownEdge.from, matchEdge.from.id());
            match.setMatch(ownEdge.to, matchEdge.to.id());
            matches.add(match);
        }
        visited.add(ownEdge.from);
        visited.add(ownEdge.to);

        while ((ownEdge = queue.pop(visited, false)) != null) {
            matchPartners = pattern2.findEdgesByRelation(ownEdge.relation);
            if (matchPartners.isEmpty()) {
                return null;
            }
            List<Match> updatedMatches = new ArrayList<>();
            for (ViolationPattern.Edge matchEdge : matchPartners) {
                for (Match match : matches) {
                    Match matchResult1 = matchNodes(match, ownEdge.from, matchEdge.from, visited);
                    Match matchResult2 = matchNodes(match, ownEdge.to, matchEdge.to, visited);
                    if (matchResult1 == null || matchResult2 == null) {
                        continue;
                    }
                    updatedMatches.add(Match.merge(matchResult1, matchResult2));
                }
            }
            matches = updatedMatches;
            if (matches.isEmpty()) {
                return null;
            }
        }
        return matches.get(0);
    }

    private Match matchNodes(Match match, Node ownNode, Node matchNode, NodeSet visited) {
        if (match.atNode(ownNode) != -1) {
            if (match.atNode(ownNode) != matchNode.id()) {
                return null;
            }
        } else {
            if (!ownNode.containsSets(matchNode)) {
                return null;
            }
            Match newMatch = match.with(ownNode, matchNode.id());
            visited.add(ownNode);
            return newMatch;
        }
        return match;
    }
    // ------------------------------------------------------------------------------------------
    // Substitution

    public Conjunction<CAATLiteral> substituteWithMatch(Match match) {
        List<CAATLiteral> substituted = new ArrayList<>();
        for (Edge edge : edges) {
            RelationGraph graph = edge.graph;
            Node from = edge.from;
            Node to = edge.to;
            CAATLiteral lit = new EdgeLiteral(graph, new com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge(match.atNode(from), match.atNode(to)), !edge.isNegated);
            substituted.add(lit);
        }
        return new Conjunction<>(substituted);
    }

    // ------------------------------------------------------------------------------------------
    // Validation

    public boolean validateStaticCoverage() {
        Set<Node> uncovered = new HashSet<>();
        uncovered.addAll(nodes);
        for (Edge edge : edges) {
            if (!edge.isStatic) {
                uncovered.remove(edge.from);
                uncovered.remove(edge.to);
            }
        }
        return uncovered.isEmpty();
    }

    // Depth-first search to check whether the pattern is a connected graph without static edges
    public boolean newValidateStaticCoverage() {
        List<Node> marked = new ArrayList<>();
        List<Node> uncovered = new ArrayList<>(nodes);
        List<Edge> toRemove = new ArrayList<>();
        List<Edge> unused = new ArrayList<>(edges);

        Node current;
        marked.add(uncovered.get(0));
        while (!unused.isEmpty() && !marked.isEmpty()) {
            current = marked.get(marked.size() - 1);
            uncovered.remove(current);
            for (Edge edge : unused) {
                if (edge.isStatic || edge.isNegated) {
                    toRemove.add(edge);
                    continue;
                }
                if (edge.from == current) {
                    marked.add(edge.to);
                    toRemove.add(edge);
                    break;
                }
            }
            if (marked.get(marked.size() - 1) == current) {
                marked.remove(current);
            }
            unused.removeAll(toRemove);
        }
        return uncovered.isEmpty();
    }


    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (Edge edge : edges) {
            sb.append(edge.toString());
            sb.append("  ");
        }
        return sb.toString();
    }

    // Checks whether static edges are shortcuts of longer paths with active edges.
    // If so, there is no need to make a join over the whole may-set.
    // Otherwise, the may-set will be restricted by the events currently active on a join
    public void findShortcutEdges(SetPredicate executedSet) {
        Map<Node, Set<Edge>> activeOutgoing = new HashMap<>();
        Set<Edge> staticEdges = new HashSet<>();
        for (Edge edge : edges) {
            if (edge.isStatic) {
                staticEdges.add(edge);
            } else {
                activeOutgoing.computeIfAbsent(edge.from, k -> new HashSet<>()).add(edge);
            }
        }
        for (Edge staticEdge : staticEdges) {
            Edge newEdge = findPathOver(staticEdge, activeOutgoing);
            if (newEdge != null) {
                edges.remove(staticEdge);
                edges.add(newEdge);
            } else {
                staticEdge.from.addSet(executedSet);
                staticEdge.to.addSet(executedSet);
            }
        }
    }

    private Edge findPathOver(Edge edge, Map<Node, Set<Edge>> outgoing) {
        ArrayDeque<Node> queue = new ArrayDeque<>(outgoing.size());
        Set<Node> visited = new HashSet<>();
        queue.add(edge.from);
        visited.add(edge.from);
        while (!queue.isEmpty()) {
            Node current = queue.pop();
            Set<Edge> edges = outgoing.getOrDefault(current, Set.of());
            for (Edge otherEdge : edges) {
                Node intermediateTarget = otherEdge.to;
                if (edge.to.equals(intermediateTarget)) {
                    return new Edge(edge.relation, edge.graph, edge.from, edge.to, edge.isNegated, edge.isStatic, true);
                }
                if (!visited.contains(intermediateTarget)) {
                    queue.add(intermediateTarget);
                    visited.add(intermediateTarget);
                }
            }
        }
        return null;
    }

    // ===============================================================================================
    // ================================ Internal classes =============================================
    // ===============================================================================================

    private static class NodeSet {
        private int nodeSet = 0;

        private static int toIndex(Node node) {
            return 1 << node.id;
        }

        public boolean contains(Node node) {
            return (nodeSet & toIndex(node)) != 0;
        }

        public boolean add(Node node) {
            final int old = nodeSet;
            nodeSet |= toIndex(node);
            return old != nodeSet;
        }
    }

    private static class EdgeQueue {
        private final List<Edge> unmatchedEdges;

        public EdgeQueue(ViolationPattern pattern) {
            unmatchedEdges = new ArrayList<>(pattern.edges);
        }

        public void remove(Edge edge) {
            unmatchedEdges.remove(edge);
        }

        public Edge pop(NodeSet nodes) {
            return pop(nodes, true);
        }

        public Edge pop(NodeSet visited, boolean handleSpecialCases) {
            if (unmatchedEdges.isEmpty()) {
                return null;
            }

            Edge candidate = null;
            for (Edge e : unmatchedEdges) {
                final boolean fromVisited = visited.contains(e.from);
                final boolean toVisited = visited.contains(e.to);
                if (fromVisited && toVisited) {
                    candidate = e;
                    break;
                } else if (candidate == null && !e.isStatic && !e.isNegated && (fromVisited || toVisited)) {
                    candidate = e;
                } else if (candidate == null && !e.isShortcut && !e.isNegated && (fromVisited || toVisited)) {
                    candidate = e;
                } else if (candidate == null && !handleSpecialCases && (fromVisited || toVisited)) {
                    candidate = e;
                }
            }

            assert candidate != null;
            remove(candidate);
            return candidate;
        }
    }

    public class Node {
        private static final Map<String, Set<String>> inclusionMap = createInclusionMap();
        int id;
        private final List<SetPredicate> sets = new ArrayList<>();

        public Node(int id) {
            this.id = id;
        }

        public void integrateSet(SetPredicate set) {
            if (set == null) {
                return;
            }
            Set<String> included = inclusionMap.get(set.toString());
            sets.add(set);
            if (included == null) {
                return;
            }
            Set<SetPredicate> removeSets = new HashSet<>();
            for (SetPredicate curSet : sets) {
                if (included.contains(curSet.toString())) {
                    removeSets.add(curSet);
                }
            }
            sets.removeAll(removeSets);
        }

        public void integrateSets(Collection<SetPredicate> otherSets) {
            otherSets.forEach(this::integrateSet);
        }

        public void addSet(SetPredicate set) {
            if (!sets.contains(set)) {
                sets.add(set);
            }
        }

        public void addSets(Collection<SetPredicate> sets) {
            for (SetPredicate set : sets) {
                addSet(set);
            }
        }

        public boolean containsSet(SetPredicate set) {
            return sets.contains(set);
        }

        public boolean containsSets(Set<SetPredicate> sets) {
            return this.sets.containsAll(sets);
        }

        public boolean containsSets(Node otherNode) {
            return sets.containsAll(otherNode.sets);
        }

        public int id() {
            return id;
        }

        public Node copy() {
            Node newNode = new Node(id);
            newNode.addSets(sets);
            return newNode;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("n#").append(id);
            if (!sets.isEmpty()) {
                str.append("(");
            }
            for (SetPredicate set : sets) {
                str.append(set).append(" ");
            }
            if (!sets.isEmpty()) {
                str.deleteCharAt(str.length() - 1).append(")");
            }
            return str.toString();
        }

        @Override
        public boolean equals(Object obj) { return this == obj; }

        private static Map<String, Set<String>> createInclusionMap() {
            Map<String, Set<String>> map = new HashMap<>();
            map.put("W", Set.of("M"));
            map.put("R", Set.of("M"));
            return map;
        }
    }

    public record Edge(Relation relation, RelationGraph graph, Node from, Node to, boolean isNegated, boolean isStatic, boolean isShortcut) {
        @Override
        public String toString() {
            return String.format("%s-%s%s->%s", from, isNegated ? "¬" : "", relation.getNameOrTerm(), to);
        }
        @Override
        public boolean equals(Object obj) { return this == obj; }
    }

    public static class Match {
        private final ViolationPattern pattern;
        private final int[] nodeId2EventId;

        public int atNode(Node node) { return nodeId2EventId[node.id]; }
        private void setMatch(Node node, int id) { nodeId2EventId[node.id] = id; }
        private void setMatch(int nodeId, int id) { nodeId2EventId[nodeId] = id; }

        private Match(ViolationPattern pattern, int[] mapping) {
            this.pattern = pattern;
            this.nodeId2EventId = mapping;
        }

        public Match(ViolationPattern pattern) {
            this(pattern, new int[pattern.nodes.size()]);
            Arrays.fill(nodeId2EventId, -1);
        }

        public Match with(Node node, int match) {
            assert atNode(node) == -1;
            int[] updatedMatching = Arrays.copyOf(this.nodeId2EventId, this.nodeId2EventId.length);
            updatedMatching[node.id] = match;
            return new Match(pattern, updatedMatching);
        }

        private Match with(int nodeId, int match) {
            assert nodeId2EventId[nodeId] == -1;
            int[] updatedMatching = Arrays.copyOf(this.nodeId2EventId, this.nodeId2EventId.length);
            updatedMatching[nodeId] = match;
            return new Match(pattern, updatedMatching);
        }

        // expects matches to be non-conflicting:
        // no diverging node matches and same length
        public static Match merge(Match firstMatch, Match secondMatch) {
            assert firstMatch.nodeId2EventId.length == secondMatch.nodeId2EventId.length;
            Match resultingMatch = new Match(firstMatch.pattern, Arrays.copyOf(firstMatch.nodeId2EventId, firstMatch.nodeId2EventId.length));
            for (int i = 0; i < firstMatch.nodeId2EventId.length; i++) {
                int ownValue = resultingMatch.nodeId2EventId[i];
                int otherValue = secondMatch.nodeId2EventId[i];
                int maxValue = Math.max(ownValue, otherValue);
                if (maxValue != -1) {
                    if (ownValue == otherValue) {
                        continue;
                    }
                    assert ownValue == -1 || otherValue == -1;
                    resultingMatch.setMatch(i, -1);
                    resultingMatch = resultingMatch.with(i, maxValue);
                }
            }
            return resultingMatch;
        }

        public int[] toArray() { return nodeId2EventId; }

        @Override
        public String toString() {
            return pattern.nodes.stream().map(n -> n + "->" + atNode(n)).collect(Collectors.joining(
                    ", ", "[ ", " ]"
            ));
        }
    }
}
