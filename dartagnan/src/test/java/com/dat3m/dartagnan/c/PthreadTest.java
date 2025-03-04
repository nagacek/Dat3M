package com.dat3m.dartagnan.c;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.OnlineRefinementSolver;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.configuration.Arch.*;
import static com.dat3m.dartagnan.utils.ResourceHelper.getTestResourcePath;
import static com.dat3m.dartagnan.utils.Result.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PthreadTest extends AbstractCTest {

    public PthreadTest(String name, Arch target, Result expected) {
        super(name, target, expected);
    }

    @Override
    protected Provider<String> getProgramPathProvider() {
        return Provider.fromSupplier(() -> getTestResourcePath("pthread/" + name + ".ll"));
    }

    @Override
    protected long getTimeout() {
        return 60000000;
    }

    protected Provider<Integer> getBoundProvider() {
        return Provider.fromSupplier(() -> 2);
    }

    @Parameterized.Parameters(name = "{index}: {0}, target={1}")
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{
                {"fib_safe-5", TSO, PASS},
                {"fib_safe-5", ARM8, PASS},
                {"fib_safe-5", POWER, PASS},
                {"fib_safe-5", RISCV, PASS},
                {"fib_safe-6", TSO, PASS},
                {"fib_safe-6", ARM8, PASS},
                {"fib_safe-6", POWER, PASS},
                {"fib_safe-6", RISCV, PASS},
                {"fib_safe-7", TSO, PASS},
                {"fib_safe-7", ARM8, PASS},
                {"fib_safe-7", POWER, PASS},
                {"fib_safe-7", RISCV, PASS},
                {"fib_safe-10", TSO, PASS},
                {"fib_safe-10", ARM8, PASS},
                {"fib_safe-10", POWER, PASS},
                {"fib_safe-10", RISCV, PASS},
                {"fib_safe-11", TSO, PASS},
                {"fib_safe-11", ARM8, PASS},
                {"fib_safe-11", POWER, PASS},
                {"fib_safe-11", RISCV, PASS},
                {"fib_safe-12", TSO, PASS},
                {"fib_safe-12", ARM8, PASS},
                {"fib_safe-12", POWER, PASS},
                {"fib_safe-12", RISCV, PASS},
                {"triangular-1", TSO, PASS},
                {"triangular-1", ARM8, PASS},
                {"triangular-1", POWER, PASS},
                {"triangular-1", RISCV, PASS},
                {"triangular-longer-1", TSO, PASS},
                {"triangular-longer-1", ARM8, PASS},
                {"triangular-longer-1", POWER, PASS},
                {"triangular-longer-1", RISCV, PASS},
                {"triangular-longer-2", TSO, PASS},
                {"triangular-longer-2", ARM8, PASS},
                {"triangular-longer-2", POWER, PASS},
                {"triangular-longer-2", RISCV, PASS},
                /*{"triangular-longest-2", TSO, PASS},
                {"triangular-longest-2", ARM8, PASS},
                {"triangular-longest-2", POWER, PASS},
                {"triangular-longest-2", RISCV, PASS}*/
        });
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