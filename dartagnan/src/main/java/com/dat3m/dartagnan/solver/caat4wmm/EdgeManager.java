package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.collect.BiMap;

import java.util.HashSet;
import java.util.Set;

public class EdgeManager {
    private final TupleSetMap edges;
    private final Set<Relation> cutRelations;
    private ExecutionGraph execGraph;
    private ExecutionModel model;

    public EdgeManager(Set<Relation> cutRelations) {
        edges = new TupleSetMap();
        this.cutRelations = cutRelations;
    }

    public void init(ExecutionModel model, ExecutionGraph execGraph) {
        this.model = model;
        this.execGraph = execGraph;
    }

    public TupleSetMap addEagerlyEncodedEdges(TupleSetMap newEdges) {
        if (newEdges == null) {
            return new TupleSetMap();
        }
        TupleSetMap distinctEdges = newEdges.difference(edges);
        edges.merge(newEdges);
        return distinctEdges;
    }

    public TupleSetMap substractFrom(TupleSetMap other) {
        return other.difference(edges);
    }

    public EdgeSetMap initCAATView() {
        return EdgeSetMap.fromTupleSetMap(edges, model, execGraph);
    }

    public Set<Relation> getRelations() {
        return cutRelations;
    }
    public Set<RelationGraph> transformCAATRelations(ExecutionGraph graph) {
        Set<RelationGraph> caatRelations = new HashSet<>();
        cutRelations.forEach(rel -> caatRelations.add(graph.getRelationGraph(rel)));
        return caatRelations;
    }

    public boolean isEagerlyEncoded(Relation rel, Tuple edge) {
        return edges.contains(rel, edge);
    }

    public boolean isEagerlyEncoded(Relation rel) {
        return edges.contains(rel);
    }

    public String toString() {
        String output = "EdgeManager is \n";
        for (Relation rel : edges.getRelations()) {
            output += rel.getName() + "\n    ";
            boolean first = true;
            for (var edge : edges.get(rel)) {
                if (!first) {
                    output += ", ";
                }
                first = false;
                output += "(" + edge.getFirst().getCId() + "," + edge.getSecond().getCId() + ")";
            }
            output += "\n";
        }
        return output;
    }
}
