package com.dat3m.dartagnan.program.analysis.valuerange;

import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.ZInterval;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.BoundedConstraintGraph;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.ProgramSummaryBounded;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

/**
 * 
 * @author Johannes Schmechel, j.schmechel@tu-bs.de
 */
public class ValueRangeAnalysis {
	
	Program program;
	Configuration config;
	AliasAnalysis aliasAnalysis;
	
	Map<Event, ZInterval> results;
	
	private ValueRangeAnalysis(Program program, Configuration config, AliasAnalysis aliasAnalysis) {
		
		this.program = program;
		this.config = config;
		this.aliasAnalysis = aliasAnalysis;
		
		this.run();	
		
	}
	
	public static ValueRangeAnalysis fromConfig(Program program, Configuration config, AliasAnalysis aliasAnalysis) {
		
		return new ValueRangeAnalysis(program, config, aliasAnalysis);
		
	}
	
	public ZInterval getResult(RegWriter regWriter) {
		
		return this.results.get(regWriter);
		
	}
	
	private void run() {
		
		ProgramSummaryBounded programSummary = new ProgramSummaryBounded(this.program, this.aliasAnalysis);
		
		BoundedConstraintGraph<ZInterval> constraintGraph = new BoundedConstraintGraph<ZInterval>(programSummary, new ZInterval());
		
		this.results = constraintGraph.getResults();
		
		this.printResults();
		
	}
	
	private void printResults() {
		
		System.out.println("==================== Results of the VRA. =====================");
		for (com.dat3m.dartagnan.program.Thread thread : this.program.getThreads()) {
			System.out.println("========== Results for thread " + thread.getId() + ". ==========");
			for (Event event : thread.getEvents()) {
				if (event instanceof RegWriter || event instanceof Store) {
					ZInterval result = (ZInterval) this.results.get(event);
					System.out.println(event.getCId() + ", " + event.toString() + ": " + result.toString());
				} else {
					System.out.println(event.getCId() + ", " + event.toString());
				}
			}
		}
		System.out.println("==================== Results of the VRA. =====================\n\n");
		
	}

}
