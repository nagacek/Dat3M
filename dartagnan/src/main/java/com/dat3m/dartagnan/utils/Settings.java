package com.dat3m.dartagnan.utils;

import java.util.HashMap;
import java.util.Map;

public class Settings {

    public static final String TACTIC = "qfbv";
    
    // TODO: UI and console options to set these flags
    public static final int FLAG_USE_SEQ_ENCODING_REL_RF            = 1;
    public static final int FLAG_CAN_ACCESS_UNINITIALIZED_MEMORY    = 2;

    private final int bound;
    private final int solver_timeout;

    private final Map<Integer, Boolean> flags = createFlags();

    private static Map<Integer, Boolean> createFlags() {
        Map<Integer, Boolean> flags = new HashMap<>(2);
        flags.put(FLAG_USE_SEQ_ENCODING_REL_RF, true);
        flags.put(FLAG_CAN_ACCESS_UNINITIALIZED_MEMORY, false);
        return flags;
    }

    public Settings(int bound, int solver_timeout){
        this.bound = Math.max(1, bound);
        this.solver_timeout = solver_timeout;
    }

    public int getBound(){
        return bound;
    }

    public int getSolverTimeout(){
        return solver_timeout;
    }

    public boolean hasSolverTimeout(){
        return solver_timeout > 0;
    }

    public boolean getFlag(int flag){
        if(!flags.containsKey(flag)){
            throw new UnsupportedOperationException("Unrecognized settings flag");
        }
        return flags.get(flag);
    }

    public void setFlag(int flag, boolean value){
        if(!flags.containsKey(flag)){
            throw new UnsupportedOperationException("Unrecognized settings flag");
        }
        flags.put(flag, value);
    }

    @Override
    public String toString(){
        return " bound=" + bound;
    }
}
