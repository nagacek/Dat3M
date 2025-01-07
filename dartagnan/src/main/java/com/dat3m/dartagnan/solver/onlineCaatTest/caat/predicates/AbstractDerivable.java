package com.dat3m.dartagnan.solver.onlineCaatTest.caat.predicates;

public abstract class AbstractDerivable implements Derivable {

    protected int time;
    protected int derivLength;

    private final boolean isBone;
    private boolean isActive;

    protected AbstractDerivable(int time, int derivLength, boolean isBone, boolean isActive) {
        this.time = time;
        this.derivLength = derivLength;
        this.isBone = isBone;
        this.isActive = isActive;
    }

    protected AbstractDerivable(int time, int derivLength, boolean isBone) {
        this(time, derivLength, isBone, !isBone);
    }

    protected AbstractDerivable(int time, int derivLength) {
        this(time, derivLength, false);
    }

    @Override
    public int getTime() { return time; }

    @Override
    public int getDerivationLength() { return derivLength; }

    @Override
    public void setActive(boolean activeness) {
        this.isActive = activeness;
    }

    public void setActive(boolean activeness, int time) {
        assert(isBone());
        setActive(activeness);
        this.time = time;
    }
    public void setActive(boolean activeness, int derivLength, int time) {
        assert(isBone());
        setActive(activeness);
        this.derivLength = derivLength;
        this.time = time;
    }


    @Override
    public boolean isBone() {
        return isBone;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

}
