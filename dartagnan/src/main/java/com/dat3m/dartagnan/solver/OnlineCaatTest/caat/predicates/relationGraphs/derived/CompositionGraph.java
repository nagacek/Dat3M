package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.derived;


import com.dat3m.dartagnan.solver.OnlineCaatTest.BoneInfo;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.PredicateHierarchy;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.MaterializedGraph;
import com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.utils.collections.SetUtil;

import java.util.*;

public class CompositionGraph extends MaterializedGraph {

    private final RelationGraph first;
    private final RelationGraph second;

    private HashMap<Edge, Integer> triggerTimes = new HashMap<>();

    @Override
    protected Set<Edge> computeFromInnerEdges() {
        HashSet<Edge> innerEdges = new HashSet<>();
        for (Object o : domain.getElements()) {
            int id = domain.getId(o);
            if (id >= 0) {
                Iterable<Edge> firstEdges = first.inEdges(id);
                for (Edge e1 : firstEdges) {
                    Iterable<Edge> secondEdges = second.outEdges(id);
                    for (Edge e2 : secondEdges) {
                        if (innerEdges.add(combine(e1, e2, Math.max(e1.getTime(), e2.getTime())))){
                            int i = 5;
                        }
                    }
                }
            }
        }
        return innerEdges;
    }

    @Override
    public List<RelationGraph> getDependencies() {
        return Arrays.asList(first, second);
    }

    public RelationGraph getFirst() { return first; }
    public RelationGraph getSecond() { return second; }

    public CompositionGraph(RelationGraph first, RelationGraph second) {
        this.first = first;
        this.second = second;
    }


    @Override
    public <TRet, TData, TContext> TRet accept(PredicateVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitGraphComposition(this, data, context);
    }

    @Override
    public void repopulate() {
        Set<Edge> fakeSet = SetUtil.fakeSet();
        if (first.getEstimatedSize() <= second.getEstimatedSize()) {
            for (Edge a : first.edges()) {
                updateFirst(a, fakeSet);
            }
        } else {
            for (Edge a : second.edges()) {
                updateSecond(a, fakeSet);
            }
        }
    }

    private Edge combine(Edge a, Edge b, int time) {
        return new Edge(a.getFirst(), b.getSecond(), time,
                Math.max(a.getDerivationLength(), b.getDerivationLength()) + 1, a.isBone() && b.isBone(), a.isActive() && b.isActive());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Edge> forwardPropagate(CAATPredicate changedSource, Collection<? extends Derivable> added, PredicateHierarchy.PropagationMode mode) {
        ArrayList<Edge> newEdges = new ArrayList<>();
        Collection<Edge> addedEdges = (Collection<Edge>) added;
        if (changedSource == first) {
            // (A+R);B = A;B + R;B
            for (Edge e : addedEdges) {
                updateFirst(e, newEdges);
            }
        }
        if (changedSource == second) {
            // A;(B+R) = A;B + A;R
            for (Edge e : addedEdges) {
                updateSecond(e, newEdges);
            }
        }
        if (changedSource == null && (mode == PredicateHierarchy.PropagationMode.DEFER || mode == PredicateHierarchy.PropagationMode.DELETE)) {
            for (Edge e : addedEdges) {
                if (simpleGraph.add(e)) {
                    newEdges.add(e);
                }
            }
        }
        // For A;A, we have the following:
        // (A+R);(A+R) = A;A + A;R + R;A + R;R = A;A + (A+R);R + R;(A+R)
        // So we add (A+R);R and R;(A+R), which is done by doing both of the above update procedures
        return newEdges;
    }

    @Override
    public int staticDerivationLength() {
        if (maxDerivationLength < 0) {
            maxDerivationLength = Math.max(first.staticDerivationLength(), second.staticDerivationLength()) + 1;
        }
        return maxDerivationLength;
    }

    @Override
    public Set<Edge> checkBoneActivation(int triggerId, int time, Set<BoneInfo> bones) {
        Set<Edge> newEdges = new HashSet<>();
        for (BoneInfo info : bones) {
            Edge edge = info.edge();
            if (info.activeEvents() && info.condition() || triggerId != edge.getFirst() && triggerId != edge.getSecond()) {
                Integer triggerTime = triggerTimes.get(edge);
                if (triggerTime == null || (int) triggerTime < 0) {
                    triggerTimes.put(edge, time);
                } else {
                    assert (triggerTime <= time);
                }
            }
            if (info.activeEvents()) {
                Integer triggerTime = triggerTimes.get(edge);
                if (triggerTime != null && triggerTime >= 0) {
                    newEdges.add(info.edge().withTime(time));
                }
            }
        }
        return newEdges;
    }

    @Override
    public void backtrackTo(int time) {
        super.backtrackTo(time);
        HashSet<Edge> toDelete = new HashSet<>();
        for (Map.Entry<Edge, Integer> triggerEntry : triggerTimes.entrySet()) {
            Integer triggerTime = triggerEntry.getValue();
            if (triggerTime != null && time < triggerTime) {
                toDelete.add(triggerEntry.getKey());
            }
        }
        for (Edge e : toDelete) {
            triggerTimes.remove(e);
        }
    }

    private void updateFirst(Edge a, Collection<Edge> addedEdges) {
        for (Edge b : second.outEdges(a.getSecond())) {
            Edge c = combine(a, b, Math.max(a.getTime(), b.getTime()));
            if (simpleGraph.add(c)) {
                addedEdges.add(c);
            }
        }
    }

    private void updateSecond(Edge b, Collection<Edge> addedEdges) {
        for (Edge a : first.inEdges(b.getFirst())) {
            Edge c = combine(a, b, Math.max(a.getTime(), b.getTime()));
            if (simpleGraph.add(c)) {
                addedEdges.add(c);
            }
        }
    }


}
