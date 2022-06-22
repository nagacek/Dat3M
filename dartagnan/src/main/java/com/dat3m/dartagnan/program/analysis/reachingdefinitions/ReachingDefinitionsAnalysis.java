package com.dat3m.dartagnan.program.analysis.reachingdefinitions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.analysis.reachingdefinitions.abstractdomain.ReachedDefinitionsList;
import com.dat3m.dartagnan.program.analysis.reachingdefinitions.constraintgraph.RDAConstraintGraph;
import com.dat3m.dartagnan.program.analysis.reachingdefinitions.constraintgraph.RDAThreadSummary;
import com.dat3m.dartagnan.program.event.core.Event;

public class ReachingDefinitionsAnalysis {
	
	Map<Event, ReachedDefinitionsList> resultList;
	
	// ========== API. ==========
	
	public Set<Event> reachedDefinitions(Event event, String regName) {
		
		ReachedDefinitionsList resultForEvent = this.resultList.get(event);
		
		if (resultForEvent == null) {
			return null;
		}
		
		Set<Event> resultForReg = resultForEvent.getResultForReg(regName);
		
		return resultForReg;
		
	}
	
	// ========== Constructors. ==========
	
	
	private ReachingDefinitionsAnalysis() {
		
		this.resultList = new HashMap<Event, ReachedDefinitionsList>();
		
	}
	
	public static ReachingDefinitionsAnalysis fromConfig(Program program, Configuration config) {
		
		ReachingDefinitionsAnalysis rda = new ReachingDefinitionsAnalysis();
		
		for (com.dat3m.dartagnan.program.Thread thread : program.getThreads()) {
			
			List<Event> commandList = thread.getEvents();
			Event entry = thread.getEntry();
			
			rda.appendThread(commandList, entry);
			
		}
		
		return rda;
		
	}
	
	public static ReachingDefinitionsAnalysis getEmptyAnalysis() {
		
		ReachingDefinitionsAnalysis rda = new ReachingDefinitionsAnalysis();
		
		return rda;
		
	}
	
	// ========== Option to add a thread by hand. ==========
	
	public void appendThread(List<Event> commandList, Event entry) {
		
		RDAThreadSummary threadSummary = new RDAThreadSummary(commandList, entry);
		
		if (!threadSummary.analysisPossible()) {
			return;
		}
		
		RDAConstraintGraph constraintGraph = new RDAConstraintGraph(threadSummary);
		
		Map<Event, ReachedDefinitionsList> threadResults = constraintGraph.getResults();
		
		for (Event event : threadResults.keySet()) {
			this.resultList.put(event, threadResults.get(event));
		}
		
	}

}
