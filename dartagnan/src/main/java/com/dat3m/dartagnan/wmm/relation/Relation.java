package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.encoding.Encoder;
import com.dat3m.dartagnan.encoding.EncodingContext;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.utils.dependable.Dependent;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.base.memory.RelCo;
import com.dat3m.dartagnan.wmm.relation.base.memory.RelLoc;
import com.dat3m.dartagnan.wmm.relation.base.memory.RelRf;
import com.dat3m.dartagnan.wmm.Constraint;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.relation.binary.BinaryRelation;
import com.dat3m.dartagnan.wmm.relation.unary.UnaryRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Florian Furbach
 */
//TODO: Remove "Encoder" once we split data and operations appropriately
public abstract class Relation implements Constraint, Encoder, Dependent<Relation> {

    protected String name;
    protected String term;

    protected VerificationTask task;
    protected Context analysisContext;

    protected TupleSet minTupleSet = null;
    protected TupleSet maxTupleSet = null;
    protected TupleSet encodeTupleSet = null;

    public Relation() { }

    public Relation(String name) {
        this();
        this.name = name;
    }

    public <T> T accept(Visitor<? extends T> visitor) {
        return visitor.visitDefinition(encodeTupleSet, this, List.of());
    }

    public <T> T accept(Visitor<? extends T> visitor, TupleSet toEncode) {
        return visitor.visitDefinition(toEncode,this, List.of());
    }

    @Override
    public List<Relation> getDependencies() {
        List<Relation> relations = getConstrainedRelations();
        // no copying required, as long as getConstrainedRelations() returns a new or unmodifiable list
        return relations.subList(1, relations.size());
    }

    // TODO: The following two methods are provided because currently Relations are treated as three things:
    //  data objects, static analysers (relation analysis) and encoders of said data objects.
    //  Once we split these aspects, we might get rid of these methods

    public void configure(Configuration config) throws InvalidConfigurationException { }
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

    public TupleSetMap addEncodeTupleSet(TupleSet tuples){
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        encodeTupleSet.addAll(Sets.intersection(tuples, maxTupleSet));
        return new TupleSetMap(this, new TupleSet(Sets.difference(encodeTupleSet, oldEncodeSet)));
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

    /**
     * @return Non-empty list of all relations directly participating in this definition.
     * The first relation is always the defined relation,
     * while the roles of the others are implementation-dependent.
     */
    @Override
    public List<Relation> getConstrainedRelations() {
        return accept(new Visitor<>() {
            @Override
            public List<Relation> visitDefinition(TupleSet toEncode, Relation rel, List<? extends Relation> dependencies) {
                List<Relation> relations = new ArrayList<>(dependencies.size() + 1);
                relations.add(rel);
                relations.addAll(dependencies);
                return relations;
            }
        });
    }

    @Override
    public String toString(){
        if(name != null){
            return name + " := " + term;
        }
        return term;
    }

    @Override
    public int hashCode(){
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return getName().equals(((Relation)obj).getName());
    }

    public BooleanFormula getSMTVar(Tuple edge, EncodingContext c) {
        return c.edgeVariable(getName(), edge.getFirst(), edge.getSecond());
    }

    protected void removeMutuallyExclusiveTuples(Set<Tuple> tupleSet) {
        ExecutionAnalysis exec = analysisContext.requires(ExecutionAnalysis.class);
        tupleSet.removeIf(t -> exec.areMutuallyExclusive(t.getFirst(), t.getSecond()));
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

    public interface Visitor <T> {
        default T visitDefinition(TupleSet toEncode, Relation rel, List<? extends Relation> dependencies) { throw new UnsupportedOperationException("applying" + getClass().getSimpleName() + " to relation " + rel); }
        default T visitUnion(TupleSet toEncode, Relation rel, Relation... operands) { return visitDefinition(toEncode, rel, List.of(operands)); }
        default T visitIntersection(TupleSet toEncode, Relation rel, Relation... operands) { return visitDefinition(toEncode, rel, List.of(operands)); }
        default T visitDifference(TupleSet toEncode, Relation rel, Relation superset, Relation complement) { return visitDefinition(toEncode, rel, List.of(superset, complement)); }
        default T visitComposition(TupleSet toEncode, Relation rel, Relation front, Relation back) { return visitDefinition(toEncode, rel, List.of(front, back)); }
        default T visitDomainIdentity(TupleSet toEncode, Relation rel, Relation operand) { return visitDefinition(toEncode, rel, List.of(operand)); }
        default T visitRangeIdentity(TupleSet toEncode, Relation rel, Relation operand) { return visitDefinition(toEncode, rel, List.of(operand)); }
        default T visitInverse(TupleSet toEncode, Relation rel, Relation operand) { return visitDefinition(toEncode, rel, List.of(operand)); }
        default T visitRecursive(TupleSet toEncode, Relation rel, Relation other) { return visitDefinition(toEncode, rel, List.of(other)); }
        default T visitTransitiveClosure(TupleSet toEncode, Relation rel, Relation operand) { return visitDefinition(toEncode, rel, List.of(operand)); }
        default T visitEmpty(TupleSet toEncode, Relation rel) { return visitDefinition(toEncode, rel, List.of()); }
        default T visitIdentity(TupleSet toEncode, Relation id, FilterAbstract set) { return visitDefinition(toEncode, id, List.of()); }
        default T visitProduct(TupleSet toEncode, Relation rel, FilterAbstract domain, FilterAbstract range) { return visitDefinition(toEncode, rel, List.of()); }
        default T visitExternal(TupleSet toEncode, Relation ext) { return visitDefinition(toEncode, ext, List.of()); }
        default T visitInternal(TupleSet toEncode, Relation int_) { return visitDefinition(toEncode, int_, List.of()); }
        default T visitProgramOrder(TupleSet toEncode, Relation po, FilterAbstract type) { return visitDefinition(toEncode, po, List.of()); }
        default T visitControl(TupleSet toEncode, Relation ctrlDirect) { return visitDefinition(toEncode, ctrlDirect, List.of()); }
        default T visitFences(TupleSet toEncode, Relation fence, FilterAbstract type) { return visitDefinition(toEncode, fence, List.of()); }
        default T visitInternalDataDependency(TupleSet toEncode, Relation idd) { return visitDefinition(toEncode, idd, List.of()); }
        default T visitCompareAndSwapDependency(TupleSet toEncode, Relation casDep) { return visitDefinition(toEncode, casDep, List.of()); }
        default T visitAddressDependency(TupleSet toEncode, Relation addrDirect) { return visitDefinition(toEncode, addrDirect, List.of()); }
        default T visitCriticalSections(TupleSet toEncode, Relation rscs) { return visitDefinition(toEncode, rscs, List.of()); }
        default T visitReadModifyWrites(TupleSet toEncode, Relation rmw) { return visitDefinition(toEncode, rmw, List.of()); }
        default T visitMemoryOrder(TupleSet toEncode, Relation co) { return visitDefinition(toEncode, co, List.of()); }
        default T visitSameAddress(TupleSet toEncode, Relation loc) { return visitDefinition(toEncode, loc, List.of()); }
        default T visitReadFrom(TupleSet toEncode, Relation rf) { return visitDefinition(toEncode, rf, List.of()); }
    }
}