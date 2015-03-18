package compiler488.codegen;

import java.util.*;
import compiler488.ast.*;
import compiler488.ast.decl.*;
import compiler488.ast.expn.*;
import compiler488.ast.stmt.*;
import compiler488.ast.type.*;
import compiler488.compiler.Main;
import compiler488.runtime.Machine;;
import compiler488.symbol.STScope;

public class CodeGenHelper {
	/** Sizes of control blocks of activation records **/
	private static int FUNC_AR_CTRLBLOCK_SIZE = 4;
	private static int PROC_AR_CTRLBLOCK_SIZE = 3;

	/** flag for tracing code generation */
	private boolean trace = Main.traceCodeGen;

	/** stack of activation records pushed but not cleaned up **/
	private Stack<ActivationRecord> activationRecords;

	/** emitted instructions **/
	private List<Short> instrs;

	public CodeGenHelper() {
		activationRecords = new Stack<ActivationRecord>();
		instrs = new ArrayList<Short>();
	}

	/*
	 * Return all emitted instructions as a list of shorts.
	 */
	public List<Short> getInstructions() {
		return instrs;
	}

	/*
	 * Prints a list of all the instructions emitted to standard out.
	 */
	public void printDebug()
	{
		System.out.println();
		System.out.println("=======================");
		System.out.println("    Instrs Emitted:    ");
		System.out.println("=======================");

		int i = 0;
		int numInstrs = instrs.size();
		while (i < numInstrs) {
			// Get operation name
			int opCode = instrs.get(i);
			String opName = Machine.instructionNames[opCode];

			// Get parameters
			int opLength = Math.max(1, Machine.instructionLength[opCode]);
			String params = "";
			for (int j = 1; j < opLength; j++) {
				Short param = instrs.get(i+j);
				String paramStr = (param == Machine.UNDEFINED) ? "UNDEFINED" : param.toString();
				params += "\t" + paramStr;
			}

			System.out.println(String.format("%05d", i+1) + "\t" + opName + params);
			i += opLength;
		}

		System.out.println("=======================");
		System.out.println();
	}

	/*
	 * Emit instructions for pushing a constant on to the stack.
	 */
	public void emitPush(short value) {
		instrs.add(Machine.PUSH);
		instrs.add(value);
	}

	/*
	 * Emit instructions for pushing a constant on to the stack.
	 */
	public void emitPush(int value) {
		emitPush((short)value);
	}

	/*
	 * Emit instructions for pushing a constant on to the stack N times.
	 * Where N is allowed to be any integer >= 0.
	 */
	public void emitPushN(int value, int numDuplicates) {
		if (numDuplicates < 0) {
			throw new IllegalArgumentException("numDuplicates must be >= 0");
		}

		switch (numDuplicates) {
			case 0:
				break;
			case 1:
				emitPush(value);
				break;
			case 2:
				emitPush(value);
				instrs.add(Machine.DUP);
				break;
			case 3:
				emitPush(value);
				instrs.add(Machine.DUP);
				instrs.add(Machine.DUP);
				break;
				
			default:
				emitPush(value);
				emitPush(numDuplicates);
				instrs.add(Machine.DUPN);
		}
	}

	/*
	 * Emit instructions for popping the top N items from the stack.
	 * Where N is allowed to be any integer >= 0.
	 */
	public void emitPop(int numToPop) {
		if (numToPop < 0) {
			throw new IllegalArgumentException("numToPop must be >= 0");
		}

		switch (numToPop) {
			case 0:
				break;
			case 1:
				instrs.add(Machine.POP);
				break;
			case 2:
				instrs.add(Machine.POP);
				instrs.add(Machine.POP);
				break;

			default:
				emitPush(numToPop);
				instrs.add(Machine.POPN);
		}
	}

	/*
	 * Emit instructions to pop a single item from the stack.
	 */
	public void emitPop() {
		emitPop(1);
	}

	/*
	 * Emit an unconditional branch instruction.
	 */
	public void emitBranch() {
		instrs.add(Machine.BR);
	}

	/*
	 * Emit instructions to print a single character to the screen.
	 */
	public void emitPrintChar(char charVal) {
		emitPush((short)charVal);
		instrs.add(Machine.PRINTC);
	}

	/*
	 * Emit instructions to print an entire string to the screen.
	 */
	public void emitPrintText(String text) {
		for (char ch: text.toCharArray()) {
			emitPrintChar(ch);
		}
	}

	/*
	 * Emit instructions to print a newline to the screen.
	 */
	public void emitPrintSkip() {
		emitPrintChar('\n');
	}

	/*
	 * Emit instructions to push an activation record on to the stack.
	 */
	public void emitActivationRecord(ActivationRecord ar, short returnAddress) {
		activationRecords.push(ar);

		// Return value if applicable
		if (ar.hasReturnValue()) {
			emitPush(Machine.UNDEFINED);
		}

		// Return address
		emitPush(returnAddress);

		// Space for block mark and local vars
		int blockMark = ar.getNumWordsToAllocateForBlockMark();
		int localStorage = ar.getNumWordsToAllocateForLocalStorage();
		emitPushN(Machine.UNDEFINED, blockMark + localStorage);
	}

	/*
	 * Emit instructions to push the activation record on to the stack.
	 */
	public void emitActivationRecord(ActivationRecord ar, int returnAddress) {
		emitActivationRecord(ar, (short)returnAddress);
	}

	/*
	 * Emit instructions to push the activation record on to the stack, given
	 * that we do not know the return address yet. Return the index in the
	 * instrs array where this value needs to be filled in.
	 */
	public int emitActivationRecord(ActivationRecord ar) {
		int curOffset = instrs.size() - 1;
		int returnAddrOffset = instrs.size() + ar.getOffsetToReturnAddress() * 2 - 1;
		emitActivationRecord(ar, Machine.UNDEFINED);

		return returnAddrOffset;
	}

	/*
	 * Emit instructions to pop the program activation record from to the stack.
	 */
	public void emitActivationRecordCleanUp() {
		// Get activation record
		ActivationRecord ar = activationRecords.pop();

		// Remove it from the stack
		int numToPop = ar.getNumWordsToPopForCleanUp();
		emitPop(numToPop);
	}
}
