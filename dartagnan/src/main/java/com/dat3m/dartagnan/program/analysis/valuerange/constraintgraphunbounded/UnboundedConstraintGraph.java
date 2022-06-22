package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class UnboundedConstraintGraph {
    
    /*
	
	Map<Event, ProgramSummaryUnbounded.VertexInformation> summary = null;
	
	Map<Event, Vertex> vertexSet = null;
	
	LinkedList<Integer> threadList = null;
	Map<Integer, List<Integer>> sccIdsForThread = null;
	Map<Integer, List<Vertex>> verticesInScc = null;
	Map<Integer, List<VertexRestrict>> verticesInSccToResolve = null;
	
	// ========== Constructor. ==========
	
	public UnboundedConstraintGraph(ProgramSummaryUnbounded programSummary) {
		
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
		this.addEdgesInProcess();
		
		this.initializeSccs();
		
		this.addEdgesBetweenProcesses();
		
	}
	
	private void initializeVertexSet() {
		
		this.vertexSet = new HashMap<Event, Vertex>();
		
		int newVertexId = 0;
		
		for (Event event : this.summary.keySet()) {
			
			if (event instanceof Local) {
				this.vertexSet.put(event, new VertexLocal(newVertexId++, event));
				continue;
			}
			
			if (event instanceof RestrictionEvent) { 
				this.vertexSet.put(event, new VertexRestrict(newVertexId++, event));
				continue;
			}
			
			if (event instanceof Load) {
				this.vertexSet.put(event, new VertexLoad(newVertexId++, event));
				continue;
			}
			
			if (event instanceof Store) {
				this.vertexSet.put(event, new VertexStore(newVertexId++, event));
				continue;
			}
			
			if (event instanceof Init) {
				this.vertexSet.put(event, new VertexStore(newVertexId++, event));
				continue;
			}
			
			if (event instanceof RegWriter) {
				this.vertexSet.put(event, new VertexUnknown(newVertexId++, event));
				continue;
			}
			
		}
		
	}
	
	private void addEdgesInProcess() {
		
		for (Event event : this.summary.keySet()) {
			
			ProgramSummaryUnbounded.VertexInformation vertexInformation = this.summary.get(event);
			Vertex vertex = this.vertexSet.get(event);
			
			if (event instanceof Local) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
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
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof Load) {
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
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
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				continue;
			}
			
			if (event instanceof Init) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				continue;
			}
			
			if (event instanceof RegWriter) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
		}
		
	}
	
	private void addEdgesBetweenProcesses() {
		
		for (Event event : this.summary.keySet()) {
			
			ProgramSummaryUnbounded.VertexInformation vertexInformation = this.summary.get(event);
			Vertex vertex = this.vertexSet.get(event);
			
			if (event instanceof Load) {
				Map<String, List<Event>> predecessors = vertexInformation.getPredecessors();
				for (String label : predecessors.keySet()) {
					for (Event predecessorEvent : predecessors.get(label)) {
						if (predecessorEvent != null) {
							Vertex predecessorVertex = this.vertexSet.get(predecessorEvent);
							vertex.addPredecessor(predecessorVertex, label);
						} else {
							vertex.addPredecessor(null, label);
						}
					}
				}
				
				continue;
			}
			
			if (event instanceof Store) {
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
						vertex.addSuccessor(successorVertex, label);
					}
				}
				continue;
			}
			
			if (event instanceof Init) {
				Map<String, List<Event>> successors = vertexInformation.getSuccessors();
				for (String label : successors.keySet()) {
					for (Event successorEvent : successors.get(label)) {
						Vertex successorVertex = this.vertexSet.get(successorEvent);
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
		
		Set<Vertex> noScc = new HashSet<Vertex>(this.vertexSet.values());
		Set<Vertex> hasScc = new HashSet<Vertex>();
		
		int nextSccId = 0;
		
		while (!noScc.isEmpty()) {
			
			Vertex currentVertex = null;
			for (Vertex v : noScc) {
				currentVertex = v;
				break;
			}
			
			Deque<Vertex> reachedVertices = this.kosarajuForwardDFS(currentVertex, hasScc);
			Set<Vertex> toConsiderForBackwardDFS = new HashSet<Vertex>(reachedVertices);
			
			while (!reachedVertices.isEmpty()) {
				
				Vertex top = reachedVertices.pop();
				
				if (hasScc.contains(top)) {
					continue;
				}
				
				toConsiderForBackwardDFS.retainAll(noScc);
				
				Set<Vertex> scc = this.kosarajuBackwardDFS(top, toConsiderForBackwardDFS);
				
				for (Vertex member : scc) {
					member.setSccId(nextSccId);
					noScc.remove(member);
					hasScc.add(member);
				}
				
				nextSccId++;
				
			}
			
		}
		
	}
	
	private Deque<Vertex> kosarajuForwardDFS(Vertex startVertex, Set<Vertex> hasScc) {
		
		Set<Vertex> visited = new HashSet<Vertex>(hasScc);
		Deque<Vertex> reverseVisitingOrder = new LinkedList<Vertex>();
		
		this.forwardDFSHelper(startVertex, reverseVisitingOrder, visited);
		
		return reverseVisitingOrder;
		
	}
	
	private void forwardDFSHelper(Vertex vertex, Deque<Vertex> reverseVisitingOrder, Set<Vertex> visited) {
		
		if (visited.contains(vertex)) {
			return;
		}
		
		visited.add(vertex);
		
		Map<String, List<Vertex>> successors = vertex.getSuccessors();
		
		for (String edgeLabel : successors.keySet()) {
			loopInner: for (Vertex successor : successors.get(edgeLabel)) {
				if (successor == null) {
					continue loopInner;
				}
				this.forwardDFSHelper(successor, reverseVisitingOrder, visited);
			}
		}
		
		reverseVisitingOrder.push(vertex);
		
	}
	
	
	
	private Set<Vertex> kosarajuBackwardDFS(Vertex startVertex, Set<Vertex> considerOnly) {
		
		Set<Vertex> visited = new HashSet<Vertex>();
		
		this.backwardDFSHelper(startVertex, visited, considerOnly);
		
		return visited;
		
	}
	
	private void backwardDFSHelper(Vertex vertex, Set<Vertex> visited, Set<Vertex> considerOnly) {
		
		if (visited.contains(vertex) || (!considerOnly.contains(vertex))) {
			return;
		}
		
		visited.add(vertex);
		
		Map<String, List<Vertex>> predecessors = vertex.getPredecessors();
		
		for (String edgeLabel : predecessors.keySet()) {
			loopInner: for (Vertex predecessor : predecessors.get(edgeLabel)) {
				if (predecessor == null) {
					continue loopInner;
				}
				this.backwardDFSHelper(predecessor, visited, considerOnly);
			}
		}
		
	}
	
	// ========== Methods for unbounded solve. ==========
	
	private void solve() {
		
		for (int threadId : threadList) {
			
			List<Integer> sccIds = sccIdsForThread.get(threadId);
			
			for (int sccId : sccIds) {
				this.solveScc(sccId);
			}
			
		}
		
	}
	
	private List<Integer> solveThread(int threadId) {
		
		return null;
		
	}
	
	private List<Integer> solveScc(int sccId) {
		
		this.initializeVertices(sccId);
		
		this.growthAnalysis(sccId);
		
		this.futureResolution(sccId);
		
		this.narrowing(sccId);
		
		return null;
		
	}
	
	private void initializeVertices(int sccId) {

		for (Vertex v : this.verticesInScc.get(sccId)) {
			v.initialize();
		}
		
	}
	
	private Set<Integer> growthAnalysis(int sccId) {
		
		Set<Integer> threadToNotify = new HashSet<Integer>();
		
		// At the beginning we want to update all vertices in the scc.
		Set<Vertex> toUpdate = new HashSet<Vertex>(this.verticesInScc.get(sccId));
		
		// Update each vertex until we have updated them all...
		while (!toUpdate.isEmpty()) {
			
			// Pick any vertex from the set.
			Vertex currentVertex = null;
			for (Vertex v : toUpdate) {
				currentVertex = v;
				break;
			}
			
			toUpdate.remove(currentVertex);
			
			// Update the vertex.
			// Get a list of vertices that now may need an update too.
			List<Vertex> vertexToNotify = currentVertex.updateGrowthAnalysis();
			
			for (Vertex v : vertexToNotify) {
				// If the vertex is in a different thread we may need to update this other thread,
				// but we do not need to update the vertex in this call to grwothAnalysis.
				// This is because different thread ==> different scc.
				if (currentVertex.getThreadId() != v.getThreadId()) {
					threadToNotify.add(v.getThreadId());
					continue;
				}
				// We only need to update vertices in the same scc.
				if (currentVertex.getSccId() == v.getSccId()) {
					toUpdate.add(v);
				}
			}
			
		}
		
		return threadToNotify;
		
	}
	
	private Set<Integer> futureResolution(int sccId) {
		
		Set<Integer> threadToNotify = new HashSet<Integer>();
		
		List<VertexRestrict> toResolve = this.verticesInSccToResolve.get(sccId);
		
		for (VertexRestrict vr : toResolve) {
			List<Vertex> vertexToNotify = vr.futureResolution();
			for (Vertex v : vertexToNotify) {
				if (vr.getThreadId() != v.getThreadId()) {
					threadToNotify.add(v.getThreadId());
				}
			}
		}
		
		return threadToNotify;
		
	}
	
	private Set<Integer> narrowing(int sccId) {
		
		Set<Integer> threadToNotify = new HashSet<Integer>();
		
		// At the beginning we want to update all vertices in the scc.
		Set<Vertex> toUpdate = new HashSet<Vertex>(this.verticesInScc.get(sccId));
		
		// Update each vertex until we have updated them all...
		while (!toUpdate.isEmpty()) {
			
			// Pick any vertex from the set.
			Vertex currentVertex = null;
			for (Vertex v : toUpdate) {
				currentVertex = v;
				break;
			}
			
			toUpdate.remove(currentVertex);
			
			// Update the vertex.
			// Get a list of vertices that now may need an update too.
			List<Vertex> vertexToNotify = currentVertex.updateNarrowing();
			
			for (Vertex v : vertexToNotify) {
				// If the vertex is in a different thread we may need to update this other thread,
				// but we do not need to update the vertex in this call to narrowing.
				// This is because different thread ==> different scc.
				if (currentVertex.getThreadId() != v.getThreadId()) {
					threadToNotify.add(v.getThreadId());
					continue;
				}
				// We only need to update vertices in the same scc.
				if (currentVertex.getSccId() == v.getSccId()) {
					toUpdate.add(v);
				}
			}
			
		}
		
		return threadToNotify;
		
	}
	
	// ========== Getting results. ==========
	
	public void getResults() {
		
	}
	
	// ========== Print methods (helpful for debugging). ==========
	
	private void printHello() {
		
		System.out.println("==================== Building and solving VRA constraint graph. ====================");
		System.out.println(System.getenv("DAT3M_HOME"));
		
	}
	
	private void printVertices() {
		
		System.out.println("==================== Vertices. ====================");
		
		List<Vertex> vertexList = new LinkedList<Vertex>(this.vertexSet.values());
		
		for (int i = 0; i < vertexList.size(); i++) {
			System.out.println(vertexList.get(i).toString());
			if (i < vertexList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printResults() {
		
	}
	
	private void printDone() {
		
		System.out.println("==================== Done building and solving VRA constraint graph. ====================\n\n");
		
	}
	
	*/

}
