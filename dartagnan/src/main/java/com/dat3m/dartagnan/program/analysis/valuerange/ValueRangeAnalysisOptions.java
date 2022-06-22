package com.dat3m.dartagnan.program.analysis.valuerange;

public class ValueRangeAnalysisOptions {
	
	protected Type type = null;
	protected int summarySize = 0;
	protected ShorteningStrategy shorteningStrategy= null;
	
	public enum Type {
		
		// The CFG of each thread may contain loops and Intervals over the integers are used.
		// WARNING: This only works if there is no thread interaction or if the AliasAnalysis
		//   takes loops into account as well. Currently this is not the case.
		UNBOUNDED,
		// The program must be bounded and Intervals over the integers are used.
		BOUNDED_ZINTERVAL, 
		// The program must be bounded and bdds are used.
		BOUNDED_BDD, 
		// The program must be bounded and octagons are used.
		BOUNDED_OCTAGON
		
	}
	
	public enum ShorteningStrategy {
		
		ADD_TO_EMPTY,
		SMALLEST_KEY,
		HAMMING_AND,
		LAST_EXCLUDE
		
	}

}
