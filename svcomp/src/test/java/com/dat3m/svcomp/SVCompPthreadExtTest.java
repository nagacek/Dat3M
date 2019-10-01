package com.dat3m.svcomp;

import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.dat3m.dartagnan.wmm.utils.Mode;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.utils.alias.Alias;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.PASS;
import static com.dat3m.dartagnan.utils.Result.UNKNOWN;
import static com.dat3m.dartagnan.wmm.utils.Arch.NONE;

@RunWith(Parameterized.class)
public class SVCompPthreadExtTest extends AbstractSVCOMPTest {

//	private static final String CAT_RESOURCE_PATH = "../";
//    private static final String BENCHMARKS_RESOURCE_PATH = "../benchmarks/C/pthread-ext/";
//
//    @Parameterized.Parameters(name = "{index}: {0} {2} -> {3} {6}")
//    public static Iterable<Object[]> data() throws IOException {
//
//        Wmm wmmSc = new ParserCat().parse(new File(CAT_RESOURCE_PATH + "cat/svcomp.cat"));
//        Settings s1 = new Settings(Mode.KNASTER, Alias.CFIS, 1);
//        Settings s2 = new Settings(Mode.KNASTER, Alias.CFIS, 2);
//        Settings s3 = new Settings(Mode.KNASTER, Alias.CFIS, 3);
//
//        return Arrays.asList(new Object[][] {      	
//        	{ BENCHMARKS_RESOURCE_PATH + "01_inc.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "02_inc_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "03_incdec.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "04_incdec_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "05_tas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	// 06: Even if there is a while(1), it seems bound events are never executed
//        	{ BENCHMARKS_RESOURCE_PATH + "06_ticket.i", PASS, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "07_rand.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "08_rand_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "09_fmaxsym.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "10_fmaxsym_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "11_fmaxsymopt.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "12_fmaxsymopt_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "13_unverif.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "14_spin2003.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "15_dekker.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "16_peterson.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "17_szymanski.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "18_read_write_lock.i", PASS, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "19_time_var_mutex.i", PASS, NONE, wmmSc, s2 },
//        	{ BENCHMARKS_RESOURCE_PATH + "20_lamport.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "23_lu-fig2.fixed.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "25_stack.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "25_stack_longer-1.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "25_stack_longer-2.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "25_stack_longest-1.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "25_stack_longest-2.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "26_stack_cas.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "26_stack_cas_longer-1.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "26_stack_cas_longer-2.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "26_stack_cas_longest-1.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "26_stack_cas_longest-2.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "27_Boop_simple_vf.i", FAIL, NONE, wmmSc, s2 },
//        	{ BENCHMARKS_RESOURCE_PATH + "28_buggy_simple_loop1_vf.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "29_conditionals_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "30_Function_Pointer3_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "31_simple_loop5_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "32_pthread5_vs.i", FAIL, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "33_double_lock_p1_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "34_double_lock_p2_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "35_double_lock_p3_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	// 36: Even if there is a while(1), it seems bound events are never executed
//        	{ BENCHMARKS_RESOURCE_PATH + "36_stack_cas_p0_vs_concur.i", PASS, NONE, wmmSc, s1 },
//        	// 37: Even if there is a while(1), it seems bound events are never executed
//        	{ BENCHMARKS_RESOURCE_PATH + "37_stack_lock_p0_vs_concur.i", PASS, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "38_rand_cas_vs_concur.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "39_rand_lock_p0_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "40_barrier_vf.i", FAIL, NONE, wmmSc, s3 },
//        	{ BENCHMARKS_RESOURCE_PATH + "41_FreeBSD_abd_kbd_sliced.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "42_FreeBSD_rdma_addr_sliced.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "43_NetBSD_sysmon_power_sliced.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "44_Solaris_space_map_sliced.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "45_monabsex1_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "46_monabsex2_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "47_ticket_lock_hc_backoff_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        	{ BENCHMARKS_RESOURCE_PATH + "48_ticket_lock_low_contention_vs.i", UNKNOWN, NONE, wmmSc, s1 },
//        });
//    }    
//
//    public SVCompPthreadExtTest(String path, Result expected, Arch target, Wmm wmm, Settings settings) {
//		super(path, expected, target, wmm, settings);
//	}
}