package com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain;

import java.math.BigInteger;
import java.util.Objects;

import com.dat3m.dartagnan.expression.op.COpBin;

public class ZInterval extends SimpleDomain {
	
	boolean empty;
	BigInteger upperBound;
	BigInteger lowerBound;
	
	// ========== Constructors. ==========
	
	public ZInterval() {
		
		this.empty = true;
		this.lowerBound = null;
		this.upperBound = null;
		
	}
	
	public ZInterval(ZInterval other) {
		
		this.empty = other.getEmpty();
		this.lowerBound = other.getLowerBound();
		this.upperBound = other.getUpperBound();
		
	}
	
	public ZInterval(BigInteger lowerBound, BigInteger upperBound) {
		
		// If we have an Interval [x, y] with x > y ...
		if (lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) > 0) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		} else {
			this.empty = false;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}
		
	}
	
	// ========== Get- and set-methods. ==========
	
	public boolean getEmpty() {
		
		return this.empty;
		
	}
	
	public BigInteger getUpperBound() {
		
		return this.upperBound;
		
	}
	
	public BigInteger getLowerBound() {
		
		return this.lowerBound;
		
	}
	
	// ========== Widening-Operator. ==========
	
	public AbstractDomain widen(AbstractDomain other) {
		
		ZInterval o = (ZInterval) other;
		
		if (this.empty || o.getEmpty()) {
			this.empty = o.getEmpty();
			this.lowerBound = o.getLowerBound();
			this.upperBound = o.getUpperBound();
			return this;
		}
		
		if (this.lowerBound != null && o.getLowerBound() != null) {
			if (this.lowerBound.compareTo(o.getLowerBound()) == 1) {
				this.lowerBound = null;
			}
		}
		
		if (this.upperBound != null && o.getUpperBound() != null) {
			if (this.upperBound.compareTo(o.getUpperBound()) == -1) {
				this.upperBound = null;
			}
		}
		
		return this;
		
	}
	
	// ========== Narrowing-Operator. ==========
	
	public AbstractDomain narrow(AbstractDomain other) {
			
		ZInterval o = (ZInterval) other;
		
		if (this.empty || o.getEmpty()) {
			this.empty = o.getEmpty();
			this.lowerBound = o.getLowerBound();
			this.upperBound = o.getUpperBound();
			return this;
		}
		
		if (this.lowerBound != null && o.getLowerBound() != null) {
			if (this.lowerBound.compareTo(o.getLowerBound()) == 1) {
				this.lowerBound = o.getLowerBound();
			}
		}
		
		if (this.upperBound != null && o.getUpperBound() != null) {
			if (this.upperBound.compareTo(o.getUpperBound()) == -1) {
				this.upperBound = o.getUpperBound();
			}
		}
		
		if (this.lowerBound == null && o.getLowerBound() != null) {
			this.lowerBound = o.getLowerBound();
		}
		
		if (this.upperBound == null && o.getUpperBound() != null) {
			this.upperBound = o.getUpperBound();
		}
	
		return this;
		
	}
	
	// ========== Numerical-operations. ==========
	
	@Override
	public AbstractDomain getEmptyCopy() {
		
		return new ZInterval();
		
	}
	
	@Override
	public AbstractDomain getCopy() {
		
		return new ZInterval(this);
		
	}
	
	@Override
	public void copy(AbstractDomain other) {
		
		ZInterval o = (ZInterval) other;
		
		this.empty = o.getEmpty();
		this.lowerBound = o.getLowerBound();
		this.upperBound = o.getUpperBound();
		
	}
	
	@Override
	public void unionWith(AbstractDomain other) {
		
		ZInterval o = (ZInterval) other;
		
		if (o.getEmpty()) {
			return;
		}
		
		if (this.empty) {
			this.empty = o.getEmpty();
			this.lowerBound = o.getLowerBound();
			this.upperBound = o.getUpperBound();
			return;
		}
		
		if (o.getLowerBound() == null) {
			this.lowerBound = null;
		}
		
		if (o.getUpperBound() == null) {
			this.upperBound = null;
		}
		
		if (this.lowerBound != null && o.getLowerBound() != null) {
			if (this.lowerBound.compareTo(o.getLowerBound()) > 0) {
				this.lowerBound = o.getLowerBound();
			}
		}
		
		if (this.upperBound != null && o.getUpperBound() != null) {
			if (this.upperBound.compareTo(o.getUpperBound()) < 0) {
				this.upperBound = o.getUpperBound();
			}
		}
		
	}
	
	@Override
	public void intersectWith(AbstractDomain other) {
		
		ZInterval o = (ZInterval) other;
		
		if (this.empty || o.getEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		if (this.lowerBound == null && o.getLowerBound() != null) {
			this.lowerBound = o.getLowerBound();
		}
		
		if (this.upperBound == null && o.getUpperBound() != null) {
			this.upperBound = o.getUpperBound();
		}
		
		if (this.lowerBound != null && o.getLowerBound() != null) {
			if (this.lowerBound.compareTo(o.getLowerBound()) < 0) {
				this.lowerBound = o.getLowerBound();
			}
		}
		
		if (this.upperBound != null && o.getUpperBound() != null) {
			if (this.upperBound.compareTo(o.getUpperBound()) > 0) {
				this.upperBound = o.getUpperBound();
			}
		}
		
		if (this.lowerBound != null && this.upperBound != null) {
			if (this.lowerBound.compareTo(this.upperBound) > 0) {
				this.empty = true;
				this.lowerBound = null;
				this.upperBound = null;
			}
		}
		
	}
	
	@Override
	public void complement() {
		
		if (this.empty == true) {
			this.empty = false;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		if (this.lowerBound == null && this.upperBound == null) {
			this.empty = true;
			return;
		}
		
		if (this.lowerBound != null && this.upperBound != null) {
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		if (this.lowerBound != null) {
			this.upperBound = this.lowerBound.add(new BigInteger("-1"));
			this.lowerBound = null;
			return;
		}
		
		if (this.upperBound != null) {
			this.lowerBound = this.upperBound.add(new BigInteger("1"));
			this.upperBound = null;
			return;
		}

	}
	
	@Override
	public void restrict(COpBin op, BigInteger compareValue) {
		
		if (this.empty) {
			return;
		}
		
		switch (op) {
			case EQ:
				ZInterval compareValueInterval = new ZInterval(compareValue, compareValue);
				this.intersectWith(compareValueInterval);
				break;
			case NEQ:
				this.exclude(compareValue);
				break;
			case GTE:
				this.lowerBound = compareValue;
				if (this.upperBound != null && this.lowerBound.compareTo(this.upperBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case LTE:
				this.upperBound = compareValue;
				if (this.lowerBound != null && this.upperBound.compareTo(this.lowerBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case GT:
				this.lowerBound = compareValue.add(new BigInteger("1"));
				if (this.upperBound != null && this.lowerBound.compareTo(this.upperBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case LT:
				this.upperBound = compareValue.add(new BigInteger("-1"));
				if (this.lowerBound != null && this.upperBound.compareTo(this.lowerBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case UGTE:
				this.lowerBound = compareValue;
				if (this.upperBound != null && this.lowerBound.compareTo(this.upperBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case ULTE:
				this.upperBound = compareValue;
				if (this.lowerBound != null && this.upperBound.compareTo(this.lowerBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case UGT:
				this.lowerBound = compareValue.add(new BigInteger("1"));
				if (this.upperBound != null && this.lowerBound.compareTo(this.upperBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			case ULT:
				this.upperBound = compareValue.add(new BigInteger("-1"));
				if (this.lowerBound != null && this.upperBound.compareTo(this.lowerBound) > 0) {
					this.empty = true;
					this.lowerBound = null;
					this.upperBound = null;
				}
				break;
			default:
				break;
		}
		
	}
	
	@Override
	public void restrict(COpBin op, AbstractDomain compareValue) {
		
		ZInterval compareInterval = (ZInterval) compareValue;
		
		if (this.empty) {
			return;
		}
		
		if (compareInterval.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		switch (op) {
			case EQ:
				this.intersectWith(compareInterval);
				break;
			case NEQ:
				if (compareInterval.getLowerBound() != null && compareInterval.getUpperBound() != null) {
					if (compareInterval.getLowerBound().equals(compareInterval.getUpperBound())) {
						this.exclude(compareInterval.getLowerBound());
					}
				}
				break;
			case GTE:
				if (compareInterval.getLowerBound() != null) {
					this.restrict(COpBin.GTE, compareInterval.getLowerBound());
				}
				break;
			case LTE:
				if (compareInterval.getUpperBound() != null) {
					this.restrict(COpBin.LTE, compareInterval.getUpperBound());
				}
				break;
			case GT:
				if (compareInterval.getLowerBound() != null) {
					this.restrict(COpBin.GT, compareInterval.getLowerBound());
				}
				break;
			case LT:
				if (compareInterval.getUpperBound() != null) {
					this.restrict(COpBin.LT, compareInterval.getUpperBound());
				}
				break;
			case UGTE:
				if (compareInterval.getLowerBound() != null) {
					this.restrict(COpBin.GTE, compareInterval.getLowerBound());
				}
				break;
			case ULTE:
				if (compareInterval.getUpperBound() != null) {
					this.restrict(COpBin.LTE, compareInterval.getUpperBound());
				}
				break;
			case UGT:
				if (compareInterval.getLowerBound() != null) {
					this.restrict(COpBin.GT, compareInterval.getLowerBound());
				}
				break;
			case ULT:
				if (compareInterval.getUpperBound() != null) {
					this.restrict(COpBin.LT, compareInterval.getUpperBound());
				}
				break;
			default:
				break;
		}
		
	}
	
	@Override
	public void getEmptyInstance() {
		
		this.empty = true;
		this.lowerBound = null;
		this.upperBound = null;
		
	}
	
	@Override
	public void getFullRange() {
		
		this.empty = false;
		this.upperBound = null;
		this.lowerBound = null;
		
	}
	
	@Override
	public void getElementFromConst(BigInteger constant) {
		
		this.empty = false;
		this.lowerBound = constant;
		this.upperBound = constant;
		
	}

	@Override
	public void getBoolean() {
		
		this.empty = false;
		this.lowerBound = new BigInteger("0");
		this.upperBound = new BigInteger("1");
		
	}

	@Override
	public void getTrue() {
		
		this.empty = false;
		this.lowerBound = new BigInteger("1");
		this.upperBound = new BigInteger("1");
		
	}

	@Override
	public void getFalse() {
		
		this.empty = false;
		this.lowerBound = new BigInteger("0");
		this.upperBound = new BigInteger("0");
		
	}
	
	@Override
	public boolean isEmpty() {
		
		return this.empty;
		
	}

	@Override
	public boolean isBoolean() {
		
		return this.isTrue() || this.isFalse() || this.isTrueAndFalse();
		
	}

	@Override
	public boolean isTrue() {
		
		if (this.empty) {
			return false;
		}
		
		if (this.lowerBound == null || this.upperBound == null) {
			return false;
		}
		
		if (!this.lowerBound.equals(new BigInteger("1"))) {
			return false;
		}
		
		if (!this.upperBound.equals(new BigInteger("1"))) {
			return false;
		}
		
		return true;
		
	}

	@Override
	public boolean isFalse() {
		
		if (this.empty) {
			return false;
		}
		
		if (this.lowerBound == null || this.upperBound == null) {
			return false;
		}
		
		if (!this.lowerBound.equals(new BigInteger("0"))) {
			return false;
		}
		
		if (!this.upperBound.equals(new BigInteger("0"))) {
			return false;
		}
		
		return true;
		
	}

	@Override
	public boolean isTrueAndFalse() {
		
		if (this.empty) {
			return false;
		}
		
		if (this.lowerBound == null || this.upperBound == null) {
			return false;
		}
		
		if (!this.lowerBound.equals(new BigInteger("0"))) {
			return false;
		}
		
		if (!this.upperBound.equals(new BigInteger("1"))) {
			return false;
		}
		
		return true;
		
	}

	@Override
	protected void evaluateEQ(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		BigInteger thisConst = this.isConst();
		BigInteger otherConst = o.isConst();
		
		if (thisConst != null && otherConst != null && thisConst.equals(otherConst)) {
			this.getTrue();
			return;
		}
		
		this.intersectWith(o);
		
		if (this.isEmpty()) {
			this.getFalse();
			return;
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateNEQ(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		BigInteger thisConst = this.isConst();
		BigInteger otherConst = o.isConst();
		
		if (thisConst != null && otherConst != null && thisConst.equals(otherConst)) {
			this.getFalse();
			return;
		}
		
		this.intersectWith(o);
		
		if (this.isEmpty()) {
			this.getTrue();
			return;
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateGTE(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) >= 0) {
				this.getTrue();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) < 0) {
				this.getFalse();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateLTE(AbstractDomain other) {

		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) > 0) {
				this.getFalse();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) <= 0) {
				this.getTrue();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateGT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) > 0) {
				this.getTrue();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) <= 0) {
				this.getFalse();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateLT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) >= 0) {
				this.getFalse();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) < 0) {
				this.getTrue();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateUGTE(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) >= 0) {
				this.getTrue();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) < 0) {
				this.getFalse();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateULTE(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) > 0) {
				this.getFalse();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) <= 0) {
				this.getTrue();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateUGT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) > 0) {
				this.getTrue();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) <= 0) {
				this.getFalse();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateULT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
			return;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (this.lowerBound != null && o.getUpperBound() != null) {
			if (this.lowerBound.compareTo(o.getUpperBound()) >= 0) {
				this.getFalse();
				return;
			}
		}
		
		if (this.upperBound != null && o.getLowerBound() != null) {
			if (this.upperBound.compareTo(o.getLowerBound()) < 0) {
				this.getTrue();
				return;
			}
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateNot() {
		
		if (this.empty) {
			return;
		}
		
		if (this.isTrue()) {
			this.getFalse();
			return;
		}
		
		if (this.isFalse()) {
			this.getTrue();
			return;
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateANDBool(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		if (!this.isBoolean() || !other.isBoolean()) {
			this.getBoolean();
			return;
		}
		
		if (this.isTrue() && other.isTrue()) {
			this.getTrue();
			return;
		}
		
		if (this.isFalse() || other.isFalse()) {
			this.getFalse();
			return;
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateORBool(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		if (!this.isBoolean() || !other.isBoolean()) {
			this.getBoolean();
			return;
		}
		
		if (this.isFalse() && other.isFalse()) {
			this.getFalse();
			return;
		}
		
		if (this.isTrue() || other.isTrue()) {
			this.getTrue();
			return;
		}
		
		this.getBoolean();
		
	}

	@Override
	protected void evaluateMINUS() {
		
		if (this.empty) {
			return;
		}
		
		BigInteger newLB = this.upperBound;
		BigInteger newUB = this.lowerBound;
		
		if (newLB != null) {
			newLB = newLB.multiply(new BigInteger("-1"));
		}
		
		if (newUB != null) {
			newUB = newUB.multiply(new BigInteger("-1"));
		}
		
		this.lowerBound = newLB;
		this.upperBound = newUB;
		
	}

	@Override
	protected void evaluateBV2UINT() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
				
	}

	@Override
	protected void evaluateBV2INT() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateINT2BV1() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateINT2BV8() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateINT2BV16() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateINT2BV32() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateINT2BV64() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC6432() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC6416() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC648() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC641() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC3216() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC328() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC321() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC168() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
	}

	@Override
	protected void evaluateTRUNC161() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateTRUNC81() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT18() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT116() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT132() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT164() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT816() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT832() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT864() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT1632() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT1664() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateZEXT3264() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT18() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT116() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT132() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT164() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT816() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
	}

	@Override
	protected void evaluateSEXT832() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT864() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT1632() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT1664() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSEXT3264() {
		
		if (this.empty) {
			return;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluatePLUS(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		ZInterval o = (ZInterval) other;
		
		if (o.getLowerBound() == null) {
			this.lowerBound = null;
		}
		
		if (o.getUpperBound() == null) {
			this.upperBound = null;
		}
		
		if (this.lowerBound != null) {
			this.lowerBound = this.lowerBound.add(o.getLowerBound());
		}
		
		if (this.upperBound != null) {
			this.upperBound = this.upperBound.add(o.getUpperBound());
		}
		
	}

	@Override
	protected void evaluateMINUS(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.evaluateMINUS();
		
		this.evaluatePLUS(other);
		
		this.evaluateMINUS();
		
	}

	@Override
	protected void evaluateMULT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateDIV(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateUDIV(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateMOD(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateANDInt(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateORInt(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateXOR(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateLSHIFT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateRSHIFT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateARSHIFT(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateSREM(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}

	@Override
	protected void evaluateUREM(AbstractDomain other) {
		
		if (this.empty) {
			return;
		}
		
		if (other.isEmpty()) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
		this.getFullRange();
		
	}
	
	// ========== Object-methods. ==========
	
	@Override
	public String toString() {
		
		if (this.empty) {
			return "EMPTY";
		}
		
		String lower = this.lowerBound == null ? "-INF" : this.lowerBound.toString();
		String upper = this.upperBound == null ? "+INF" : this.upperBound.toString();
		
		return "[" + lower + "," + upper + "]";
		
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
		if (!(o instanceof ZInterval)) {
			return false;
		}
		
		ZInterval other = (ZInterval) o;
		
		if (this.empty != other.getEmpty()) {
			return false;
		}
		
		if (!this.empty) {
			return Objects.equals(this.upperBound, other.getUpperBound()) && 
					Objects.equals(this.lowerBound, other.getLowerBound());
		}
		
		return true;
		
	}
	
	@Override
	public int hashCode() {
		
		if (this.empty) {
			return 0;
		}
		
		if (this.lowerBound == null) {
			return this.upperBound.intValue();
		}
		
		if (this.upperBound == null) {
			return this.lowerBound.intValue();
		}
		
		return this.lowerBound.intValue() + this.upperBound.intValue();
		
	}
	
	// ========== Helper. ===========
	
	public BigInteger isConst() {
		
		if (this.lowerBound != null && this.upperBound != null) {
			if (this.lowerBound.equals(upperBound)) {
				return this.lowerBound;
			}
		}
		
		return null;
		
	}
	
	private void exclude(BigInteger value) {
		
		if (this.empty) {
			return;
		}
		
		if (this.upperBound != null && this.upperBound.equals(value)) {
			this.upperBound = this.upperBound.add(new BigInteger("-1"));
		}
		
		if (this.lowerBound != null && this.lowerBound.equals(value)) {
			this.lowerBound = this.lowerBound.add(new BigInteger("1"));
		}
		
		if (this.lowerBound != null && this.upperBound != null && this.lowerBound.compareTo(this.upperBound) > 0) {
			this.empty = true;
			this.lowerBound = null;
			this.upperBound = null;
		}
		
	}

}
