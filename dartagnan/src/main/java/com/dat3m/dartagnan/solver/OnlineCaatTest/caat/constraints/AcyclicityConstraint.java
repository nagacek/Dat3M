package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.constraints;


import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.DenseIntegerSet;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.MediumDenseIntegerSet;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.ObjectPool;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.misc.PathAlgorithm;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.misc.MaterializedSubgraphView;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.*;

public class AcyclicityConstraint extends AbstractConstraint {

    private final RelationGraph constrainedGraph;

    private static final ObjectPool<DenseIntegerSet> SET_COLLECTION_POOL =
            new ObjectPool<>(DenseIntegerSet::new, 10);

    private static final int MAX_OVERSHOOT = 1;


    private final List<DenseIntegerSet> violatingSccs = new ArrayList<>();
    private final MediumDenseIntegerSet markedNodes = new MediumDenseIntegerSet();
    private ArrayList<Node> nodeMap = new ArrayList<>();
    private boolean noChanges = true;


    private final List<List<Edge>> cycles = new ArrayList<>();
    private final List<List<Edge>> undershootCycles = new ArrayList<>();
    private final List<List<Edge>> overshootEdges = new ArrayList<>();

    public AcyclicityConstraint(RelationGraph constrainedGraph) {
        this.constrainedGraph = constrainedGraph;
    }

    @Override
    public RelationGraph getConstrainedPredicate() {
        return constrainedGraph;
    }

    @Override
    public boolean checkForViolations() {
        if (!violatingSccs.isEmpty()) {
            return true;
        } else if (noChanges) {
            return false;
        }
        ensureCapacity();
        //applyChanges();
        tarjan();
        violatingSccs.sort(Comparator.comparingInt(Set::size));
        if (violatingSccs.isEmpty()) {
            noChanges = true;
        }
        return !violatingSccs.isEmpty();
    }

    @Override
    public List<List<Edge>> getViolations() {
        computeViolations();

        if (violatingSccs.isEmpty()) {
            return Collections.emptyList();
        }

        return cycles;
    }

    @Override
    public boolean checkForNearlyViolations() {
        return !undershootCycles.isEmpty();
    }

    @Override
    public List<List<Edge>> getUndershootViolations() {
        return undershootCycles;
    }

    @Override
    public List<List<Edge>> getOvershootEdges() {
        return overshootEdges;
    }

    private void computeViolations() {
        cycles.clear();
        undershootCycles.clear();
        overshootEdges.clear();

        //System.out.println("NEW CALL");

        ensureCapacity();
        //applyChanges();

        // Current implementation: For all marked events <e> in all SCCs:
        // (1) find a shortest path C from <e> to <e> (=cycle)
        // (2) remove all nodes in C from the search space (those nodes are likely to give the same cycle)
        // (3) remove chords and normalize cycle order (starting from element with smallest id)
        //System.out.println("NEW SCC");
        for (Set<Integer> scc : violatingSccs) {
            MaterializedSubgraphView subgraph = new MaterializedSubgraphView(constrainedGraph, scc);
            Set<Integer> nodes = new HashSet<>(Sets.intersection(scc, markedNodes));
            Set<Integer> overshootNodes = new HashSet<>();
            while (!nodes.isEmpty()) {
                int e = nodes.stream().findAny().get();

                List<Edge> cycle = PathAlgorithm.findShortestPath(subgraph, e, e, true);

                // mark node for overshoot cycle
                if (cycle.isEmpty()) {
                    overshootNodes.add(e);
                    nodes.remove(e);
                    continue;
                }

                cycle = new ArrayList<>(cycle);
                cycle.forEach(edge -> nodes.remove(edge.getFirst()));
                //TODO: Most cycles have chords, so a specialized algorithm that avoids
                // chords altogether would be great
                reduceChordsAndNormalize(cycle);
                if (!cycles.contains(cycle)) {
                    cycles.add(cycle);
                }
            }
            while(!overshootNodes.isEmpty()) {
                List<Edge> innerOvershootEdges = new ArrayList<>();
                int e = overshootNodes.stream().findAny().get();
                List<Edge> overshootCycle = PathAlgorithm.findShortestPath(subgraph, e, e, false);
                overshootCycle.forEach(edge -> {
                    overshootNodes.remove(edge.getFirst());
                    if (!edge.isActive()) {
                        innerOvershootEdges.add(edge);
                    }
                });
                overshootCycle.removeAll(innerOvershootEdges);
                //reduceChordsAndNormalize(overshootCycle);
                undershootCycles.add(overshootCycle);
                overshootEdges.add(innerOvershootEdges);
            }
        }

        /*if (cycles.isEmpty()) {
            int i = 5;
        }*/
    }

    /*@Override
    public void findAugmentedViolations() {

    }*/

    private void reduceChordsAndNormalize(List<Edge> cycle) {
        // Reduces chords by iteratively merging first and last edge if possible
        // Note that edges in the middle should not have chords since
        // we start with a shortest cycle rooted at some node <e>
        //TODO: after the first merge, the two nodes of the merged edge
        // both may allow further merging but we only iteratively merge one of the two.
        boolean prog;
        do {
            if (cycle.size() == 1) {
                // Self-cycles are not reducible
                return;
            }
            prog = false;
            Edge in = cycle.get(cycle.size() - 1);
            Edge out = cycle.get(0);
            Edge chord = constrainedGraph.get(new Edge(in.getFirst(), out.getSecond()));
            if (chord != null) {
                cycle.remove(cycle.size() - 1);
                cycle.set(0, chord);
                prog = true;
            }
        } while (prog);

        // Normalize
        int first = 0;
        int minId = Integer.MAX_VALUE;
        int counter = 0;
        for (Edge e : cycle) {
            if (e.getFirst() < minId) {
                minId = e.getFirst();
                first = counter;
            }
            counter++;
        }
        Collections.rotate(cycle, -first);
    }

    /*private final ArrayList<Integer> toAdd = new ArrayList<>();

    private void applyChanges() {
        ensureCapacity();
        markedNodes.addAll(toAdd);
        toAdd.clear();
    }

    private void cancelChanges() {
        ensureCapacity();
        toAdd.clear();
    }*/

    private void ensureCapacity() {
        markedNodes.ensureCapacity(domain.size());
        while (nodeMap.size() < domain.size()) {
            nodeMap.add(new Node(nodeMap.size()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onChanged(CAATPredicate predicate, Collection<? extends Derivable> added) {
        for (Edge e : (Collection<Edge>)added) {
            markedNodes.ensureCapacity(Math.max(e.getFirst(), e.getSecond()) + 1);
            if (markedNodes.add(e.getFirst()) || markedNodes.add(e.getSecond())) {
                noChanges = false;
            }
        }
    }

    @Override
    public void onPush() {
        //applyChanges();
        markedNodes.increaseLevel();
        cleanUp();
    }

    @Override
    public void onBacktrack(CAATPredicate predicate, int time) {
        //cancelChanges();
        markedNodes.resetToLevel((short)time);
        noChanges = false;

        //System.out.println("\nMarked Nodes on Backtrack in Constraint " + thisCounter + ": ");
        //System.out.println(markedNodes);
        cleanUp();
    }

    @Override
    public void onDomainInit(CAATPredicate predicate, Domain<?> domain) {
        super.onDomainInit(predicate, domain);
        cleanUp();
        int domSize = domain.size();
        markedNodes.clear();
        markedNodes.ensureCapacity(domSize);
        nodeMap = new ArrayList<>(domSize);
        for (int i = 0; i < domSize; i++) {
            nodeMap.add(i, new Node(i));
        }
    }

    @Override
    public void onPopulation(CAATPredicate predicate) {
        Preconditions.checkArgument(predicate instanceof RelationGraph, "Expected relation graph.");
        onChanged(predicate, predicate.setView());
    }

    private void cleanUp() {
        violatingSccs.forEach(SET_COLLECTION_POOL::returnToPool);
        violatingSccs.clear();
        nodeMap.clear();
        cycles.clear();
        undershootCycles.clear();
        overshootEdges.clear();
    }


    // ============== Tarjan & SCCs ================

    private final Deque<Node> stack = new ArrayDeque<>();
    private int index = 0;
    private void tarjan() {

        index = 0;
        stack.clear();

        for (Node node : nodeMap) {
            node.reset();
        }

        for (Node node : nodeMap) {
            if (!node.wasVisited() && markedNodes.contains(node.id)) {
                strongConnect(node);
            }
        }
    }

    private void strongConnect(Node v) { strongConnect(v, MAX_OVERSHOOT); }

    // The TEMP_LIST is used to temporary hold the nodes in an SCC.
    // The SCC will only actually get created if it is violating! (selfloop or size > 1)
    private static final ArrayList<Integer> TEMP_LIST = new ArrayList<>();
    private void strongConnect(Node v, int overshoot) {
        v.index = index;
        v.lowlink = index;
        stack.push(v);
        v.isOnStack = true;
        index++;

        for (Edge e : constrainedGraph.weakOutEdges(v.id)) {
            int innerOvershoot = overshoot;
            if (!e.isActive()) {
                if (overshoot > 0) {
                    innerOvershoot--;
                } else {
                    continue;
                }
            }
            Node w = nodeMap.get(e.getSecond());
            if (!w.wasVisited()) {
                strongConnect(w, innerOvershoot);
                v.lowlink = Math.min(v.lowlink, w.lowlink);
            } else if (w.isOnStack) {
                v.lowlink = Math.min(v.lowlink, w.index);
            }

            if (w == v) {
                v.hasSelfLoop = true;
            }
        }


        if (v.lowlink == v.index) {
            Node w;
            do {
                w = stack.pop();
                w.isOnStack = false;
                TEMP_LIST.add(w.id);
            } while (w != v);

            if (v.hasSelfLoop || TEMP_LIST.size() > 1) {
                DenseIntegerSet scc = SET_COLLECTION_POOL.get();
                scc.ensureCapacity(domain.size());
                scc.clear();
                scc.addAll(TEMP_LIST);
                violatingSccs.add(scc);
            }
            TEMP_LIST.clear();
        }
    }

    private static class Node {
        final int id;

        boolean hasSelfLoop = false;
        boolean isOnStack = false;
        int index = -1;
        int lowlink = -1;

        public Node(int id) {
            this.id = id;
        }

        boolean wasVisited() {
            return index != -1;
        }

        public void reset() {
            hasSelfLoop = false;
            isOnStack = false;
            index = -1;
            lowlink = -1;
        }

        @Override
        public String toString() {
            return Integer.toString(id);
        }
    }
}