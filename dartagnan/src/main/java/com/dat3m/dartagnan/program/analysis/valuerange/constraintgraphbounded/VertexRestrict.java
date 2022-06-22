package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SummaryDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class VertexRestrict<AD extends SimpleDomain> extends Vertex<AD> {
	
	BigInteger constant = null;
	COpBin operator = null;
	
	SummaryDomain<AD> input = null;
	SummaryDomain<AD> restriction = null;
	
	String targetRegName = null;
	
	// ========== Constructor. ==========

	public VertexRestrict(Event event, AD sample) {
		
		super(event, sample);
		
		RestrictionEvent restriction = (RestrictionEvent) event;
		this.constant = restriction.getCompareValue();
		this.operator = restriction.getOperator();
		
		this.targetRegName = ((RegWriter) this.event).getResultRegister().getName();
		
		this.successors.put(this.targetRegName, new LinkedList<Vertex<AD>>());
		
	}
	
	// ========== Methods from Vertex. ==========

	@Override
	public void initialize() {
		
		this.out = new SummaryDomain<AD>(this.sample);
		
	}
	
	@Override
	public List<Vertex<AD>> updateValue() {
		
		this.collectInput();
		
		if (this.constant == null) {
			this.collectRestriction();
		}
		
		if (this.constant != null) {
			this.input.restrict(this.operator, this.constant);
		} else {
			this.input.restrict(this.operator, this.restriction);
		}
		
		if (this.out.equals(this.input)) {
			this.out = this.input;
			return new LinkedList<Vertex<AD>>();
		} else {
			this.out = this.input;
			return this.successors.get(this.targetRegName);
		}
		
	}
	
	// ========== Helper methods. ==========
	
	private void collectInput() {
		
		List<Vertex<AD>> inputVertices = this.predecessors.get("input");
		
		this.input = new SummaryDomain<AD>(this.sample);
		
		for (Vertex<AD> v : inputVertices) {
			if (v == null) {
				SummaryDomain<AD> zero = new SummaryDomain<AD>(this.sample);
				zero.getElementFromConst(new BigInteger("0"));
				this.input.unionWith(zero);
			} else if (!this.sameScc(v)) {
				this.input.addAD(v.getOut().sumDomSummarize());
			} else {
				this.input.unionWith(v.getOut());
			}
		}
		
	}
	
	private void collectRestriction() {
		
		List<Vertex<AD>> restrictionVertices = this.predecessors.get("restriction");
		
		this.restriction = new SummaryDomain<AD>(this.sample);
		
		for (Vertex<AD> v : restrictionVertices) {
			if (v == null) {
				SummaryDomain<AD> zero = new SummaryDomain<AD>(this.sample);
				zero.getElementFromConst(new BigInteger("0"));
				this.restriction.unionWith(zero);
			} else if (!this.sameScc(v)) {
				this.restriction.addAD(v.getOut().sumDomSummarize());
			} else {
				this.restriction.unionWith(v.getOut());
			}
		}
		
	}
	
	// ========== Object methods. ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Restrict-Vertex\n";
		
		outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
		
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
