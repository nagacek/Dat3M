package com.dat3m.dartagnan.wmm.analysis.newRelationAnalysis.newWmm;

import com.dat3m.dartagnan.wmm.analysis.newRelationAnalysis.Knowledge;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Map;

public interface Axiom extends Constraint {

    // Returns a list of Knowledge.Delta's matching the relation list returned by <getConstrainedRelations>
    // This method is sound even if it returns imprecise information.
    Map<Relation,Knowledge.Delta> computeInitialKnowledgeClosure(Map<Relation, Knowledge> know);


    // Encodes this constraint into a formula.
    // PRECONDITIONS:
    // - The method may assume that <know> is already encoded and hence all its information is reliable.
    // - Furthermore, it is guaranteed that <computeActiveSet> was called before encoding and that
    //   (1) the provided knowledge to <computeActiveSet> was at least as accurate as <know>
    //   and (2) that all tuples returned by <computeActiveSet> have been encoded.
    BooleanFormula encodeAxiom(Map<Relation, Knowledge> know, EncodingContext ctx);
}