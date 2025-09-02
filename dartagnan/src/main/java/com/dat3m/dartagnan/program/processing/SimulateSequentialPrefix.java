package com.dat3m.dartagnan.program.processing;

import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.processing.ExprSimplifier;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.program.Entrypoint;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.*;
import com.dat3m.dartagnan.program.event.core.*;
import com.dat3m.dartagnan.program.event.core.threading.ThreadCreate;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import com.google.common.base.Preconditions;

import java.util.*;


public class SimulateSequentialPrefix implements ProgramProcessor {

    private final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private final SequentialPropagator propagator = new SequentialPropagator();
    private Label curGoto;

    private SimulateSequentialPrefix() {
    }

    public static SimulateSequentialPrefix newInstance() {
        return new SimulateSequentialPrefix();
    }

    // ====================================================================================

    @Override
    public void run(Program program) {
        Set<MemoryObject> dynamicallyAllocated = new HashSet<>();
        Map<Expression, Expression> addressToValue = new HashMap<>();
        Map<Register, Expression> registerToValue = new HashMap<>();
        List<Event> toRemove = new ArrayList<>();
        curGoto = null;

        // litmus tests and spirv
        if (program.getEntrypoint() instanceof Entrypoint.Resolved || program.getEntrypoint() instanceof Entrypoint.Grid) {
            return;
        }

        propagator.registerToValue = registerToValue;

        program.getMemory().getObjects().forEach(memory ->
                memory.getInitializedFields().forEach(field ->
                        addressToValue.put(calcAddress(memory, field), memory.getInitialValue(field))
                )
        );

        for (Event cur : program.getThreadEvents()) {
            if (cur instanceof ThreadCreate) {
                incorporateResults(program, toRemove, addressToValue, dynamicallyAllocated);
                return;
            }

            // dependencies between sequential events are not handled
            if (!cur.getUsers().isEmpty() && cur instanceof MemoryEvent) {
                incorporateResults(program, toRemove, addressToValue, dynamicallyAllocated);
                return;
            }

            CondResult cond = handleJumps(cur);
            if (cond == CondResult.SKIP) {
                continue;
            } else if (cond == CondResult.ABORT) {
                incorporateResults(program, toRemove, addressToValue, dynamicallyAllocated);
                return;
            }

            if (cur instanceof Alloc alloc) {
                MemoryObject allocatedObject = alloc.getAllocatedObject();
                Preconditions.checkState(allocatedObject.hasKnownSize(), "Cannot simulate dynamic allocation of unknown size.");

                registerToValue.put(alloc.getResultRegister(), allocatedObject);
                dynamicallyAllocated.add(allocatedObject);
            } else if (cur instanceof Load load) {
                Expression address = load.getAddress().accept(propagator);
                Expression curValue = addressToValue.get(address);
                Preconditions.checkState(address.getNonDetValues().isEmpty(), "Cannot simulate dynamic allocation of unknown address.");

                if (curValue != null) {
                    Register resultRegister = load.getResultRegister();
                    Type resultType = resultRegister.getType();
                    Type curValueType = curValue.getType();
                    if (!resultType.equals(curValueType) && resultType instanceof IntegerType intResultType && curValueType instanceof IntegerType) {
                        curValue = expressions.makeIntegerCast(curValue, intResultType, false).accept(propagator);
                    }
                    registerToValue.put(resultRegister, curValue);
                    Local replaceEvent = new Local(resultRegister, curValue);
                    load.replaceBy(replaceEvent);
                } else {
                    toRemove.add(load);
                }
            } else if (cur instanceof Store store && !(store.getSuccessor() instanceof ThreadCreate)) {
                Expression address = store.getAddress().accept(propagator);
                Expression curValue = store.getMemValue().accept(propagator);
                Preconditions.checkState(address.getNonDetValues().isEmpty(), "Cannot simulate dynamic allocation of unknown address.");
                if (!curValue.getRegs().isEmpty()) {
                    addressToValue.remove(address);
                } else {
                    addressToValue.put(address, curValue);
                }
                toRemove.add(cur);
            } else if (cur instanceof Local local) {
                Register resultRegister = local.getResultRegister();
                Expression curValue = local.getExpr().accept(propagator);
                registerToValue.put(resultRegister, curValue);
            }
        }
        incorporateResults(program, toRemove, addressToValue, dynamicallyAllocated);
    }

    private enum CondResult {
        SKIP,
        CONTINUE,
        ABORT
    }
    private CondResult handleJumps(Event cur) {
        if (curGoto != null) {
            if (cur instanceof Label label && curGoto.equals(label)) {
                curGoto = null;
            }
            return CondResult.SKIP;
        }
        if (cur instanceof CondJump jump) {
            Expression guard = jump.getGuard().accept(propagator);
            if (!guard.equals(expressions.makeTrue()) && !guard.equals(expressions.makeFalse())) {
                return CondResult.ABORT;
            }
            jump.setGuard(guard);
            Preconditions.checkState(guard.getNonDetValues().isEmpty(), "Cannot simulate guards with unknown values.");
            if (guard.equals(expressions.makeTrue())) {
                curGoto = jump.getLabel();
                return CondResult.SKIP;
            }
        }
        return CondResult.CONTINUE;
    }

    private void incorporateResults(Program program, List<Event> toRemove, Map<Expression, Expression> addressToValue, Set<MemoryObject> dynamicallyAllocated) {
        List<Init> inits = program.getThreadEvents(Init.class);
        for (Init init : inits) {
            Expression initValue = addressToValue.get(init.getAddress());
            if (initValue != null) {
                init.getBase().setInitialValue(init.getOffset(), initValue);
                init.setMemValue(initValue);
                dynamicallyAllocated.remove(init.getBase());
            }
        }
        for (MemoryObject mem : dynamicallyAllocated) {
            for (int i = 0; i < mem.getKnownSize(); i++) {
                Expression memValue = addressToValue.get(calcAddress(mem, i));
                if (memValue != null) {
                    mem.setInitialValue(i, memValue);
                    program.addInit(mem, i);
                }
            }
        }
        toRemove.forEach(Event::tryDelete);
    }

    private Expression calcAddress(MemoryObject mem, int offset) {
        return offset == 0 ? mem : expressions.makeAdd(mem, expressions.makeValue(offset, (IntegerType) mem.getType()));
    }

    private static class SequentialPropagator extends ExprSimplifier {

        private Map<Register, Expression> registerToValue;

        SequentialPropagator() {
            super(true);
        }

        @Override
        public Expression visitRegister(Register reg) {
            return registerToValue.getOrDefault(reg, reg);
        }
    }
}
