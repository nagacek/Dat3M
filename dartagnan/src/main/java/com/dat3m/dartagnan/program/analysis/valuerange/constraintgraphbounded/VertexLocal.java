package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.AbstractDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SummaryDomain;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class VertexLocal<AD extends SimpleDomain> extends Vertex<AD> {
	
	ExprInterface expr = null;
	
	Map<String, AbstractDomain> termIn = null; 
	SummaryDomain<AD> in = null;
	
	String targetRegName = null;
	
	// ========== Constructor. ==========

	public VertexLocal(Event event, AD sample) {
		
		super(event, sample);
		
		Local local = (Local) event;
		this.expr = local.getExpr();
		
		this.targetRegName = ((RegWriter) this.event).getResultRegister().getName();
		
		this.successors.put(this.targetRegName, new LinkedList<Vertex<AD>>());
		
	}
	
	// ========== Methods from Vertex. ==========
	
	@Override
	public void initialize() {
		
		this.out = new SummaryDomain<AD>(sample);
		
	}
	
	@Override
	public List<Vertex<AD>> updateValue() {
		
		this.collectTermIn();
		
		this.calculateIn();
		
		if (this.out.equals(this.in)) {
			this.out = this.in;
			return new LinkedList<Vertex<AD>>();
		} else {
			this.out = this.in;
			return this.successors.get(this.targetRegName);
		}
		
	}
	
	// ========== Helper methods. ===========
	
	private void collectTermIn() {
		
		this.termIn = new HashMap<String, AbstractDomain>();
			
		Set<String> inputRegNames = this.predecessors.keySet();
		
		for (String regName : inputRegNames) {
			
			List<Vertex<AD>> inputVertices = this.predecessors.get(regName);
			SummaryDomain<AD> collectedValues = new SummaryDomain<AD>(this.sample);
			
			for (Vertex<AD> v : inputVertices) {
				if (v == null) {
					SummaryDomain<AD> zero = new SummaryDomain<AD>(this.sample);
					zero.getElementFromConst(new BigInteger("0"));
					collectedValues.unionWith(zero);
				} else {
					if (!this.sameScc(v)) {
						collectedValues.addAD(v.getOut().sumDomSummarize());
					} else {
						collectedValues.unionWith(v.getOut());
					}
				}
			}
			
			this.termIn.put(regName, collectedValues);
			
		}
			
	}
	
	private void calculateIn() {
		
		this.in = new SummaryDomain<AD>(this.sample);
		
		this.in.evaluateExpression(this.termIn, this.expr);
		
	}
	
	// ========== Object methods. ===========
	
		@Override public String toString() {
			
		String outString = "";
		
		outString += "Type: Local-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
		outString += "Expression: " + this.expr.toString() + "\n";
		
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
