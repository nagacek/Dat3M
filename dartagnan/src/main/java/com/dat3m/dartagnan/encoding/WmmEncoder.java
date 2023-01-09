package com.dat3m.dartagnan.encoding;

import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.RecursiveGroup;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

@Options
public class WmmEncoder implements Encoder {

    private static final Logger logger = LogManager.getLogger(WmmEncoder.class);

    private final EncodingContext context;
    private boolean isInitialized = false;

    // =====================================================================

    private WmmEncoder(EncodingContext c) {
        context = c;
        c.getAnalysisContext().requires(RelationAnalysis.class);
    }

    public static WmmEncoder withContext(EncodingContext context) throws InvalidConfigurationException {
        WmmEncoder encoder = new WmmEncoder(context);
        context.getTask().getConfig().inject(encoder);
        return encoder;
    }

    @Override
    public void initializeEncoding(SolverContext ctx) {
        Wmm memoryModel = context.getTask().getMemoryModel();
        for(String relName : Wmm.BASE_RELATIONS) {
            memoryModel.getRelation(relName);
        }

        for(RecursiveGroup recursiveGroup : memoryModel.getRecursiveGroups()){
            recursiveGroup.setDoRecurse();
        }

        for(Relation relation : memoryModel.getRelations()){
            relation.initializeEncoding(ctx);
        }

        for (Axiom axiom : memoryModel.getAxioms()) {
            axiom.initializeEncoding(ctx);
        }

        // ====================== Compute encoding information =================
        for (Axiom ax : memoryModel.getAxioms()) {
            ax.getRelation().addEncodeTupleSet(ax.getEncodeTupleSet());
        }

        for (RecursiveGroup recursiveGroup : Lists.reverse(memoryModel.getRecursiveGroups())) {
            recursiveGroup.updateEncodeTupleSets();
        }

        isInitialized = true;
    }

    private void checkInitialized() {
        Preconditions.checkState(isInitialized, "initializeEncoding must get called before encoding.");
    }

    public BooleanFormula encodeFullMemoryModel() {
        return context.getBooleanFormulaManager().and(
                encodeRelations(),
                encodeConsistency()
        );
    }

    // Initializes everything just like encodeAnarchicSemantics but also encodes all
    // relations that are needed for the axioms (but does NOT encode the axioms themselves yet)
    // NOTE: It avoids encoding relations that do NOT affect the axioms, i.e. unused relations
    public BooleanFormula encodeRelations() {
        checkInitialized();
        logger.info("Encoding relations");
        Wmm memoryModel = context.getTask().getMemoryModel();
        final DependencyGraph<Relation> depGraph = DependencyGraph.from(
                Iterables.concat(
                        Iterables.transform(Wmm.BASE_RELATIONS, memoryModel::getRelation), // base relations
                        Iterables.transform(memoryModel.getAxioms(), Axiom::getRelation) // axiom relations
                )
        );
        RelationEncoder v = new RelationEncoder(context);
        BooleanFormula enc = v.getBmgr().makeTrue();
        for (Relation rel : depGraph.getNodeContents()) {
            enc = v.getBmgr().and(enc, rel.accept(v));
        }
        return enc;
    }

    // Encodes all axioms. This should be called after <encodeRelations>
    public BooleanFormula encodeConsistency() {
        checkInitialized();
        logger.info("Encoding consistency");
        Wmm memoryModel = context.getTask().getMemoryModel();
        final BooleanFormulaManager bmgr = context.getBooleanFormulaManager();
        return memoryModel.getAxioms().stream()
                .filter(ax -> !ax.isFlagged())
                .map(ax -> ax.consistent(ax.getRelation().getEncodeTupleSet(), context))
                .reduce(bmgr.makeTrue(), bmgr::and);
    }

}
