package com.dat3m.dartagnan.program.analysis.reachingdefinitions.constraintgraph;

import com.dat3m.dartagnan.expression.*;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.*;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

import java.util.*;

public class RDAThreadSummary {
	
	List<Event> commandList = null;
	Event entry = null;
	
	Set<String> registers = null;
	
	Map<Event, VertexInformation> summary = null;
	
	public RDAThreadSummary(List<Event> commandList, Event entry) {
		
		this.commandList = commandList;
		this.entry = entry;
		
		this.build();
		
	}
	
	// ========== Get methods. ==========
	
	public Map<Event, VertexInformation> getSummary() {
		
		return this.summary;
		
	}
	
	public boolean analysisPossible() {
		
		boolean registersEmpty = this.registers == null || this.registers.size() == 0;
		boolean commandsEmpty = this.commandList == null || this.commandList.size() == 0;
		
		return !registersEmpty && !commandsEmpty;
		
	}
	
	// ========== Build methods. ==========
	
	private void build() {
		
		//this.printIntroduction();
		
		this.initializeRegisters();
		
		if (this.registers == null || this.registers.isEmpty()) {
			
			//this.printStop();
			
			return;
		}
		
		//this.printRegisters();
		
		this.initializeSummary();
		
		//this.printVertices();
		
		//this.printDone();
		
	}
	
	private void initializeRegisters() {
		
		this.registers = new HashSet<String>();
		
		for (Event event : this.commandList) {
			
			if (event instanceof RegWriter) {
				
				this.registers.add(((RegWriter) event).getResultRegister().getName());
				
				if (event instanceof Local) {
					Local local = (Local) event;
					this.extractRegNames(this.registers, local.getExpr());
					continue;
				}
				
				if (event instanceof RestrictionEvent) {
					RestrictionEvent restriction = (RestrictionEvent) event;
					if (restriction.getCompareRegister() != null) {
						this.registers.add(restriction.getCompareRegister().getName());
					}
					continue;
				}
				
			}
			
			if (event instanceof Store) {
				Store store = (Store) event;
				this.extractRegNames(this.registers, store.getMemValue());
				continue;
			}
			
		}
		
	}
		
	private void extractRegNames(Set<String> regNames, ExprInterface expr) {
		
		if (expr instanceof Register) {
			regNames.add(((Register) expr).getName());
			return;
		}
		
		if (expr instanceof Atom) {
			Atom atom = (Atom) expr;
			this.extractRegNames(regNames, atom.getLHS());
			this.extractRegNames(regNames, atom.getRHS());
			return;
		}
		
		if (expr instanceof BExprBin) {
			BExprBin bExpr = (BExprBin) expr;
			this.extractRegNames(regNames, bExpr.getLHS());
			this.extractRegNames(regNames, bExpr.getRHS());
			return;
		}
		
		if (expr instanceof BExprUn) {
			BExprUn bExpr = (BExprUn) expr;
			this.extractRegNames(regNames, bExpr.getInner());
			return;
		}
		
		if (expr instanceof IExprBin) {
			IExprBin iExpr = (IExprBin) expr;
			this.extractRegNames(regNames, iExpr.getLHS());
			this.extractRegNames(regNames, iExpr.getRHS());
			return;
		}
		
		if (expr instanceof IExprUn) {
			IExprUn iExpr = (IExprUn) expr;
			this.extractRegNames(regNames, iExpr.getInner());
			return;
		}
		
		if (expr instanceof IfExpr) {
			IfExpr ifExpr = (IfExpr) expr;
			this.extractRegNames(regNames, ifExpr.getGuard());
			this.extractRegNames(regNames, ifExpr.getTrueBranch());
			this.extractRegNames(regNames, ifExpr.getFalseBranch());
			return;
		}
		
	}
	
	private void initializeSummary() {
		
		this.summary = new HashMap<Event, VertexInformation>();
		
		for (Event event : this.commandList) {
			this.summary.put(event, new VertexInformation(event));
		}
		
		this.addEntryInformation();
		this.addPreAndSucc();
		
	}
	
	private void addEntryInformation() {
		
		this.summary.get(this.entry).setIsEntry(true);
		this.summary.get(this.entry).setRegNames(this.registers);
		
	}
	
	private void addPreAndSucc() {
		
		for (int i = 0; i < this.commandList.size() - 1; i++) {
			
			Event event = this.commandList.get(i);
			Event nextEvent = this.commandList.get(i + 1);
			
			if (event instanceof CondJump) {
				CondJump jump = (CondJump) event;
				Label jumpLabel = jump.getLabel();
				this.summary.get(jump).addSuccessor(jumpLabel);
				this.summary.get(jumpLabel).addPredecessor(jump);
				if (!jump.isGoto()) {
					this.summary.get(jump).addSuccessor(nextEvent);
					this.summary.get(nextEvent).addPredecessor(jump);
				}
				continue;
			}
			
			if (event instanceof IfAsJump) {
				IfAsJump jump = (IfAsJump) event;
				Label jumpLabel = jump.getLabel();
				this.summary.get(jump).addSuccessor(jumpLabel);
				this.summary.get(jumpLabel).addPredecessor(jump);
				if (!jump.isGoto()) {
					this.summary.get(jump).addSuccessor(nextEvent);
					this.summary.get(nextEvent).addPredecessor(jump);
				}
				continue;
			}
			
			this.summary.get(event).addSuccessor(nextEvent);
			this.summary.get(nextEvent).addPredecessor(event);
			
		}
		
		Event event = this.commandList.get(this.commandList.size() - 1);
		
		if (event instanceof CondJump) {
			CondJump jump = (CondJump) event;
			Label jumpLabel = jump.getLabel();
			this.summary.get(jump).addSuccessor(jumpLabel);
			this.summary.get(jumpLabel).addPredecessor(jump);
			return;
		}
		
		if (event instanceof IfAsJump) {
			IfAsJump jump = (IfAsJump) event;
			Label jumpLabel = jump.getLabel();
			this.summary.get(jump).addSuccessor(jumpLabel);
			this.summary.get(jumpLabel).addPredecessor(jump);
			return;
		}
		
	}
	
	// ========== Data-structures. ==========
	
	public class VertexInformation {
		
		Event event = null;
		
		List<Event> predecessors = null;
		List<Event> successors = null;
		
		boolean isEntry = false;
		Set<String> regNames = null;
		
		public VertexInformation(Event event) {
			
			this.event = event;
			
			this.predecessors = new LinkedList<Event>();
			this.successors = new LinkedList<Event>();
			
		}
		
		public List<Event> getPredecessors() {
			
			return this.predecessors;
			
		}
		
		public List<Event> getSuccessors() {
			
			return this.successors;
			
		}
		
		public boolean getIsEntry() {
			
			return this.isEntry;
			
		}
		
		public Set<String> getRegNames() {
			
			return this.regNames;
			
		}
		
		public void addPredecessor(Event e) {
			
			this.predecessors.add(e);
			
		}
		
		public void addSuccessor(Event e) {
			
			this.successors.add(e);
			
		}
		
		public void setIsEntry(boolean isEntry) {
			
			this.isEntry = isEntry;
			
		}
		
		public void setRegNames(Set<String> regNames) {
			
			this.regNames = regNames;
			
		}
		
		@Override
		public String toString() {
			
			String outString = "";
			
			outString += "Event: " + this.event.getCId() + "; " + this.event.toString() + "\n";
			
			outString += "Predecessors: ";
			if (this.predecessors.isEmpty()) {
				outString += "EMPTY";
			} else {
				for (int i = 0; i < this.predecessors.size(); i++) {
					outString += "[" + this.predecessors.get(i).getCId() + "]";
					if (i < this.predecessors.size() - 1) {
						outString += ",";
					}
				}
			}
			outString += "\n";
			
			outString += "Successors: ";
			if (this.successors.isEmpty()) {
				outString += "EMPTY";
			} else {
				for (int i = 0; i < this.successors.size(); i++) {
					outString += "[" + this.successors.get(i).getCId() + "]";
					if (i < this.successors.size() - 1) {
						outString += ",";
					}
				}
			}
			outString += "\n";
			
			outString += "Entry: " + (this.isEntry ? "Yes" : "No") + "\n";
			
			outString += "Regnames: ";
			if (this.regNames == null) {
				outString += "NULL";
			} else if (this.regNames.isEmpty()) {
				outString += "EMPTY";
			} else {
				List<String> regNamesList = new LinkedList<String>(this.regNames); 
				for (int i = 0; i < regNamesList.size(); i++) {
					outString += regNamesList.get(i);
					if (i < regNamesList.size() - 1) {
						outString += ",";
					}
				}
			}
			
			return outString;
			
		} 
		
	}
	
	// ========= Print-methods. ==========
	
	private void printIntroduction() {
		
		System.out.println("==================== Building thread summary for RDA. ====================");
		
		System.out.println("========== Commands. ==========");
		
		for (Event e : this.commandList) {
			System.out.println(e.getCId() + ": " + e.toString());
		}
		
		System.out.println("========== Entry. ==========");

		System.out.println(this.entry.getCId() + ": " + this.entry.toString());
		
	}
	
	private void printStop() {
		
		System.out.println("==================== No registers. No RDA for this thread. ====================\n\n");
		
	}
	
	private void printRegisters() {
		
		System.out.println("========== Registers. ==========");
		
		for (String regName : this.registers) {
			System.out.println(regName);
		}
		
	}
	
	private void printVertices() {
	
		System.out.println("========== Vertices. ==========");
		
		List<Event> eventList = new LinkedList<Event>(this.summary.keySet());
		for (int i = 0; i < eventList.size(); i++) {
			Event e = eventList.get(i);
			System.out.println("Vertix for Event: " + e.getCId());
			System.out.println(this.summary.get(e).toString());
			if (i < eventList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printDone() {
		
		System.out.println("==================== Done building RDA thread summary. ====================\n\n");
		
	}
	
}
