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
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.Store;

public class VertexStore<AD extends SimpleDomain> extends Vertex<AD> {
	
	ExprInterface expr = null;
	
	Map<String, AbstractDomain> termIn = null;
	SummaryDomain<AD> in = null;
	
	LinkedList<Integer> toFilter = null;
	
	// ========== Constructor. ==========

	public VertexStore(Event event, AD sample) {
		
		super(event, sample);
		
		if (event instanceof Store) {
			Store store = (Store) event;
			this.expr = store.getMemValue();
		} else if (event instanceof Init) {
			Init init = (Init) event;
			this.expr = init.getMemValue();
		}
		
		this.toFilter = new LinkedList<Integer>();
		this.toFilter.add(this.event.getCId());
		
		this.successors.put("store", new LinkedList<Vertex<AD>>());
		
	}
	
	// ========== Methods from Vertex. ==========
	
	@Override
	public void initialize() {
		
		this.out = new SummaryDomain<AD>(this.sample);
		
	}
	
	@Override
	public List<Vertex<AD>> updateValue() {

		this.collectTermIn();
		
		this.calculateIn();
		
		for (int filter : this.toFilter) {
			this.in.sumDomFilterByIndex(filter);
		}
		
		this.in.sumDomAddIndex(this.event.getCId());
		
		if (this.out.equals(this.in)) {
			this.out = this.in;
			return new LinkedList<Vertex<AD>>();
		} else {
			this.out = this.in;
			return this.successors.get("store");
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
	
	// ========== Object methods ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Store-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
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
