package com.dat3m.dartagnan;

import com.dat3m.dartagnan.analysis.Refinement;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.*;
import com.dat3m.dartagnan.verification.RefinementTask;
import com.dat3m.dartagnan.wmm.utils.Arch;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import static com.dat3m.dartagnan.analysis.Base.runAnalysisAssumeSolver;
import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static com.dat3m.dartagnan.wmm.utils.Arch.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CLocksTestAlternative {


    static final int TIMEOUT = 5000;

    private String path;
    private Arch target;
    private final Result expected;

    @ClassRule
    public static CSVInitRule csvInitRule = new CSVInitRule( "assume", "refinement");

    private final MethodSpecificProvider<String> methodNameProvider =
            MethodSpecificProvider.fromMethodName(Map.of(
                    "testAssume", "assume",
                    "testRefinement", "refinement")
            );

    private final Supplier<Arch> targetProvider = () -> target;
    private final PathProvider pathProvider = PathProvider.addPrefix(() -> path, TEST_RESOURCE_PATH + "locks/");
    private final SettingsProvider settingsProvider = new SettingsProvider.Builder().build(); // Default settings
    private final ProgramProvider programProvider = new ProgramProvider(pathProvider);
    private final WmmProvider wmmProvider = new WmmProvider(targetProvider);
    private final TaskProvider taskProvider = new TaskProvider(programProvider, wmmProvider, targetProvider, settingsProvider);
    private final SolverContextProvider contextProvider = new SolverContextProvider();
    private final ProverProvider proverProvider = new ProverProvider(contextProvider, () -> new ProverOptions[] { ProverOptions.GENERATE_MODELS });
    private final CSVLogger csvLogger  = new CSVLogger(methodNameProvider, pathProvider);
    private final Timeout timeout = Timeout.millis(TIMEOUT);


    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(pathProvider)
            .around(settingsProvider)
            .around(programProvider)
            .around(wmmProvider)
            .around(taskProvider)
            .around(methodNameProvider)
            .around(csvLogger) // csvLogger needs to get created before the timeout rule to be able to detect timeouts
            .around(timeout) // Timeout needs to get created before the Context/Prover due to threading issues
            .around(contextProvider)
            .around(proverProvider);


	@Parameterized.Parameters(name = "{index}: {0} target={1}")
    public static Iterable<Object[]> data() throws IOException {
		return Arrays.asList(new Object[][]{
                {"ttas-5.bpl", TSO, UNKNOWN},
                {"ttas-5.bpl", ARM8, UNKNOWN},
                {"ttas-5.bpl", POWER, UNKNOWN},
                {"ttas-5-acq2rx.bpl", TSO, UNKNOWN},
                {"ttas-5-acq2rx.bpl", ARM8, UNKNOWN},
                {"ttas-5-acq2rx.bpl", POWER, UNKNOWN},
                {"ttas-5-rel2rx.bpl", TSO, UNKNOWN},
                {"ttas-5-rel2rx.bpl", ARM8, FAIL},
                {"ttas-5-rel2rx.bpl", POWER, FAIL},
                {"ticketlock-3.bpl", TSO, UNKNOWN},
                {"ticketlock-3.bpl", ARM, UNKNOWN},
                {"ticketlock-3.bpl", POWER, UNKNOWN},
                {"ticketlock-3-acq2rx.bpl", TSO, UNKNOWN},
                {"ticketlock-3-acq2rx.bpl", ARM, UNKNOWN},
                {"ticketlock-3-acq2rx.bpl", POWER, UNKNOWN},
                {"ticketlock-3-rel2rx.bpl", TSO, UNKNOWN},
                {"ticketlock-3-rel2rx.bpl", ARM, FAIL},
                {"ticketlock-3-rel2rx.bpl", POWER, FAIL}
                //TODO: Add remaining tests
        });

    }

    public CLocksTestAlternative(String path, Arch target, Result expected) {
        this.path = path;
        this.target = target;
        this.expected = expected;
    }

    @Test
    public void testAssume() throws Exception {
	    assertEquals(expected, runAnalysisAssumeSolver(contextProvider.get(), proverProvider.get(), taskProvider.get()));
    }

    @Test
    public void testRefinement() throws Exception {
            assertEquals(expected, Refinement.runAnalysisSaturationSolver(contextProvider.get(), proverProvider.get(),
                    RefinementTask.fromVerificationTaskWithDefaultBaselineWMM(taskProvider.get())));
    }
}