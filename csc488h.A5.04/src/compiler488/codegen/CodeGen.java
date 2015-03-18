package compiler488.codegen;

import java.io.*;
import java.util.*;
import compiler488.ast.*;
import compiler488.ast.decl.*;
import compiler488.ast.expn.*;
import compiler488.ast.stmt.*;
import compiler488.ast.type.*;
import compiler488.compiler.*;
import compiler488.codegen.ActivationRecord.*;
import compiler488.runtime.*;
import compiler488.symbol.*;
import compiler488.symbol.SymbolTable.*;
import compiler488.symbol.STScope.ScopeKind;

/**	  CodeGenerator.java
 *<pre>
 *  Code Generation Conventions
 *
 *  To simplify the course project, this code generator is
 *  designed to compile directly to pseudo machine memory
 *  which is available as the private array memory[]
 *
 *  It is assumed that the code generator places instructions
 *  in memory in locations
 *
 *	  memory[ 0 .. startMSP - 1 ]
 *
 *  The code generator may also place instructions and/or
 *  constants in high memory at locations (though this may
 *  not be necessary)
 *	  memory[ startMLP .. Machine.memorySize - 1 ]
 *
 *  During program exection the memory area
 *	  memory[ startMSP .. startMLP - 1 ]
 *  is used as a dynamic stack for storing activation records
 *  and temporaries used during expression evaluation.
 *  A hardware exception (stack overflow) occurs if the pointer
 *  for this stack reaches the memory limit register (mlp).
 *
 *  The code generator is responsible for setting the global
 *  variables:
 *	  startPC		 initial value for program counter
 *	  startMSP		initial value for msp
 *	  startMLP		initial value for mlp
 * </pre>
 * @author <B>
 *	  Haohan Jiang (g3jiangh)
 *	  Maria Yancheva (c2yanche)
 *	  Timo Vink (c4vinkti)
 *	  Chandeep Singh (g2singh)
 * </B>
 */
public class CodeGen extends BaseASTVisitor
{
	/** flag for tracing code generation */
	private boolean trace = Main.traceCodeGen ;

	/** helper to emit code **/
	private CodeGenHelper instrs;

	/** Symbol table from semantic analysis **/
	private SymbolTable symbolTable;

	/**
	 * Constructor to initialize code generation
	 */
	public CodeGen(SymbolTable st)
	{
		symbolTable = st;
		instrs = new CodeGenHelper();
	}

	/**
	 *  Write generated code to machine memory and set the PC, MSP, and MLP.
	 *
	 *  @throws MemoryAddressException  from Machine.writeMemory
	 *  @throws ExecutionException	  from Machine.writeMemory
	 */
	public void writeToMachine()
		throws MemoryAddressException, ExecutionException
	{
		// Trace emitted instruction
		if (trace) {
			instrs.printDebug();
		}

		// Write instructions to machine memory
		short counter = 0;
		List<Short> instructions = instrs.getInstructions();
		for (short instr : instructions) {
			Machine.writeMemory(counter, instr);
			counter++;
		}

		// Set initial values for registers
		Machine.setPC((short)1);
		Machine.setMSP((short)(counter + 1));
		Machine.setMLP((short)(Machine.memorySize - 1));
	}

	@Override
	public void enterVisit(Program program)
	{
		instrs.emitHalt();
		ActivationRecord ar = new ActivationRecord(program.getSTScope());
		instrs.emitActivationRecord(ar, 0);
	}

	@Override
	public void exitVisit(Program program)
	{
		instrs.emitActivationRecordCleanUp();
		instrs.emitBranch();
	}

	@Override
	public void exitVisit(PutStmt putStmt)
	{
		int numIntExpns = putStmt.getNumChildrenOfType(symbolTable, SymbolType.INTEGER);
		instrs.emitPrintInt(numIntExpns);
	}

	@Override
	public void exitVisitLHS(AssignStmt assignStmt)
	{
		instrs.removeLastEmittedLoad();
	}

	@Override
	public void exitVisit(AssignStmt assignStmt)
	{
		instrs.emitStore();
	}

	@Override
	public void exitVisit(TextConstExpn textConstExpn)
	{
		instrs.emitPrintText(textConstExpn.getValue());
	}

	@Override
	public void exitVisit(SkipConstExpn skipConstExpn)
	{
		instrs.emitPrintSkip();
	}

	@Override
	public void exitVisit(ArithExpn arithExpn)
	{
		String operation = arithExpn.getOpSymbol();
		switch (operation) {
			case ArithExpn.OP_PLUS:
				instrs.emitAdd();
				break;
			case ArithExpn.OP_MINUS:
				instrs.emitSubtract();
				break;
			case ArithExpn.OP_TIMES:
				instrs.emitMultiply();
				break;
			case ArithExpn.OP_DIVIDE:
				instrs.emitDivide();
				break;

			default:
				String msg = "Unknown ArithExpn operation: " + operation;
				throw new UnsupportedOperationException(msg);
		}
	}

	@Override
	public void exitVisit(UnaryMinusExpn unaryMinusExpn)
	{
		instrs.emitNegateInt();
	}

	@Override
	public void enterVisit(BoolExpn boolExpn)
	{
		if (boolExpn.getOpSymbol() == BoolExpn.OP_AND) {
			// A and B <=> not(not A or not B)
			// Set up the outer not, and the not for A
			instrs.emitPushBoolValue(true);
			instrs.emitPushBoolValue(true);
		}
	}

	@Override
	public void exitVisitLHS(BoolExpn boolExpn)
	{
		if (boolExpn.getOpSymbol() == BoolExpn.OP_AND) {
			// Complete not for A, set up not for B
			instrs.emitSubtract();
			instrs.emitPushBoolValue(true);
		}
	}

	@Override
	public void exitVisit(BoolExpn boolExpn)
	{
		String operation = boolExpn.getOpSymbol();
		switch (operation) {
			case BoolExpn.OP_OR:
				instrs.emitOr();
				break;
			case BoolExpn.OP_AND:
				// Complete not for B, OR reults, and complete outer not
				instrs.emitSubtract();
				instrs.emitOr();
				instrs.emitSubtract();
				break;

			default:
				String msg = "Unknown BoolExpn operation: " + operation;
				throw new UnsupportedOperationException(msg);
		}
	}

	@Override
	public void enterVisit(NotExpn notExpn)
	{
		instrs.emitPushBoolValue(true);
	}

	@Override
	public void exitVisit(NotExpn notExpn)
	{
		instrs.emitSubtract();
	}

	@Override
	public void exitVisit(IntConstExpn intConstExpn)
	{
		instrs.emitPushValue(intConstExpn.getValue());
	}

	@Override
	public void exitVisit(BoolConstExpn boolConstExpn)
	{
		instrs.emitPushBoolValue(boolConstExpn.getValue());
	}

	@Override
	public void exitVisit(IdentExpn identExpn) {
		// Get the symbol table entry
		STScope scope = identExpn.getContainingSTScope();
		String ident = identExpn.getIdent();
		SymbolTableEntry ste = symbolTable.searchGlobalFrom(ident, scope);

		if (ste.getKind() == SymbolKind.FUNCTION) {
			// Function call without parameters
			throw new UnsupportedOperationException("Not implemented yet");
		}
		else {
			// Variable or parameter
			instrs.emitLoadVar(scope, ident);
		}
	}
}
