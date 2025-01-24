package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.domain;

import com.dat3m.dartagnan.program.event.Event;

import java.util.Collection;
import java.util.Stack;

public class SolverDomain extends GenericDomain<Event>{

    private final Stack<Event> activationOrder;
    private final Stack<Integer> backtrackPoints;

    public SolverDomain(Collection<Event> domain){
        super(domain);
        this.activationOrder = new Stack<>();
        this.backtrackPoints = new Stack<>();
    }

    public SolverDomain(){
        super();
        this.activationOrder = new Stack<>();
        this.backtrackPoints = new Stack<>();
    }

    @Override
    public int push() {
        backtrackPoints.push(trueSize());
        return backtrackPoints.size();
    }

    @Override
    public int resetElements(int clusterNum) {
        int reqSize = activationOrder.size();
        for (int i = 0; i < clusterNum; i++) {
            reqSize = backtrackPoints.pop();
        }
        for (int i = activationOrder.size(); i > reqSize; i--) {
            Event toRemove = activationOrder.pop();
            //System.out.println("-"+getId(toRemove));
            toRemove.backtrack();
        }
        return activationOrder.size();
    }

    @Override
    public int addElement(Event obj) {
        Event ev = obj;
        int id = weakGetId(obj);
        if (id < 0) {
            ev.setActive(true);
            id = super.addElement(ev);
        } else {
            ev = weakGetObjectById(id);
            if (ev.isActive()) {
                return -1;
            } else {
                ev.setActive(true);
            }
        }
        activationOrder.push(ev);
        return id;
    }

    public int weakAddElement(Event obj) {
        obj.setActive(false);
        return super.addElement(obj);
    }

    @Override
    public int getId(Object obj) {
        if (!(obj instanceof Event)) {
            return -1;
        }
        Event ev = (Event)obj;
        if (!ev.isActive()) {
            return -1;
        }
        return super.getId(obj);
    }

    public int weakGetId(Object obj) {
        return super.getId(obj);
    }

    @Override
    public Event getObjectById(int id) {
        Event ev = super.getObjectById(id);
        if (ev != null && !ev.isActive()) {
            return null;
        }
        return ev;
    }

    @Override
    public Event weakGetObjectById(int id) {
        return super.getObjectById(id);
    }

    public int trueSize() {
        return activationOrder.size();
    }

}
