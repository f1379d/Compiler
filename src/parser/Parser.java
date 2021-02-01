package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import token.Token;
import exceptions.AnalyzerException;



public class Parser {


	public static Terminal epsilon = new Terminal(0, "EPSILON");


	public static Terminal endOfProgram = new Terminal(-1, "ENDOFPROGRAM");


	private NonTerminal startSymbol;


	private List<Rule> rules;


	private Set<Symbol> alphabet;


	private Map<String, Symbol> nameToSymbol;


	private Map<Symbol, Set<Terminal>> firstSet;


	private Map<Symbol, Set<Terminal>> followSet;


	private Map<SimpleEntry<NonTerminal, Terminal>, Symbol[]> parsingTable;


	private Stack<Terminal> input;


	private List<Rule> sequenceOfAppliedRules;



	public Parser() {
		rules = new ArrayList<Rule>();
		alphabet = new HashSet<Symbol>();
		nameToSymbol = new HashMap<String, Symbol>();
		alphabet.add(epsilon);
		firstSet = new HashMap<Symbol, Set<Terminal>>();
		followSet = new HashMap<Symbol, Set<Terminal>>();
		parsingTable = new HashMap<SimpleEntry<NonTerminal, Terminal>, Symbol[]>();
		sequenceOfAppliedRules = new ArrayList<Rule>();
	}


	public void parse(File grammarFile, List<Token> list) throws FileNotFoundException,
			AnalyzerException {
		parseRules(grammarFile);
		calculateFirst();
		calculateFollow();
		buildParsingTable();
		input = convertTokensToStack(list);
		performParsingAlgorithm();
	}


	public List<Rule> getSequenceOfAppliedRules() {
		return sequenceOfAppliedRules;
	}


	private void performParsingAlgorithm() throws AnalyzerException {
		Stack<Symbol> stack = new Stack<Symbol>();
		stack.push(endOfProgram);
		stack.push(startSymbol);
		int parsedTokensCount = 0;
		do {

			Symbol stackTop = stack.peek();
			Terminal inputTop = input.peek();
			if (stackTop.isTerminal()) {
				if (stackTop.equals(inputTop)) {
					stack.pop();
					input.pop();
					parsedTokensCount++;
				} else {
					throw new AnalyzerException("Syntax error after token #" + parsedTokensCount,
							parsedTokensCount);
				}
			} else {
				SimpleEntry<NonTerminal, Terminal> tableKey = new SimpleEntry<NonTerminal, Terminal>(
						(NonTerminal) stackTop, inputTop);
				if (parsingTable.containsKey(tableKey)) {
					stack.pop();
					Symbol[] tableEntry = parsingTable.get(tableKey);
					for (int j = tableEntry.length - 1; j > -1; j--) {
						if (!tableEntry[j].equals(epsilon))
							stack.push(tableEntry[j]);
					}
					sequenceOfAppliedRules.add(getRule((NonTerminal) stackTop, tableEntry));
				} else {
					throw new AnalyzerException("Syntax error after token #" + parsedTokensCount,
							parsedTokensCount);
				}
			}
		} while (!stack.isEmpty() && !input.isEmpty());

		if (!input.isEmpty()) {
			throw new AnalyzerException("Syntax error after token #" + parsedTokensCount,
					parsedTokensCount);
		}
	}


	private Stack<Terminal> convertTokensToStack(List<Token> inputTokens) {
		Stack<Terminal> input = new Stack<Terminal>();
		Collections.reverse(inputTokens);
		input.push(endOfProgram);
		for (Token token : inputTokens) {
			Terminal s = (Terminal) nameToSymbol.get(token.getTokenString());
			if (s == null) {
				switch (token.getTokenType()) {
				case Identifier:
					s = (Terminal) nameToSymbol.get("id");
					break;
				case IntConstant:
					s = (Terminal) nameToSymbol.get("intConst");
					break;
				case DoubleConstant:
					s = (Terminal) nameToSymbol.get("doubleConst");
					break;
				default:
					throw new RuntimeException("Somethig is wrong!");
				}
			}
			input.push(s);
		}
		return input;
	}


	private void buildParsingTable() {
		for (Rule r : rules) {
			Symbol[] rightSide = r.getRightSide();
			NonTerminal leftSide = r.getLeftSide();
			Set<Terminal> firstSetForRightSide = first(rightSide);
			Set<Terminal> followSetForLeftSide = followSet.get(leftSide);

			for (Terminal s : firstSetForRightSide) {
				parsingTable.put(new SimpleEntry<NonTerminal, Terminal>(leftSide, s), rightSide);
			}

			if (firstSetForRightSide.contains(epsilon)) {
				for (Terminal s : followSetForLeftSide) {
					parsingTable
							.put(new SimpleEntry<NonTerminal, Terminal>(leftSide, s), rightSide);
				}
			}
		}
	}

	private void calculateFirst() {
		for (Symbol s : alphabet) {
			firstSet.put(s, new HashSet<Terminal>());
		}
		for (Symbol s : alphabet) {
			first(s);
		}
	}


	private void first(Symbol s) {
		Set<Terminal> first = firstSet.get(s);
		Set<Terminal> auxiliarySet;
		if (s.isTerminal()) {
			first.add((Terminal) s);
			return;
		}

		for (Rule r : getRulesWithLeftSide((NonTerminal) s)) {
			Symbol[] rightSide = r.getRightSide();
			first(rightSide[0]);
			auxiliarySet = new HashSet<Terminal>(firstSet.get(rightSide[0]));
			auxiliarySet.remove(epsilon);
			first.addAll(auxiliarySet);

			for (int i = 1; i < rightSide.length
					&& firstSet.get(rightSide[i - 1]).contains(epsilon); i++) {
				first(rightSide[i]);
				auxiliarySet = new HashSet<Terminal>(firstSet.get(rightSide[i]));
				auxiliarySet.remove(epsilon);
				first.addAll(auxiliarySet);
			}

			boolean allContainEpsilon = true;
			for (Symbol rightS : rightSide) {
				if (!firstSet.get(rightS).contains(epsilon)) {
					allContainEpsilon = false;
					break;
				}
			}
			if (allContainEpsilon)
				first.add(epsilon);
		}
	}


	private Set<Terminal> first(Symbol[] chain) {
		Set<Terminal> firstSetForChain = new HashSet<Terminal>();
		Set<Terminal> auxiliarySet;
		auxiliarySet = new HashSet<Terminal>(firstSet.get(chain[0]));
		auxiliarySet.remove(epsilon);
		firstSetForChain.addAll(auxiliarySet);

		for (int i = 1; i < chain.length && firstSet.get(chain[i - 1]).contains(epsilon); i++) {
			auxiliarySet = new HashSet<Terminal>(firstSet.get(chain[i]));
			auxiliarySet.remove(epsilon);
			firstSetForChain.addAll(auxiliarySet);
		}

		boolean allContainEpsilon = true;
		for (Symbol s : chain) {
			if (!firstSet.get(s).contains(epsilon)) {
				allContainEpsilon = false;
				break;
			}
		}
		if (allContainEpsilon)
			firstSetForChain.add(epsilon);

		return firstSetForChain;
	}

	private void calculateFollow() {
		for (Symbol s : alphabet) {
			if (s.isNonTerminal())
				followSet.put(s, new HashSet<Terminal>());
		}

		Map<SimpleEntry<Symbol, Symbol>, Boolean> callTable = new HashMap<SimpleEntry<Symbol, Symbol>, Boolean>();
		for (Symbol firstS : alphabet) {
			for (Symbol secondS : alphabet) {
				callTable.put(new SimpleEntry<Symbol, Symbol>(firstS, secondS), false);
			}
		}

		NonTerminal firstSymbol = rules.get(0).getLeftSide();
		followSet.get(firstSymbol).add(endOfProgram);
		for (Symbol s : alphabet) {
			if (s.isNonTerminal()) {
				follow((NonTerminal) s, null, callTable);
			}
		}
	}


	private void follow(NonTerminal s, Symbol caller,
			Map<SimpleEntry<Symbol, Symbol>, Boolean> callTable) {
		Boolean called = callTable.get(new SimpleEntry<Symbol, Symbol>(caller, s));
		if (called != null) {
			if (called == true)
				return;
			else
				callTable.put(new SimpleEntry<Symbol, Symbol>(caller, s), true);
		}

		Set<Terminal> follow = followSet.get(s);
		Set<Terminal> auxiliarySet;

		List<SimpleEntry<NonTerminal, Symbol[]>> list = getLeftSideRightChain(s);
		for (SimpleEntry<NonTerminal, Symbol[]> pair : list) {
			Symbol[] rightChain = pair.getValue();
			NonTerminal leftSide = pair.getKey();
			if (rightChain.length != 0) {
				auxiliarySet = first(rightChain);
				auxiliarySet.remove(epsilon);
				follow.addAll(auxiliarySet);
				if (first(rightChain).contains(epsilon)) {
					follow(leftSide, s, callTable);
					follow.addAll(followSet.get(leftSide));
				}
			} else {
				follow(leftSide, s, callTable);
				follow.addAll(followSet.get(leftSide));
			}
		}
	}


	private void parseRules(File grammarFile) throws FileNotFoundException {
		nameToSymbol.put("EPSILON", epsilon);

		Scanner data = new Scanner(grammarFile);
		int code = 1;
		int ruleNumber = 0;
		while (data.hasNext()) {
			StringTokenizer t = new StringTokenizer(data.nextLine());
			String symbolName = t.nextToken();
			if (!nameToSymbol.containsKey(symbolName)) {
				Symbol s = new NonTerminal(code, symbolName);
				if (code == 1)
					startSymbol = (NonTerminal) s;
				nameToSymbol.put(symbolName, s);
				alphabet.add(s);
				code++;
			}
			t.nextToken();// ->

			NonTerminal leftSide = (NonTerminal) nameToSymbol.get(symbolName);
			while (t.hasMoreTokens()) {
				List<Symbol> rightSide = new ArrayList<Symbol>();
				do {
					symbolName = t.nextToken();
					if (!symbolName.equals("|")) {
						if (!nameToSymbol.containsKey(symbolName)) {
							Symbol s;
							if (Character.isUpperCase(symbolName.charAt(0)))
								s = new NonTerminal(code++, symbolName);
							else
								s = new Terminal(code++, symbolName);
							nameToSymbol.put(symbolName, s);
							alphabet.add(s);
						}
						rightSide.add(nameToSymbol.get(symbolName));
					}
				} while (!symbolName.equals("|") && t.hasMoreTokens());
				rules.add(new Rule(ruleNumber++, leftSide, rightSide.toArray(new Symbol[] {})));
			}
		}
	}


	private Set<Rule> getRulesWithLeftSide(NonTerminal nonTerminalSymbol) {
		Set<Rule> set = new HashSet<Rule>();
		for (Rule r : rules) {
			if (r.getLeftSide().equals(nonTerminalSymbol))
				set.add(r);
		}
		return set;
	}


	private List<SimpleEntry<NonTerminal, Symbol[]>> getLeftSideRightChain(Symbol s) {
		List<SimpleEntry<NonTerminal, Symbol[]>> list = new ArrayList<SimpleEntry<NonTerminal, Symbol[]>>();
		for (Rule r : rules) {
			Symbol[] rightChain = r.getRightSide();
			int index = Arrays.asList(rightChain).indexOf(s);
			if (index != -1) {
				rightChain = Arrays.copyOfRange(rightChain, index + 1, rightChain.length);
				list.add(new SimpleEntry<NonTerminal, Symbol[]>(r.getLeftSide(), rightChain));
			}
		}
		return list;
	}


	private Rule getRule(NonTerminal leftSide, Symbol[] rightSide) {
		Set<Rule> setOfRules = getRulesWithLeftSide(leftSide);
		for (Rule r : setOfRules) {
			if (rightSide.length != r.getRightSide().length)
				continue;
			for (int i = 0; i < rightSide.length; i++) {
				if (r.getRightSide()[i] != rightSide[i])
					break;
				else {
					if (i == rightSide.length - 1) {
						return r;
					}
				}
			}
		}

		return null;
	}
}
