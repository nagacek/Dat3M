package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.encoding.WmmEncoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.dependable.Dependent;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.relation.binary.BinaryRelation;
import com.dat3m.dartagnan.wmm.relation.unary.UnaryRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;
import static com.dat3m.dartagnan.wmm.utils.Utils.edge;

/**
 *
 * @author Florian Furbach
 */
public abstract class Relation implements Dependent<Relation> {

    public static boolean PostFixApprox = false;

    protected String name;
    protected String term;

    protected boolean forceDoEncode = false;

    @Override
    public List<Relation> getDependencies() {
        return Collections.emptyList();
    }

    public abstract void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs);

    public void propagate(RelationAnalysis ra, RelationAnalysis.Buffer buf, RelationAnalysis.Observable obs) {
    }

    /**
     * Called once for each relation during the initialization of a {@link WmmEncoder}.
     * Marks relationships as relevant to the consistency property.
     * Non-maximal tuples and minimal tuples should not be marked.
     * @param task
     * Provides program, memory model, property, and more.
     * @param buf
     * Receives relationships required to be represented by a variable to be properly constrained.
     * @param obs
     * Register for later updates to the set.
     */
    public void activate(VerificationTask task, WmmEncoder.Buffer buf, WmmEncoder.Observable obs) {
    }

    public String getName() {
        return name != null ? name : term;
    }

    public Relation setName(String name){
        this.name = name;
        return this;
    }

    public String getTerm(){
        return term;
    }

    public boolean getIsNamed(){
        return name != null;
    }

    public BooleanFormula encode(Set<Tuple> encodeTupleSet, WmmEncoder encoder) {
        return encoder.getSolverContext().getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    public BooleanFormula getSMTVar(Tuple edge, VerificationTask task, SolverContext ctx) {
        RelationAnalysis ra = task.getAnalysisContext().get(RelationAnalysis.class);
        return ra.getMinTupleSet(this).contains(edge)
            ? execution(edge.getFirst(), edge.getSecond(), task.getAnalysisContext().get(ExecutionAnalysis.class), ctx)
            : ra.getMaxTupleSet(this).contains(edge)
            ? edge(getName(), edge.getFirst(), edge.getSecond(), ctx)
            : ctx.getFormulaManager().getBooleanFormulaManager().makeFalse();
    }

    public final BooleanFormula getSMTVar(Event e1, Event e2, VerificationTask task, SolverContext ctx) {
        return getSMTVar(new Tuple(e1, e2), task, ctx);
    }

    // ========================== Utility methods =========================
    
    public boolean isStaticRelation() {
    	return this instanceof StaticRelation;
    }
    
    public boolean isUnaryRelation() {
    	return this instanceof UnaryRelation;
    }
    
    public boolean isBinaryRelation() {
    	return this instanceof BinaryRelation;
    }
    
    public boolean isRecursiveRelation() {
    	return this instanceof RecursiveRelation;
    }

    public Relation getInner() {
        return (isUnaryRelation() || isRecursiveRelation()) ? getDependencies().get(0) : null;
    }
    
    public Relation getFirst() {
    	return isBinaryRelation() ? getDependencies().get(0) : null;
    }
    
    public Relation getSecond() {
    	return isBinaryRelation() ? getDependencies().get(1) : null;
    }

    @Override
    public String toString() {
        return name == null ? term : name + " := " + term;
    }
}