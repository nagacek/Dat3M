package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.unary.RelTransRef;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.List;

public class DynamicEagerEncoder {

    public static TupleSetMap determineEncodedTuples(TupleSetMap chosen, DependencyGraph<Relation> rels) {
        TupleSetMap toEncode = new TupleSetMap();
        for (var entry : chosen.getEntries()) {
            Relation next = getRelationFromName(rels.getNodeContents(), entry.getKey());
            // TODO: Find out what is going on with RelTransRef
            if (next != null && next.getDependencies() != null && next.getDependencies().size() > 0 && !(next instanceof RelTransRef)) {
                toEncode.merge(next.addEncodeTupleSet(entry.getValue()));
            }
        }
        return toEncode;
    }

    public static BooleanFormula encodeEagerly(TupleSetMap toEncode, DependencyGraph<Relation> rels, SolverContext ctx) {
        BooleanFormulaManager manager = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula eagerEncoding = manager.makeTrue();
        for (var entry : toEncode.getEntries()) {
            Relation rel = getRelationFromName(rels.getNodeContents(), entry.getKey());
            eagerEncoding = manager.and(eagerEncoding, rel.encodeApprox(ctx, entry.getValue()));
        }
        return eagerEncoding;
    }

    private static Relation getRelationFromName(List<Relation> nodeContents, String name) {
        for (Relation relation : nodeContents) {
            if (name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }
}
