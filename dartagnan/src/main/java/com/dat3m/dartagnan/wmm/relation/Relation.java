package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.encoding.Encoder;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.utils.dependable.Dependent;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.relation.binary.BinaryRelation;
import com.dat3m.dartagnan.wmm.relation.unary.UnaryRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Collections;
import java.util.List;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;
import static com.dat3m.dartagnan.wmm.utils.Utils.edge;

/**
 *
 * @author Florian Furbach
 */
//TODO: Remove "Encoder" once we split data and operations appropriately
public abstract class Relation implements Encoder, Dependent<Relation> {

    public static boolean PostFixApprox = false;

    protected String name;
    protected String term;

    protected VerificationTask task;
    protected Context analysisContext;

    protected TupleSet minTupleSet = null;
    protected TupleSet maxTupleSet = null;
    protected TupleSet encodeTupleSet = null;

    protected int recursiveGroupId = 0;
    protected boolean forceUpdateRecursiveGroupId = false;
    protected boolean forceDoEncode = false;

    @Override
    public List<Relation> getDependencies() {
        return Collections.emptyList();
    }

    public int getRecursiveGroupId(){
        return recursiveGroupId;
    }

    public void setRecursiveGroupId(int id){
        forceUpdateRecursiveGroupId = true;
        recursiveGroupId = id;
    }

    public int updateRecursiveGroupId(int parentId){
        return recursiveGroupId;
    }

    // TODO: The following two methods are provided because currently Relations are treated as three things:
    //  data objects, static analysers (relation analysis) and encoders of said data objects.
    //  Once we split these aspects, we might get rid of these methods

    // Due to being an encoder
    public void initializeEncoding(SolverContext ctx) {
    	Preconditions.checkState(this.maxTupleSet != null && this.minTupleSet != null,
    			String.format("No available relation data to encode %s. Perform RelationAnalysis before encoding.", this));
        this.encodeTupleSet = new TupleSet();
    }

    // TODO: We misuse <task> as data object and analysis information object.
    // Due to partaking in relation analysis
    public void initializeRelationAnalysis(VerificationTask task, Context context) {
        this.task = task;
        this.analysisContext = context;
        this.maxTupleSet = null;
        this.minTupleSet = null;
    }

    public abstract TupleSet getMinTupleSet();

    public abstract TupleSet getMaxTupleSet();

    public TupleSet getMinTupleSetRecursive(){
        return getMinTupleSet();
    }

    public TupleSet getMaxTupleSetRecursive(){
        return getMaxTupleSet();
    }

    public TupleSet getEncodeTupleSet(){
        return encodeTupleSet;
    }

    /**
     * Tries to mark a set of event pairs as relevant to the consistency property.
     * Also propagates active sets to its children.
     * @param tuples
     * May contain tuples whose truth value in memory-consistent executions of the program are trivial.
     * In this case, they are not marked and do not propagate.
     */
    public void addEncodeTupleSet(TupleSet tuples){
        encodeTupleSet.addAll(Sets.difference(Sets.intersection(tuples, maxTupleSet),minTupleSet));
    }

    protected TupleSet truncated(TupleSet tuples) {
        return new TupleSet(Sets.difference(Sets.intersection(Sets.difference(tuples, encodeTupleSet), maxTupleSet),minTupleSet));
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

    public BooleanFormula encode(SolverContext ctx) {
        return ctx.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    public BooleanFormula getSMTVar(Tuple edge, SolverContext ctx) {
        return getMinTupleSet().contains(edge)
            ? execution(edge.getFirst(), edge.getSecond(), analysisContext.get(ExecutionAnalysis.class), ctx)
            : getMaxTupleSet().contains(edge)
            ? edge(getName(), edge.getFirst(), edge.getSecond(), ctx)
            : ctx.getFormulaManager().getBooleanFormulaManager().makeFalse();
    }

    public final BooleanFormula getSMTVar(Event e1, Event e2, SolverContext ctx) {
        return getSMTVar(new Tuple(e1, e2), ctx);
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