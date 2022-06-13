package com.dat3m.dartagnan.compilation;

import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.configuration.Arch;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class C112PPCTest extends AbstractCompilationTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
        return buildLitmusTests("litmus/C11/");
    }

    public C112PPCTest(String path) {
        super(path);
    }

	@Override
	protected Provider<Arch> getSourceProvider() {
		return () -> Arch.C11;
	}

    @Override
    protected Provider<Wmm> getSourceWmmProvider() {
        return Providers.createWmmFromName(() -> "c11");
    }

	@Override
	protected Provider<Arch> getTargetProvider() {
		return () -> Arch.POWER;
	}

	@Override
	protected List<String> getCompilationBreakers() {
		return Arrays.asList(
				"manual/IRIW-sc-sc-acq-sc-acq-sc",
				"manual/RWC-sc-acq-sc-sc-sc")
		.stream().map(p -> ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/C11/" + p + ".litmus").collect(Collectors.toList());
	}
}