package com.dat3m.dartagnan.solver.caat4wmm.propagator;


import com.dat3m.dartagnan.encoding.Decoder;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.solver.caat.domain.GenericDomain;
import com.dat3m.dartagnan.solver.caat4wmm.*;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.solver.propagator.PropagatorExecutionGraph;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.Relation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.PropagatorBackend;
import org.sosy_lab.java_smt.basicimpl.AbstractUserPropagator;

import java.util.*;

public class DummyPropagator extends AbstractUserPropagator {
    private final RefinementModel refinementModel;
    private final Decoder decoder;


    private final Set<Relation> staticRelations = new HashSet<>();
    private final Set<Relation> trackedRelations = new HashSet<>();

    private int patternCount = 0;
    private long joinTime = 0;
    private long patternTime = 0;

    public DummyPropagator(RefinementModel refinementModel, EncodingContext encCtx, Context analysisContext, Refiner refiner, ExecutionModel model, ExecutionGraph executionGraph) {
        this.refinementModel = refinementModel;
        this.decoder = new Decoder(encCtx, refinementModel);
    }

    @Override
    public void initializeWithBackend(PropagatorBackend backend) {
        super.initializeWithBackend(backend);
        getBackend().notifyOnKnownValue();

        for (BooleanFormula tLiteral : decoder.getDecodableFormulas()) {
            Decoder.Info info = decoder.decode(tLiteral);
            if (info.edges().stream().anyMatch(i -> trackedRelations.contains(refinementModel.translateToOriginal(i.relation()))
                    || staticRelations.contains(refinementModel.translateToOriginal(i.relation())))) {
                getBackend().registerExpression(tLiteral);
            }
        }
    }

    @Override
    public void onKnownValue(BooleanFormula expr, boolean value) {
        patternCount++;
    }


    @Override
    public void onPop(int numLevels) {
        patternCount++;
    }

    @Override
    public void onPush() {
        patternCount++;
    }

    public String printStats() {
        StringBuilder str = new StringBuilder();
        str.append("#Applied patterns: ").append(patternCount).append("\n");
        str.append("Pattern matching time (ms): ").append(joinTime).append("\n");
        str.append("Substitution application time (ms): ").append(patternTime).append("\n");

        return str.toString();
    }
}
