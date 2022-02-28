package com.dat3m.dartagnan.wmm.analysis.relationAnalysis.example;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.analysis.relationAnalysis.Knowledge;
import com.dat3m.dartagnan.wmm.analysis.relationAnalysis.newWmm.CATConstraint;
import com.dat3m.dartagnan.wmm.analysis.relationAnalysis.newWmm.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

public class Acyclic extends CATConstraint {

    private TupleSet transitiveMinSet;

    public Acyclic(Relation rel) { super(rel);}

    @Override
    public List<Knowledge.Delta> computeInitialKnowledgeClosure(Map<Relation, Knowledge> know) {
        Knowledge k = know.get(rel);

        ExecutionAnalysis exec = analysisContext.get(ExecutionAnalysis.class);
        TupleSet minSet = k.getMustSet();

        // (1) Approximate transitive closure of minSet (only gets computed when crossEdges are available)
        List<Tuple> crossEdges = minSet.stream()
                .filter(t -> t.isCrossThread() && !t.getFirst().is(Tag.INIT))
                .collect(Collectors.toList());
        TupleSet transMinSet = crossEdges.isEmpty() ? minSet : new TupleSet(minSet);
        for (Tuple crossEdge : crossEdges) {
            Event e1 = crossEdge.getFirst();
            Event e2 = crossEdge.getSecond();

            List<Event> ingoing = new ArrayList<>();
            ingoing.add(e1); // ingoing events + self
            minSet.getBySecond(e1).stream().map(Tuple::getFirst)
                    .filter(e -> exec.isImplied(e, e1))
                    .forEach(ingoing::add);


            List<Event> outgoing = new ArrayList<>();
            outgoing.add(e2); // outgoing edges + self
            minSet.getByFirst(e2).stream().map(Tuple::getSecond)
                    .filter(e -> exec.isImplied(e, e2))
                    .forEach(outgoing::add);

            for (Event in : ingoing) {
                for (Event out : outgoing) {
                    transMinSet.add(new Tuple(in, out));
                }
            }
        }

        // Disable all edges opposing the transitive min set
        this.transitiveMinSet = transMinSet;
        return Collections.singletonList(new Knowledge.Delta(transitiveMinSet.inverse(), new TupleSet()));
    }

    @Override
    public List<Knowledge.Delta> computeIncrementalKnowledgeClosure(Relation changed, Knowledge.Delta delta, Map<Relation, Knowledge> know) {
        assert  changed == getConstrainedRelation();
        Knowledge.Delta newDelta = new Knowledge.Delta();
        if (delta.getEnabledSet().isEmpty()) {
            // We can only derive new knowledge from added edges, so if we have none, we return early
            return Collections.singletonList(newDelta);
        }

        for (Tuple t : delta.getEnabledSet()) {
            if (transitiveMinSet.add(t)) {
                newDelta.getDisabledSet().add(t.getInverse());
            }
        }
        //TODO: we should transitively close <transitiveMinSet>
        return Collections.singletonList(newDelta);
    }

    @Override
    public List<TupleSet> computeActiveSets(Map<Relation, Knowledge> know) {
        // ====== Construct [Event -> Successor] mapping ======
        Map<Event, Collection<Event>> succMap = new HashMap<>();
        TupleSet relMay = know.get(rel).getMaySet();
        for (Tuple t : relMay) {
            succMap.computeIfAbsent(t.getFirst(), key -> new ArrayList<>()).add(t.getSecond());
        }

        // ====== Compute SCCs ======
        DependencyGraph<Event> depGraph = DependencyGraph.from(succMap.keySet(), succMap);
        TupleSet activeSet = new TupleSet();
        for (Set<DependencyGraph<Event>.Node> scc : depGraph.getSCCs()) {
            for (DependencyGraph<Event>.Node node1 : scc) {
                for (DependencyGraph<Event>.Node node2 : scc) {
                    Tuple t = new Tuple(node1.getContent(), node2.getContent());
                    if (relMay.contains(t)) {
                        activeSet.add(t);
                    }
                }
            }
        }

        if (GlobalSettings.REDUCE_ACYCLICITY_ENCODE_SETS) {
            //TODO: Recompute <transitiveMinSet> as it may be outdated
            // and worse yet, it may be actually wrong (though this should not happen)
            TupleSet reduct = TupleSet.approximateTransitiveMustReduction(
                    analysisContext.get(ExecutionAnalysis.class), transitiveMinSet);
            // Remove (must(r)+ \ reduct(must(r)+)
            activeSet.removeAll(Sets.difference(transitiveMinSet, reduct));
        }

        return Collections.singletonList(activeSet);
    }
}
