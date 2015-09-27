/**
 * 
 */
package org.sinnlabs.dbvim.evaluator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.sinnlabs.dbvim.db.Value;
import org.sinnlabs.dbvim.evaluator.exceptions.ParseException;
import org.sinnlabs.dbvim.ui.IField;

/**
 *  An evaluator that is able to evaluate boolean expressions on field values.
 * @author peter.liverovsky
 *
 */
public class DatabaseConditionBuilder {
	
	private final ConditionTokenizer tokenizer;
	private final Map<String, Function> functions;
	private final Map<String, List<Operator>> operators;
	private final Map<String, Constant> constants;
	private final String functionArgumentSeparator;
	private final Map<String, BracketPair> functionBrackets;
	private final Map<String, BracketPair> expressionBrackets;
	
	/** A constant that represents NULL value */
	public static final Constant NULL = new Constant("$NULL$");

	/** The negate unary operator in the standard operator precedence.*/
	public static final Operator NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 5, "-");
	/** The negate unary operator in the Excel like operator precedence.*/
	public static final Operator NEGATE_HIGH = new Operator("-", 1, Operator.Associativity.RIGHT, 7, "-");
	/** The substraction operator.*/
	public static final Operator MINUS = new Operator("-", 2, Operator.Associativity.LEFT, 3, "-");
	/** The addition operator.*/
	public static final Operator PLUS = new Operator("+", 2, Operator.Associativity.LEFT, 3, "+");
	/** The multiplication operator.*/
	public static final Operator MULTIPLY = new Operator("*", 2, Operator.Associativity.LEFT, 4, "*");
	/** The division operator.*/
	public static final Operator DIVIDE = new Operator("/", 2, Operator.Associativity.LEFT, 4, "/");
	/** The <a href="http://en.wikipedia.org/wiki/Modulo_operation">modulo operator</a>.*/
	public static final Operator MODULO = new Operator("%", 2, Operator.Associativity.LEFT, 4, "%");
	/** The equality operator */
	public static final Operator EQ = new Operator("=", 2, Operator.Associativity.LEFT, 2, "=");
	/** The not equality operator */
	public static final Operator NOT_EQ = new Operator("!=", 2, Operator.Associativity.LEFT, 2, "<>");
	/** The grate then operator */
	public static final Operator GT = new Operator(">", 2, Operator.Associativity.LEFT, 2, ">");
	/** The grate or equal operator */
	public static final Operator GE = new Operator(">=", 2, Operator.Associativity.LEFT, 2, ">=");
	/** The less then operator */
	public static final Operator LT = new Operator("<", 2, Operator.Associativity.LEFT, 2, "<");
	/** The less or equal operator */
	public static final Operator LE = new Operator("<=", 2, Operator.Associativity.LEFT, 2, "<=");
	/** The LIKE operator */
	public static final Operator LIKE = new Operator("LIKE", 2, Operator.Associativity.LEFT, 2, "LIKE");
	/** The logical AND operator */
	public static final Operator AND = new Operator("AND", 2, Operator.Associativity.LEFT, 1, "AND");
	/** The logical OR operator */
	public static final Operator OR = new Operator("OR", 2, Operator.Associativity.LEFT, 1, "OR");
	/** The logical NOT operator */
	public static final Operator NOT = new Operator("NOT", 1, Operator.Associativity.RIGHT, 2, "NOT");
	
	/** The standard whole set of predefined operators */
	private static final Operator[] OPERATORS = new Operator[]{NEGATE, MINUS, PLUS, MULTIPLY,
		DIVIDE, MODULO, EQ, NOT_EQ, GT, GE, LT, LE, LIKE, AND, OR, NOT};
	
	/** The whole set of predefined constants */
	private static final Constant[] CONSTANTS = new Constant[]{NULL};

	
	private static Parameters DEFAULT_PARAMETERS;
	private static final ThreadLocal<NumberFormat> FORMATTER = new ThreadLocal<NumberFormat>() {
	  @Override
	  protected NumberFormat initialValue() {
	  	return NumberFormat.getNumberInstance(Locale.US);
	  }
	};
	
	private static Parameters getParameters() {
		if (DEFAULT_PARAMETERS == null) {
			DEFAULT_PARAMETERS = getDefaultParameters();
		}
		return DEFAULT_PARAMETERS;
	}
	
	/** Gets a copy of DoubleEvaluator default parameters.
	 * <br>The returned parameters contains all the predefined operators, functions and constants.
	 * <br>Each call to this method create a new instance of Parameters. 
	 * @return a Paramaters instance
	 */
	private static Parameters getDefaultParameters() {
		Parameters result = new Parameters();
		result.addOperators(Arrays.asList(OPERATORS));
		//result.addFunctions(Arrays.asList(FUNCTIONS));
		result.addConstants(Arrays.asList(CONSTANTS));
		result.addFunctionBracket(BracketPair.PARENTHESES);
		result.addExpressionBracket(BracketPair.PARENTHESES);
		return result;
	}
	
	public DatabaseConditionBuilder() {
		Parameters parameters = getParameters();
		final ArrayList<String> tokenDelimitersBuilder = new ArrayList<String>();
		this.functions = new HashMap<String, Function>();
		this.operators = new HashMap<String, List<Operator>>();
		this.constants = new HashMap<String, Constant>();
		this.functionBrackets = new HashMap<String, BracketPair>();
		for (final BracketPair pair : parameters.getFunctionBrackets()) {
			functionBrackets.put(pair.getOpen(), pair);
			functionBrackets.put(pair.getClose(), pair);
			tokenDelimitersBuilder.add(pair.getOpen());
			tokenDelimitersBuilder.add(pair.getClose());
		}
		this.expressionBrackets = new HashMap<String, BracketPair>();
		for (final BracketPair pair : parameters.getExpressionBrackets()) {
			expressionBrackets.put(pair.getOpen(), pair);
			expressionBrackets.put(pair.getClose(), pair);
			tokenDelimitersBuilder.add(pair.getOpen());
			tokenDelimitersBuilder.add(pair.getClose());
		}
		if (operators!=null) {
			for (Operator ope : parameters.getOperators()) {
				tokenDelimitersBuilder.add(ope.getSymbol());
				List<Operator> known = this.operators.get(ope.getSymbol());
				if (known==null) {
					known = new ArrayList<Operator>();
					this.operators.put(ope.getSymbol(), known);
				}
				known.add(ope);
				if (known.size()>1) {
					validateHomonyms(known);
				}
			}
		}
		boolean needFunctionSeparator = false;
		if (parameters.getFunctions()!=null) {
			for (Function function : parameters.getFunctions()) {
				this.functions.put(parameters.getTranslation(function.getName()), function);
				if (function.getMaximumArgumentCount()>1) {
					needFunctionSeparator = true;
				}
			}			
		}
		if (parameters.getConstants()!=null) {
			for (Constant constant : parameters.getConstants()) {
				this.constants.put(parameters.getTranslation(constant.getName()), constant);
			}
		}
		functionArgumentSeparator = parameters.getFunctionArgumentSeparator();
		if (needFunctionSeparator) {
			tokenDelimitersBuilder.add(functionArgumentSeparator);
		}
		tokenizer = new ConditionTokenizer('.', tokenDelimitersBuilder);
	}
	
	/** Validates that homonym operators are valid.
	 * <br>Homonym operators are operators with the same name (like the unary - and the binary - operators)
	 * <br>This method is called when homonyms are passed to the constructor.
	 * <br>This default implementation only allows the case where there's two operators, one binary and one unary.
	 * Subclasses can override this method in order to accept others configurations. 
	 * @param operators The operators to validate.
	 * @throws IllegalArgumentException if the homonyms are not compatibles.
	 * @see #guessOperator(Token, List)
	 */
	protected void validateHomonyms(List<Operator> operators) {
		if (operators.size()>2) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Builds condition expression for prepared statement
	 * @param expression - Expression to be processed
	 * @param values - List of all form values
	 * @param sorted - Prepared sorted list of values for PreparedStatement
	 * @return - where clause string
	 * @throws ParseException 
	 */
	public String buildCondition(String expression, List<IField<?>> fields, List<Value<?>> sorted) throws ParseException {
		String condition = "";
		final Iterator<String> tokens = tokenize(expression);
		Token previous = null;
		Token leftOperand = null;
		int brackets = 0;
		
		while (tokens.hasNext()) {
			// read one token from the input stream
			String strToken = tokens.next();
			final Token token = toToken(previous, strToken, fields);
			if (token.isOpenBracket()) {
				// If the token is a left parenthesis, then push it onto the stack.
				condition += "(";
				brackets++;
				if (previous!=null && previous.isFunction()) {
					if (!functionBrackets.containsKey(token.getBrackets().getOpen())) {
						throw new IllegalArgumentException("Invalid bracket after function: "+strToken);
					}
				} else {
					if (!expressionBrackets.containsKey(token.getBrackets().getOpen())) {
						throw new IllegalArgumentException("Invalid bracket in expression: "+strToken);
					}
				}
			} else if (token.isCloseBracket()) {
				if (previous==null || brackets==0) {
					throw new IllegalArgumentException("expression can't start with a close bracket");
				}
				if (previous.isFunctionArgumentSeparator()) {
					throw new IllegalArgumentException("argument is missing");
				}
				condition += ")";
				brackets--;
			} else if (token.isFunctionArgumentSeparator()) {
				if (previous==null) {
					throw new IllegalArgumentException("expression can't start with a function argument separator");
				}
				// Verify that there was an argument before this separator
				if (previous.isOpenBracket() || previous.isFunctionArgumentSeparator()) {
					// The cases were operator miss an operand are detected elsewhere.
					throw new IllegalArgumentException("argument is missing");
				}
				// If the token is a function argument separator
				condition+=",";
			} else if (token.isFunction()) {
				// If the token is a function token, then push it onto the stack.
				condition+=token.getFunction().getName();
			} else if (token.isOperator()) {
				// If the token is an operator, op1, then:
				condition+=" " + token.getOperator().getDbSymbol();
				leftOperand = previous;
			} else if (token.isField()) {
				// If token is a field
				condition+=" " + token.getField().getMapping();
			} else {
				// If the token is a number (identifier), a constant or a variable, then add its value to the output queue.
				if ((previous!=null) && previous.isLiteral()) {
					throw new IllegalArgumentException("A literal can't follow another literal");
				}
				Value<?> v = toValue(token, null, previous, leftOperand);
				condition += " ?";
				sorted.add(v);
				//output(values, token, evaluationContext);
			}
			previous = token;
		}
		if (brackets != 0) {
			throw new IllegalArgumentException("Invalid bracket in expression.");
		}
		return condition;
	}
	
	@SuppressWarnings("unchecked")
	protected Value<?> toValue(Token token, Object evaluationContext,
			Token previous, Token lastField) {
		
		if (token.isLiteral()) { // If the token is a literal, a constant, or a variable name
			String literal = token.getLiteral();
			Constant ct = this.constants.get(literal);
			Value<?> value = convert(ct, previous, lastField);
			if (value==null && evaluationContext!=null && (evaluationContext instanceof AbstractVariableSet)) {
				value = ((AbstractVariableSet<Value<?>>)evaluationContext).get(literal);
			}
			if (value != null) {
				return value;
			}
			if (lastField.isField()) {
				Value<?> v = convert(literal, lastField.getField());
				if (v == null) {
					throw new IllegalArgumentException("Incorrect value for field " + 
							lastField.getLiteral() + " value: " + literal);
				}
				return v;
			}
		} else {
			throw new IllegalArgumentException();
		}
		return null;
	}
	
	protected Value<?> convert(String s, IField<?> field) {
		return field.fromString(s);
	}
	
	protected Value<?> convert(Constant c, Token previous, Token lastField) {
		if (NULL.equals(c) && lastField.isField()) {
			return convert((String)null, lastField.getField());
		}
		return null;
	}

	protected Token toToken(Token previous, String token, List<IField<?>> fields) {
		if (token.equals(functionArgumentSeparator)) {
			return Token.FUNCTION_ARG_SEPARATOR;
		} else if (functions.containsKey(token)) {
			return Token.buildFunction(functions.get(token));
		} else if (operators.containsKey(token)) {
			List<Operator> list = operators.get(token);
			return (list.size()==1) ? Token.buildOperator(list.get(0)) : Token.buildOperator(guessOperator(previous, list));
		} else {
			final BracketPair brackets = getBracketPair(token);
			if (brackets!=null) {
				if (brackets.getOpen().equals(token)) {
					return Token.buildOpenToken(brackets);
				} else {
					return Token.buildCloseToken(brackets);
				}
			} else if (token.startsWith("'") && token.endsWith("'")) {
					String fname = token.substring(1, token.length()-1);
					for (IField<?> f : fields) {
						if (f.getId().equals(fname))
							return Token.buildFieldToken(f);
					}
					throw new IllegalArgumentException("Field not found: " + fname);
			} else if (token.startsWith("\"") && token.endsWith("\"")) {
				String lit = token.substring(1, token.length()-1);
				return Token.buildLiteral(lit);
			} else {
				return Token.buildLiteral(token);
			}
		}
	}
	
	/** When a token can be more than one operator (homonym operators), this method guesses the right operator.
	 * <br>A very common case is the - sign in arithmetic computation which can be an unary or a binary operator, depending
	 * on what was the previous token. 
	 * <br><b>Warning:</b> maybe the arguments of this function are not enough to deal with all the cases.
	 * So, this part of the evaluation is in alpha state (method may change in the future).
	 * @param previous The last parsed tokens (the previous token in the infix expression we are evaluating). 
	 * @param candidates The candidate tokens.
	 * @return A token
	 * @see #validateHomonyms(List)
	 */
	protected Operator guessOperator(Token previous, List<Operator> candidates) {
		final int argCount = ((previous!=null) && (previous.isCloseBracket() || previous.isLiteral())) ? 2 : 1;
		for (Operator operator : candidates) {
			if (operator.getOperandCount()==argCount) {
				return operator;
			}
		}
		return null;
	}
	
	private BracketPair getBracketPair(String token) {
		BracketPair result = expressionBrackets.get(token);
		return result==null ? functionBrackets.get(token) : result;
	}

	/** Converts the evaluated expression into tokens.
	 * <br>Example: The result for the expression "<i>-1+min(10,3)</i>" is an iterator on "-", "1", "+", "min", "(", "10", ",", "3", ")".
	 * <br>By default, the operators symbols, the brackets and the function argument separator are used as delimiter in the string.
	 * @param expression The expression that is evaluated
	 * @return A string iterator.
	 * @throws ParseException 
	 */
	protected Iterator<String> tokenize(String expression) throws ParseException {
		return tokenizer.tokenize(expression);
	}
}
