package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.ProgramSummaryBounded;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.Vertex;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.VertexLoad;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.VertexLocal;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.VertexRestrict;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.VertexStore;
import com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded.VertexUnknown;
import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class BoundedConstraintGraph<AD extends SimpleDomain> {
	
	AD sample = null;
	
	Map<Event, ProgramSummaryBounded.VertexInformation> summary = null;
	
	Map<Event, Vertex<AD>> vertexSet = null;
	List<Integer> sccIdsTopologicallyOrdered = null;
	Map<Integer, List<Vertex<AD>>> verticesPerScc = null;
	
	// ========== Constructor. ==========
	
	public BoundedConstraintGraph(ProgramSummaryBounded programSummary, AD sample) {
		
		this.sample = sample;
		
		this.summary = programSummary.getSummary();
		
		this.printHello();
		
		this.build();
		
		this.printVertices();
		
		this.solve();
		
		this.printResults();
		
		this.printDone();
		
	}
	
	// ========== Build. ==========
	
	public void build() {
		
		this.initializeVertexSet();
		this.addEdges();
		this.initializeSccs();
		
	}
	
	private void initializeVertexSet() {
		
		this.vertexSet = new HashMap<Event, Vertex<AD>>();
		
		for (Event event : this.summary.keySet()) {
			
			if (event instanceof Local) {
				this.vertexSet.put(event, new VertexLocal<AD>(event, this.sample));
				continue;
			}
			
			if (event instanceof RestrictionEvent) { 
				this.vertexSet.put(event, new VertexRestrict<AD>(event, this.sample));
				continue;
			}
			
			if (event instanceof Load) {
				this.vertexSet.put(event, new VertexLoad<AD>(event, this.sample));
				continue;
			}
			
			if (event instanceof Store) {
				this.vertexSet.put(event, new VertexStore<AD>(event, this.sample));
				continue;
			}
			
			if (event instanceof Init) {
				this.vertexSet.put(event, new VertexStore<AD>(event, this.sample));
				continue;
			}
			
			if (event instanceof RegWriter) {
				this.vertexSet.put(event, new VertexUnknown<AD>(event, this.sample));
				continue;
			}
			
		}
		
	}
	
	private void addEdges() {
		
		for (Event event : this.summary.keySet()) {
			
			ProgramSummaryBounded.VertexInformation vertexInformation = this.summary.get(event);
			Vertex<AD> vertex = this.vertexSet.get(event);
			
			if (event instanceof Local) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof RestrictionEvent) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof Load) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof Store) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof Init) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof RegWriter) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex<AD> predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex<AD> successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
				
			}
			
		}
		
	}
	
	private void initializeSccs() {
		
		this.findSccs();
		
	}
	
	private void findSccs() {
		
		this.verticesPerScc = new HashMap<Integer, List<Vertex<AD>>>();
		this.sccIdsTopologicallyOrdered = new LinkedList<Integer>();
		
		Set<Vertex<AD>> noScc = new HashSet<Vertex<AD>>(this.vertexSet.values());
		Set<Vertex<AD>> hasScc = new HashSet<Vertex<AD>>();
		
		int nextSccId = 0;
		
		while (!noScc.isEmpty()) {
			
			Vertex<AD> currentVertex = null;
			for (Vertex<AD> v : noScc) {
				currentVertex = v;
				break;
			}
			
			List<Integer> foundSccs = new LinkedList<Integer>();
			
			Deque<Vertex<AD>> reachedVertices = this.kosarajuForwardDFS(currentVertex, hasScc);
			Set<Vertex<AD>> toConsiderForBackwardDFS = new HashSet<Vertex<AD>>(reachedVertices);
			
			while (!reachedVertices.isEmpty()) {
				
				Vertex<AD> top = reachedVertices.pop();
				
				if (hasScc.contains(top)) {
					continue;
				}
				
				toConsiderForBackwardDFS.retainAll(noScc);
				
				Set<Vertex<AD>> scc = this.kosarajuBackwardDFS(top, toConsiderForBackwardDFS);
				
				for (Vertex<AD> member : scc) {
					member.setSccId(nextSccId);
					if (!this.verticesPerScc.containsKey(nextSccId)) {
						this.verticesPerScc.put(nextSccId, new LinkedList<Vertex<AD>>());
					}
					this.verticesPerScc.get(nextSccId).add(member);
					noScc.remove(member);
					hasScc.add(member);
				}
				
				foundSccs.add(nextSccId);
				
				nextSccId++;
				
			}
			
			this.sccIdsTopologicallyOrdered.addAll(0, foundSccs);
			
		}
		
	}
	
	private Deque<Vertex<AD>> kosarajuForwardDFS(Vertex<AD> startVertex, Set<Vertex<AD>> hasScc) {
		
		Set<Vertex<AD>> visited = new HashSet<Vertex<AD>>(hasScc);
		Deque<Vertex<AD>> reverseVisitingOrder = new LinkedList<Vertex<AD>>();
		
		this.forwardDFSHelper(startVertex, reverseVisitingOrder, visited);
		
		return reverseVisitingOrder;
		
	}
	
	private void forwardDFSHelper(Vertex<AD> vertex, Deque<Vertex<AD>> reverseVisitingOrder, Set<Vertex<AD>> visited) {
		
		if (visited.contains(vertex)) {
			return;
		}
		
		visited.add(vertex);
		
		Map<String, List<Vertex<AD>>> successors = vertex.getSuccessors();
		
		for (String edgeLabel : successors.keySet()) {
			loopInner: for (Vertex<AD> successor : successors.get(edgeLabel)) {
				if (successor == null) {
					continue loopInner;
				}
				this.forwardDFSHelper(successor, reverseVisitingOrder, visited);
			}
		}
		
		reverseVisitingOrder.push(vertex);
		
	}
	
	
	
	private Set<Vertex<AD>> kosarajuBackwardDFS(Vertex<AD> startVertex, Set<Vertex<AD>> considerOnly) {
		
		Set<Vertex<AD>> visited = new HashSet<Vertex<AD>>();
		
		this.backwardDFSHelper(startVertex, visited, considerOnly);
		
		return visited;
		
	}
	
	private void backwardDFSHelper(Vertex<AD> vertex, Set<Vertex<AD>> visited, Set<Vertex<AD>> considerOnly) {
		
		if (visited.contains(vertex) || (!considerOnly.contains(vertex))) {
			return;
		}
		
		visited.add(vertex);
		
		Map<String, List<Vertex<AD>>> predecessors = vertex.getPredecessors();
		
		for (String edgeLabel : predecessors.keySet()) {
			loopInner: for (Vertex<AD> predecessor : predecessors.get(edgeLabel)) {
				if (predecessor == null) {
					continue loopInner;
				}
				this.backwardDFSHelper(predecessor, visited, considerOnly);
			}
		}
		
	}
	
	// ========== Methods for unbounded solve. ==========
	
	private void solve() {
		
		for (int sccId : this.sccIdsTopologicallyOrdered) {
			System.out.println("Solving SCC: " + sccId);
			System.out.println("SCC-sice:" + this.verticesPerScc.get(sccId).size());
			this.solveScc(sccId);
		}
		
	}
	
	private void solveScc(int sccId) {
		
		this.initializeVertices(sccId);
		
		Set<Vertex<AD>> changed = new HashSet<Vertex<AD>>(this.verticesPerScc.get(sccId));
		Set<Vertex<AD>> newChanged = new HashSet<Vertex<AD>>();
		
		int bound = this.verticesPerScc.get(sccId).size();
		int iteration = 0;
		
		while (iteration < bound + 1) {
		    
    		while (!changed.isEmpty()) {
    			
    			Vertex<AD> currentVertex = null;
    			for(Vertex<AD> v : changed) {
    				currentVertex = v;
    				break;
    			}
    			
    			changed.remove(currentVertex);
    			
    			List<Vertex<AD>> toNotifyList = currentVertex.updateValue();
    			
    			for (Vertex<AD> toNotify : toNotifyList) {
    				if (toNotify.getSccId() == sccId) {
    					newChanged.add(toNotify);
    				}
    			}
    			
    		}
    		
    		changed = new HashSet<Vertex<AD>>(newChanged);
    		newChanged.clear();
    		
    		iteration++;
    		
		}
		
	}
	
	private void initializeVertices(int sccId) {

		for (Vertex<AD> v : this.verticesPerScc.get(sccId)) {
			v.initialize();
		}
		
	}
	
	// ========== Getting results. ==========
	
	public Map<Event, AD> getResults() {
		
		Map<Event, AD> results = new HashMap<Event, AD>();
		
		for (Event event : this.vertexSet.keySet()) {
			
			if (event instanceof RegWriter && !(event instanceof RestrictionEvent)) {
				
				Vertex<AD> eventVertex = this.vertexSet.get(event);
				AD result = eventVertex.getOut().sumDomSummarize();
				
				results.put(event, result);
				
			}
			
			if (event instanceof Store && !(event instanceof RestrictionEvent)) {
                
                Vertex<AD> eventVertex = this.vertexSet.get(event);
                AD result = eventVertex.getOut().sumDomSummarize();
                
                results.put(event, result);
                
            }
			
		}
		
		return results;
		
	}
	
	// ========== Print methods (helpful for debugging). ==========
	
	private void printHello() {
		
		System.out.println("==================== Building and solving VRA constraint graph. ====================");
		System.out.println(System.getenv("DAT3M_HOME"));
		
	}
	
	private void printVertices() {
		
		System.out.println("==================== Vertices. ====================");
		
		List<Vertex<AD>> vertexList = new LinkedList<Vertex<AD>>(this.vertexSet.values());
		
		for (int i = 0; i < vertexList.size(); i++) {
			System.out.println(vertexList.get(i).toString());
			if (i < vertexList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printResults() {
		
		System.out.println("==================== Results ====================");
		
		for (Event event : this.vertexSet.keySet()) {
			System.out.println("" + event.getCId() + ", " + event.toString() + ": " + this.vertexSet.get(event).getOut().sumDomSummarize().toString());
		}
		
	}
	
	private void printDone() {
		
		System.out.println("==================== Done building and solving VRA constraint graph. ====================\n\n");
		
	}

}
