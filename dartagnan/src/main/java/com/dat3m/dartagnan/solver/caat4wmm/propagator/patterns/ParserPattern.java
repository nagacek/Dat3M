package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import com.dat3m.dartagnan.program.filter.Filter;
import com.dat3m.dartagnan.wmm.Relation;

import java.util.List;

public record ParserPattern(List<ParserPattern.Edge> edges) {

    public record Edge(Relation r, ParserPattern.Node n1, ParserPattern.Node n2, boolean isNegative) {}

    public record Node(int id, List<Filter> filters) {}
}
