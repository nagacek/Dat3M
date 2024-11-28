package com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates;

import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.domain.Domain;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.misc.PredicateListener;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates.relationGraphs.derived.RecursiveGraph;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.Relation;
import com.dat3m.dartagnan.wmm.utils.EventGraph;

import java.util.*;
import java.util.stream.Collectors;


/*
    A PredicateHiearchy has the following uses:
        - it maintains the dependency structure of CAATPredicates
        - it initializes/populates derived CAATPredicates
        - it propagates changes topologically along the dependency structure
        - it maintains a set of PredicateListeners to detect changes to CAATPredicates (e.g. for constraints)
 */
public class PredicateHierarchy {
    private final Map<CAATPredicate, Set<PredicateListener>> listenersMap;
    private final DependencyGraph<CAATPredicate> dependencyGraph;
    private final Set<CAATPredicate> basePredicates;

    private final PriorityQueue<Task> tasks = new PriorityQueue<>();
    private final PriorityQueue<Task> deferredTasks = new PriorityQueue<>();

    private Map<? extends CAATPredicate, Set<Derivable>> activeSets;
    private Domain<?> domain;


    // ============== Construction ===============

    public PredicateHierarchy(Set<CAATPredicate> predicates) {
        dependencyGraph = DependencyGraph.from(predicates);
        // The hashmap is created with a special load factor and capacity to avoid resizing.
        listenersMap = new HashMap<>(dependencyGraph.getNodeContents().size() * 4 / 3, 0.75f);
        dependencyGraph.getNodeContents().forEach(x -> listenersMap.put(x, new HashSet<>()));

        basePredicates = dependencyGraph.getNodeContents().stream()
                .filter(p -> p.getDependencies().isEmpty()).collect(Collectors.toSet());
    }

    // ========================================

    // ============= Accessors =================

    public Set<CAATPredicate> getPredicates() { return listenersMap.keySet(); }

    public Set<CAATPredicate> getBasePredicates() { return basePredicates; }

    // The list of graphs in topological order
    public List<CAATPredicate> getPredicateList() { return dependencyGraph.getNodeContents(); }

    public DependencyGraph<CAATPredicate> getDependencyGraph() {
        return dependencyGraph;
    }

    public Domain<?> getDomain() { return domain; }

    // ==========================================

    // ============== Initialization =============

    public void validate(int time, boolean active) {
        if (active) {
            System.out.println("-------------------------------");
        }
        for (CAATPredicate pred : getPredicateList()) {
            Set<Derivable> activeSet = activeSets.get(pred);
            if (activeSet != null) {

                pred.validate(time, activeSet, active);
            }
        }
    }

    public void initializeToDomain(Domain<?> domain) {
        this.domain = domain;
        for (CAATPredicate pred : getPredicateList()) {
            pred.initializeToDomain(domain);
            for (PredicateListener listener : listenersMap.get(pred)) {
                listener.onDomainInit(pred, domain);
            }
        }
    }

    public void initializeActiveSets(Map<RelationGraph, Set<Derivable>> activeSets) {
        this.activeSets = activeSets;
    }

    // Computes the content of all derived predicates in topological order
    public void populate() {
        for (Set<DependencyGraph<CAATPredicate>.Node> scc : dependencyGraph.getSCCs()) {
            Set<CAATPredicate> recGrp = scc.stream().map(DependencyGraph.Node::getContent).collect(Collectors.toSet());
            if (recGrp.size() == 1) {
                recGrp.stream().findAny().get().repopulate();
            } else {
                populateRecursively(recGrp.stream().findAny().get(), recGrp, new HashSet<>());

                // For all recursive relations, initialize the propagation
                recGrp.stream().filter(x -> x instanceof RecursiveGraph).forEach(x -> {
                    for(CAATPredicate dep : x.getDependencies()) {
                        createPropagationTask(dep, x, new ArrayList<>(dep.setView()), 0);
                    }
                });

                // Propagate within this recursive group
                while (!tasks.isEmpty()) {
                    handleTask(tasks.poll(), true, PropagationMode.NORMAL);
                }
            }
        }

        // Notify listeners about population
        for (CAATPredicate pred : getPredicateList()) {
            for (PredicateListener listener : listenersMap.get(pred)) {
                listener.onPopulation(pred);
            }
        }
    }

    private void populateRecursively(CAATPredicate pred, Set<CAATPredicate> recGroup, Set<CAATPredicate> initialized) {
        if (initialized.contains(pred) || !recGroup.contains(pred)) {
            return;
        }

        if (pred instanceof RecursiveGraph) {
            pred.repopulate();
            initialized.add(pred);
        }
        for (CAATPredicate dep : pred.getDependencies()) {
            populateRecursively(dep, recGroup, initialized);
        }
        if (!initialized.contains(pred)) {
            pred.repopulate();
            initialized.add(pred);
        }
    }

    // ==========================================

    // ============== Propagation ===============

    public enum PropagationMode {
        NORMAL,
        DEFER,
        DELETE
    }

    // Should only ever be used on base predicates
    public void addAndPropagate(CAATPredicate pred, Collection<? extends Derivable> props) {
        addAndPropagate(pred, props, PropagationMode.NORMAL);
    }

    public void addAndPropagate(CAATPredicate pred, Collection<? extends Derivable> props, PropagationMode mode) {
        if (!basePredicates.contains(pred) && PropagationMode.NORMAL == mode) {
            throw new UnsupportedOperationException("Cannot directly add elements to derived predicate: " + pred.getName());
        }
        createPropagationTask(null, pred, props, 0);
        forwardPropagate(mode);
    }

    public void triggerDeferredPropagation() {
        while (!deferredTasks.isEmpty()) {
            handleTask(deferredTasks.poll(), false, PropagationMode.DEFER);
        }
    }

    public void onPush() {
        for (CAATPredicate pred : getPredicateList()) {
            listenersMap.get(pred).forEach(listener -> listener.onPush());
        }
    }

    public void backtrackTo(int time) {
        for (CAATPredicate pred : getPredicateList()) {
            pred.backtrackTo(time);
            listenersMap.get(pred).forEach(listener -> listener.onBacktrack(pred, time));
        }
    }

    // --------------------------------------------

    private void forwardPropagate(PropagationMode mode) {
        while (!tasks.isEmpty()) {
            handleTask(tasks.poll(), false, mode);
        }
    }

    private void createPropagationTask(CAATPredicate from, CAATPredicate target, Collection<? extends Derivable> added, int priority) {
        if (target == null || added.isEmpty()) {
            return;
        }
        tasks.add(new Task(from, target, added, priority));
    }

    private void handleTask(Task task, boolean withinRecGroup, PropagationMode mode) {
        CAATPredicate target = task.target;
        CAATPredicate from = task.from;
        Collection<? extends Derivable> added = task.added;

        Collection<? extends Derivable> newlyAdded = target.forwardPropagate(from, added, mode);
        if (newlyAdded.isEmpty()) {
            // Nothing has changed, so we don't create new propagation tasks
            return;
        }

        for (PredicateListener listener : listenersMap.get(target)) {
            listener.onChanged(target, newlyAdded);
        }

        if (mode == PropagationMode.DELETE) {
            return;
        }

        List<DependencyGraph<CAATPredicate>.Node> dependents = dependencyGraph.get(target).getDependents();
        if (withinRecGroup) {
            // Limits propagation to EventGraphs in the same recursive Group.
            Set<DependencyGraph<CAATPredicate>.Node> recGroup = dependencyGraph.get(target).getSCC();
            dependents = dependents.stream().filter(recGroup::contains).collect(Collectors.toList());
        }
        for (DependencyGraph<CAATPredicate>.Node dependent : dependents) {
            Task newTask = new Task(target, dependent.getContent(), newlyAdded, dependent.getTopologicalIndex());
            if (mode == PropagationMode.NORMAL) {
                tasks.add(newTask);
            }
            if (mode == PropagationMode.DEFER) {
                deferredTasks.add(newTask);
            }
        }
    }

    // ==========================================

    // ============== Listeners =================

    public void informListeners(CAATPredicate pred, Collection<? extends Derivable> added) {
        for (PredicateListener listener : listenersMap.get(pred)) {
            listener.onChanged(pred, added);
        }
    }

    public boolean addListener(CAATPredicate pred, PredicateListener listener) {
        if (!listenersMap.containsKey(pred)) {
            return false;
        }
        return listenersMap.get(pred).add(listener);
    }

    public boolean removeListener(CAATPredicate pred, PredicateListener listener) {
        if (!listenersMap.containsKey(pred)) {
            return false;
        }
        return listenersMap.get(pred).remove(listener);
    }

    public boolean removeListener(PredicateListener listener) {
        boolean changed = false;
        for (Set<PredicateListener> listeners : listenersMap.values()) {
            changed |= listeners.remove(listener);
        }
        return changed;
    }


    public void clearListeners() {
        listenersMap.values().forEach(Set::clear);
    }

    // ================= Internal structures ===================


    private static class Task implements Comparable<Task> {
        private final CAATPredicate from;
        private final CAATPredicate target;
        private final Collection<? extends Derivable> added;
        private final int priority;

        public Task(CAATPredicate from, CAATPredicate target, Collection<? extends Derivable> added, int priority) {
            this.from = from;
            this.target = target;
            this.added = added;
            this.priority = priority;
        }

        @Override
        public int compareTo(Task o) {
            return this.priority - o.priority;
        }

    }
}
