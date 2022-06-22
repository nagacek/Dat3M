package com.dat3m.dartagnan.program.analysis.valuerange.events;

import java.math.BigInteger;

import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;

/**
 * This Event is inserted into the code as part of the value-range-analysis and removed from the
 * code once the analysis is over.
 * @author Johannes Schmechel, j.schmechel@tu-bs.de
 */
public class RestrictionEvent extends Event implements RegWriter {
	
	protected Register resultRegister = null;
	
	protected Register compareRegister = null;
	protected BigInteger compareValue = null;
	protected COpBin operator = null;
	protected Event jump = null;
	
	// ========== Constructor. ==========
	
	public RestrictionEvent(int cId, com.dat3m.dartagnan.program.Thread thread, Register resultRegister, Register compareRegister, COpBin operator, Event jump) {
		
		this.cId = cId;
		this.thread = thread;
		this.resultRegister = resultRegister;
		this.compareRegister = compareRegister;
		this.operator = operator;
		this.jump = jump;
		
	}
	
	public RestrictionEvent(int cId, com.dat3m.dartagnan.program.Thread thread, Register resultRegister, BigInteger compareValue, COpBin operator, Event jump) {
			
		this.cId = cId;
		this.thread = thread;
		this.resultRegister = resultRegister;
		this.compareValue = compareValue;
		this.operator = operator;
		this.jump = jump;
			
	}

	// ========== Get- and set-methods. ==========
	
	@Override
	public Register getResultRegister() {
		
		return this.resultRegister;
		
	}
	
	public Register getCompareRegister() {
		
		return this.compareRegister;
		
	}
	
	public BigInteger getCompareValue() {
		
		return this.compareValue;
		
	}
	
	public COpBin getOperator() {
		
		return this.operator;
		
	}
	
	public Event getJump() {
		
		return this.jump;
		
	}
	
	@Override
	public String toString() {
		String string = "Restricting " + this.resultRegister.getName() + " to values " + this.operator + " ";
		if (compareRegister != null) {
			string += this.compareRegister.getName() + ".";
		} else {
			string += this.compareValue + ".";
		}
		return string;
	}

}
