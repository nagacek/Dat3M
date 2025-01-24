package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.OnlineRefinementSolver;
import com.dat3m.dartagnan.verification.solving.PropagatorSolver;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.java_smt.api.PropagatorBackend;

import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.utils.Result.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LFDSTest_acyc extends AbstractCTest {

    public LFDSTest_acyc(String name, Arch target, Result expected) {
        super(name, target, expected);
    }

    @Override
    protected Provider<String> getProgramPathProvider() {
        return () -> getTestResourcePath("lfds/" + name + ".ll");
    }

    @Override
    protected long getTimeout() {
        return 600000;
    }

    protected Provider<Integer> getBoundProvider() {
        return () -> 2;
    }

    @Parameterized.Parameters(name = "{index}: {0}, target={1}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"dglm", C11, UNKNOWN},
                {"dglm-CAS-relaxed", C11, UNKNOWN},
                {"ms", C11, UNKNOWN},
                {"ms-CAS-relaxed", C11, UNKNOWN},
                {"treiber", C11, UNKNOWN},
                {"treiber-CAS-relaxed", C11, UNKNOWN},
                {"chase-lev", C11, PASS},
                // These have an extra thief that violate the assertion
                {"chase-lev-fail", C11, FAIL},
                // These are simplified from the actual C-code in benchmarks/lfds
                // and contain fewer calls to push to improve verification time
                // We only have two instances to make the CI faster
                //{"safe_stack", C11, FAIL},
                {"hash_table", C11, PASS},
                // MP is correct under TSO
                {"hash_table-fail", C11, PASS},
        });
    }

    @Test
    public void testAcyc() throws Exception {
        PropagatorSolver s = PropagatorSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
    }

    @Test
    public void testAssume() throws Exception {
        AssumeSolver s = AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
    }

    @Test
    public void testRefinement() throws Exception {
        RefinementSolver s = RefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
    }

    @Test
    public void testOnlineRefinement() throws Exception {
        OnlineRefinementSolver s = OnlineRefinementSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get());
        assertEquals(expected, s.getResult());
    }
}