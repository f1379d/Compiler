package parser;

import java.util.Arrays;


public class Rule {


	private int ruleNumber;


	private NonTerminal leftSide;


	private Symbol[] rightSide;


	public Rule(int ruleNumber, NonTerminal leftSide, Symbol[] rightSide) {
		this.ruleNumber = ruleNumber;
		this.leftSide = leftSide;
		this.rightSide = rightSide;
	}

	public int getRuleNumber() {
		return ruleNumber;
	}

	public NonTerminal getLeftSide() {
		return leftSide;
	}

	public Symbol[] getRightSide() {
		return rightSide;
	}

	@Override
	public String toString() {
		return "Rule number: " + ruleNumber + "| " + leftSide + " -> " + Arrays.toString(rightSide);
	}

}