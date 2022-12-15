package com.dat3m.dartagnan.solver.caat.predicates.misc;

import com.dat3m.dartagnan.solver.caat.predicates.CAATPredicate;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;

import java.util.*;

public class SccComplexity {
    private final HashMap<Set<DependencyGraph<CAATPredicate>.Node>, Integer> complexity;

    public SccComplexity() {
        complexity = new HashMap<>();
    }

    // is expected to be called in topological order
    // TODO: principle does not work as exit points are usually recursive relations themselves
    public void initializeComplexities(Set<DependencyGraph<CAATPredicate>.Node> scc) {
        Set<CAATPredicate> exitingPoints = findExitingPoints(scc);
        Set<Derivable> minComplexities = new HashSet<>();
        for (CAATPredicate pred : exitingPoints) {
            Derivable minDer = null;
            int minComplexity = -1;
            for (Iterator<? extends Derivable> it = pred.valueIterator(); it.hasNext(); ) {
                Derivable der = it.next();
                int derComplexity = der.getComplexity();
                if (minDer == null || derComplexity < minComplexity) {
                    minDer = der;
                    minComplexity = derComplexity;
                }
            }
            assert(minDer != null);
            minComplexities.add(minDer);
            complexity.put(scc, minComplexities.stream().map(Derivable::getComplexity).max((Comparator.comparingInt(a -> a))).get());

            // inform recursive predicated about their complexity
            int chosenComplexity = complexity.get(scc);
            for (DependencyGraph<CAATPredicate>.Node node : scc) {
                node.getContent().injectStaticComplexity(chosenComplexity);
            }
        }
    }

    private Set<CAATPredicate> findExitingPoints(Set<DependencyGraph<CAATPredicate>.Node> scc) { // TODO: what about axioms? are constrained relations exiting points, too?
        Set<CAATPredicate> exitingPoints = new HashSet<>();
        for (DependencyGraph<CAATPredicate>.Node node : scc) {
            for (DependencyGraph<CAATPredicate>.Node dependent : node.getDependents()) {
                if (!scc.contains(dependent)) {
                    exitingPoints.add(node.getContent());
                    break;
                }
            }
        }
        return exitingPoints;
    }
}
