package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;

import java.util.ArrayList;
import java.util.List;

public class Consequence {
    private final List<CAATLiteral> assignments = new ArrayList<>();
    private final List<CAATLiteral> consequences = new ArrayList<>();

    public boolean isConflict() { return consequences.isEmpty(); }

    public List<CAATLiteral> getAssignments() {
        return assignments;
    }

    public List<CAATLiteral> getConsequences() {
        return consequences;
    }

    public void addAssignment(CAATLiteral assignment) {
        assignments.add(assignment);
    }

    public void addConsequence(CAATLiteral consequence) {
        consequences.add(consequence);
    }
}
