package com.dat3m.dartagnan.program.processing;

import ap.interpolants.StructuredPrograms;
import com.dat3m.dartagnan.expression.Expression;
import com.dat3m.dartagnan.expression.ExpressionFactory;
import com.dat3m.dartagnan.expression.Type;
import com.dat3m.dartagnan.expression.processing.ExprSimplifier;
import com.dat3m.dartagnan.expression.type.IntegerType;
import com.dat3m.dartagnan.expression.type.TypeFactory;
import com.dat3m.dartagnan.program.Entrypoint;
import com.dat3m.dartagnan.program.IRHelper;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.*;
import com.dat3m.dartagnan.program.event.core.*;
import com.dat3m.dartagnan.program.event.core.annotations.StringAnnotation;
import com.dat3m.dartagnan.program.event.core.threading.ThreadCreate;
import com.dat3m.dartagnan.program.event.functions.FunctionCall;
import com.dat3m.dartagnan.program.event.lang.dat3m.DynamicThreadCreate;
import com.dat3m.dartagnan.program.event.lang.dat3m.DynamicThreadJoin;
import com.dat3m.dartagnan.program.event.lang.svcomp.NonDetChoice;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import com.google.common.base.Preconditions;

import java.util.*;
import java.util.function.Predicate;


public class SimulateSequentialPrefix implements ProgramProcessor {
    private final Intrinsics intrinsics;
    private final List<Event> pendingIntrinsics = new ArrayList<>();

    private static final int MAX_LOOP = 1000;
    private final Type ptrType = TypeFactory.getInstance().getPointerType();
    private final ExpressionFactory expressions = ExpressionFactory.getInstance();
    private final SequentialPropagator propagator = new SequentialPropagator();
    private Label curGoto;
    private Map<CondJump, Integer> jumpBounds = new HashMap<>();

    private final Map<Expression, Expression> addressToValue = new HashMap<>();
    private final Map<Register, Expression> registerToValue = new HashMap<>();
    private final Map<Alloc, MemoryObject> dynamicAllocations = new HashMap<>();
    private final Map<Load, Local> replaceEvents = new HashMap<>();
    private final List<MemoryObject> staticAllocations = new ArrayList<>();
    private final List<Event> toRemove = new ArrayList<>();
    private final Map<CondJump, Expression> guardEvals = new HashMap<>();

    private final Predicate<Event> doHandle = e -> e instanceof Local || e instanceof Load || e instanceof Store
            || e instanceof Alloc || e instanceof Label || e instanceof CondJump || e instanceof FunctionCall
            || e instanceof Assert;

    private SimulateSequentialPrefix(Intrinsics intrinsics) {
        this.intrinsics = intrinsics;
    }

    public static SimulateSequentialPrefix newInstance(Intrinsics intrinsics) {
        return new SimulateSequentialPrefix(intrinsics);
    }

    // ====================================================================================

    @Override
    public void run(Program program) {
        curGoto = null;

        // litmus tests and spirv
        if (program.getEntrypoint() instanceof Entrypoint.Resolved || program.getEntrypoint() instanceof Entrypoint.Grid) {
            return;
        }

        propagator.registerToValue = registerToValue;

        program.getMemory().getObjects().forEach(memory ->
                memory.getInitializedFields().forEach(field -> {
                            addressToValue.put(calcAddress(memory, field), memory.getInitialValue(field));
                            staticAllocations.add(memory);
                        }
                )
        );

        Event entry = program.getEntrypoint().getEntryFunctions().get(0).getEntry();
        Event cur = entry;
        while (cur != null) {
            if (cur instanceof ThreadCreate || cur instanceof DynamicThreadCreate || cur instanceof DynamicThreadJoin
                    || (cur instanceof Assert && peekSuccessor(cur) == null) || cur instanceof NonDetChoice) {
                incorporateResults(entry, cur);
                return;
            }

            // dependencies between sequential events are not handled
            if (!cur.getUsers().isEmpty() && cur instanceof MemoryEvent) {
                incorporateResults(entry, cur);
                return;
            }

            CondResult cond = handleJumps(cur);
            if (cond == CondResult.SKIP) {
                cur = curGoto == null ? getSuccessor(cur) : curGoto;
                continue;
            } else if (cond == CondResult.ABORT || cond == CondResult.UNKNOWN) {
                incorporateResults(entry, cur);
                return;
            }

            if (cur instanceof Alloc alloc) {
                MemoryObject allocatedObject = program.getMemory().allocate(alloc);
                dynamicAllocations.put(alloc, allocatedObject);
                registerToValue.put(alloc.getResultRegister(), allocatedObject);
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
                } else {
                    toRemove.add(load);
                }
            } else if (cur instanceof Store store && !(peekSuccessor(cur) instanceof ThreadCreate)) {
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
            } else if (cur instanceof Assert assertion) {
                Expression evaluation = assertion.getExpression().accept(propagator);
                Assert concreteAssertion = assertion;
                if (!evaluation.equals(expressions.makeTrue()) && !evaluation.equals(assertion.getExpression())) {
                    concreteAssertion = EventFactory.newAssert(evaluation, "Assertion " + assertion.getExpression() + " failed.");
                    assertion.insertBefore(concreteAssertion);
                }
                if (evaluation.equals(expressions.makeFalse())) {
                    incorporateResults(entry, concreteAssertion);
                    return;
                }
            }
            handleIntrinsic(peekSuccessor(cur));
            cur = getSuccessor(cur);
        }
        incorporateResults(entry, cur);
    }

    private enum CondResult {
        SKIP,
        CONTINUE,
        ABORT,
        UNKNOWN
    }
    private CondResult handleJumps(Event cur) {
        if (curGoto != null) {
            if (cur instanceof Label label && curGoto.equals(label)) {
                curGoto = null;
            } else {
                return CondResult.SKIP;
            }
        }
        if (cur instanceof CondJump jump) {
            jumpBounds.putIfAbsent(jump, 0);
            Expression guard = jump.getGuard().accept(propagator);
            if (!guard.equals(expressions.makeTrue()) && !guard.equals(expressions.makeFalse())) {
                return CondResult.ABORT;
            }
            Preconditions.checkState(guard.getNonDetValues().isEmpty(), "Cannot simulate guards with unknown values.");
            guardEvals.put(jump, guard);
            if (guard.equals(expressions.makeTrue())) {
                if (jumpBounds.computeIfPresent(jump, (key, value) -> value + 1) > MAX_LOOP) {
                    return CondResult.UNKNOWN;
                }
                curGoto = jump.getLabel();
                return CondResult.SKIP;
            }
        }
        return CondResult.CONTINUE;
    }

    private void handleIntrinsic(Event next) {
        List<Event> inlining = intrinsics.getInlining(next);
        if (!inlining.isEmpty() && pendingIntrinsics.isEmpty() && next.getSuccessor() != null) {
            inlining.add(next.getSuccessor());
        }
        pendingIntrinsics.addAll(0, inlining);
    }

    private Event peekSuccessor(Event cur) {
        if (!pendingIntrinsics.isEmpty()) {
            return pendingIntrinsics.get(0);
        }
        return cur.getSuccessor();
    }

    private Event getSuccessor(Event cur) {
        if (!pendingIntrinsics.isEmpty()) {
            return pendingIntrinsics.remove(0);
        }
        return cur.getSuccessor();
    }

    private void incorporateResults(Event first, Event last) {
        removeCodeFromTo(first, last);
        initMemory();
        initRegisters(first);
    }

    private void removeCodeFromTo(Event first, Event last) {
        if (last == null) {
            IRHelper.bulkDelete(new HashSet<>(first.getSuccessor().getSuccessors()));
            return;
        }
        List<Event> remainder = new ArrayList<>();
        Event cur = first.getSuccessor();
        Event next;
        while (cur.getLocalId() < last.getLocalId()) {
            if (!doHandle.test(cur)) {
                cur = cur.getSuccessor();
                continue;
            }
            // TODO: Abort mid-intrinsic
            if (cur instanceof Label label) {
                Set<EventUser> users = label.getUsers();
                EventUser latestUser = users.stream()
                        .max(Comparator.comparingInt(EventUser::getLocalId)).orElse(null);
                if (latestUser == null) {
                    next = cur.getSuccessor();
                    cur.tryDelete();
                } else if (latestUser.getLocalId() < last.getLocalId()) {
                    next = latestUser.getSuccessor();
                    Set<Event> deleteBatch = new HashSet<>(IRHelper.getEventsFromTo(cur, latestUser, true));
                    Set<Event> failedDeletion = IRHelper.bulkDelete(deleteBatch);
                    assert(failedDeletion.isEmpty());
                } else {
                    // If the simulation aborts mid-loop, copies of the remaining instructions are inserted
                    // before the loop and the loop can then be re-entered with updated values
                    next = last;
                    List<Event> afterLast = IRHelper.getEventsFromTo(last, latestUser, true);
                    final Map<Event, Event> copyCtx = new HashMap<>();
                    remainder.addAll(IRHelper.copyEvents(afterLast, e -> {}, copyCtx));
                    label.insertBefore(remainder);
                }
            } else if (cur instanceof CondJump jump) {
                Expression eval = guardEvals.get(jump);
                assert eval != null;
                if (eval.equals(expressions.makeTrue())) {
                    Label label = jump.getLabel();
                    assert last.getLocalId() > label.getLocalId();
                    next = label.getSuccessor();
                    Set<Event> deleteBatch = new HashSet<>(IRHelper.getEventsFromTo(cur, label, true));
                    Set<Event> failedDeletion = IRHelper.bulkDelete(deleteBatch);
                    assert(failedDeletion.isEmpty());
                } else if (eval.equals(expressions.makeFalse())) {
                    next = cur.getSuccessor();
                    cur.tryDelete();
                } else {
                    return;
                }
            } else if (cur instanceof Assert assertion && assertion.getExpression().equals(expressions.makeFalse())) {
                IRHelper.bulkDelete(new HashSet<>(cur.getSuccessor().getSuccessors()));
                return;
            } else {
                next = cur.getSuccessor();
                cur.tryDelete();
            }
            cur = next;
        }
    }

    private void initRegisters(Event entry) {
        registerToValue.forEach((reg, val) -> {
            Local initReg = new Local(reg, val);
            entry.insertAfter(initReg);
        });
    }

    private void initMemory() {
        for (MemoryObject mem : staticAllocations) {
            initMemValues(mem);
        }
        for (Alloc alloc : dynamicAllocations.keySet()) {
            MemoryObject mem = dynamicAllocations.get(alloc);
            initMemValues(mem);
            mem.setIsSimulated();
        }
    }

    /*private void incorporateResults() {
        for (MemoryObject mem : staticAllocations) {
            initMemValues(mem);
        }
        for (Alloc alloc : dynamicAllocations.keySet()) {
            MemoryObject mem = dynamicAllocations.get(alloc);
            initMemValues(mem);
            mem.setIsSimulated();
            toRemove.add(alloc);
        }
        toRemove.forEach(Event::tryDelete);
        replaceEvents.forEach(AbstractEvent::replaceBy);
    }*/

    private void initMemValues(MemoryObject mem) {
        for (int i = 0; i < mem.getKnownSize(); i++) {
            Expression initValue = addressToValue.get(calcAddress(mem, i));
            if (initValue != null) {
                mem.setInitialValue(i, initValue);
            }
        }
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
