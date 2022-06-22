package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.ZInterval;
import com.dat3m.dartagnan.program.event.core.Event;

public abstract class Vertex {
    
    /*
	
	Event event;
	
	int vertexId = -1;
	int threadId = -1;
	int sccId = -1;
		
	Map<String, List<Vertex>> predecessors = null;
	Map<String, List<Vertex>> successors = null;
	
	ZInterval out;
	
	// ========== Constructor. ==========
	
	// DONE.
	public Vertex(int vertexId, Event event) {
		
		this.vertexId = vertexId;
		this.threadId = event.getThread().getId();
		
		this.predecessors = new HashMap<String, List<Vertex>>();
		this.successors = new HashMap<String, List<Vertex>>();
		
		this.out = null;
		
		this.event = event;
		
	}
	
	// ========== Get- and set methods. ==========
	
	// DONE.
	public int getVertexId() {
		
		return this.vertexId;
		
	}
	
	// DONE.
	public int getThreadId() {
		
		return this.threadId;
		
	}
	
	// DONE.
	public int getSccId() {
		
		return this.sccId;
		
	}
	
	// DONE.
	public ZInterval getOut() {
		
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
	public Map<String, List<Vertex>> getPredecessors() {
		
		return this.predecessors;
		
	}
	
	// DONE.
	public Map<String, List<Vertex>> getSuccessors() {
		
		return this.successors;
	}
	
	// DONE.
	public void addPredecessor(Vertex target, String label) {
		
		if (!this.predecessors.containsKey(label)) {
			this.predecessors.put(label, new LinkedList<Vertex>());
		}
		
		this.predecessors.get(label).add(target);
		
	}
	
	// DONE.
	public void addSuccessor(Vertex target, String label) {
		
		if (!this.successors.containsKey(label)) {
			this.successors.put(label, new LinkedList<Vertex>());
		}
		
		this.successors.get(label).add(target);
		
	}
	
	// ========== Important methods. ==========
	
	// DONE.
	public abstract void initialize();
	
	// DONE.
	public abstract List<Vertex> updateGrowthAnalysis();
	
	// DONE.
	public abstract List<Vertex> updateNarrowing();	
	
	// ========== Object methods. ==========
	
	// DONE.
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
		if (!(o instanceof Vertex)) {
			return false;
		}
		Vertex otherVertex = (Vertex) o;
		return this.vertexId == otherVertex.getVertexId();
	}
	
	// DONE.
	@Override
	public int hashCode() {
		
		return this.vertexId;
		
	}
	
	*/
	
}
