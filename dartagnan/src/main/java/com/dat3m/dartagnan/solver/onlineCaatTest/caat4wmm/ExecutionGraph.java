package com.dat3m.dartagnan.solver.onlineCaatTest.caat4wmm;

import com.dat3m.dartagnan.solver.onlineCaatTest.caat.CAATModel;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.constraints.AcyclicityConstraint;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.constraints.EmptinessConstraint;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.constraints.IrreflexivityConstraint;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.base.SimpleGraph;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.derived.*;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat4wmm.RefinementModel;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.axiom.ForceEncodeAxiom;
import com.dat3m.dartagnan.wmm.definition.*;
import com.dat3m.dartagnan.wmm.utils.EventGraph;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import java.util.*;
import java.util.stream.Collectors;

public class ExecutionGraph {

    // ================== Fields =====================

    // All following variables can be considered "final".
    // Some are not declared final for purely technical reasons but all of them get
    // assigned during construction.

    private final RefinementModel refinementModel;
    private final BiMap<Relation, RelationGraph> relationGraphMap;
    private final BiMap<Axiom, Constraint> constraintMap;
    private final Set<Relation> cutRelations;

    private CAATModel caatModel;

    // =================================================

    // ============= Construction & Init ===============

    public ExecutionGraph(RefinementModel refinementModel) {
        this.refinementModel = refinementModel;
        relationGraphMap = HashBiMap.create();
        constraintMap = HashBiMap.create();
        this.cutRelations = refinementModel.computeBoundaryRelations().stream()
                .filter(r -> r.getName().map(n -> !Wmm.ANARCHIC_CORE_RELATIONS.contains(n)).orElse(true))
                .map(refinementModel::translateToOriginal)
                .collect(Collectors.toSet());
        constructMappings();
    }

    public void initializeToDomain(Domain<?> domain) {
        caatModel.initializeToDomain(domain);
    }

    public void initializeActiveSets(Map<RelationGraph, Set<Derivable>> activeSets) {
        caatModel.initializeActiveSets(activeSets);
    }

    public void validate(int time, boolean active) {
        caatModel.validate(time, active);
    }

    // --------------------------------------------------

    private void constructMappings() {
        final Wmm memoryModel = refinementModel.getOriginalModel();

        Set<RelationGraph> graphs = new HashSet<>();
        Set<Constraint> constraints = new HashSet<>();
        DependencyGraph<Relation> dependencyGraph = DependencyGraph.from(memoryModel.getRelations());

        for (Set<DependencyGraph<Relation>.Node> component : dependencyGraph.getSCCs()) {
            if (component.size() > 1) {
                for (DependencyGraph<Relation>.Node node : component) {
                    Relation relation = node.getContent();
                    if (relation.isRecursive()) {
                        RecursiveGraph graph = new RecursiveGraph();
                        graph.setName(relation.getNameOrTerm() + "_rec");
                        graphs.add(graph);
                        relationGraphMap.put(relation, graph);
                    }
                }
                for (DependencyGraph<Relation>.Node node : component) {
                    Relation relation = node.getContent();
                    if (relation.isRecursive()) {
                        // side effect leads to calculation of children
                        RecursiveGraph graph = (RecursiveGraph) relationGraphMap.get(relation);
                        graph.setConcreteGraph(createGraphFromRelation(relation));
                    }
                }
            }
        }

        for (Axiom axiom : memoryModel.getAxioms()) {
            if (axiom instanceof ForceEncodeAxiom || axiom.isFlagged()) {
                continue;
            }
            Constraint constraint = getOrCreateConstraintFromAxiom(axiom);
            constraints.add(constraint);
        }


        caatModel = CAATModel.from(graphs, constraints);
    }

    // =================================================

    // ================ Accessors =======================

    public CAATModel getCAATModel() {
        return caatModel;
    }

    public Domain<?> getDomain() {
        return caatModel.getDomain();
    }

    public BiMap<Relation, RelationGraph> getRelationGraphMap() {
        return Maps.unmodifiableBiMap(relationGraphMap);
    }

    public BiMap<Axiom, Constraint> getAxiomConstraintMap() {
        return Maps.unmodifiableBiMap(constraintMap);
    }

    public Set<Relation> getCutRelations() {
        return cutRelations;
    }

    public RelationGraph getRelationGraph(Relation rel) {
        return relationGraphMap.get(rel);
    }

    public RelationGraph getRelationGraphByName(String name) {
        return getRelationGraph(refinementModel.getOriginalModel().getRelation(name));
    }

    public Constraint getConstraint(Axiom axiom) {
        return constraintMap.get(axiom);
    }

    public Collection<Constraint> getConstraints() {
        return constraintMap.values();
    }

    // ====================================================

    // ==================== Mutation ======================

    public void backtrackTo(int time) {
        caatModel.getHierarchy().backtrackTo(time);
    }

    // =======================================================

    // ==================== Analysis ======================

    public boolean checkInconsistency() {
        return caatModel.checkInconsistency();
    }

    // =======================================================

    //=================== Reading the WMM ====================

    private Constraint getOrCreateConstraintFromAxiom(Axiom axiom) {
        if (constraintMap.containsKey(axiom)) {
            return constraintMap.get(axiom);
        }

        Constraint constraint;
        RelationGraph innerGraph = getOrCreateGraphFromRelation(axiom.getRelation());
        if (axiom.isAcyclicity()) {
            constraint = new AcyclicityConstraint(innerGraph);
        } else if (axiom.isEmptiness()) {
            constraint = new EmptinessConstraint(innerGraph);
        } else if (axiom.isIrreflexivity()) {
            constraint = new IrreflexivityConstraint(innerGraph);
        } else {
            throw new UnsupportedOperationException("The axiom " + axiom + " is not recognized.");
        }

        constraintMap.put(axiom, constraint);
        return constraint;
    }

    private RelationGraph getOrCreateGraphFromRelation(Relation rel) {
        if (relationGraphMap.containsKey(rel)) {
            return relationGraphMap.get(rel);
        }
        RelationGraph graph = createGraphFromRelation(rel);
        relationGraphMap.put(rel, graph);
        return graph;
    }

    private RelationGraph createGraphFromRelation(Relation rel) {
        RelationGraph graph;
        Class<?> relClass = rel.getDefinition().getClass();
        List<Relation> dependencies = rel.getDependencies();

        // ===== Filter special relations ======
        if (cutRelations.contains(rel)) {
            graph = new SimpleGraph();
        } else if (relClass == Inverse.class || relClass == TransitiveClosure.class || relClass == RangeIdentity.class) {
            RelationGraph g = getOrCreateGraphFromRelation(dependencies.get(0));
            graph = relClass == Inverse.class ? new InverseGraph(g) :
                    relClass == TransitiveClosure.class ? new TransitiveGraph(g) :
                            new RangeIdentityGraph(g);
        } else if (relClass == Union.class || relClass == Intersection.class) {
            RelationGraph[] graphs = new RelationGraph[dependencies.size()];
            for (int i = 0; i < graphs.length; i++) {
                graphs[i] = getOrCreateGraphFromRelation(dependencies.get(i));
            }
            graph = relClass == Union.class ? new UnionGraph(graphs) :
                    new IntersectionGraph(graphs);
        } else if (relClass == Composition.class || relClass == Difference.class) {
            RelationGraph g1 = getOrCreateGraphFromRelation(dependencies.get(0));
            RelationGraph g2 = getOrCreateGraphFromRelation(dependencies.get(1));
            graph = relClass == Composition.class ? new CompositionGraph(g1, g2) :
                    new DifferenceGraph(g1, g2);
        } else if (relClass == CartesianProduct.class) {
            // Unreachable for now, since Wmm treats these as base relations and we cut all base relations.
            graph = new SimpleGraph();
        } else {
            graph = new SimpleGraph();
        }

        graph.setName(rel.getNameOrTerm());
        return graph;
    }

    // =======================================================


}
