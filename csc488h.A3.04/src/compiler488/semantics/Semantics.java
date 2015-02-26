package compiler488.semantics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import compiler488.ast.*;
import compiler488.ast.decl.*;
import compiler488.ast.expn.*;
import compiler488.ast.stmt.*;
import compiler488.ast.type.*;
import compiler488.symbol.*;
import compiler488.symbol.SymbolTable.*;


/*
 * Implement semantic analysis for compiler 488
 * @author <B> Haohan Jiang (g3jiangh)
 *						 Maria Yancheva (c2yanche)
 *						 Timo Vink (c4vinkti)
 *						 Chandeep Singh (g2singh)
 *				 </B>
 */
public class Semantics implements ASTVisitor {
	/*
	 * Flag for tracing semantic analysis.
	 */
	private boolean trace = false;

	/*
	 * File sink for semantic analysis trace.
	 */
	private String traceFile = new String();
	public FileWriter tracer;
	public File f;

	/*
	 * Construct symbol table concurrently with semantic checking
	 */
	private SymbolTable symbolTable;

	/*
	 * Accummulate errors and raise an exception after all semantic rules have
	 */
	private SemanticErrorCollector errors;

	/*
	 * SemanticAnalyzer constructor
	 */
	public Semantics (boolean trace) {
		this.trace = trace;
		symbolTable = new SymbolTable();
		errors = new SemanticErrorCollector();
	}

	/*
	 * Called once by the parser at the end of compilation.
	 */
	public void Finalize() throws SemanticErrorException {
		if (errors.any()) {
			errors.raiseException();
		}
	}

	public SymbolTable getSymbolTable() {
		return symbolTable;
	}

	@Override
	public void enterVisit(BaseAST baseAST) {
	}

	@Override
	public void exitVisit(BaseAST baseAST) {
	}

	@Override
	public void enterVisit(ArrayDeclPart arrayDeclPart) {
		// Check first dimension
		Integer lowerBound = arrayDeclPart.getLowerBoundary1();
		Integer upperBound = arrayDeclPart.getUpperBoundary1();
		if (lowerBound > upperBound) {
			String msg = String.format(
				"Invalid bounds on first dimension of array '%s'. Lower bound '%d' must not be greater than upper bound '%d'.",
				arrayDeclPart.getName(), lowerBound, upperBound
			);
			errors.add(arrayDeclPart.getSourceCoord(), msg);
		};

		// Check second dimension if applicable
		if (arrayDeclPart.isTwoDimensional()) {
			lowerBound = arrayDeclPart.getLowerBoundary2();
			upperBound = arrayDeclPart.getUpperBoundary2();

			if (lowerBound > upperBound) {
				String msg = String.format(
					"Invalid bounds on second dimension of array '%s'. Lower bound '%d' must not be greater than upper bound '%d'.",
					arrayDeclPart.getName(), lowerBound, upperBound
				);
				errors.add(arrayDeclPart.getSourceCoord(), msg);
			}
		}
	}

	@Override
	public void enterVisit(MultiDeclarations multiDeclarations) {
		// Add a symbol for every element in the declaration (into current scope)
		// (The value for the newly inserted elements is blank: in the project
		// language assignment cannot happen simultaneously with declaration)
		SymbolType declType = multiDeclarations.getType().toSymbolType();
		for (DeclarationPart elem : multiDeclarations.getParts()) {
			String elemName = elem.getName();
			SymbolKind elemKind = elem.getKind();

			// Check if identifier already exists in current scope
			SymbolTableEntry searchResult = symbolTable.search(elemName);
			if (searchResult != null) {
				// Detected a re-declaration in same scope
				SourceCoord originalDeclCoord = searchResult.getNode().getSourceCoord();
				String msg = String.format(
					"Redeclaration of '%s' not allowed in same scope. Original declaration at %s.",
					elemName, originalDeclCoord
				);
				errors.add(elem.getSourceCoord(), msg);
			}
			else {
				boolean success = symbolTable.insert(elemName, declType, elemKind, "", elem);
				if (success) {
					elem.setSTEntry(symbolTable.search(elemName));
				} else {
					throw new IllegalStateException("Insertion in symbol table failed.");
				}
			}
		}
	}

	@Override
	public void enterVisit(RoutineDecl routineDecl) {
		/**
		 * S11/S12: Declare function with/without parameters and with specified type
		 * S17/S18: Declare procedure with/without parameters
		 */

		// Record routine declaration in symbol table
		String routineName = routineDecl.getName();
		SymbolType routineType = null;
		SymbolKind routineKind = SymbolKind.PROCEDURE;

		if (routineDecl.isFunctionDecl()) {
			routineType = routineDecl.getType().toSymbolType();
			routineKind = SymbolKind.FUNCTION;

			// S53: check that a function body contains at least one return statement
			Scope routineScope = routineDecl.getBody();
			ReturnStmt returnStatement = routineScope.containsReturn();
			if (returnStatement == null) {
				errors.add(routineDecl.getSourceCoord(), "Function '" + routineName + "' must contain at least one return statement.");
			}
		}

		// Check for existing declaration
		if (symbolTable.search(routineName) != null) {
			// Detected a re-declaration in same scope
			errors.add(routineDecl.getSourceCoord(), "Re-declaration of identifier " + routineName + " not allowed in same scope.");
		}
		else {
			boolean success = symbolTable.insert(routineName, routineType, routineKind, "", routineDecl);
			if (success) {
				routineDecl.setSTEntry(symbolTable.search(routineName));
			} else {
				errors.add(routineDecl.getSourceCoord(), "Unable to declare identifier " + routineName);
			}
		}

		// Begin new scope
		symbolTable.enterScope();
	}

	@Override
	public void exitVisit(RoutineDecl routineDecl) {
		symbolTable.exitScope();
	}

	@Override
	public void enterVisit(ScalarDecl scalarDecl) {
		String declId = scalarDecl.getName();
		SymbolType declType = scalarDecl.getType().toSymbolType();

		// Check if identifier already exists in current scope
		if (symbolTable.search(declId) != null) {
			// Detected a re-declaration in same scope
			errors.add(scalarDecl.getSourceCoord(), "Re-declaration of identifier " + declId + " not allowed in same scope.");
		}
		else {
			boolean success = symbolTable.insert(declId, declType, SymbolKind.PARAMETER, "", scalarDecl);
			if (success) {
				scalarDecl.setSTEntry(symbolTable.search(declId));
			} else {
				errors.add(scalarDecl.getSourceCoord(), "Unable to declare identifier " + declId);
			}
		}
	}

	@Override
	public void enterVisit(ArithExpn arithExpn) {
		assertIsIntExpn(arithExpn);
	}

	@Override
	public void enterVisit(BoolExpn boolExpn) {
		assertIsBoolExpn(boolExpn);
	}

	@Override
	public void enterVisit(CompareExpn compareExpn) {
		assertIsBoolExpn(compareExpn);
	}

	@Override
	public void enterVisit(EqualsExpn equalsExpn) {
		assertIsBoolExpn(equalsExpn);
	}

	@Override
	public void enterVisit(FunctionCallExpn functionCallExpn) {
		// S40: check that identifier has been declared as a function
		String functionName = functionCallExpn.getIdent();
		if (symbolTable.searchGlobal(functionName) == null) {
			errors.add(functionCallExpn.getSourceCoord(), "Function '" + functionName + "' cannot be used before it has been declared.");
		}
		else if (symbolTable.searchGlobal(functionName).getKind() != SymbolKind.FUNCTION) {
			errors.add(functionCallExpn.getSourceCoord(), "Identifier '" + functionName + "' cannot be used as a function because it has been declared as " + symbolTable.searchGlobal(functionName).getKind() + ".");
		}

		// S42/S43: check that the number of parameters declared in the function declaration is the same as the
		// number of arguments passed to the function call expression
		SymbolTableEntry declaredFunc = symbolTable.searchGlobal(functionName);
		RoutineDecl declaredFuncASTNode = (RoutineDecl)declaredFunc.getNode();
		int numArgs = functionCallExpn.getArguments().size();
		int numParams = declaredFuncASTNode.getParameters().size();
		if (numArgs != numParams) {
			errors.add(functionCallExpn.getSourceCoord(), "Function '" + functionName + "' is called with " + numArgs + " arguments, but requires " + numParams + " parameters.");
		}

		// S36: Check that type of argument expression matches type of corresponding formal parameter
		assertArgmtAndParamTypesMatch(functionCallExpn.getArguments(), declaredFuncASTNode.getParameters());
	}

	@Override
	public void enterVisit(IdentExpn identExpn) {
		// S37: check that identifier has been declared as a scalar variable
		// S39: check that identifier has been declared as a parameter
		// S40: check that identifier has been declared as a function
		String identName = identExpn.getIdent();
		SymbolTableEntry identSymbol = symbolTable.searchGlobal(identName);
		if (identSymbol == null) {
			errors.add(identExpn.getSourceCoord(), "Identifier '" + identName + "' cannot be used before it has been declared.");
		}
		else if (identSymbol.getKind() != SymbolKind.VARIABLE &&
				 identSymbol.getKind() != SymbolKind.PARAMETER &&
				 identSymbol.getKind() != SymbolKind.FUNCTION ) {
			errors.add(identExpn.getSourceCoord(), "Identifier '" + identName + "' cannot be used in this context because it has been declared as " + symbolTable.searchGlobal(identName).getKind() + ".");
		}
		else if (identSymbol.getKind() == SymbolKind.FUNCTION) {
			// S42: check that the function has no parameters
			int numParams = ((RoutineDecl)identSymbol.getNode()).getParameters().size();
			if ( numParams > 0 ) {
				errors.add(identExpn.getSourceCoord(), "Function '" + identName + "' is called with no arguments, but requires " + numParams + " parameters.");
			}
		}
	}

	@Override
	public void enterVisit(NotExpn notExpn) {
		assertIsBoolExpn(notExpn);
	}

	@Override
	public void enterVisit(SubsExpn subsExpn) {
		// S38: check that identifier has been declared as an array
		String arrayName = subsExpn.getVariable();
		if (symbolTable.searchGlobal(arrayName) == null) {
			errors.add(subsExpn.getSourceCoord(), "Array '" + arrayName + "' cannot be used before it has been declared.");
		}
		else if (symbolTable.searchGlobal(arrayName).getKind() != SymbolKind.ARRAY) {
			errors.add(subsExpn.getSourceCoord(), "Identifier '" + arrayName + "' cannot be used as an array because it has been declared as " + symbolTable.searchGlobal(arrayName).getKind() + ".");
		}

		// S31: check that subscripts are int expressions
		assertIsIntExpn(subsExpn.getSubscript1());
		if (subsExpn.isTwoDimensional()) {
			assertIsIntExpn(subsExpn.getSubscript2());
		}
	}

	@Override
	public void enterVisit(UnaryMinusExpn unaryMinusExpn) {
		assertIsIntExpn(unaryMinusExpn);
	}

	@Override
	public void exitVisit(AssignStmt assignStmt) {
		// S34: Check that variable and expression in assignment are the same type
		SymbolType lType = assignStmt.getLval().getExpnType(symbolTable);
		SymbolType rType = assignStmt.getRval().getExpnType(symbolTable);
		if (lType != rType) {
			errors.add(
				assignStmt.getRval().getSourceCoord(),
				"LHS type (" + lType + ") in assignment differs from RHS type (" + rType + ")"
			);
		}
	}

	@Override
	public void enterVisit(ExitStmt exitStmt) {
		// Only do S30 check if "exit when"
		if (exitStmt.getExpn() != null) {
			assertIsBoolExpn(exitStmt.getExpn());
		}

		/**
		 * S50 Check that exit statement is directly inside a loop
		 *	Checks if symbols "loop" or "while" have been declared in the scope
		 *	If not, throws an error
		 */
		BaseAST currNode = exitStmt;
		boolean foundLoop = false;
		while(currNode != null)
		{
			if (currNode instanceof LoopStmt || currNode instanceof WhileDoStmt) {
				foundLoop = true;
				break;
			}
			if (currNode instanceof RoutineDecl || currNode instanceof AnonFuncExpn) {
				break;
			}
			currNode = currNode.getParentNode();
		}

		if (!foundLoop){
			errors.add(exitStmt.getSourceCoord(), "EXIT not contained in LOOP or WHILE statements");
		}
	}

	@Override
	public void enterVisit(GetStmt getStmt) {
		// S31: check that variables are integers
		for (Expn expn : getStmt.getInputs()) {
			assertIsIntExpn(expn);
		}
	}

	@Override
	public void enterVisit(IfStmt ifStmt) {
		assertIsBoolExpn(ifStmt.getCondition());
	}

	@Override
	public void enterVisit(ProcedureCallStmt procedureCallStmt) {
		// S41: check that identifier has been declared as a procedure
		String procName = procedureCallStmt.getName();
		if (symbolTable.searchGlobal(procName) == null) {
			errors.add(procedureCallStmt.getSourceCoord(), "Procedure '" + procName + "' cannot be used before it has been declared.");
		}
		else if (symbolTable.searchGlobal(procName).getKind() != SymbolKind.PROCEDURE) {
			errors.add(procedureCallStmt.getSourceCoord(), "Identifier '" + procName + "' cannot be used as a procedure because it has been declared as " + symbolTable.searchGlobal(procName).getKind() + ".");
		}

		// S42/S43: check that the number of parameters declared in the procedure declaration is the same as the
			// number of arguments passed to the procedure call statement
		SymbolTableEntry declaredProc = symbolTable.searchGlobal(procName);
		RoutineDecl declaredProcASTNode = (RoutineDecl)declaredProc.getNode();
		int numArgs = procedureCallStmt.getArguments().size();
		int numParams = declaredProcASTNode.getParameters().size();
		if (numArgs != numParams) {
			errors.add(procedureCallStmt.getSourceCoord(), "Procedure '" + procName + "' is called with " + numArgs + " arguments, but requires " + numParams + " parameters.");
		}

		// S36: Check that type of argument expression matches type of corresponding formal parameter
		assertArgmtAndParamTypesMatch(procedureCallStmt.getArguments(), declaredProcASTNode.getParameters());
	}

	@Override
	public void enterVisit(Program program) {
		symbolTable.enterScope();
	}

	@Override
	public void exitVisit(Program program) {
		symbolTable.exitScope();
	}

	@Override
	public void enterVisit(PutStmt putStmt) {
		// S31: Check that type of expression or variable is integer
		// Also added in check for text/skip
		for (Printable printable : putStmt.getOutputs()) {
			Expn expn = (Expn)printable;
			SymbolType type = expn.getExpnType(symbolTable);
			if (type != SymbolType.INTEGER &&
				type != SymbolType.TEXT &&
				type != SymbolType.SKIP) {
				String msg = String.format("Can't 'put' %s expression. Can only 'put' integer expressions, text, and skips", type);
				errors.add(expn.getSourceCoord(), msg);
			}
		}
	}

	@Override
	public void enterVisit(ReturnStmt returnStmt) {
		// S51-52: Ensure return is in a function or procedure
		BaseAST currNode = returnStmt;
		RoutineDecl parentRoutineDecl = null;
		while(currNode != null && !(currNode instanceof AnonFuncExpn))
		{
			if (currNode instanceof RoutineDecl) {
				parentRoutineDecl = (RoutineDecl)currNode;
				break;
			}
			currNode = currNode.getParentNode();
		}

		if (parentRoutineDecl == null){
			errors.add(returnStmt.getSourceCoord(), "Return statement is not in the scope of a function or procedure.");
		}
		else if (parentRoutineDecl.isFunctionDecl()) {
			// S35: Check that expression type matches the return type of enclosing function
			SymbolType returnStatementType = returnStmt.getValue().getExpnType(symbolTable);
			SymbolType routineType = parentRoutineDecl.getType().toSymbolType();
			if (routineType != returnStatementType) {
				String msg = String.format(
					"Return statement type '%s' does not match function type '%s'.",
					returnStatementType, routineType
				);
				errors.add(returnStmt.getSourceCoord(), msg);
			}
		}
	}

	@Override
	public void enterVisit(WhileDoStmt whileDoStmt) {
		assertIsBoolExpn(whileDoStmt.getExpn());
	}

	// compare expn type to expectedType, and error if not the same
	private void checkExpnType(Expn expn, SymbolType expectedType) {
		SymbolType type = expn.getExpnType(symbolTable);
		if (type != expectedType) {
			String msg = String.format("%s expression expected, but is %s.", expectedType, type);
			errors.add(expn.getSourceCoord(), msg);
		}
	}

	// S30: check that type of expression is boolean
	private void assertIsBoolExpn(Expn expn) {
		checkExpnType(expn, SymbolType.BOOLEAN);
	}

	// S31: check that type of expression is integer
	private void assertIsIntExpn(Expn expn) {
		checkExpnType(expn, SymbolType.INTEGER);
	}

	// S36: Check that type of argument expression matches type of corresponding formal parameter
	private void assertArgmtAndParamTypesMatch(ASTList<Expn> argList, ASTList<ScalarDecl> paramList) {
		Iterator<Expn> argExpnIter = argList.iterator();
		Iterator<ScalarDecl> paramsIter	= paramList.iterator();
		int count = 1;
		while (argExpnIter.hasNext()) {
			Expn nextArg = argExpnIter.next();
			SymbolType aType = nextArg.getExpnType(symbolTable);
			SymbolType pType = paramsIter.next().getSTEntry().getType();
			if (aType != pType) {
				errors.add(
					nextArg.getSourceCoord(),
					"Arg expression " + count + "'s type (" + aType + ") does not match expected param type (" + pType + ")");
			}
			count++;
		}
	}
}
