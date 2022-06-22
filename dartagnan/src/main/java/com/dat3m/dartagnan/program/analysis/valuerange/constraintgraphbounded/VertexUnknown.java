package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.util.LinkedList;
import java.util.List;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SummaryDomain;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class VertexUnknown<AD extends SimpleDomain> extends Vertex<AD> {
	
	String targetRegName = null;
	
	// ========== Constructor. ==========

	// DONE.
	public VertexUnknown(Event event, AD sample) {
		
		super(event, sample);
		
		this.targetRegName = ((RegWriter) this.event).getResultRegister().getName();
		
		this.successors.put(this.targetRegName, new LinkedList<Vertex<AD>>());

	}
	
	// ========== Methods from Vertex. ==========
	
	// DONE.
	@Override
	public void initialize() {

		this.out = new SummaryDomain<AD>(this.sample);
		
	}
	
	@Override
	public List<Vertex<AD>> updateValue() {
		
		this.out = new SummaryDomain<AD>(this.sample);
		this.out.getFullRange();
		
		return this.predecessors.get(this.targetRegName);
				
	}
	
	// ========== Object methods. ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: UnkownRegWriter-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
		outString += "Scc-Id: " + this.sccId + "\n";
		
		outString += "Predecessors:\n";
		if (this.predecessors.isEmpty()) {
			outString += "EMPTY";
		} else {
			List<String> labelList = new LinkedList<String>(this.predecessors.keySet());
			for (int j = 0; j < labelList.size(); j++) {
				String currentLabel = labelList.get(j);
				outString += currentLabel + ": ";
				List<Vertex<AD>> vertexList = this.predecessors.get(currentLabel);
				for (int i = 0; i < vertexList.size(); i++) {
					Vertex<AD> currentVertex = vertexList.get(i);
					if (currentVertex == null) {
						outString += "NULL";
					} else {
						outString += "[Event: " + currentVertex.getEvent().getCId() + ", " + currentVertex.getEvent().toString() + "]";
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
				List<Vertex<AD>> vertexList = this.successors.get(currentLabel);
				for (int i = 0; i < vertexList.size(); i++) {
					Vertex<AD> currentVertex = vertexList.get(i);
					if (currentVertex == null) {
						outString += "NULL";
					} else {
						outString += "[Event: " + currentVertex.getEvent().getCId() + ", " + currentVertex.getEvent().toString() + "]";
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

}