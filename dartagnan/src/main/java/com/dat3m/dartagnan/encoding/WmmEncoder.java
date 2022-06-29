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
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dat3m.dartagnan.configuration.OptionNames.CO_ANTISYMMETRY;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.difference;

@Options
public class WmmEncoder implements Encoder {

    private static final Logger logger = LogManager.getLogger(WmmEncoder.class);

    private final VerificationTask task;
    private final Wmm memoryModel;
    private final SolverContext ctx;
    private final HashMap<Relation,TupleSet> activeMap = new HashMap<>();

    @Option(
        name=CO_ANTISYMMETRY,
        description="Encodes the antisymmetry of coherences explicitly.",
        secure=true)
    private boolean antisymmetry = false;

    // =====================================================================

    private WmmEncoder(VerificationTask task, SolverContext ctx) {
        this.task = task;
        this.memoryModel = task.getMemoryModel();
        task.getAnalysisContext().requires(RelationAnalysis.class);
        this.ctx = ctx;
    }

    public static WmmEncoder create(VerificationTask task, SolverContext ctx) throws InvalidConfigurationException {
        WmmEncoder encoder = new WmmEncoder(checkNotNull(task), checkNotNull(ctx));
        task.getConfig().inject(encoder);
        logger.info("{}: {}",CO_ANTISYMMETRY,encoder.antisymmetry);
        encoder.initializeEncoding();
        return encoder;
    }

    /**
     * Instances live during {@link WmmEncoder}'s initialization phase.
     * Pass requests for tuples to be added to the encoding, between relations.
     */
    public interface Buffer {
        /**
         * Called by {@link Relation#activate(VerificationTask, Buffer, Observable) Relation}
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

    /**
     * Instances are associated with a relation of a soon-to-be-encoded memory model.
     */
    public interface Listener {
        /**
         * Reacts to a batch of activated tuples.
         * @param active
         * Immutable collection of newly-active event pairs in the associated relation.
         */
        void notify(Set<Tuple> active);
    }

    public interface Observable {
        void listen(Relation relation, Listener listener);
    }

    public boolean doEncodeAntisymmetry() {
        return antisymmetry;
    }

    private void initializeEncoding() {
        for(String relName : Wmm.BASE_RELATIONS) {
            memoryModel.getRelationRepository().getRelation(relName);
        }

        Map<Relation,List<Listener>> listener = new HashMap<>();
        Map<Relation, Set<Tuple>> queue = new HashMap<>();
        Buffer buffer = (rel, set) -> queue.merge(rel,set,Sets::union);
        Observable observable = (rel, lis) -> listener.computeIfAbsent(rel, k -> new ArrayList<>()).add(lis);
        for(Relation relation : memoryModel.getRelationRepository().getRelations()){
            activeMap.put(relation, new TupleSet());
            relation.activate(task,buffer,observable);
        }

        // ====================== Compute encoding information =================
        for (Axiom ax : memoryModel.getAxioms()) {
            Set<Tuple> set = ax.getEncodeTupleSet(task);
            if(!set.isEmpty()) {
                queue.merge(ax.getRelation(), set, Sets::union);
            }
        }

        while(!queue.isEmpty()) {
            Relation relation = queue.keySet().iterator().next();
            TupleSet active = activeMap.get(relation);
            Set<Tuple> delta = new HashSet<>(difference(queue.remove(relation),active));
            if(delta.isEmpty()) {
                continue;
            }
            active.addAll(delta);
            for(Listener lis : listener.getOrDefault(relation, List.of())) {
                lis.notify(delta);
            }
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
            expr = bmgr.and(expr, ax.consistent(this));
        }
        return expr;
    }

    public VerificationTask getTask() {
        return task;
    }

    /**
     * @param relation
     * Element of the memory model.
     * @return
     * Read-only collection of tuples to be encoded.
     */
    public TupleSet getActiveSet(Relation relation) {
        return activeMap.get(relation);
    }

    @Override
    public SolverContext getSolverContext() {
        return ctx;
    }

    private BooleanFormula encode(Relation r) {
        return r.encode(activeMap.get(r), this);
    }
}
