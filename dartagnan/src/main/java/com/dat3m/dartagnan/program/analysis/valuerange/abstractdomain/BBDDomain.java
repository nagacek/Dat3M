package com.dat3m.dartagnan.program.analysis.valuerange.abstractdomain;

import java.math.BigInteger;

import com.dat3m.dartagnan.expression.op.COpBin;

public class BBDDomain extends SimpleDomain {

	@Override
	public AbstractDomain getEmptyCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractDomain getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void copy(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unionWith(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void intersectWith(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void complement() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restrict(COpBin op, BigInteger compareValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restrict(COpBin op, AbstractDomain compareValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getEmptyInstance() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getFullRange() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getElementFromConst(BigInteger constant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getBoolean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getTrue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getFalse() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBoolean() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTrue() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFalse() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTrueAndFalse() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void evaluateEQ(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateNEQ(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateGTE(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateLTE(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateGT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateLT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateUGTE(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateULTE(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateUGT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateULT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateNot() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateANDBool(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateORBool(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateMINUS() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateBV2UINT() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateBV2INT() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateINT2BV1() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateINT2BV8() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateINT2BV16() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateINT2BV32() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateINT2BV64() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC6432() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC6416() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC648() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC641() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC3216() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC328() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC321() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC168() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC161() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateTRUNC81() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT18() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT116() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT132() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT164() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT816() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT832() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT864() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT1632() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT1664() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateZEXT3264() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT18() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT116() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT132() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT164() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT816() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT832() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT864() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT1632() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT1664() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSEXT3264() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluatePLUS(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateMINUS(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateMULT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateDIV(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateUDIV(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateMOD(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateANDInt(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateORInt(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateXOR(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateLSHIFT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateRSHIFT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateARSHIFT(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateSREM(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void evaluateUREM(AbstractDomain other) {
		// TODO Auto-generated method stub
		
	}

}
