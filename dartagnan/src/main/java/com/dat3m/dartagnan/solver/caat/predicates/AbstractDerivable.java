package com.dat3m.dartagnan.solver.caat.predicates;

public abstract class AbstractDerivable implements Derivable {

    protected final int time;
    protected final int derivLength;
    protected final int complexity;

    protected AbstractDerivable(int time, int derivLength, int complexity) {
        this.time = time;
        this.derivLength = derivLength;
        this.complexity = complexity;
    }

    @Override
    public int getTime() { return time; }

    @Override
    public int getDerivationLength() { return derivLength; }

    @Override
    public int getComplexity() { return complexity; }
}
