package com.dat3m.dartagnan.solver.caat4wmm.propagator.patterns;

import java.io.Serializable;
import java.util.Comparator;

public interface Regulator {
    double getScore();
    void reward(double rewardFactor);
    void punish(double punishmentFactor);
    Object adaptToScale(double scale);

    class RegulatorComparator<T extends Regulator> implements Comparator<T> {
        @Override
        public int compare(Regulator r1, Regulator r2) {
           return Double.compare(r2.getScore(), r1.getScore());
        }
    }
}
