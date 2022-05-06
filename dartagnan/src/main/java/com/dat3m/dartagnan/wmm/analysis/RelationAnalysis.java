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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import java.util.Set;

public class RelationAnalysis {

    private RelationAnalysis(VerificationTask task, Context context, Configuration config) {
        context.requires(AliasAnalysis.class);
        context.requires(BranchEquivalence.class);
        context.requires(WmmAnalysis.class);
        run(task, context);
    }

    public static RelationAnalysis fromConfig(VerificationTask task, Context context, Configuration config) throws InvalidConfigurationException {
        return new RelationAnalysis(task, context, config);
    }

    public TupleSet getMaxTupleSet(Relation relation) {
        return relation.getMaxTupleSet();
    }

    public TupleSet getMinTupleSet(Relation relation) {
        return relation.getMinTupleSet();
    }

    private void run(VerificationTask task, Context context) {
        // Init data context so that each relation is able to compute its may/must sets.
        Wmm memoryModel = task.getMemoryModel();
        for (Axiom ax : memoryModel.getAxioms()) {
            ax.getRelation().updateRecursiveGroupId(ax.getRelation().getRecursiveGroupId());
        }

        // ------------------------------------------------
        for(String relName : Wmm.BASE_RELATIONS){
            memoryModel.getRelationRepository().getRelation(relName).initializeRelationAnalysis(task, context);
        }
        for (Relation rel : memoryModel.getRelationRepository().getRelations()) {
            rel.initializeRelationAnalysis(task, context);
        }
        for (Axiom ax : memoryModel.getAxioms()) {
            ax.initializeRelationAnalysis(task, context);
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
