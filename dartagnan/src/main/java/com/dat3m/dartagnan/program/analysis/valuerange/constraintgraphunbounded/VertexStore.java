package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.AbstractDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.ZInterval;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.Store;

public class VertexStore extends Vertex {
    
    /*
	
	ExprInterface expr = null;
	
	ZInterval in = null;
	
	// ========== Constructor. ==========

	public VertexStore(int vertexId, Event event) {
		
		super(vertexId, event);
		
		if (event instanceof Store) {
			Store store = (Store) event;
			this.expr = store.getMemValue();
		} else if (event instanceof Init) {
			Init init = (Init) event;
			this.expr = init.getMemValue();
		}
		
	}
	
	// ========== Methods from Vertex. ==========
	
	@Override
	public void initialize() {
		
		this.out = new ZInterval();
		this.out.getFullRange();
		
	}

	@Override
	public List<Vertex> updateGrowthAnalysis() {

		this.collectIn();
		
		ZInterval savedOut = new ZInterval(this.out);
		
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
	
	// ========== Helper methods. ===========
	
	private void collectIn() {
		
		Map<String, AbstractDomain> inRegs = new HashMap<String, AbstractDomain>();
			
		Set<String> inputRegNames = this.predecessors.keySet();
		
		for (String regName : inputRegNames) {
			List<Vertex> inputVertices = this.predecessors.get(regName);
			ZInterval collectedValues = new ZInterval();
			for (Vertex v : inputVertices) {
				if (v.getOut() != null) {
					collectedValues.unionWith(v.getOut());
				}
			}
			inRegs.put(regName, collectedValues);
		}
		
		this.in.evaluateExpression(inRegs, this.expr);
			
	}
	
	// ========== Object methods ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Store-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
		outString += "Vertex-Id: " + this.vertexId + "\n";
		
		outString += "Thread-Id: " + this.threadId + "\n";
		
		outString += "Scc-Id: " + this.sccId + "\n";
		
		outString += "Expression: " + this.expr.toString() + "\n";
		
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
