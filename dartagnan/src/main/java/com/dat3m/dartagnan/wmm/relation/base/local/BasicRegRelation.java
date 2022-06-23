package com.dat3m.dartagnan.wmm.relation.base.local;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.analysis.Dependency;
import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.base.stat.StaticRelation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Collection;

import static com.dat3m.dartagnan.encoding.ProgramEncoder.execution;

abstract class BasicRegRelation extends StaticRelation {

    abstract Collection<Register> getRegisters(Event regReader);

    protected abstract Collection<Event> getEvents(Program program);

    @Override
    public void initialize(RelationAnalysis ra, RelationAnalysis.SetBuffer buf, RelationAnalysis.SetObservable obs) {
        TupleSet maxTupleSet = new TupleSet();
        TupleSet minTupleSet = new TupleSet();
        Dependency dep = ra.getTask().getAnalysisContext().requires(Dependency.class);
        for(Event regReader : getEvents(ra.getTask().getProgram())){
            for(Register register : getRegisters(regReader)){
                Dependency.State r = dep.of(regReader, register);
                for(Event regWriter : r.may) {
                    maxTupleSet.add(new Tuple(regWriter, regReader));
                }
                for(Event regWriter : r.must) {
                    minTupleSet.add(new Tuple(regWriter, regReader));
                }
            }
        }
        buf.send(this,maxTupleSet,minTupleSet);
    }

    @Override
    public BooleanFormula getSMTVar(Tuple t, VerificationTask task, SolverContext ctx) {
        ExecutionAnalysis exec = task.getAnalysisContext().requires(ExecutionAnalysis.class);
        RelationAnalysis ra = task.getAnalysisContext().requires(RelationAnalysis.class);
        return ra.getMinTupleSet(this).contains(t) ?
                execution(t.getFirst(), t.getSecond(), exec, ctx) :
        		ra.getMaxTupleSet(this).contains(t) ?
        				task.getProgramEncoder().dependencyEdge(t.getFirst(), t.getSecond(), ctx) :
        				ctx.getFormulaManager().getBooleanFormulaManager().makeFalse();
    }
}