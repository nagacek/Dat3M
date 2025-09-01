package com.dat3m.dartagnan.expression;

import com.dat3m.dartagnan.expression.processing.ExpressionInspector;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.memory.FinalMemoryValue;
import com.dat3m.dartagnan.program.memory.MemoryObject;
import com.dat3m.dartagnan.program.misc.NonDetValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public interface Expression {

    Type getType();
    ImmutableList<Expression> getOperands();
    ExpressionKind getKind();
    <T> T accept(ExpressionVisitor<T> visitor);

    default ImmutableSet<Register> getRegs() {
        class RegisterCollector implements ExpressionInspector {
            private final ImmutableSet.Builder<Register> regs = ImmutableSet.builder();
            @Override
            public Expression visitRegister(Register reg) {
                regs.add(reg);
                return reg;
            }
        }

        final RegisterCollector collector = new RegisterCollector();
        this.accept(collector);
        return collector.regs.build();
    }

    default ImmutableSet<MemoryObject> getMemoryObjects() {
        class MemoryObjectCollector implements ExpressionInspector {
            private final ImmutableSet.Builder<MemoryObject> objects = ImmutableSet.builder();
            @Override
            public MemoryObject visitMemoryObject(MemoryObject memObj) {
                objects.add(memObj);
                return memObj;
            }

            @Override
            public Expression visitFinalMemoryValue(FinalMemoryValue val) {
                objects.add(val.getMemoryObject());
                return val;
            }
        }

        final MemoryObjectCollector collector = new MemoryObjectCollector();
        this.accept(collector);
        return collector.objects.build();
    }

    default ImmutableSet<NonDetValue> getNonDetValues() {
        class NonDetValueCollector implements ExpressionInspector {
            private final ImmutableSet.Builder<NonDetValue> nonDets = ImmutableSet.builder();
            @Override
            public Expression visitNonDetValue(NonDetValue nonDet) {
                nonDets.add(nonDet);
                return nonDet;
            }
        }

        final NonDetValueCollector collector = new NonDetValueCollector();
        this.accept(collector);
        return collector.nonDets.build();
    }
}
