package com.dat3m.dartagnan.expression.type;

import com.dat3m.dartagnan.expression.Type;

public record TypeOffset(Type type, int offset) {

    private static final TypeFactory types = TypeFactory.getInstance();

    public static TypeOffset of(Type type, int index) {
        if (index == 0) {
            return new TypeOffset(type, 0);
        }
        if (type instanceof ArrayType arrayType) {
            Type elType = arrayType.getElementType();
            return new TypeOffset(elType, types.getMemorySizeInBytes(elType) * index);
        }
        if (type instanceof AggregateType aggregateType) {
            return aggregateType.getTypeOffsets().get(index);
        }
        String error = String.format("Cannot compute offset of index %d into type %s.", index, type);
        throw new UnsupportedOperationException(error);
    }
}