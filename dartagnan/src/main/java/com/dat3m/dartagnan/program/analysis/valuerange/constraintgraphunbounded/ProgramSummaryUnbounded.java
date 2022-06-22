package com.dat3m.dartagnan.program.analysis.valuerange.constraintgraphunbounded;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.analysis.AliasAnalysis;
import com.dat3m.dartagnan.program.analysis.reachingdefinitions.ReachingDefinitionsAnalysis;
import com.dat3m.dartagnan.program.analysis.valuerange.events.RestrictionEvent;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Label;
import com.dat3m.dartagnan.program.event.core.IfAsJump;
import com.dat3m.dartagnan.program.event.core.CondJump;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.MemEvent;
import com.dat3m.dartagnan.program.event.core.Store;
import com.dat3m.dartagnan.program.event.core.Init;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;
import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.expression.IExprBin;
import com.dat3m.dartagnan.expression.IExprUn;
import com.dat3m.dartagnan.expression.Atom;
import com.dat3m.dartagnan.expression.BConst;
import com.dat3m.dartagnan.expression.BExprBin;
import com.dat3m.dartagnan.expression.BExprUn;
import com.dat3m.dartagnan.expression.IValue;
import com.dat3m.dartagnan.expression.IfExpr;
import com.dat3m.dartagnan.expression.op.BOpUn;
import com.dat3m.dartagnan.expression.op.COpBin;

public class ProgramSummaryUnbounded {
    
    /*
	
	Program program = null;
	AliasAnalysis aliasAnalysis = null;
	ReachingDefinitionsAnalysis rda = null;
	
	Set<Integer> threadIds = null;
	Map<Integer, List<Event>> threadEventsMap = null;
	Map<Integer, Event> threadEntryMap = null;
	
	Map<Label, Integer> numberJumpToLabel = null;
	Map<MemEvent, List<MemEvent>> loadFrom;

	int minNewCid = -1;
	
	Map<Event, VertexInformation> summary = null;
	
	// ========== Constructor. ==========
	
	public ProgramSummaryUnbounded(Program program, AliasAnalysis aliasAnalysis) {
		
		this.program = program;
		this.aliasAnalysis = aliasAnalysis;
		
		this.printHello();
		this.printOriginalProgram();
		
		this.initializeThreadMaps();
		this.initializeMinNewCid();
		this.initializeNumberJumpToLabel();
		this.insertRestrictionEvents();
		
		this.printModifiedProgram();
		
		this.printPerformRDABegin();
		
		this.initializeSummary();
		
		this.printPerformRDAEnd();
		this.printSummary();
		this.printDone();
		
	}
	
	// ========== Get- and set-methods. ==========
	
	public Map<Event, VertexInformation> getSummary() {
		
		return this.summary;
		
	}
	
	// ========== Building-methods. ==========
	
	private void initializeThreadMaps() {
		
		this.initializeThreadIds();
		this.initializeThreadEventsMap();
		this.initializeThreadEntryMap();
		
	}
	
	// DONE. written
	private void initializeThreadIds() {
		
		this.threadIds = new HashSet<Integer>();
		
		for (com.dat3m.dartagnan.program.Thread thread : this.program.getThreads()) {
			this.threadIds.add(thread.getId());
		}
		
	}
	
	// DONE. written
	private void initializeThreadEventsMap() {
		
		this.threadEventsMap = new HashMap<Integer, List<Event>>();
		
		for (com.dat3m.dartagnan.program.Thread thread : this.program.getThreads()) {
			this.threadEventsMap.put(thread.getId(), new LinkedList<Event>(thread.getEvents()));
		}
		
	}
	
	// DONE. written
	private void initializeThreadEntryMap() {
		
		this.threadEntryMap = new HashMap<Integer, Event>();
		
		for(com.dat3m.dartagnan.program.Thread thread : this.program.getThreads()) {
			this.threadEntryMap.put(thread.getId(), thread.getEntry());
		}
		
	}
	
	// DONE. written
	private void initializeMinNewCid() {
		
		this.minNewCid = -1;
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				if (this.minNewCid <= event.getCId()) {
					this.minNewCid = event.getCId() + 1;
				}
			}
		}
		
	}
	
	// DONE. written
	private void initializeNumberJumpToLabel() {
		
		this.numberJumpToLabel = new HashMap<Label, Integer>();
		
		for (int threadId : this.threadIds) {
			
			List<Event> eventList = this.threadEventsMap.get(threadId);
			
			for (Event event : eventList) {
				if (event instanceof Label) {
					this.numberJumpToLabel.put((Label) event, 0); 
				}	
			}
			
			for (Event event : eventList) {
				if (event instanceof CondJump) {
					CondJump jump = (CondJump) event;
					Label jumpLabel = jump.getLabel();
					int currentNumJumps = this.numberJumpToLabel.get(jumpLabel);
					this.numberJumpToLabel.put(jumpLabel, currentNumJumps + 1); 
				} else if (event instanceof IfAsJump) {
					IfAsJump jump = (IfAsJump) event;
					Label jumpLabel = jump.getLabel();
					int currentNumJumps = this.numberJumpToLabel.get(jumpLabel);
					this.numberJumpToLabel.put(jumpLabel, currentNumJumps + 1); 
				}
			}
			
		}
		
	}
	
	private void insertRestrictionEvents() {
		
		Set<Event> jumps = new HashSet<Event>();
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				if (event instanceof CondJump || event instanceof IfAsJump) {
					jumps.add(event);
				}
			}
		}
		
		for (Event jump : jumps) {
			this.insertRestrictionEventsForJump(jump);
		}
		
	}
	
	private void insertRestrictionEventsForJump(Event jump) {
		
		ExprInterface guard = null;
		Label label = null;
		
		if (jump instanceof IfAsJump) {
			guard = ((IfAsJump) jump).getGuard();
			label = ((IfAsJump) jump).getLabel();
		} else if (jump instanceof CondJump) {
			guard = ((CondJump) jump).getGuard();
			label = ((CondJump) jump).getLabel();
		} else {
			return;
		}
		
		ExprInterface lhs = null;
		COpBin cmpOp = null;
		ExprInterface rhs = null;
		
		if (this.matchPatternNotAtom(guard)) {
			Atom atom = (Atom) ((BExprUn) guard).getInner();
			if (this.matchPatternRegCompReg(atom) || this.matchPatternRegCompConst(atom)) {
				lhs = ((Atom) atom).getLHS();
				cmpOp = this.negate(((Atom) atom).getOp());
				rhs = ((Atom) atom).getRHS();
			} else if (this.matchPatternConstCompReg(atom)) {
				lhs = ((Atom) atom).getRHS();
				cmpOp = this.negate(this.turnAround(((Atom) atom).getOp()));
				rhs = ((Atom) atom).getLHS();
			}
		} else {
			if (this.matchPatternRegCompReg(guard) || this.matchPatternRegCompConst(guard)) {
				lhs = ((Atom) guard).getLHS();
				cmpOp = ((Atom) guard).getOp();
				rhs = ((Atom) guard).getRHS();
			} else if (this.matchPatternConstCompReg(guard)) {
				lhs = ((Atom) guard).getRHS();
				cmpOp = this.turnAround(((Atom) guard).getOp());
				rhs = ((Atom) guard).getLHS();
			}
		}
		
		if (cmpOp == null) {
			return;
		}
		
		com.dat3m.dartagnan.program.Thread thread = jump.getThread();
		
		// No-branch.
		
		if (rhs instanceof IValue) {
			RestrictionEvent restriction = new RestrictionEvent(this.minNewCid++, thread, (Register) lhs, ((IValue) rhs).getValue(), cmpOp, jump);
			this.insertAfterEvent(jump, restriction);
		} else {
			RestrictionEvent restriction1 = new RestrictionEvent(this.minNewCid++, thread, (Register) lhs, (Register) rhs, cmpOp, jump);
			RestrictionEvent restriction2 = new RestrictionEvent(this.minNewCid++, thread, (Register) rhs, (Register) lhs, this.turnAround(cmpOp), jump);
			this.insertAfterEvent(jump, restriction2);
			this.insertAfterEvent(jump, restriction1);
		}
		
		// Yes-branch.
		
		if (this.numberJumpToLabel.get(label) != 1) {
			return;
		}
		
		Label newLabel = new Label("NEWLABEL" + this.minNewCid);
		newLabel.setThread(thread);
		newLabel.setCId(this.minNewCid++);
		this.insertAfterEvent(label, newLabel);
		
		CondJump newJump = new CondJump(BConst.TRUE, newLabel);
		newJump.setThread(thread);
		newJump.setCId(this.minNewCid++);
		this.insertBeforeEvent(label, newJump);
		
		if (rhs instanceof IValue) {
			RestrictionEvent restriction = new RestrictionEvent(this.minNewCid++, thread, (Register) lhs, ((IValue) rhs).getValue(), this.negate(cmpOp), jump);
			this.insertAfterEvent(label, restriction);
		} else {
			RestrictionEvent restriction1 = new RestrictionEvent(this.minNewCid++, thread, (Register) lhs, (Register) rhs, this.negate(cmpOp), jump);
			RestrictionEvent restriction2 = new RestrictionEvent(this.minNewCid++, thread, (Register) rhs, (Register) lhs, this.negate(this.turnAround(cmpOp)), jump);
			this.insertAfterEvent(label, restriction2);
			this.insertAfterEvent(label, restriction1);
		}
		
	}
	
	private boolean matchPatternNotAtom(ExprInterface expr) {
		
		if (!(expr instanceof BExprUn)) {
			return false;
		}
		
		BExprUn not = (BExprUn) expr;
		
		if (!not.getOp().equals(BOpUn.NOT)) {
			return false;
		}
		
		if (!(not.getInner() instanceof Atom)) {
			return false;
		}
		
		return true;
		
	}
	
	private boolean matchPatternRegCompReg(ExprInterface expr) {
		
		if (!(expr instanceof Atom)) {
			return false;
		}
		
		Atom atom = (Atom) expr;
		
		ExprInterface lhs = atom.getLHS();
		if (!(lhs instanceof Register)) {
			return false;
		}
		
		ExprInterface rhs = atom.getRHS();
		if (!(rhs instanceof Register)) {
			return false;
		}
		
		return true;
		
	}
	
	private boolean matchPatternRegCompConst(ExprInterface expr) {
		
		if (!(expr instanceof Atom)) {
			return false;
		}
		
		Atom atom = (Atom) expr;
		
		ExprInterface lhs = atom.getLHS();
		if (!(lhs instanceof Register)) {
			return false;
		}
		
		ExprInterface rhs = atom.getRHS();
		if (!(rhs instanceof IValue)) {
			return false;
		}
		
		return true;
		
	}
	
	private boolean matchPatternConstCompReg(ExprInterface expr) {
		
		if (!(expr instanceof Atom)) {
			return false;
		}
		
		Atom atom = (Atom) expr;
		
		ExprInterface lhs = atom.getLHS();
		if (!(lhs instanceof IValue)) {
			return false;
		}
		
		ExprInterface rhs = atom.getRHS();
		if (!(rhs instanceof Register)) {
			return false;
		}
		
		return true;
		
	}
	
	private COpBin negate(COpBin operator) {
		
		switch (operator) {
			case EQ:
				return COpBin.NEQ;
			case NEQ:
				return COpBin.EQ;
			case GTE:
				return COpBin.LT;
			case LTE:
				return COpBin.GT;
			case GT:
				return COpBin.LTE;
			case LT:
				return COpBin.GTE;
			case UGTE:
				return COpBin.ULT;
			case ULTE:
				return COpBin.UGT;
			case UGT:
				return COpBin.ULTE;
			case ULT:
				return COpBin.UGTE;
			default:
				return null;
		}
		
	}
	
	private COpBin turnAround(COpBin operator) {
		
		switch (operator) {
			case EQ:
				return COpBin.EQ;
			case NEQ:
				return COpBin.NEQ;
			case GTE:
				return COpBin.LTE;
			case LTE:
				return COpBin.GTE;
			case GT:
				return COpBin.LT;
			case LT:
				return COpBin.GT;
			case UGTE:
				return COpBin.ULTE;
			case ULTE:
				return COpBin.UGTE;
			case UGT:
				return COpBin.ULT;
			case ULT:
				return COpBin.UGT;
			default:
				return null;
		}
		
	}
	
	private int getIndexOfEvent(Event event) {
		
		int threadId = event.getThread().getId();
		List<Event> threadEventList = this.threadEventsMap.get(threadId);
		
		for (int i = 0; i < threadEventList.size(); i++) {
			if (threadEventList.get(i).equals(event)) {
				return i;	
			}
		}
		
		return -1;
		
	}
	
	private void insertBeforeEvent(Event anchor, Event toInsert) {
		
		int anchorPosition = this.getIndexOfEvent(anchor);
		
		int threadId = anchor.getThread().getId();
		List<Event> threadEventList = this.threadEventsMap.get(threadId);
		
		threadEventList.add(anchorPosition, toInsert);
		
	}
	
	private void insertAfterEvent(Event anchor, Event toInsert) {
		
		int anchorPosition = this.getIndexOfEvent(anchor);
		
		int threadId = anchor.getThread().getId();
		List<Event> threadEventList = this.threadEventsMap.get(threadId);
		
		threadEventList.add(anchorPosition + 1, toInsert);
		
	}
	
	private void initializeSummary() {
		
		this.runReachingDefinitions();
		this.initializeLoadFrom();
		
		this.summary = new HashMap<Event, VertexInformation>();
		
		this.createVertexInformation();
		this.addPreAndSuc();
		
	}
	
	private void runReachingDefinitions() {
		
		this.rda = ReachingDefinitionsAnalysis.getEmptyAnalysis();
		
		for (int threadId : this.threadIds) {
			
			List<Event> commandList = this.threadEventsMap.get(threadId);
			Event entry = this.threadEntryMap.get(threadId);
			
			this.rda.appendThread(commandList, entry);
			
		}
		
	}
	
	private void initializeLoadFrom() {
		
		this.loadFrom = new HashMap<MemEvent, List<MemEvent>>();
		
		Set<MemEvent> loadSet = this.getLoads();
		Set<MemEvent> storeSet = this.getStores();
		
		for (MemEvent load : loadSet) {
			this.loadFrom.put(load, new LinkedList<MemEvent>());
			for (MemEvent store : storeSet) {
				if (this.aliasAnalysis.mayAlias(load, store)) {
					this.loadFrom.get(load).add(store);
				}
			}
		}
		
	}
	
	private Set<MemEvent> getLoads() {
		
		Set<MemEvent> loadSet = new HashSet<MemEvent>();
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				if (event instanceof Load) {
					loadSet.add((MemEvent) event);
				}
			}
		}
		
		return loadSet;
		
	}
	
	private Set<MemEvent> getStores() {
		
		Set<MemEvent> storeSet = new HashSet<MemEvent>();
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				if (event instanceof Store || event instanceof Init) {
					storeSet.add((MemEvent) event);
				}
			}
		}
		
		return storeSet;
		
	}
	
	private void createVertexInformation() {
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				
				if (event instanceof Local) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
				if (event instanceof RestrictionEvent) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
				if (event instanceof Load) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
				if (event instanceof Store) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
				if (event instanceof Init) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
				if (event instanceof RegWriter) {
					this.summary.put(event, new VertexInformation(event));
					continue;
				}
				
			}
		}
		
	}
	
	private void addPreAndSuc() {
		
		for (int threadId : this.threadIds) {
			for (Event event : this.threadEventsMap.get(threadId)) {
				
				if (event instanceof Local) {
					this.addPreToLocal((Local) event);
					continue;
				}
				
				if (event instanceof RestrictionEvent) {
					this.addPreToRestriction((RestrictionEvent) event);
					continue;
				}
				
				if (event instanceof Load) {
					this.addPreToLoad((Load) event);
					continue;
				}
				
				if (event instanceof Store) {
					this.addPreToStore((Store) event);
					continue;
				}
				
				if (event instanceof Init) {
					this.addPreToInit((Init) event);
					continue;
				}
				
				if (event instanceof RegWriter) {
					this.addPreToRegWriter((RegWriter) event);
					continue;
				}
				
			}
		}
		
	}
	
	private void addPreToLocal(Local local) {
		
		Set<String> regNames = this.extractRegNames(local.getExpr());

		VertexInformation localInfo = this.summary.get(local);
		
		for (String regName : regNames) {
			Set<Event> possibleSources = this.rda.reachedDefinitions(local, regName);
			if (possibleSources == null) {
				continue;
			}
			for (Event source : possibleSources) {
				localInfo.addPredecessor(regName, source);
				if (source != null) {
					VertexInformation sourceInfo = this.summary.get(source);
					sourceInfo.addSuccessor(regName, local);
				}
			}
		}
		
	}
	
	private void addPreToRestriction(RestrictionEvent restriction) {
		
		VertexInformation restrictionInfo = this.summary.get(restriction);
		
		String inputRegName = restriction.getResultRegister().getName();
		String compareRegName = null;
		if (restriction.getCompareRegister() != null) {
			compareRegName = restriction.getCompareRegister().getName();
		}
		
		Event jump = restriction.getJump();
		
		Set<Event> inputEvents = this.rda.reachedDefinitions(jump, inputRegName);
		Set<Event> compareEvents = null;
		if (compareRegName != null) {
			compareEvents = this.rda.reachedDefinitions(jump, compareRegName);
		}
		
		if (inputEvents != null) {
			for (Event input : inputEvents) {
				restrictionInfo.addPredecessor("input", input);
				if (input != null) {
					VertexInformation inputInfo = this.summary.get(input);
					inputInfo.addSuccessor(inputRegName, restriction);
				}
			}
		}
		
		if (compareEvents != null) {
			for (Event compare : compareEvents) {
				restrictionInfo.addPredecessor("restriction", compare);
				if (compare != null) {
					VertexInformation compareInfo = this.summary.get(compare);
					compareInfo.addSuccessor(compareRegName, restriction);
				}
			}
		}
		
	}
	
	private void addPreToLoad(Load load) {
		
		VertexInformation loadInfo = this.summary.get(load);
		
		for (MemEvent mem : this.loadFrom.get(load)) {
			loadInfo.addPredecessor("load", mem);
			VertexInformation storeInfo = this.summary.get(mem);
			storeInfo.addSuccessor("store", load);
		}
		
	}
	
	private void addPreToStore(Store store) {
		
		Set<String> regNames = this.extractRegNames(store.getMemValue());

		VertexInformation storeInfo = this.summary.get(store);
		
		for (String regName : regNames) {
			Set<Event> possibleSources = this.rda.reachedDefinitions(store, regName);
			if (possibleSources == null) {
				continue;
			}
			for (Event source : possibleSources) {
				storeInfo.addPredecessor(regName, source);
				if (source != null) {
					VertexInformation sourceInfo = this.summary.get(source);
					sourceInfo.addSuccessor(regName, store);
				}
			}
		}
		
	}
	
	private void addPreToInit(Init init) {
		
		// Events of type Init do not have any input.
		
	}
	
	private void addPreToRegWriter(RegWriter regWriter) {
		
		// RegWriters of an unknown type do not have any input.
		
	}
	
	private Set<String> extractRegNames(ExprInterface expr) {
		
		Set<String> regNames = new HashSet<String>();
		
		this.extractRegNames(regNames, expr);
		
		return regNames;
		
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
	
	// ========== Data-structures. ==========
	
	public class VertexInformation {
		
		Event event = null;
		
		Map<String, List<Event>> predecessors = null;
		Map<String, List<Event>> successors = null;
		
		public VertexInformation(Event event) {
			
			this.event = event;
			
			this.predecessors = new HashMap<String, List<Event>>();
			this.successors = new HashMap<String, List<Event>>();
			
		}
		
		public Map<String, List<Event>> getPredecessors() {
			
			return this.predecessors;
			
		}
		
		public Map<String, List<Event>> getSuccessors() {
			
			return this.successors;
			
		}
		
		public void addPredecessor(String label, Event target) {
			
			if (!this.predecessors.containsKey(label)) {
				this.predecessors.put(label, new LinkedList<Event>());
			}
			
			this.predecessors.get(label).add(target);
			
		}
		
		public void addSuccessor(String label, Event target) {
			
			if (!this.successors.containsKey(label)) {
				this.successors.put(label, new LinkedList<Event>());
			}
			
			this.successors.get(label).add(target);
			
		}
		
		@Override
		public String toString() {
			
			String outString = "";
			
			outString += "Event:\n";
			outString += this.event.getCId() + ": " + this.event.toString() + "\n";
			
			outString += "Predecessors:\n";
			if (this.predecessors.isEmpty()) {
				outString += "EMPTY\n";
			} else {
				for (String regName : this.predecessors.keySet()) {
					outString += "Register " + regName + ": ";
					List<Event> predecessorList = this.predecessors.get(regName);
					for (int i = 0; i < predecessorList.size(); i++) {
						Event currentEvent = predecessorList.get(i);
						if (currentEvent == null) {
							outString += "NULL";
						} else {
							outString += "[" + currentEvent.getCId() + "; " + currentEvent.toString() + "]";
						}
						if (i < predecessorList.size() - 1) {
							outString += ",";
						}
					}
					outString += "\n";
				}
			}
			
			outString += "Successors:\n";
			if (this.successors.isEmpty()) {
				outString += "EMPTY";
			} else {
				for (String regName : this.successors.keySet()) {
					outString += "Register " + regName + ": ";
					List<Event> successorList = this.successors.get(regName);
					for (int i = 0; i < successorList.size(); i++) {
						Event currentEvent = successorList.get(i);
						if (currentEvent == null) {
							outString += "[NULL]";
						} else {
							outString += "[" + currentEvent.getCId() + "; " + currentEvent.toString() + "]";
						}
						if (i < successorList.size() - 1) {
							outString += ",";
						}
					}
				}
			}
			
			return outString;
			
		}
		
	}
	
	// ========== Print-commands (Helpful for debugging). ==========
	
	private void printHello() {
		
		System.out.println("==================== Building program summary for VRA. ====================");
		
	}
	
	private void printOriginalProgram() {
		
		System.out.println("==================== Printing original program. ====================");
		
		List<com.dat3m.dartagnan.program.Thread> threads = this.program.getThreads();
		
		for (com.dat3m.dartagnan.program.Thread thread : threads) {
			System.out.println("========== Printing thread: " + thread.getId() + ". ==========");
			List<Event> events = thread.getEvents();
			for (Event e : events) {
				System.out.println(e.getCId() + " " + e.getClass().getSimpleName() + ": " + e.toString());
			}
		}
		
	}
	
	private void printModifiedProgram() {
		
		System.out.println("==================== Printing modified program. ====================");
		
		for (int threadId : this.threadIds) {
			System.out.println("========== Printing thread: " + threadId + ". ==========");
			List<Event> events = this.threadEventsMap.get(threadId);
			for (Event e : events) {
				System.out.println(e.getCId() + " " + e.getClass().getSimpleName() + ": " + e.toString());
			}
		}
		
	}
	
	private void printPerformRDABegin() {
		
		System.out.println("==================== Performing RDA. Begin... ====================\n\n");
		
	}
	
	private void printPerformRDAEnd() {
		
		System.out.println("==================== Performing RDA. End. ====================");
		
	}
	
	private void printSummary() {
		
		System.out.println("==================== Printing VRA summary. ====================");
		
		List<Event> eventList = new LinkedList<Event>(this.summary.keySet());
		for (int i = 0; i < eventList.size(); i++) {
			Event currentEvent = eventList.get(i);
			VertexInformation eventInfo = this.summary.get(currentEvent);
			System.out.println(eventInfo.toString());
			if (i < eventList.size() - 1) {
				System.out.println("==========");
			}
		}
		
	}
	
	private void printDone() {
		
		System.out.println("==================== Done building VRA-summary. ====================\n\n");
		
	}
	
	*/

}
