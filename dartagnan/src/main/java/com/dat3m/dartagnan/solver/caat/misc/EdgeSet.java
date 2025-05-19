package com.dat3m.dartagnan.solver.caat.misc;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.Domain;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.utils.graph.EventGraph;

import java.util.*;

public class EdgeSet {
    private final List<Set<Integer>> edgeMap;

    private EdgeSet() {
        edgeMap = new ArrayList<>();
    }

    public static EdgeSet from(Domain<Event> domain, EventGraph graph) {
        EdgeSet edgeSet = new EdgeSet();
        for (Map.Entry<Event, Set<Event>> entry : graph.getOutMap().entrySet()) {
            int firstId = domain.getId(entry.getKey());
            edgeSet.ensureCapacity(firstId);
            Set<Integer> edges = edgeSet.getOutgoingEdges(firstId);
            for (Event secondEvent : entry.getValue()) {
                int secondId = domain.getId(secondEvent);
                edges.add(secondId);
            }
        }
        return edgeSet;
    }

    private void ensureCapacity(int maxId) {
        while (edgeMap.size() <= maxId) {
            edgeMap.add(new HashSet<>());
        }
    }

    public Set<Integer> getOutgoingEdges(int firstId) {
        if (firstId < edgeMap.size()) {
            return edgeMap.get(firstId);
        } else {
            return Collections.emptySet();
        }
    }

}
