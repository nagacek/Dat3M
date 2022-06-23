package com.dat3m.dartagnan.wmm.analysis;

import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.BranchEquivalence;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.RecursiveRelation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import java.util.Set;

public class RelationAnalysis {

    private final VerificationTask task;

    private RelationAnalysis(VerificationTask task) {
        this.task = task;
        Context context = task.getAnalysisContext();
        context.requires(AliasAnalysis.class);
        context.requires(BranchEquivalence.class);
        context.requires(WmmAnalysis.class);
    }

    public static RelationAnalysis fromConfig(VerificationTask task) throws InvalidConfigurationException {
        RelationAnalysis ra = new RelationAnalysis(task);
        ra.run();
        return ra;
    }

    public VerificationTask getTask() {
        return task;
    }

    public TupleSet getMaxTupleSet(Relation relation) {
        return relation.getMaxTupleSet();
    }

    public TupleSet getMinTupleSet(Relation relation) {
        return relation.getMinTupleSet();
    }

    private void run() {
        // Init data context so that each relation is able to compute its may/must sets.
        Wmm memoryModel = task.getMemoryModel();
        for (Axiom ax : memoryModel.getAxioms()) {
            ax.getRelation().updateRecursiveGroupId(ax.getRelation().getRecursiveGroupId());
        }

        // ------------------------------------------------
        for(String relName : Wmm.BASE_RELATIONS){
            memoryModel.getRelationRepository().getRelation(relName).initializeRelationAnalysis(task);
        }
        for (Relation rel : memoryModel.getRelationRepository().getRelations()) {
            rel.initializeRelationAnalysis(task);
        }

        // ------------------------------------------------
        for(String relName : Wmm.BASE_RELATIONS){
            Relation baseRel = memoryModel.getRelationRepository().getRelation(relName);
            baseRel.getMaxTupleSet();
            baseRel.getMinTupleSet();
        }
        for(Set<RecursiveRelation> recursiveGroup : memoryModel.getRecursiveGroups()) {
            boolean changed1 = true;

            while(changed1) {
                changed1 = false;
                for(RecursiveRelation relation1 : recursiveGroup) {
                    relation1.setDoRecurse();
                    int oldSize1 = relation1.getMaxTupleSet().size();
                    if(oldSize1 != relation1.getMaxTupleSetRecursive().size()) {
                        changed1 = true;
                    }
                }
            }
            boolean changed = true;

            while(changed) {
                changed = false;
                for(RecursiveRelation relation : recursiveGroup) {
                    relation.setDoRecurse();
                    int oldSize = relation.getMinTupleSet().size();
                    if(oldSize != relation.getMinTupleSetRecursive().size()) {
                        changed = true;
                    }
                }
            }
        }
        for (Axiom ax : memoryModel.getAxioms()) {
            ax.getRelation().getMaxTupleSet();
            ax.getRelation().getMinTupleSet();
        }


    }
}
