package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.util.LinkedList;
import java.util.List;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.ZInterval;
import com.dat3m.dartagnan.program.event.core.Event;

public class VertexLoad extends Vertex {
    
    /*
	
	ZInterval in = null;
	ZInterval savedIn = null;
	
	// ========== Constructor. ==========

	public VertexLoad(int vertexId, Event event) {
		
		super(vertexId, event);
		
	}
	
	// ========== Methods from Vertex. ==========
	
	@Override
	public void initialize() {
		
		this.out = new ZInterval();
		
	}

	@Override
	public List<Vertex> updateGrowthAnalysis() {
		
		this.collectIn();
		
		ZInterval savedOut = new ZInterval(this.out);
		
		this.out.widen(this.in);
		
		if (this.out.equals(savedOut)) {
			return new LinkedList<Vertex>();
		} else {
			return this.successors.get("");
		}
		
	}

	@Override
	public List<Vertex> updateNarrowing() {
		
		this.collectIn();
		
		ZInterval savedOut = new ZInterval(this.out);
		
		this.out.narrow(this.in);
		
		if (this.out.equals(savedOut)) {
			return new LinkedList<Vertex>();
		} else {
			return this.successors.get("");
		}
		
	}
	
	// ========== Methods for thread-interaction. ==========
	
	public void saveIn() {
		
		this.savedIn = new ZInterval(this.in);
		
	}
	
	public boolean inEqualsSaved() {
		
		this.collectIn();
		
		return this.in.equals(this.savedIn);
		
	}
	
	// ========== Helper methods. ===========
	
	private void collectIn() {
		
		List<Vertex> inputVertices = this.predecessors.get("");
		
		this.in = new ZInterval();
		
		for (Vertex v : inputVertices) {
			if (v.getOut() != null) {
				this.in.unionWith(v.getOut());
			}
		}
		
	}
	
	// ========== Object methods. ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Load-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
		outString += "Vertex-Id: " + this.vertexId + "\n";
		
		outString += "Thread-Id: " + this.threadId + "\n";
		
		outString += "Scc-Id: " + this.sccId + "\n";
		
		outString += "Predecessors:\n";
		if (this.predecessors.isEmpty()) {
			outString += "EMPTY";
		} else {
			List<String> labelList = new LinkedList<String>(this.predecessors.keySet());
			for (int j = 0; j < labelList.size(); j++) {
				String currentLabel = labelList.get(j);
				outString += currentLabel + ": ";
				List<Vertex> vertexList = this.predecessors.get(currentLabel);
				for (int i = 0; i < vertexList.size(); i++) {
					Vertex currentVertex = vertexList.get(i);
					if (currentVertex == null) {
						outString += "NULL";
					} else {
						outString += "[V: " + currentVertex.getVertexId() + "; E: " + currentVertex.getEvent().getCId() + ", " + currentVertex.getEvent().toString() + "]";
					}
					if (i < vertexList.size() - 1) {
						outString += ", ";
					}
				}
				if (j < labelList.size() - 1) {
					outString += "\n";
				}
			}
		}
		
		outString += "\n";
		
		outString += "Successors:\n";
		if (this.successors.isEmpty()) {
			outString += "EMPTY";
		} else {
			List<String> labelList = new LinkedList<String>(this.successors.keySet());
			for (int j = 0; j < labelList.size(); j++) {
				String currentLabel = labelList.get(j);
				outString += currentLabel + ": ";
				List<Vertex> vertexList = this.successors.get(currentLabel);
				for (int i = 0; i < vertexList.size(); i++) {
					Vertex currentVertex = vertexList.get(i);
					if (currentVertex == null) {
						outString += "NULL";
					} else {
						outString += "[V: " + currentVertex.getVertexId() + "; E: " + currentVertex.getEvent().getCId() + ", " + currentVertex.getEvent().toString() + "]";
					}
					if (i < vertexList.size() - 1) {
						outString += ", ";
					}
				}
				if (j < labelList.size() - 1) {
					outString += "\n";
				}
			}
		}
		
		return outString;
		
	}
	
	*/

}
