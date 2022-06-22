package com.dat3m.dartagnan.verification;

import com.dat3m.dartagnan.exception.UnsatisfiedRequirementException;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

public class Context {
    private final Context prototype;
    private final ClassToInstanceMap<Object> metaDataMap;

    private Context(Context p) {
        prototype = p;
        metaDataMap = MutableClassToInstanceMap.create();
    }

    public static Context create() {
        return new Context(null);
    }

    public static Context createCopyFrom(Context context) {
        Context ctx = new Context(null);
        ctx.metaDataMap.putAll(context.metaDataMap);
        return ctx;
    }

    public static Context create(Context prototype) {
        return new Context(prototype);
    }

    // =============================================

    public <T> boolean has(Class<T> c) {
        return metaDataMap.containsKey(c);
    }

    public <T> T get(Class<T> c) {
        T result = metaDataMap.getInstance(c);
        return result != null || prototype == null ? result : prototype.get(c);
    }

    public <T> boolean invalidate(Class<T> c) {
        return metaDataMap.remove(c) == null;
    }

    public <T> boolean register(Class<T> c, T instance) {
        if(metaDataMap.containsKey(c)) {
            return false;
        }
        metaDataMap.putInstance(c, instance);
        return true;
    }

    public <T> T requires(Class<T> c) {
        T instance = get(c);
        if (instance == null) {
            throw new UnsatisfiedRequirementException("Requires " + c.getSimpleName());
        }
        return instance;
    }

}
