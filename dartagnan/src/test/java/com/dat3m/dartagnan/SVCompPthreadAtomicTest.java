package com.dat3m.dartagnan;

import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.dat3m.dartagnan.wmm.utils.Mode;
import com.dat3m.dartagnan.parsers.boogie.C2BoogieRunner;
import com.dat3m.dartagnan.parsers.boogie.SVCOMPSanitizer;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.utils.alias.Alias;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static com.dat3m.dartagnan.wmm.utils.Arch.NONE;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SVCompPthreadAtomicTest {

    private static final String CAT_RESOURCE_PATH = "../";
    private static final String BENCHMARKS_RESOURCE_PATH = "../benchmarks/C/pthread-atomic/";

    @Parameterized.Parameters(name = "{index}: {0} {2} -> {3} {6}")
    public static Iterable<Object[]> data() throws IOException {

        Wmm wmmSc = new ParserCat().parse(new File(CAT_RESOURCE_PATH + "cat/sc.cat"));
        Wmm wmmTso = new ParserCat().parse(new File(CAT_RESOURCE_PATH + "cat/tso.cat"));

        Settings s1 = new Settings(Mode.KNASTER, Alias.CFIS, 1);

        return Arrays.asList(new Object[][] {      	
        	{ BENCHMARKS_RESOURCE_PATH + "dekker.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "dekker.i", FAIL, NONE, wmmTso, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "gcd-2.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "lamport.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "lamport.i", FAIL, NONE, wmmTso, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "peterson.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "peterson.i", FAIL, NONE, wmmTso, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "qrcu-1.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "qrcu-2.i", FAIL, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "read_write_lock-1.i", PASS, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "read_write_lock-2.i", FAIL, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "scull.i", PASS, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "szymanski.i", UNKNOWN, NONE, wmmSc, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "szymanski.i", FAIL, NONE, wmmTso, s1 },
        	{ BENCHMARKS_RESOURCE_PATH + "time_var_mutex.i", PASS, NONE, wmmSc, s1 },
        });
    }
    
    private String programFilePath;
    private Result expected;
    private Arch target;
    private Wmm wmm;
    private Settings settings;

    public SVCompPthreadAtomicTest(String path, Result expected, Arch target, Wmm wmm, Settings settings) {
		this.programFilePath = new C2BoogieRunner(new SVCOMPSanitizer(path).run()).run();
        this.expected = expected;
        this.target = target;
        this.wmm = wmm;
        this.settings = settings;
    }

    // 5 Minutes timeout
    @Test(timeout=300000)
    public void test() {
        try {
            File file = new File(programFilePath);
			Program program = new ProgramParser().parse(file);
			file.delete();
            Context ctx = new Context();
            Solver solver = ctx.mkSolver(ctx.mkTactic(Settings.TACTIC));
            assertEquals(expected, Dartagnan.testProgram(solver, ctx, program, wmm, target, settings));
            ctx.close(); 
		} catch (IOException e){
            fail("Missing resource file");
        }
   }
}