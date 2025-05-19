package com.dat3m.dartagnan.solver.caat4wmm.propagator;

import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.utils.logic.DNF;

public interface Extractor {
    void extract(DNF<CoreLiteral> inconsistencyReasons);
}
