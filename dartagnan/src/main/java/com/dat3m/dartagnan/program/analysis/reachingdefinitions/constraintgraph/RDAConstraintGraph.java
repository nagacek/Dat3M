package com.dat3m.dartagnan.program.analysis.reachingdefinitions.constraintgraph;

import com.dat3m.dartagnan.program.analysis.reachingdefinitions.abstractdomain.ReachedDefinitionsList;
import com.dat3m.dartagnan.program.event.core.Event;

import java.util.*;

public class RDAConstraintGraph {
	
	RDAThreadSummary threadSummary = null;
	Map<Event, Vertex> vertexList = null;
	
	int vertexId = 0;
	
	public RDAConstraintGraph(RDAThreadSummary threadSummary) {
		
		this.threadSummary = threadSummary;
		this.vertexList = null;
		
		this.vertexId = 0;
		
		//this.printHello();
		
		this.build();
		
		//this.printVertices();
		
		this.solve();
		
		//this.printResults();
		
		//this.printDone();
		
	}
	
	public void build() {
		
		this.vertexList = new HashMap<Event, Vertex>();
		
		Map<Event, RDAThreadSummary.VertexInformation> summary = threadSummary.getSummary();
		
		for (Event event : summary.keySet()) {
			this.vertexList.put(event, new Vertex(this.vertexId++, event));
		}
		
		for (Event event : summary.keySet()) {
			
			RDAThreadSummary.VertexInformation vertexInformation = summary.get(event);
			Vertex vertex = this.vertexList.get(event);
					
			for (Event predecessorEvent : vertexInformation.getPredecessors()) {
				vertex.addPredecessor(this.vertexList.get(predecessorEvent));
			}
			
			for (Event successorEvent : vertexInformation.getSuccessors()) {
				vertex.addSuccessor(this.vertexList.get(successorEvent));
			}
			
			vertex.setIsEntry(vertexInformation.getIsEntry());
			vertex.setRegNames(vertexInformation.getRegNames());
			
		}
		
	}
	
	public void solve() {
		
		for (Vertex v : this.vertexList.values()) {
			v.initialize();
		}
		
		Set<Vertex> changed = new HashSet<Vertex>(this.vertexList.values());
		
		while (!changed.isEmpty()) {
			
			Vertex currentVertex = null;
			for (Vertex v : changed) {
				currentVertex = v;
				break;
			}
			
			changed.remove(currentVertex);
			
			List<Vertex> toNotify = currentVertex.updateValue();
			
			changed.addAll(toNotify);
			
		}
		
	}
	
	public Map<Event, ReachedDefinitionsList> getResults() {
		
		Map<Event, ReachedDefinitionsList> resultMap = new HashMap<Event, ReachedDefinitionsList>();
		
		for (Event event : this.vertexList.keySet()) {
			ReachedDefinitionsList resultForVertex = this.vertexList.get(event).getIn();
			resultMap.put(event, resultForVertex);
		}
		
		return resultMap;
		
	}
	
	public class Vertex {
		
		int id = -1;
		
		List<Vertex> predecessors = null;
		List<Vertex> successors = null;
		
		ReachedDefinitionsList in = null;
		ReachedDefinitionsList out = null;
		
		Event event = null;
		
		boolean isEntry = false;
		Set<String> regNames = null;
		
		public Vertex(int id, Event event) {
			
			this.id = id;
			this.event = event;
			
			this.predecessors = new LinkedList<Vertex>();
			this.successors = new LinkedList<Vertex>();
			
		}
		
		public int getId() {
			
			return this.id;
			
		}
		
		public Event getEvent() {
			
			return this.event;
			
		}
		
		public ReachedDefinitionsList getOut() {
			
			return this.out;
			
		}
		
		public ReachedDefinitionsList getIn() {
			
			return this.in;
			
		}
		
		public void addPredecessor(Vertex v) {
			
			this.predecessors.add(v);
			
		}
		
		public void addSuccessor(Vertex v) {
			
			this.successors.add(v);
			
		}
		
		public void setIsEntry(boolean isEntry) {
			
			this.isEntry = isEntry;
			
		}
		
		public void setRegNames(Set<String> regNames) {
			
			this.regNames = regNames;
			
		}
		
		public void initialize() {

			this.in = new ReachedDefinitionsList();
			if (this.isEntry) {
				this.in.addInitialValues(this.regNames);
			}
			this.out = new ReachedDefinitionsList(this.in);
			this.out.applyTransferFunction(this.event);
			
		}
		
		public List<Vertex> updateValue() {
			
			this.in = new ReachedDefinitionsList();
			for (Vertex v : this.predecessors) {
				this.in.unionWith(v.getOut());
			}
			if (this.isEntry) {
				this.in.addInitialValues(this.regNames);
			}
			ReachedDefinitionsList oldOut = this.out;
			this.out = new ReachedDefinitionsList(this.in);
			this.out.applyTransferFunction(this.event);
			if (this.out.equals(oldOut)) {
				return new LinkedList<Vertex>();
			} else {
				return this.successors;
			}
			
		}
		
		@Override
		public String toString() {
			
			String outString = "";
			
			outString += "Id: " + this.id+ "\n";
			
			outString += "Predecessors: ";
			if (this.predecessors == null) {
				outString += "NULL";
			} else if (this.predecessors.isEmpty()) {
				outString += "EMPTY";
			} else {
				for (int i = 0; i < this.predecessors.size(); i++) {
					outString += "[" + this.predecessors.get(i).getId() + "]";
					if (i < this.predecessors.size() - 1) {
						outString += ",";
					}
				}
			} 
			outString += "\n";
			
			outString += "Successors: ";
			if (this.successors == null) {
				outString += "NULL";
			} else if (this.successors.isEmpty()) {
				outString += "EMPTY";
			} else {
				for (int i = 0; i < this.successors.size(); i++) {
					outString += "[" + this.successors.get(i).getId() + "]";
					if (i < this.successors.size() - 1) {
						outString += ",";
					}
				}
			} 
			outString += "\n";
			
			outString += "In: " + (this.in == null ? "NULL" : "NOT NULL") + "\n";
			
			outString += "Out: " + (this.out == null ? "NULL" : "NOT NULL") + "\n";
			
			outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
			
			outString += "IsEntry: " + this.isEntry + "\n";
			
			outString += "RegNames: ";
			if (this.regNames == null) {
				outString += "NULL";
			} else if (this.regNames.isEmpty()) {
				outString += "EMPTY";
			} else {
				List<String> regNamesList = new LinkedList<String>(this.regNames); 
				for (int i = 0; i < this.regNames.size(); i++) {
					outString += regNamesList.get(i);
					if (i < this.successors.size() - 1) {
						outString += ",";
					}
				}
			}
			
			return outString;
				
		}
		
		@Override
		public boolean equals(Object o) {
			
			if (o == null) {
				return false;
			}
			if (!(o instanceof Vertex)) {
				return false;
			}
			Vertex otherVertex = (Vertex) o;
			return this.id == otherVertex.getId();
		}
		
		@Override
		public int hashCode() {
			
			return this.id;
			
		}
		
	}
	
	private void printHello() {
		
		System.out.println("==================== Building and Solving an RDA-constraint graph. ====================");
		
	}
	
	private void printVertices() {
		
		System.out.println("========== Vertices. ==========");
		List<Event> eventList = new LinkedList<Event>(this.vertexList.keySet());
		for (int i = 0; i < eventList.size(); i++) {
			Event currentEvent = eventList.get(i);
			Vertex v = this.vertexList.get(currentEvent);
			System.out.println(v.toString());
			if (i < eventList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printResults() {
		
		System.out.println("========== Results. ==========");
		List<Event> eventList = new LinkedList<Event>(this.vertexList.keySet());
		for (int i = 0; i < eventList.size(); i++) {
			Event currentEvent = eventList.get(i);
			System.out.println("Results for Event: " + currentEvent.getCId() + "; " + currentEvent.toString());
			Vertex v = this.vertexList.get(currentEvent);
			ReachedDefinitionsList rdl = v.getIn();
			if (rdl == null) {
				System.out.println("NULL");
			} else {
				System.out.println(rdl.toString());
			}
			if (i < eventList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printDone() {
		
		System.out.println("==================== Done building and Solving an RDA-constraint graph. ====================\n\n");
		
	}

}
