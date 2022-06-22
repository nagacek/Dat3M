package com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain;

import java.math.BigInteger;
import java.util.Map;

import com.dat3m.dartagnan.expression.Atom;
import com.dat3m.dartagnan.expression.BConst;
import com.dat3m.dartagnan.expression.BExpr;
import com.dat3m.dartagnan.expression.BExprBin;
import com.dat3m.dartagnan.expression.BExprUn;
import com.dat3m.dartagnan.expression.BNonDet;
import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.expression.IExprBin;
import com.dat3m.dartagnan.expression.IExprUn;
import com.dat3m.dartagnan.expression.INonDet;
import com.dat3m.dartagnan.expression.IConst;
import com.dat3m.dartagnan.expression.IfExpr;
import com.dat3m.dartagnan.expression.op.BOpBin;
import com.dat3m.dartagnan.expression.op.BOpUn;
import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.expression.op.IOpBin;
import com.dat3m.dartagnan.expression.op.IOpUn;
import com.dat3m.dartagnan.program.Register;

public abstract class AbstractDomain {
	
	// ========== Entry point. ==========
	
	public void evaluateExpression(Map<String, AbstractDomain> varValues, ExprInterface expression) {
		
		if (expression instanceof Register) {
			this.evaluateRegister(varValues, (Register) expression);
			return;
		}
		
		if (expression instanceof BExpr) {
			this.evaluateBExpr(varValues, (BExpr) expression);
			return;
		}
		
		if (expression instanceof IExpr) {
			this.evaluateIExpr(varValues, (IExpr) expression);
			return;
		}
		
		this.getFullRange();
		
	}
	
	// ========== Register. ==========
	
	protected void evaluateRegister(Map<String, AbstractDomain> varValues, Register register) {
		
		this.copy(varValues.get(register.getName()));
		
	}
	
	// ========== BExpr. ==========
	
	protected void evaluateBExpr(Map<String, AbstractDomain> varValues, BExpr bExpr) {
		
		if (bExpr instanceof Atom) {
			this.evaluateAtom(varValues, (Atom) bExpr);
			return;
		}
		
		if (bExpr instanceof BConst) {
			this.evaluateBConst(varValues, (BConst) bExpr);
			return;
		}
		
		if (bExpr instanceof BNonDet) {
			this.evaluateBNonDet(varValues, (BNonDet) bExpr);
			return;
		}
		
		if (bExpr instanceof BExprUn) {
			this.evaluateBExprUn(varValues, (BExprUn) bExpr); 
			return;
		}
		
		if (bExpr instanceof BExprBin) {
			this.evaluateBExprBin(varValues, (BExprBin) bExpr);
			return;
		}
		
		this.getFullRange();
		
	}
	
	protected void evaluateAtom(Map<String, AbstractDomain> varValues, Atom atom) {
		
		// Get LHS ...
		this.evaluateExpression(varValues, atom.getLHS());
		// Get RHS ...
		AbstractDomain rhs = this.getEmptyCopy();
		rhs.evaluateExpression(varValues, atom.getRHS());
		// Get OP ...
		COpBin op = atom.getOp();
		
		// Apply ...
		this.evaluateCOpBin(op, rhs);
		
	}
	
	protected void evaluateCOpBin(COpBin op, AbstractDomain rhs) {
		
		switch (op) {
			case EQ:
				this.evaluateEQ(rhs);
				break;
			case NEQ:
				this.evaluateNEQ(rhs);
				break;
			case GTE:
				this.evaluateGTE(rhs);
				break;
			case LTE:
				this.evaluateLTE(rhs);
				break;
			case GT:
				this.evaluateGT(rhs);
				break;
			case LT:
				this.evaluateLT(rhs);
				break;
			case UGTE:
				this.evaluateUGTE(rhs);
				break;
			case ULTE:
				this.evaluateULTE(rhs);
				break;
			case UGT:
				this.evaluateUGT(rhs);
				break;
			case ULT:
				this.evaluateULT(rhs);
				break;
			default:
				this.getFullRange();
				break;
		}
		
	}
	
	protected void evaluateBConst(Map<String, AbstractDomain> varValues, BConst bConst) {
		
		boolean boolValue = bConst.getBoolValue(null, null, null);
		
		if (boolValue) {
			this.getTrue();
		} else {
			this.getFalse();
		}
		
	}
	
	protected void evaluateBNonDet(Map<String, AbstractDomain> varValues, BNonDet bNonDet) {
		
		this.getBoolean();
		
	}
	
	protected void evaluateBExprUn(Map<String, AbstractDomain> varValues, BExprUn bExprUn) {
		
		this.evaluateExpression(varValues, bExprUn.getInner());
		BOpUn op = bExprUn.getOp();

		this.evaluateBOpUn(op);
		
	}
	
	protected void evaluateBOpUn(BOpUn op) {
		
		switch (op) {
			case NOT:
				this.evaluateNot();
				break;
			default:
				this.getFullRange();
				break;
		}
		
	}
	
	protected void evaluateBExprBin(Map<String, AbstractDomain> varValues, BExprBin bExprBin) {
		
		// Get LHS ...
		this.evaluateExpression(varValues, bExprBin.getLHS());
		
		// Get RHS ...
		AbstractDomain rhs = this.getEmptyCopy();
		rhs.evaluateExpression(varValues, bExprBin.getRHS());
		
		// Get OP ...
		BOpBin op = bExprBin.getOp();
		
		// Apply ...
		this.evaluateBOpBin(op, rhs);
		
	}
	
	protected void evaluateBOpBin(BOpBin op, AbstractDomain rhs) {
		
		switch (op) {
			case AND:
				this.evaluateANDBool(rhs);
				break;
			case OR:
				this.evaluateORBool(rhs);
				break;
			default:
				this.getFullRange();
				break;
		}
		
	}
	
	// ========== IExpr. ==========
	
	protected void evaluateIExpr(Map<String, AbstractDomain> varValues, ExprInterface iExpr) {
		
		if (iExpr instanceof IConst) {
			this.evaluateIConst(varValues, (IConst) iExpr);
			return;
		}
		
		if (iExpr instanceof INonDet) {
			this.evaluateINonDet(varValues, (INonDet) iExpr);
			return;
		}
		
		if (iExpr instanceof IfExpr) {
			this.evaluateIfExpr(varValues, (IfExpr) iExpr);
			return;
		}
		
		if (iExpr instanceof IExprUn) {
			this.evaluateIExprUn(varValues, (IExprUn) iExpr);
			return;
		}
		
		if (iExpr instanceof IExprBin) {
			this.evaluateIExprBin(varValues, (IExprBin) iExpr);
			return;
		}
		
		this.getFullRange();
		
	}
	
	protected void evaluateIConst(Map<String, AbstractDomain> varValues, IConst iValue) {
		
		this.getElementFromConst(iValue.getValue());
		
	}
	
	protected void evaluateINonDet(Map<String, AbstractDomain> varValues, INonDet iNonDet) {
		
		this.getFullRange();
		
	}
	
	protected void evaluateIfExpr(Map<String, AbstractDomain> varValues, IfExpr ifExpr) {
		
		// Get guard ... 
		this.evaluateExpression(varValues, ifExpr.getGuard());
		
		// Get tBranch ...
		AbstractDomain tBranch = this.getEmptyCopy();
		tBranch.evaluateExpression(varValues, ifExpr.getTrueBranch());
		
		// Get fBranch ...
		AbstractDomain fBranch = this.getEmptyCopy();
		fBranch.evaluateExpression(varValues, ifExpr.getFalseBranch());
		
		if (this.isTrueAndFalse()) {
			this.copy(tBranch);
			this.unionWith(fBranch);
			return;
		}
		
		if (this.isTrue()) {
			this.copy(tBranch);
			return;
		}
		
		if (this.isFalse()) {
			this.copy(fBranch);
			return;
		}
		
		this.getFullRange();
		
	}
	
	protected void evaluateIExprUn(Map<String, AbstractDomain> varValues, IExprUn iExprUn) {
		
		this.evaluateExpression(varValues, iExprUn.getInner());
		IOpUn op = iExprUn.getOp();
		
		this.evaluateIOpUn(op);
		
	}
	
	protected void evaluateIOpUn(IOpUn op) {
		
		switch (op) {
			case MINUS:
				this.evaluateMINUS();
				break;
			case BV2UINT:
				this.evaluateBV2UINT();
				break;
			case BV2INT:
				this.evaluateBV2INT();
				break;
			case INT2BV1:
				this.evaluateINT2BV1();
				break;
			case INT2BV8:
				this.evaluateINT2BV8();
				break;
			case INT2BV16:
				this.evaluateINT2BV16();
				break;
			case INT2BV32:
				this.evaluateINT2BV32();
				break;
			case INT2BV64:
				this.evaluateINT2BV64();
				break;
			case TRUNC6432:
				this.evaluateTRUNC6432();
				break;
			case TRUNC6416:
				this.evaluateTRUNC6416();
				break;
			case TRUNC648:
				this.evaluateTRUNC648();
				break;
			case TRUNC641:
				this.evaluateTRUNC641();
				break;
			case TRUNC3216:
				this.evaluateTRUNC3216();
				break;
			case TRUNC328:
				this.evaluateTRUNC328();
				break;
			case TRUNC321:
				this.evaluateTRUNC321();
				break;
			case TRUNC168:
				this.evaluateTRUNC168();
				break;
			case TRUNC161:
				this.evaluateTRUNC161();
				break;
			case TRUNC81:
				this.evaluateTRUNC81();
				break;
			case ZEXT18:
				this.evaluateZEXT18();
				break;
			case ZEXT116:
				this.evaluateZEXT116();
				break;
			case ZEXT132:
				this.evaluateZEXT132();
				break;
			case ZEXT164:
				this.evaluateZEXT164();
				break;
			case ZEXT816:
				this.evaluateZEXT816();
				break;
			case ZEXT832:
				this.evaluateZEXT832();
				break;
			case ZEXT864:
				this.evaluateZEXT864();
				break;
			case ZEXT1632:
				this.evaluateZEXT1632();
				break;
			case ZEXT1664:
				this.evaluateZEXT1664();
				break;
			case ZEXT3264:
				this.evaluateZEXT3264();
				break;
			case SEXT18:
				this.evaluateSEXT18();
				break;
			case SEXT116:
				this.evaluateSEXT116();
				break;
			case SEXT132:
				this.evaluateSEXT132();
				break;
			case SEXT164:
				this.evaluateSEXT164();
				break;
			case SEXT816:
				this.evaluateSEXT816();
				break;
			case SEXT832:
				this.evaluateSEXT832();
				break;
			case SEXT864:
				this.evaluateSEXT864();
				break;
			case SEXT1632:
				this.evaluateSEXT1632();
				break;
			case SEXT1664:
				this.evaluateSEXT1664();
				break;
			case SEXT3264:
				this.evaluateSEXT3264();
				break;
			default:
				this.getFullRange();
				break;
		}
		
	}
	
	protected void evaluateIExprBin(Map<String, AbstractDomain> varValues, IExprBin iExprBin) {
		
		// Get LHS ...
		this.evaluateExpression(varValues, iExprBin.getLHS());
		
		// Get RHS ...
		AbstractDomain rhs = this.getEmptyCopy();
		rhs.evaluateExpression(varValues, iExprBin.getRHS());
		
		// Get OP ...
		IOpBin op = iExprBin.getOp();
		
		this.evaluateIOpBin(op, rhs);
		
	}
	
	protected void evaluateIOpBin(IOpBin op, AbstractDomain rhs) {
		
		switch (op) {
			case PLUS:
				this.evaluatePLUS(rhs);
				break;
			case MINUS:
				this.evaluateMINUS(rhs);
				break;
			case MULT:
				this.evaluateMULT(rhs);
				break;
			case DIV:
				this.evaluateDIV(rhs);
				break;
			case UDIV:
				this.evaluateUDIV(rhs);
				break;
			case MOD:
				this.evaluateMOD(rhs);
				break;
			case AND:
				this.evaluateANDInt(rhs);
				break;
			case OR:
				this.evaluateORInt(rhs);
				break;
			case XOR:
				this.evaluateXOR(rhs);
				break;
			case L_SHIFT:
				this.evaluateLSHIFT(rhs);
				break;
			case R_SHIFT:
				this.evaluateRSHIFT(rhs);
				break;
			case AR_SHIFT:
				this.evaluateARSHIFT(rhs);
				break;
			case SREM:
				this.evaluateSREM(rhs);
				break;
			case UREM:
				this.evaluateUREM(rhs);
				break;
			default:
				this.getFullRange();
				break;
		}
		
	}
	
	// ========== Other. ==========
	
	// Create new instances of AD ...
	public abstract AbstractDomain getEmptyCopy(); //ZI:DONE
	public abstract AbstractDomain getCopy(); //ZI:DONE
	
	// Copy from other instance of AD ...
	public abstract void copy(AbstractDomain other); //ZI:DONE
	
	// Set operations ...
	public abstract void unionWith(AbstractDomain other); //ZI:DONE
	public abstract void intersectWith(AbstractDomain other); //ZI:DONE
	public abstract void complement(); //ZI:DONE
	
	// Restriction operations ...
	public abstract void restrict(COpBin op, BigInteger compareValue); //ZI:DONE
	public abstract void restrict(COpBin op, AbstractDomain compareValue); //ZI:DONE
	
	// Get certain values ...
	public abstract void getEmptyInstance(); //ZI:DONE
	public abstract void getFullRange(); //ZI:DONE
	public abstract void getElementFromConst(BigInteger constant); //ZI:DONE
	public abstract void getBoolean(); //ZI:DONE
	public abstract void getTrue(); //ZI:DONE
	public abstract void getFalse(); //ZI:DONE
	
	// Test for boolean values ...
	public abstract boolean isEmpty(); //ZI:DONE
	public abstract boolean isBoolean(); //ZI:DONE
	public abstract boolean isTrue(); //ZI:DONE
	public abstract boolean isFalse(); //ZI:DONE
	public abstract boolean isTrueAndFalse(); //ZI:DONE
	
	// Comparison operators ...
	protected abstract void evaluateEQ(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateNEQ(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateGTE(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateLTE(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateGT(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateLT(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateUGTE(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateULTE(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateUGT(AbstractDomain other); //ZI:DONE
	protected abstract void evaluateULT(AbstractDomain other); //ZI:DONE
	
	// Boolean unary operators ...
	protected abstract void evaluateNot();
	
	// Boolean binary operators ...
	protected abstract void evaluateANDBool(AbstractDomain other);
	protected abstract void evaluateORBool(AbstractDomain other);
	
	// Integer unary operators ...
	protected abstract void evaluateMINUS();
	protected abstract void evaluateBV2UINT();
	protected abstract void evaluateBV2INT();
	protected abstract void evaluateINT2BV1();
	protected abstract void evaluateINT2BV8();
	protected abstract void evaluateINT2BV16();
	protected abstract void evaluateINT2BV32();
	protected abstract void evaluateINT2BV64();
	protected abstract void evaluateTRUNC6432();
	protected abstract void evaluateTRUNC6416();
	protected abstract void evaluateTRUNC648();
	protected abstract void evaluateTRUNC641();
	protected abstract void evaluateTRUNC3216();
	protected abstract void evaluateTRUNC328();
	protected abstract void evaluateTRUNC321();
	protected abstract void evaluateTRUNC168();
	protected abstract void evaluateTRUNC161();
	protected abstract void evaluateTRUNC81();
	protected abstract void evaluateZEXT18();
	protected abstract void evaluateZEXT116();
	protected abstract void evaluateZEXT132();
	protected abstract void evaluateZEXT164();
	protected abstract void evaluateZEXT816();
	protected abstract void evaluateZEXT832();
	protected abstract void evaluateZEXT864();
	protected abstract void evaluateZEXT1632();
	protected abstract void evaluateZEXT1664();
	protected abstract void evaluateZEXT3264();
	protected abstract void evaluateSEXT18();
	protected abstract void evaluateSEXT116();
	protected abstract void evaluateSEXT132();
	protected abstract void evaluateSEXT164();
	protected abstract void evaluateSEXT816();
	protected abstract void evaluateSEXT832();
	protected abstract void evaluateSEXT864();
	protected abstract void evaluateSEXT1632();
	protected abstract void evaluateSEXT1664();
	protected abstract void evaluateSEXT3264();
	
	// Integer binary operators ...
	protected abstract void evaluatePLUS(AbstractDomain other);
	protected abstract void evaluateMINUS(AbstractDomain other);
	protected abstract void evaluateMULT(AbstractDomain other);
	protected abstract void evaluateDIV(AbstractDomain other);
	protected abstract void evaluateUDIV(AbstractDomain other);
	protected abstract void evaluateMOD(AbstractDomain other);
	protected abstract void evaluateANDInt(AbstractDomain other);
	protected abstract void evaluateORInt(AbstractDomain other);
	protected abstract void evaluateXOR(AbstractDomain other);
	protected abstract void evaluateLSHIFT(AbstractDomain other);
	protected abstract void evaluateRSHIFT(AbstractDomain other);
	protected abstract void evaluateARSHIFT(AbstractDomain other);
	protected abstract void evaluateSREM(AbstractDomain other);
	protected abstract void evaluateUREM(AbstractDomain other);

}
