package com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dat3m.dartagnan.expression.IfExpr;
import com.dat3m.dartagnan.expression.op.BOpBin;
import com.dat3m.dartagnan.expression.op.BOpUn;
import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.expression.op.IOpBin;
import com.dat3m.dartagnan.expression.op.IOpUn;

public class SummaryDomain<AD extends SimpleDomain> extends AbstractDomain {
	
	protected AD sample = null;
	protected Map<Set<Integer>, AD> summary;
	boolean totalSummary = true;
	
	// ========== Constructors. ==========
	
	public SummaryDomain(AD sample) {
		
		this.sample = sample;
		this.summary = new HashMap<Set<Integer>, AD>();
		
	}
	
	public SummaryDomain(SummaryDomain<AD> other) {
		
		this.sample = other.getSample();
		
		this.summary = new HashMap<Set<Integer>, AD>();
		for (Set<Integer> key : other.getSummary().keySet()) {
			Set<Integer> newKey = new HashSet<Integer>(key);
			@SuppressWarnings("unchecked") // Different types of SimpleDomain cannot be combined.
			AD newAD = (AD) other.getSummary().get(key).getCopy();
			this.summary.put(newKey, newAD);
		}
		
	}
	
	// ========== Get- and set-methods. ==========
	
	public AD getSample() {
		
		return this.sample;
		
	}
	
	public Map<Set<Integer>, AD> getSummary() {
		
		return this.summary;
		
	}
	
	// ========== SumDom-methods. ==========+
	
	private void sumDomAdd(Set<Integer> key, AD value) {
	    
	    if (this.totalSummary) {
	        
	        if(!this.summary.containsKey(new HashSet<Integer>())) {
	            this.summary.put(new HashSet<Integer>(), value);
	        }
	        
	        this.summary.get(new HashSet<Integer>()).unionWith(value);
	        
	    } else {
	        
	        if(!this.summary.containsKey(key)) {
                this.summary.put(key, value);
            }
	        this.summary.get(key).unionWith(value);
	        
	    }
	    
	}
	
	public void sumDomAddIndex(int index) {
	    
	    if (!this.totalSummary) {
		
    		List<Set<Integer>> keyList = new LinkedList<Set<Integer>>(this.summary.keySet());
    		
    		for (Set<Integer> key : keyList) {
    			AD oldElement = this.summary.get(key);
    			this.summary.remove(key);
    			key.add(index);
    			this.summary.put(key, oldElement);
    		}
		
	    }
		
	}
	
	public void sumDomFilterByIndex(int index) {
	    
	    if (!this.totalSummary) {
		
    		List<Set<Integer>> toRemoveList = new LinkedList<Set<Integer>>();
    		
    		for (Set<Integer> key : this.summary.keySet()) {
    			if (key.contains(index)) {
    				toRemoveList.add(key);
    			}
    		}
    		
    		for (Set<Integer> toRemove : toRemoveList) {
    			this.summary.remove(toRemove);
    		}
		
	    }
		
	}
	
	public void addAD(AD toAdd) {
		
		Set<Integer> empty = new HashSet<Integer>();
		
		if (!this.summary.containsKey(empty)) {
			this.summary.put(empty, toAdd);
		} else {
			this.summary.get(empty).unionWith(toAdd);
		}
		
	}
	
	protected void sumDomApplyBOpBin(BOpBin op, SummaryDomain<AD> rhs) {
		
		Map<Set<Integer>, AD> lhsSummary = this.summary;
		Map<Set<Integer>, AD> rhsSummary = rhs.getSummary();
	
		this.summary = new HashMap<Set<Integer>, AD>();
		
		for (Set<Integer> lhsKey : lhsSummary.keySet()) {
			for (Set<Integer> rhsKey : rhsSummary.keySet()) {
				// Determine the new key ...
				Set<Integer> newKey = new HashSet<Integer>();
				newKey.addAll(lhsKey);
				newKey.addAll(rhsKey);
				// Determine the new value ...
				@SuppressWarnings("unchecked")
				AD newValue = (AD) lhsSummary.get(lhsKey).getCopy();
				newValue.evaluateBOpBin(op, rhsSummary.get(rhsKey));
				// Check if empty ...
				if (newValue.isEmpty()) {
					continue;
				}
				// Add to summary ...
				this.sumDomAdd(newKey, newValue);
			}
		}
		
	}
	
	protected void sumDomApplyBOpUn(BOpUn op) {
		
		List<Set<Integer>> toRemoveList = new LinkedList<Set<Integer>>();
		
		for (Set<Integer> key : this.summary.keySet()) {
			AD currentElement = this.summary.get(key);
			currentElement.evaluateBOpUn(op);
			if (currentElement.isEmpty()) {
				toRemoveList.add(key);
			}
		}
		
		for (Set<Integer> toRemove : toRemoveList) {
			this.summary.remove(toRemove);
		}
		
	}
	
	protected void sumDomApplyCOpBin(COpBin op, SummaryDomain<AD> rhs) {
		
		Map<Set<Integer>, AD> lhsSummary = this.summary;
		Map<Set<Integer>, AD> rhsSummary = rhs.getSummary();
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		for (Set<Integer> lhsKey : lhsSummary.keySet()) {
			for (Set<Integer> rhsKey : rhsSummary.keySet()) {
				// Determine the new key ...
				Set<Integer> newKey = new HashSet<Integer>();
				newKey.addAll(lhsKey);
				newKey.addAll(rhsKey);
				// Determine the new value ...
				@SuppressWarnings("unchecked")
				AD newValue = (AD) lhsSummary.get(lhsKey).getCopy();
				newValue.evaluateCOpBin(op, rhsSummary.get(rhsKey));
				// Check if empty ...
				if (newValue.isEmpty()) {
					continue;
				}
				// Add to summary ...
				this.sumDomAdd(newKey, newValue);
			}
		}
		
	}
	
	protected void sumDomApplyIOpBin(IOpBin op, SummaryDomain<AD> rhs) {
		
		Map<Set<Integer>, AD> lhsSummary = this.summary;
		Map<Set<Integer>, AD> rhsSummary = rhs.getSummary();
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		for (Set<Integer> lhsKey : lhsSummary.keySet()) {
			for (Set<Integer> rhsKey : rhsSummary.keySet()) {
				// Determine the new key ...
				Set<Integer> newKey = new HashSet<Integer>();
				newKey.addAll(lhsKey);
				newKey.addAll(rhsKey);
				// Determine the new value ...
				@SuppressWarnings("unchecked")
				AD newValue = (AD) lhsSummary.get(lhsKey).getCopy();
				newValue.evaluateIOpBin(op, rhsSummary.get(rhsKey));
				// Check if empty ...
				if (newValue.isEmpty()) {
					continue;
				}
				// Add to summary ...
				this.sumDomAdd(newKey, newValue);
			}
		}
		
	}
	
	protected void sumDomApplyIOpUn(IOpUn op) {
		
		List<Set<Integer>> toRemoveList = new LinkedList<Set<Integer>>();
		
		for (Set<Integer> key : this.summary.keySet()) {
			AD currentElement = this.summary.get(key);
			currentElement.evaluateIOpUn(op);
			if (currentElement.isEmpty()) {
				toRemoveList.add(key);
			}
		}
		
		for (Set<Integer> toRemove : toRemoveList) {
			this.summary.remove(toRemove);
		}
		
	}
	
	public AD sumDomSummarize() {
		
		@SuppressWarnings("unchecked")
		AD sum = (AD) this.sample.getEmptyCopy();
		
		for (Set<Integer> key : this.summary.keySet()) {
			AD currentElement = this.summary.get(key);
			sum.unionWith(currentElement);
		}
		
		return sum;
		
	}
	
	// ========== Overridden methods from AbstractDomain. ==========
	
	@Override
	protected void evaluateIfExpr(Map<String, AbstractDomain> varValues, IfExpr ifExpr) {
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		// Get guard ... 
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> guard = (SummaryDomain<AD>) this.getEmptyCopy();
		guard.evaluateExpression(varValues, ifExpr.getGuard());
		
		Map<Set<Integer>, AD> guardSummary = guard.getSummary();
		
		// Get tBranch ...
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> tBranch = (SummaryDomain<AD>) this.getEmptyCopy();
		tBranch.evaluateExpression(varValues, ifExpr.getTrueBranch());
		
		Map<Set<Integer>, AD> tBranchSummary = tBranch.getSummary();
		
		// Get fBranch ...
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> fBranch = (SummaryDomain<AD>) this.getEmptyCopy();
		fBranch.evaluateExpression(varValues, ifExpr.getFalseBranch());
		
		Map<Set<Integer>, AD> fBranchSummary = fBranch.getSummary();
		
		for (Set<Integer> guardKey : guardSummary.keySet()) {
			AD currentGuard = guardSummary.get(guardKey);
			if (currentGuard.isTrueAndFalse()) {
				for (Set<Integer> tBranchKey : tBranchSummary.keySet()) {
					for (Set<Integer> fBranchKey : fBranchSummary.keySet()) {
						Set<Integer> newKey = new HashSet<Integer>();
						newKey.addAll(guardKey);
						newKey.addAll(tBranchKey);
						newKey.addAll(fBranchKey);
						@SuppressWarnings("unchecked")
						AD newValue = (AD) tBranchSummary.get(tBranchKey).getCopy();
						newValue.unionWith(fBranchSummary.get(fBranchKey));
						if (this.summary.containsKey(newKey)) {
							this.summary.get(newKey).unionWith(newValue);
						} else {
							this.summary.put(newKey, newValue);
						}
					}
				}
			} else if (currentGuard.isFalse()) {
				for (Set<Integer> fBranchKey : fBranchSummary.keySet()) {
					Set<Integer> newKey = new HashSet<Integer>();
					newKey.addAll(guardKey);
					newKey.addAll(fBranchKey);
					@SuppressWarnings("unchecked")
					AD newValue = (AD) fBranchSummary.get(fBranchKey).getCopy();
					if (this.summary.containsKey(newKey)) {
						this.summary.get(newKey).unionWith(newValue);
					} else {
						this.summary.put(newKey, newValue);
					}
				}
			} else if (currentGuard.isTrue()) {
				for (Set<Integer> tBranchKey : tBranchSummary.keySet()) {
					Set<Integer> newKey = new HashSet<Integer>();
					newKey.addAll(guardKey);
					newKey.addAll(tBranchKey);
					@SuppressWarnings("unchecked")
					AD newValue = (AD) tBranchSummary.get(tBranchKey).getCopy();
					if (this.summary.containsKey(newKey)) {
						this.summary.get(newKey).unionWith(newValue);
					} else {
						this.summary.put(newKey, newValue);
					}
				}
			}
		}
		
	}
	
	// ========== Abstract methods from AbstractDomain. ==========

	@Override
	public AbstractDomain getEmptyCopy() {
		
		return new SummaryDomain<AD>(this.sample);
		
	}

	@Override
	public AbstractDomain getCopy() {
		
		return new SummaryDomain<AD>(this);
		
	}

	@Override
	public void copy(AbstractDomain other) {
		
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> o = (SummaryDomain<AD>) other;
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		Map<Set<Integer>, AD> otherSummary = o.getSummary();
		
		for (Set<Integer> key : otherSummary.keySet()) {
			Set<Integer> newKey = new HashSet<Integer>(key);
			@SuppressWarnings("unchecked")
			AD newValue = (AD) otherSummary.get(key).getCopy();
			this.summary.put(newKey, newValue);
		}
		
	}

	@Override
	public void unionWith(AbstractDomain other) {
		
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> o = (SummaryDomain<AD>) other;
		
		Map<Set<Integer>, AD> otherSummary = o.getSummary();
		
		for (Set<Integer> key : otherSummary.keySet()) {
			if (this.summary.containsKey(key)) {
				this.summary.get(key).unionWith(otherSummary.get(key));
			} else {
				Set<Integer> newKey = new HashSet<Integer>(key);
				@SuppressWarnings("unchecked")
				AD newValue = (AD) otherSummary.get(key).getCopy();
				this.summary.put(newKey, newValue);
			}
		}
		
	}

	@Override
	public void intersectWith(AbstractDomain other) {
		
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> o = (SummaryDomain<AD>) other;
		
		Map<Set<Integer>, AD> lhsSummary = this.summary;
		Map<Set<Integer>, AD> rhsSummary = o.getSummary();
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		for (Set<Integer> lhsKey : lhsSummary.keySet()) {
			for (Set<Integer> rhsKey : rhsSummary.keySet()) {
				// Determine the new key ...
				Set<Integer> newKey = new HashSet<Integer>();
				newKey.addAll(lhsKey);
				newKey.addAll(rhsKey);
				// Determine the new value ...
				@SuppressWarnings("unchecked")
				AD newValue = (AD) lhsSummary.get(lhsKey).getCopy();
				newValue.intersectWith(rhsSummary.get(rhsKey));
				// Check if empty ...
				if (newValue.isEmpty()) {
					continue;
				}
				// Add to summary ...
				if (this.summary.containsKey(newKey)) {
					this.summary.get(newKey).unionWith(newValue);
				} else {
					this.summary.put(newKey, newValue);
				}
			}
		}
		
	}

	@Override
	public void complement() {
		
		List<Set<Integer>> toRemoveList = new LinkedList<Set<Integer>>();
		
		for (Set<Integer> key : this.summary.keySet()) {
			AD value = this.summary.get(key);
			value.complement();
			if (value.isEmpty()) {
				toRemoveList.add(key);
			}
		}
		
		for (Set<Integer> toRemove : toRemoveList) {
			this.summary.remove(toRemove);
		}
		
	}
	
	@Override
	public void restrict(COpBin op, BigInteger compareValue) {
		
		List<Set<Integer>> toRemoveList = new LinkedList<Set<Integer>>();
		
		for (Set<Integer> key : this.summary.keySet()) {
			AD value = this.summary.get(key);
			value.restrict(op, compareValue);
			if (value.isEmpty()) {
				toRemoveList.add(key);
			}
		}
		
		for (Set<Integer> toRemove : toRemoveList) {
			this.summary.remove(toRemove);
		}
		
	}

	@Override
	public void restrict(COpBin op, AbstractDomain compareValue) {
		
		Map<Set<Integer>, AD> currentSummary = this.getSummary();
		
		@SuppressWarnings("unchecked")
		SummaryDomain<AD> other = (SummaryDomain<AD>) compareValue;
		Map<Set<Integer>, AD> compareValueSummary = other.getSummary();
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		for (Set<Integer> key : currentSummary.keySet()) {
			for (Set<Integer> compareKey : compareValueSummary.keySet()) {
				Set<Integer> newKey = new HashSet<Integer>();
				newKey.addAll(key);
				newKey.addAll(compareKey);
				@SuppressWarnings("unchecked")
				AD newValue = (AD) currentSummary.get(key).getCopy();
				newValue.restrict(op, compareValueSummary.get(compareKey));
				if (!newValue.isEmpty()) {
					if (this.summary.containsKey(newKey)) {
						this.summary.get(newKey).unionWith(newValue);
					} else {
						this.summary.put(newKey, newValue);
					}
				}
			}
		}
		
	}
	
	@Override
	public void getEmptyInstance() {
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
	}

	@Override
	public void getFullRange() {

		this.summary = new HashMap<Set<Integer>, AD>();
		
		Set<Integer> newKey = new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		AD newValue = (AD) this.sample.getEmptyCopy();
		newValue.getFullRange();
		
		this.summary.put(newKey, newValue);
		
	}

	@Override
	public void getElementFromConst(BigInteger constant) {
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		Set<Integer> newKey = new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		AD newValue = (AD) this.sample.getEmptyCopy();
		newValue.getElementFromConst(constant);
		
		this.summary.put(newKey, newValue);
		
	}

	@Override
	public void getBoolean() {
		
		this.summary = new HashMap<Set<Integer>, AD>();
		
		Set<Integer> newKey = new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		AD newValue = (AD) this.sample.getEmptyCopy();
		newValue.getBoolean();
		
		this.summary.put(newKey, newValue);
		
	}

	@Override
	public void getTrue() {
		
		Set<Integer> newKey = new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		AD newValue = (AD) this.sample.getEmptyCopy();
		newValue.getTrue();
		
		this.summary.put(newKey, newValue);
		
	}

	@Override
	public void getFalse() {
		
		Set<Integer> newKey = new HashSet<Integer>();
		@SuppressWarnings("unchecked")
		AD newValue = (AD) this.sample.getEmptyCopy();
		newValue.getFalse();
		
		this.summary.put(newKey, newValue);
		
	}
	
	@Override
	public boolean isEmpty() {

		return this.summary.isEmpty();
		
	}

	@Override
	public boolean isBoolean() {
		
		if (this.summary.isEmpty()) {
			return false;
		}
		
		for (Set<Integer> key : this.summary.keySet()) {
			if (!this.summary.get(key).isBoolean()) {
				return false;
			}
		}
		
		return true;
		
	}

	@Override
	public boolean isTrue() {
		
		if (this.summary.isEmpty()) {
			return false;
		}
		
		for (Set<Integer> key : this.summary.keySet()) {
			if (!this.summary.get(key).isTrue()) {
				return false;
			}
		}
		
		return true;
		
	}

	@Override
	public boolean isFalse() {
		
		if (this.summary.isEmpty()) {
			return false;
		}
		
		for (Set<Integer> key : this.summary.keySet()) {
			if (!this.summary.get(key).isFalse()) {
				return false;
			}
		}
		
		return true;
		
	}

	@Override
	public boolean isTrueAndFalse() {
		
		if (this.summary.isEmpty()) {
			return false;
		}
		
		for (Set<Integer> key : this.summary.keySet()) {
			if (!this.summary.get(key).isTrueAndFalse()) {
				return false;
			}
		}
		
		return true;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateEQ(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.EQ, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateNEQ(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.NEQ, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateGTE(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.GTE, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateLTE(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.LTE, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateGT(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.GT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateLT(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.LT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateUGTE(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.UGTE, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateULTE(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.ULTE, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateUGT(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.UGT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateULT(AbstractDomain other) {
		
		this.sumDomApplyCOpBin(COpBin.ULT, (SummaryDomain<AD>) other);
		
	}

	@Override
	protected void evaluateNot() {
		
		this.sumDomApplyBOpUn(BOpUn.NOT);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateANDBool(AbstractDomain other) {
		
		this.sumDomApplyBOpBin(BOpBin.AND, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateORBool(AbstractDomain other) {
		
		this.sumDomApplyBOpBin(BOpBin.OR, (SummaryDomain<AD>) other);
		
	}

	@Override
	protected void evaluateMINUS() {
		
		this.sumDomApplyIOpUn(IOpUn.MINUS);
		
	}

	@Override
	protected void evaluateBV2UINT() {
		
		this.sumDomApplyIOpUn(IOpUn.BV2UINT);
		
	}

	@Override
	protected void evaluateBV2INT() {
		
		this.sumDomApplyIOpUn(IOpUn.BV2INT);
		
	}

	@Override
	protected void evaluateINT2BV1() {
		
		this.sumDomApplyIOpUn(IOpUn.INT2BV1);
		
	}

	@Override
	protected void evaluateINT2BV8() {
		
		this.sumDomApplyIOpUn(IOpUn.INT2BV8);
		
	}

	@Override
	protected void evaluateINT2BV16() {
		
		this.sumDomApplyIOpUn(IOpUn.INT2BV16);
		
	}

	@Override
	protected void evaluateINT2BV32() {
		
		this.sumDomApplyIOpUn(IOpUn.INT2BV32);
		
	}

	@Override
	protected void evaluateINT2BV64() {
		
		this.sumDomApplyIOpUn(IOpUn.INT2BV64);
		
	}

	@Override
	protected void evaluateTRUNC6432() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC6432);
		
	}

	@Override
	protected void evaluateTRUNC6416() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC6416);
		
	}

	@Override
	protected void evaluateTRUNC648() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC648);
		
	}

	@Override
	protected void evaluateTRUNC641() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC641);
		
	}

	@Override
	protected void evaluateTRUNC3216() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC3216);
		
	}

	@Override
	protected void evaluateTRUNC328() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC328);
		
	}

	@Override
	protected void evaluateTRUNC321() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC321);
		
	}

	@Override
	protected void evaluateTRUNC168() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC168);
		
	}

	@Override
	protected void evaluateTRUNC161() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC161);
		
	}

	@Override
	protected void evaluateTRUNC81() {
		
		this.sumDomApplyIOpUn(IOpUn.TRUNC81);
		
	}

	@Override
	protected void evaluateZEXT18() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT18);
		
	}

	@Override
	protected void evaluateZEXT116() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT116);
		
	}

	@Override
	protected void evaluateZEXT132() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT132);
		
	}

	@Override
	protected void evaluateZEXT164() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT164);
		
	}

	@Override
	protected void evaluateZEXT816() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT816);
		
	}

	@Override
	protected void evaluateZEXT832() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT832);
		
	}

	@Override
	protected void evaluateZEXT864() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT864);
		
	}

	@Override
	protected void evaluateZEXT1632() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT1632);
		
	}

	@Override
	protected void evaluateZEXT1664() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT1664);
		
	}

	@Override
	protected void evaluateZEXT3264() {
		
		this.sumDomApplyIOpUn(IOpUn.ZEXT3264);
		
	}

	@Override
	protected void evaluateSEXT18() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT18);
		
	}

	@Override
	protected void evaluateSEXT116() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT116);
		
	}

	@Override
	protected void evaluateSEXT132() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT132);
		
	}

	@Override
	protected void evaluateSEXT164() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT164);
		
	}

	@Override
	protected void evaluateSEXT816() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT816);
		
	}

	@Override
	protected void evaluateSEXT832() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT832);
		
	}

	@Override
	protected void evaluateSEXT864() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT864);
		
	}

	@Override
	protected void evaluateSEXT1632() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT1632);
		
	}

	@Override
	protected void evaluateSEXT1664() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT1664);
		
	}

	@Override
	protected void evaluateSEXT3264() {
		
		this.sumDomApplyIOpUn(IOpUn.SEXT3264);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluatePLUS(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.PLUS, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateMINUS(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.MINUS, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateMULT(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.MULT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateDIV(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.DIV, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateUDIV(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.UDIV, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateMOD(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.MOD, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateANDInt(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.AND, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateORInt(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.OR, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateXOR(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.XOR, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateLSHIFT(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.L_SHIFT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateRSHIFT(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.R_SHIFT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateARSHIFT(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.AR_SHIFT, (SummaryDomain<AD>) other);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateSREM(AbstractDomain other) {
		
		
		this.sumDomApplyIOpBin(IOpBin.SREM, (SummaryDomain<AD>) other);
		
	}

	
	@SuppressWarnings("unchecked")
	@Override
	protected void evaluateUREM(AbstractDomain other) {
		
		this.sumDomApplyIOpBin(IOpBin.UREM, (SummaryDomain<AD>) other);
		
	}
	
	// ========== Object methods. ==========
	
	@Override
	public String toString() {
		
		if (this.summary.isEmpty()) {
			return "EMPTY";
		}
		
		String outString = "";
		
		List<Set<Integer>> keyList = new LinkedList<Set<Integer>>(this.summary.keySet());
		for (int j = 0; j < keyList.size(); j++) {
			outString += "{";
			List<Integer> keyValues = new LinkedList<Integer>(keyList.get(j));
			for (int i = 0; i < keyValues.size(); i++) {
				outString += keyValues.get(i);
				if (i < keyValues.size() - 1) {
					outString += ",";
				}
			}
			outString += "}: ";
			outString += this.summary.get(keyList.get(j)).toString();
			if (j < keyList.size() - 1) {
				outString += "\n";
			}
		}
		
		return outString;
		
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
		
		if (!(o instanceof SummaryDomain<?>)) {
			return false;
		}
		
		SummaryDomain<?> other = (SummaryDomain<?>) o;
		
		return this.summary.equals(other.getSummary());
		
	}
	
	@Override
	public int hashCode() {
		
		return this.summary.hashCode();
		
	}

}