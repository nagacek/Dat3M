package com.dat3m.dartagnan.program.analysis.reachingdefinitions.abstractdomain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class ReachedDefinitionsList {
	
	Map<String, Set<Event>> reachedDefinitions;
	
	// ========== Constructors. ==========
	
	public ReachedDefinitionsList() {
		
		this.reachedDefinitions = new HashMap<String, Set<Event>>();
		
	}
	
	public ReachedDefinitionsList(ReachedDefinitionsList rdl) {
		
		this.reachedDefinitions = new HashMap<String, Set<Event>>();
		
		Map<String, Set<Event>> otherRd = rdl.getReachedDefinitions();
		for (String reg : otherRd.keySet()) {
			this.reachedDefinitions.put(reg, new HashSet<Event>(otherRd.get(reg)));
		}
		
	}
	
	// ========== Get and set methods. ==========
	
	public Map<String, Set<Event>> getReachedDefinitions() {
		
		return this.reachedDefinitions;
		
	}
	
	public Set<Event> getResultForReg(String regName) {
		
		return this.reachedDefinitions.get(regName);
		
	}
	
	// ========== Other methods. ==========
	
	public void unionWith(ReachedDefinitionsList rdl) {
		
		Map<String, Set<Event>> otherRd = rdl.getReachedDefinitions();
		
		for (String reg : otherRd.keySet()) {
			if (!this.reachedDefinitions.containsKey(reg)) {
				this.reachedDefinitions.put(reg, new HashSet<Event>());
			}
			this.reachedDefinitions.get(reg).addAll(otherRd.get(reg));
		}
		
	}
	
	public void applyTransferFunction(Event event) {

		if (event instanceof RegWriter) {
			String writtenTo = ((RegWriter) event).getResultRegister().getName();
			if (!this.reachedDefinitions.containsKey(writtenTo)) {
				this.reachedDefinitions.put(writtenTo, new HashSet<Event>());
			}
			this.reachedDefinitions.get(writtenTo).clear();
			this.reachedDefinitions.get(writtenTo).add(event);
		}
		
	}
	
	public void addInitialValues(Set<String> regNames) {
		
		for (String regName : regNames) {
			if (!this.reachedDefinitions.containsKey(regName)) {
				this.reachedDefinitions.put(regName, new HashSet<Event>());
			}
			this.reachedDefinitions.get(regName).add(null);
		}
		
	}
	
	// ========== Method overrides from Object. ==========
	
	@Override
	public String toString() {
		
		String outString = "{";
		
		LinkedList<String> outputList = new LinkedList<String>();
		
		for (String regName : this.reachedDefinitions.keySet()) {
			for (Event event : this.reachedDefinitions.get(regName)) {
				String eventName = (event == null ? "null" : "" + event.getCId());
				outputList.add("(" + regName + ", [" + eventName + "])");
			}
		}
		
		for (int i = 0; i < outputList.size(); i++) {
			outString += outputList.get(i);
			if (i < outputList.size() - 1) {
				outString += ", ";
			}
		}
		
		outString += "}";
		
		return outString;
		
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
		
		if (!(o instanceof ReachedDefinitionsList)) {
			return false;
		}
		
		ReachedDefinitionsList otherRdl = (ReachedDefinitionsList) o;
		Map<String, Set<Event>> otherRd = otherRdl.getReachedDefinitions();
		
		return this.reachedDefinitions.equals(otherRd);
		
	}
	
	@Override 
	public int hashCode() {
		
		return this.reachedDefinitions.hashCode();
		
	}
	
}
