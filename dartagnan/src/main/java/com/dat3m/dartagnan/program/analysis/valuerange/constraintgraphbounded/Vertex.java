package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SummaryDomain;
import com.dat3m.dartagnan.program.event.core.Event;

public abstract class Vertex<AD extends SimpleDomain> {
	
	AD sample;
	
	// The event the vertex is associated with.
	// Each vertex is uniquely defined by the CId of its event.
	Event event;

	int sccId = -1;
		
	Map<String, List<Vertex<AD>>> predecessors = null;
	Map<String, List<Vertex<AD>>> successors = null;
	
	SummaryDomain<AD> out;
	
	// ========== Constructor. ==========
	
	// DONE.
	public Vertex(Event event, AD sample) {
		
		this.sample = sample;
		
		this.predecessors = new HashMap<String, List<Vertex<AD>>>();
		this.successors = new HashMap<String, List<Vertex<AD>>>();
		
		this.out = null;
		
		this.event = event;
		
	}
	
	// ========== Get- and set methods. ==========
	
	// DONE.
	public int getSccId() {
		
		return this.sccId;
		
	}
	
	// DONE.
	public SummaryDomain<AD> getOut() {
		
		return this.out;
		
	}

	// DONE.
	public Event getEvent() {
		
		return this.event;
		
	}

	// DONE.
	public void setSccId(int sccId) {
		
		this.sccId = sccId;
		
	}
	
	// DONE.
	public Map<String, List<Vertex<AD>>> getPredecessors() {
		
		return this.predecessors;
		
	}
	
	// DONE.
	public Map<String, List<Vertex<AD>>> getSuccessors() {
		
		return this.successors;
	}
	
	// DONE.
	public void addPredecessor(Vertex<AD> target, String label) {
		
		if (!this.predecessors.containsKey(label)) {
			this.predecessors.put(label, new LinkedList<Vertex<AD>>());
		}
		
		this.predecessors.get(label).add(target);
		
	}
	
	// DONE.
	public void addSuccessor(Vertex<AD> target, String label) {
		
		if (!this.successors.containsKey(label)) {
			this.successors.put(label, new LinkedList<Vertex<AD>>());
		}
		
		this.successors.get(label).add(target);
		
	}
	
	// ========== Important methods. ==========
	
	// DONE.
	public abstract void initialize();
	
	// DONE.
	public abstract List<Vertex<AD>> updateValue();
	
	// ========== Helper methods. ==========
	
	protected boolean sameScc(Vertex<AD> other) {
		
		return other.getSccId() == this.sccId;
		
	}
	
	// ========== Object methods. ==========
	
	// DONE.
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
		if (!(o instanceof Vertex<?>)) {
			return false;
		}

		Vertex<?> otherVertex = (Vertex<?>) o;
		return this.event.getCId() == otherVertex.getEvent().getCId();
		
	}
	
	// DONE.
	@Override
	public int hashCode() {
		
		return this.event.getCId();
		
	}

}
