package com.dat3m.dartagnan.solver.caat.predicates;

public interface Derivable {

    int getTime();
    int getDerivationLength();
    int getComplexity();

    Derivable with(int time, int derivationLength, int complexity);


    // ================== Defaults ====================
    default Derivable withTime(int time) { return with(time, getDerivationLength(), getComplexity()); }
    default Derivable withDerivationLength(int derivationLength) { return with(getTime(), getDerivationLength(), getComplexity()); }
    default Derivable withComplexity(int complexity) { return with(getTime(), getDerivationLength(), complexity); }
}
