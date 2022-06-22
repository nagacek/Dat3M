package com.dat3m.dartagnan.encoding;

import org.sosy_lab.java_smt.api.SolverContext;

public interface Encoder {

    SolverContext getSolverContext();
}