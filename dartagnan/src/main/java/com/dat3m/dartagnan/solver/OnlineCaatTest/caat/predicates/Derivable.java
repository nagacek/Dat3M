package com.dat3m.dartagnan.solver.OnlineCaatTest.caat.predicates;

public interface Derivable {

    int getTime();
    int getDerivationLength();

    Derivable with(int time, int derivationLength);

    boolean isBone();
    boolean isActive();

    void setActive(boolean activeness);


    // ================== Defaults ====================
    default Derivable withTime(int time) { return with(time, getDerivationLength()); }
    default Derivable withDerivationLength(int derivationLength) { return with(getTime(), getDerivationLength()); }

    default Derivable asBone() { return with(getTime(), getDerivationLength()); }
}
