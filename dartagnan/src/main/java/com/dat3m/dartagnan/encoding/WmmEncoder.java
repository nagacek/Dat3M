package com.dat3m.dartagnan.encoding;

import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.difference;


public class WmmEncoder implements Encoder {

    private static final Logger logger = LogManager.getLogger(WmmEncoder.class);

    private final VerificationTask task;
    private final Wmm memoryModel;
    private final SolverContext ctx;

    // =====================================================================

    private WmmEncoder(VerificationTask task, SolverContext ctx) {
        this.task = task;
        this.memoryModel = task.getMemoryModel();
        task.getAnalysisContext().requires(RelationAnalysis.class);
        this.ctx = ctx;
    }

    public static WmmEncoder create(VerificationTask task, SolverContext ctx) throws InvalidConfigurationException {
        WmmEncoder encoder = new WmmEncoder(checkNotNull(task), checkNotNull(ctx));
        encoder.initializeEncoding();
        return encoder;
    }

    /**
     * Instances live during {@link WmmEncoder}'s initialization phase.
     * Pass requests for tuples to be added to the encoding, between relations.
     */
    public interface Buffer {
        /**
         * Called by {@link Relation#activate(Set,VerificationTask,Buffer) Relation}
         * whenever a batch of new relationships has been marked.
         * Performs duplicate elimination (i.e. if multiple relations send overlapping tuples)
         * and delays their insertion into the encoder's set to decrease the number of propagations.
         * @param rel
         * Target relation, will be notified of this batch, eventually.
         * @param tuples
         * Collection of event pairs in {@code rel}, that are requested to be encoded.
         * Subset of {@code rel}'s may set, and disjoint from its must set.
         */
        void send(Relation rel, Set<Tuple> tuples);
    }

    private void initializeEncoding() {
        for(String relName : Wmm.BASE_RELATIONS) {
            memoryModel.getRelationRepository().getRelation(relName);
        }

        for(Relation relation : memoryModel.getRelationRepository().getRelations()){
            relation.initializeEncoding(ctx);
        }

        for (Axiom axiom : memoryModel.getAxioms()) {
            axiom.initializeEncoding(ctx);
        }

        // ====================== Compute encoding information =================
        Map<Relation, Set<Tuple>> queue = new HashMap<>();
        for (Axiom ax : memoryModel.getAxioms()) {
            Set<Tuple> set = ax.getEncodeTupleSet();
            if(!set.isEmpty()) {
                queue.merge(ax.getRelation(), set, Sets::union);
            }
        }

        while(!queue.isEmpty()) {
            Relation relation = queue.keySet().iterator().next();
            TupleSet delta = new TupleSet(difference(queue.remove(relation),relation.getEncodeTupleSet()));
            if(delta.isEmpty()) {
                continue;
            }
            relation.addEncodeTupleSet(delta);
            relation.activate(delta, task, (rel, set) -> queue.merge(rel,set,Sets::union));
        }
    }

    public BooleanFormula encodeFullMemoryModel(SolverContext ctx) {
        return ctx.getFormulaManager().getBooleanFormulaManager().and(
                encodeRelations(ctx), encodeConsistency(ctx)
        );
    }

    // This methods initializes all relations and encodes all base relations
    // It does NOT encode the axioms nor any non-base relation yet!
    public BooleanFormula encodeAnarchicSemantics(SolverContext ctx) {
        logger.info("Encoding anarchic semantics");
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = bmgr.makeTrue();
        for(String relName : Wmm.BASE_RELATIONS){
            enc = bmgr.and(enc, encode(memoryModel.getRelationRepository().getRelation(relName)));
        }

        return enc;
    }

    // Initializes everything just like encodeAnarchicSemantics but also encodes all
    // relations that are needed for the axioms (but does NOT encode the axioms themselves yet)
    // NOTE: It avoids encoding relations that do NOT affect the axioms, i.e. unused relations
    public BooleanFormula encodeRelations(SolverContext ctx) {
        logger.info("Encoding relations");
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = encodeAnarchicSemantics(ctx);
        for(Relation r : memoryModel.getRelationRepository().getRelations()) {
            if(!r.getIsNamed() || !Wmm.BASE_RELATIONS.contains(r.getName())) {
                enc = bmgr.and(enc, encode(r));
            }
        }
        return enc;
    }

    // Encodes all axioms. This should be called after <encodeRelations>
    public BooleanFormula encodeConsistency(SolverContext ctx) {
        logger.info("Encoding consistency");
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula expr = bmgr.makeTrue();
        for (Axiom ax : memoryModel.getAxioms()) {
        	// Flagged axioms do not act as consistency filter
        	if(ax.isFlagged()) {
        		continue;
        	}
            expr = bmgr.and(expr, ax.consistent(ctx));
        }
        return expr;
    }

    public VerificationTask getTask() {
        return task;
    }

    @Override
    public SolverContext getSolverContext() {
        return ctx;
    }

    private BooleanFormula encode(Relation r) {
        return r.encode(r.getEncodeTupleSet(), this);
    }
}
