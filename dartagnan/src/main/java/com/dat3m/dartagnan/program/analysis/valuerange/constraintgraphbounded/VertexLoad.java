package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphbounded;

import java.util.LinkedList;
import java.util.List;

import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SimpleDomain;
import com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain.SummaryDomain;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

public class VertexLoad<AD extends SimpleDomain> extends Vertex<AD> {
	
	SummaryDomain<AD> in = null;
	
	List<Integer> toFilter = null;
	
	String outPutReg = null;
	
	// ========== Constructor. ==========

	public VertexLoad(Event event, AD sample) {
		
		super(event, sample);
		
		this.toFilter = new LinkedList<Integer>();
		this.toFilter.add(this.event.getCId());
		
		this.outPutReg = ((RegWriter) this.event).getResultRegister().getName();
		
		this.successors.put(this.outPutReg, new LinkedList<Vertex<AD>>());
		
	}
	
	// ========== Methods from Vertex. ==========
	
	@Override
	public void initialize() {
		
		this.out = new SummaryDomain<AD>(sample);
		
	}
	
	@Override
	public List<Vertex<AD>> updateValue() {

		this.collectIn();
		
		for (int filter : this.toFilter) {
			this.in.sumDomFilterByIndex(filter);
		}
		
		this.in.sumDomAddIndex(this.event.getCId());
		
		if (this.out.equals(this.in)) {
			this.out = this.in;
			return new LinkedList<Vertex<AD>>();
		} else {
			this.out = this.in;
			return this.successors.get(this.outPutReg);
		}
		
	}

	
	// ========== Helper methods. ===========
	
	private void collectIn() {
		
		List<Vertex<AD>> inputVertices = this.predecessors.get("load");
		
		this.in = new SummaryDomain<AD>(this.sample);
		
		for (Vertex<AD> v : inputVertices) {
			if (!this.sameScc(v)) {
				this.in.addAD(v.getOut().sumDomSummarize());
			} else {
				this.in.unionWith(v.getOut());
			}
		}
		
	}
	
	// ========== Object methods. ===========
	
	@Override public String toString() {
		
		String outString = "";
		
		outString += "Type: Load-Vertex\n";
		
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
