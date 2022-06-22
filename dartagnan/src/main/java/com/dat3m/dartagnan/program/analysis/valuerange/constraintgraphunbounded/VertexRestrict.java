package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.ZInterval;
import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.Event;

public class VertexRestrict extends Vertex {
    
    /*
	
	BigInteger constant = null;
	COpBin operator = null;
	
	boolean canResolveDuringGrowthAnalysis = false;
	
	ZInterval input = null;
	ZInterval restriction = null;
	
	ZInterval restrictionInterval = null;
	
	// ========== Constructor. ==========

	public VertexRestrict(int vertexId, Event event) {
		
		super(vertexId, event);
		
		RestrictionEvent restriction = (RestrictionEvent) event;
		this.constant = restriction.getCompareValue();
		this.operator = restriction.getOperator();
		
	}
	
	// ========== Methods from Vertex. ==========

	@Override
	public void initialize() {
		
		this.out = new ZInterval();
		
	}
	
	@Override
	public List<Vertex> updateGrowthAnalysis() {
		
		this.collectInput();
		
		ZInterval savedOut = new ZInterval(this.out);
		
		this.out = new ZInterval(this.input);
		
		if (this.out.equals(savedOut)) {
			return new LinkedList<Vertex>();
		} else {
			return this.successors.get("");
		}
		
	}

	@Override
	public List<Vertex> updateNarrowing() {
		
		this.collectInput();
		
		ZInterval savedOut = new ZInterval(this.out);
		
		this.out = new ZInterval(this.input);
		
		this.out.intersectWith(this.restrictionInterval);
		
		if (this.out.equals(savedOut)) {
			return new LinkedList<Vertex>();
		} else {
			return this.successors.get("");
		}
		
	}
	
	// ========== Future-resolution. ==========
	
	public List<Vertex> futureResolution() {
		
		this.collectInput();
		this.collectRestriction();
		
		this.setRestrictionInterval();
		
		ZInterval savedOut = new ZInterval(this.out);
		
		this.out = this.input;
		
		this.out.intersectWith(this.restrictionInterval);
		
		if (this.out.equals(savedOut)) {
			return new LinkedList<Vertex>();
		} else {
			return this.successors.get("");
		}
		
	}
	
	// ========== Helper-methods. ==========
	
	private void setRestrictionInterval() {
		
		this.restrictionInterval = new ZInterval(this.restriction);
	
		if (this.constant == null) {
			this.restrictionInterval.restrictionFromOperator(this.operator);
		} else {
			this.restrictionInterval.restrictionFromOperatorAndConst(this.operator, this.constant);
		}
		
	}
	
	private void collectInput() {
		
		List<Vertex> inputVertices = this.predecessors.get("input");
		
		this.input = new ZInterval();
		for (Vertex v : inputVertices) {
			if (v.getOut() != null) {
				this.input.unionWith(v.getOut());
			}
		}
		
	}
	
	private void collectRestriction() {
		
		List<Vertex> restrictionVertices = this.predecessors.get("restriction");
		
		this.restriction = new ZInterval();
		for (Vertex v : restrictionVertices) {
			if (v.getOut() != null) {
				this.restriction.unionWith(v.getOut());
			}
		}
		
	}
	
	// ========== Object methods. ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Restrict-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
		outString += "Vertex-Id: " + this.vertexId + "\n";
		
		outString += "Thread-Id: " + this.threadId + "\n";
		
		outString += "Scc-Id: " + this.sccId + "\n";
		
		outString += "Constant: " + this.constant + "\n";
		
		outString += "Operator: " + this.operator + "\n";
		
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
