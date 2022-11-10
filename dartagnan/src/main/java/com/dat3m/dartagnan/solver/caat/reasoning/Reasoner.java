package com.dat3m.dartagnan.solver.caat.reasoning;

import com.dat3m.dartagnan.solver.caat.constraints.AcyclicityConstraint;
import com.dat3m.dartagnan.solver.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.caat.misc.EdgeDirection;
import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.misc.PredicateVisitor;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat.predicates.sets.SetPredicate;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;

import java.util.*;
import java.util.stream.Collectors;

import static com.dat3m.dartagnan.solver.caat.misc.PathAlgorithm.findShortestPath;

@SuppressWarnings("unchecked")
public class Reasoner {

    private final GraphVisitor graphVisitor = new GraphVisitor();
    private final SetVisitor setVisitor = new SetVisitor();
    private final Set<RelationGraph> externalCut;

    public Reasoner(Set<RelationGraph> externalCut) {
        this.externalCut = externalCut;
    }

    // ========================== Reason computation ==========================

    public DNF<CAATLiteral> computeViolationReasons(Constraint constraint, Context toCut) {
        if (!constraint.checkForViolations()) {
            return DNF.FALSE();
        }

        CAATPredicate pred = constraint.getConstrainedPredicate();
        Collection<? extends Collection<? extends Derivable>> violations = constraint.getViolations();

        List<List<List<Conjunction<CAATLiteral>>>> reasonList = new ArrayList<>(violations.size());

        if (constraint instanceof AcyclicityConstraint) {
            // For acyclicity constraints, it is likely that we encounter the same
            // edge multiple times (as it can be part of different cycles)
            // so we memoize the computed reasons and reuse them if possible.
            final RelationGraph constrainedGraph = (RelationGraph) pred;
            final int mapSize = violations.stream().mapToInt(Collection::size).sum() * 4 / 3;
            final Map<Edge, List<Conjunction<CAATLiteral>>> reasonMap = new HashMap<>(mapSize);
            for (Collection<Edge> violation : (Collection<Collection<Edge>>)violations) {
                 reasonList.add(violation.stream()
                        .map(edge -> reasonMap.computeIfAbsent(edge, key -> computeReason(constrainedGraph, key, toCut)))
                        .collect(Collectors.toList()));
            }
        } else {
            for (Collection<? extends Derivable> violation : violations) {
                 reasonList.add(violation.stream()
                        .map(edge -> computeReason(pred, edge, toCut))
                        .collect(Collectors.toList()));
            }
        }

        DNF<CAATLiteral> reasons = DNF.FALSE();

        for (List<List<Conjunction<CAATLiteral>>> violationReasonList : reasonList) {
            DNF<CAATLiteral> edgeReasons = DNF.TRUE();
            for (List<Conjunction<CAATLiteral>> edgeReasonsList : violationReasonList) {
                DNF<CAATLiteral> singleEdgeReason = DNF.FALSE();
                for (Conjunction<CAATLiteral> singleEdgeReasonConj : edgeReasonsList) {
                    // multiple reasons for a single edge
                    singleEdgeReason = singleEdgeReason.or(new DNF<>(singleEdgeReasonConj));
                }
                // all edges participating in a violation
                edgeReasons = edgeReasons.and(singleEdgeReason);
            }
            // all violations
            reasons = reasons.or(edgeReasons);
        }

        return reasons;
    }

    public List<Conjunction<CAATLiteral>> computeReason(CAATPredicate pred, Derivable prop, Context toCut) {
        if (pred instanceof RelationGraph && prop instanceof Edge) {
            return computeReason((RelationGraph) pred, (Edge) prop, toCut);
        } else if (pred instanceof SetPredicate && prop instanceof Element) {
            return computeReason((SetPredicate) pred, (Element) prop);
        } else {
            return List.of(Conjunction.FALSE());
        }
    }


    public List<Conjunction<CAATLiteral>> computeReason(RelationGraph graph, Edge edge, Context toCut) {
        if (!graph.contains(edge)) {
            return List.of(Conjunction.FALSE());
        }

        // Difference is handled on its own, therefore all literals are positive
        if (externalCut.contains(graph)) {
            toCut.chooseRelation(graph);
            return List.of(new EdgeLiteral(graph.getName(), edge, false).toSingletonReason());
        }
        if (toCut.isCovered(graph, edge)) {
            return List.of(new EdgeLiteral(graph.getName(), edge, false).toSingletonReason());
        }

        List<Conjunction<CAATLiteral>> reason = graph.accept(graphVisitor, edge, toCut);
        for (Conjunction<CAATLiteral> singleReason : reason) {
            assert !singleReason.isFalse();
        }
        return reason;
    }

    public List<Conjunction<CAATLiteral>> computeReason(SetPredicate set, Element ele) {
        if (!set.contains(ele)) {
            return List.of(Conjunction.FALSE());
        }

        List<Conjunction<CAATLiteral>> reason = set.accept(setVisitor, ele, null);
        for (Conjunction<CAATLiteral> singleReason : reason) {
            assert !singleReason.isFalse();
        }
        return reason;
    }

    // ======================== Visitors ==========================
    /*
        The visitors are used to traverse the structure of the predicate hierarchy
        and compute reasons for each predicate
     */

    private class GraphVisitor implements PredicateVisitor<List<Conjunction<CAATLiteral>>, Edge, Context> {

        @Override
        public List<Conjunction<CAATLiteral>> visit(CAATPredicate predicate, Edge edge, Context toCut) {
            throw new IllegalArgumentException(predicate.getName() + " is not supported in reasoning computation");
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitGraphUnion(RelationGraph graph, Edge edge, Context toCut) {
            List<Conjunction<CAATLiteral>> reason = new ArrayList<>();
            for (RelationGraph g : (List<RelationGraph>) graph.getDependencies()) {
                Edge e = g.get(edge);
                if (e != null) {
                    reason.addAll(computeReason(g, e, toCut));
                }
            }

            assert !reason.isEmpty();
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;

        }

        @Override
        public List<Conjunction<CAATLiteral>> visitGraphIntersection(RelationGraph graph, Edge edge, Context toCut) {
            List<List<Conjunction<CAATLiteral>>> reasonList = new ArrayList<>();
            for (RelationGraph g : (List<RelationGraph>) graph.getDependencies()) {
                Edge e = g.get(edge);
                if (e != null) {
                    reasonList.add(computeReason(g, e, toCut));
                }
            }
            if (reasonList.size() == 0) {
                reasonList.add(List.of(Conjunction.TRUE()));
            }
            List<Conjunction<CAATLiteral>> reason = ANDingReasons(reasonList);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitGraphComposition(RelationGraph graph, Edge edge, Context toCut) {
            RelationGraph first = (RelationGraph) graph.getDependencies().get(0);
            RelationGraph second = (RelationGraph) graph.getDependencies().get(1);

            // We use the first composition that we find
            List<Conjunction<CAATLiteral>> reason;
            if (first.getEstimatedSize(edge.getFirst(), EdgeDirection.OUTGOING)
                    <= second.getEstimatedSize(edge.getSecond(), EdgeDirection.INGOING)) {
                for (Edge e1 : first.outEdges(edge.getFirst())) {
                    if (e1.getDerivationLength() >= edge.getDerivationLength()) {
                        continue;
                    }
                    Edge e2 = second.get(new Edge(e1.getSecond(), edge.getSecond()));
                    if (e2 != null && e2.getDerivationLength() < edge.getDerivationLength()) {
                        reason = ANDingReasons(computeReason(first, e1, toCut), computeReason(second, e2, toCut));
                        for (Conjunction<CAATLiteral> singleReason : reason) {
                            assert !singleReason.isFalse();
                        }
                        return reason;
                    }
                }
            } else {
                for (Edge e2 : second.inEdges(edge.getSecond())) {
                    if (e2.getDerivationLength() >= edge.getDerivationLength()) {
                        continue;
                    }
                    Edge e1 = first.get(new Edge(edge.getFirst(), e2.getFirst()));
                    if (e1 != null && e1.getDerivationLength() < edge.getDerivationLength()) {
                        reason = ANDingReasons(computeReason(first, e1, toCut), computeReason(second, e2, toCut));
                        for (Conjunction<CAATLiteral> singleReason : reason) {
                            assert !singleReason.isFalse();
                        }
                        return reason;
                    }
                }
            }

            throw new IllegalStateException("Did not find a reason for " + edge + " in " + graph.getName());
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitCartesian(RelationGraph graph, Edge edge, Context toCut) {
            SetPredicate lhs = (SetPredicate) graph.getDependencies().get(0);
            SetPredicate rhs = (SetPredicate) graph.getDependencies().get(1);

            List<Conjunction<CAATLiteral>> lhsReason = computeReason(lhs, lhs.getById(edge.getFirst()));
            List<Conjunction<CAATLiteral>> rhsReason = computeReason(rhs, rhs.getById(edge.getSecond()));

            List<Conjunction<CAATLiteral>> reason = ANDingReasons(lhsReason, rhsReason);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitGraphDifference(RelationGraph graph, Edge edge, Context toCut) {
            RelationGraph lhs = (RelationGraph) graph.getDependencies().get(0);
            RelationGraph rhs = (RelationGraph) graph.getDependencies().get(1);

            if (rhs.getDependencies().size() > 0) {
                // TODO: Check if rhs is recursive
                toCut.chooseRelation(rhs);
            }

            List<Conjunction<CAATLiteral>> reason = computeReason(lhs, edge, toCut);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                singleReason.and(new EdgeLiteral(rhs.getName(), edge, true).toSingletonReason());
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitInverse(RelationGraph graph, Edge edge, Context toCut) {
            List<Conjunction<CAATLiteral>> reason = computeReason((RelationGraph) graph.getDependencies().get(0),
                    edge.inverse().withDerivationLength(edge.getDerivationLength() - 1), toCut);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitSetIdentity(RelationGraph graph, Edge edge, Context toCut) {
            assert edge.isLoop();

            SetPredicate inner = (SetPredicate) graph.getDependencies().get(0);
            Element e = inner.getById(edge.getFirst());
            return computeReason(inner, e);
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitRangeIdentity(RelationGraph graph, Edge edge, Context toCut) {
            assert edge.isLoop();

            RelationGraph inner = (RelationGraph) graph.getDependencies().get(0);
            for (Edge inEdge : inner.inEdges(edge.getSecond())) {
                // We use the first edge we find
                if (inEdge.getDerivationLength() < edge.getDerivationLength()) {
                    List<Conjunction<CAATLiteral>> reason = computeReason(inner, inEdge, toCut);
                    for (Conjunction<CAATLiteral> singleReason : reason) {
                        assert !singleReason.isFalse();
                    }
                    return reason;
                }
            }
            throw new IllegalStateException("RangeIdentityGraph: No matching edge is found");
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitReflexiveClosure(RelationGraph graph, Edge edge, Context toCut) {
            if (edge.isLoop()) {
                return List.of(Conjunction.TRUE());
            } else {
                List<Conjunction<CAATLiteral>> reason = computeReason((RelationGraph) graph.getDependencies().get(0), edge, toCut);
                for (Conjunction<CAATLiteral> singleReason : reason) {
                    assert !singleReason.isFalse();
                }
                return reason;
            }
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitTransitiveClosure(RelationGraph graph, Edge edge, Context toCut) {
            RelationGraph inner = (RelationGraph) graph.getDependencies().get(0);
            List<Edge> path = findShortestPath(inner, edge.getFirst(), edge.getSecond(), edge.getDerivationLength() - 1);
            List<List<Conjunction<CAATLiteral>>> pathReasons = new ArrayList<>();
            for (Edge e : path) {
                pathReasons.add(computeReason(inner, e, toCut));
            }
            List<Conjunction<CAATLiteral>> reason = ANDingReasons(pathReasons);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitRecursiveGraph(RelationGraph graph, Edge edge, Context toCut) {
            List<Conjunction<CAATLiteral>> reason = computeReason((RelationGraph) graph.getDependencies().get(0), edge, toCut);
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitBaseGraph(RelationGraph graph, Edge edge, Context toCut) {
            return List.of(new EdgeLiteral(graph.getName(), edge, false).toSingletonReason());
        }
    }

    private class SetVisitor implements PredicateVisitor<List<Conjunction<CAATLiteral>>, Element, Void> {

        // ============================ Sets =========================

        @Override
        public List<Conjunction<CAATLiteral>> visitSetUnion(SetPredicate set, Element ele, Void unused) {
            List<Conjunction<CAATLiteral>> reason = new ArrayList<>();
            for (SetPredicate s : set.getDependencies()) {
                Element e = s.get(ele);
                if (e != null) {
                    reason.addAll(computeReason(s, e));
                }
            }
            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitSetIntersection(SetPredicate set, Element ele, Void unused) {
            List<List<Conjunction<CAATLiteral>>> reasonList = new ArrayList<>();
            for (SetPredicate s : set.getDependencies()) {
                Element e = set.get(ele);
                if (e != null) {
                    reasonList.add(computeReason(s, e));
                }
            }

            List<Conjunction<CAATLiteral>> reason = ANDingReasons(reasonList);

            for (Conjunction<CAATLiteral> singleReason : reason) {
                assert !singleReason.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitSetDifference(SetPredicate set, Element ele, Void unused) {
            SetPredicate lhs = set.getDependencies().get(0);
            SetPredicate rhs = set.getDependencies().get(1);

            if (rhs.getDependencies().size() > 0) {
                throw new IllegalStateException(String.format("Cannot compute reason of element %s in " +
                        "set difference %s because its right-hand side %s is derived.", ele, set, rhs));
            }

            List<Conjunction<CAATLiteral>> reason = computeReason(lhs, ele);
            Conjunction<CAATLiteral> newReason = new ElementLiteral(rhs.getName(), ele, true).toSingletonReason();
            for (Conjunction<CAATLiteral> sub : reason) {
                sub.and(newReason);
                assert !sub.isFalse();
            }
            return reason;
        }

        @Override
        public List<Conjunction<CAATLiteral>> visitBaseSet(SetPredicate set, Element ele, Void unused) {
            return List.of(new ElementLiteral(set.getName(), ele, false).toSingletonReason());
        }
    }

    //============================================= Helper Methods ===============================================

    // expects first and second to be non-empty; static reasoning has to be specified explicitly
    private List<Conjunction<CAATLiteral>> ANDingReasons(List<Conjunction<CAATLiteral>> first, List<Conjunction<CAATLiteral>> second) {
        List<Conjunction<CAATLiteral>> intersection = new ArrayList<>();
        for (Conjunction<CAATLiteral> firstReason : first) {
            for (Conjunction<CAATLiteral> secondReason : second) {
                Conjunction<CAATLiteral> composed = new Conjunction<>(firstReason.getLiterals());
                composed.and(secondReason);
                intersection.add(composed);
            }
        }
        return intersection;
    }

    private List<Conjunction<CAATLiteral>> ANDingReasons(List<List<Conjunction<CAATLiteral>>> reasons) {
        if (reasons.size() == 0) {
            return new ArrayList<>();
        }
        List<Conjunction<CAATLiteral>> workingSet = reasons.remove(0);
        for (List<Conjunction<CAATLiteral>> singleReason : reasons) {
            workingSet = ANDingReasons(workingSet, singleReason);
        }
        return workingSet;
    }
}
