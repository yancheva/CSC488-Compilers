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
		System.out.println("=========================");
		System.out.println("     Instrs Emitted:     ");
		System.out.println("=========================");

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

		System.out.println("=========================");
		System.out.println();
	}

	/*
	 * Emit halt instruction
	 */
	public void emitHalt() {
		instrs.add(Machine.HALT);
	}

	/*
	 * Emit instructions for pushing a constant on to the stack.
	 */
	public void emitPushValue(short value) {
		instrs.add(Machine.PUSH);
		instrs.add(value);
	}

	/*
	 * Emit instructions for pushing a constant on to the stack.
	 */
	public void emitPushValue(int value) {
		emitPushValue((short)value);
	}

	/*
	 * Emit instructions for pushing a constant on to the stack N times.
	 * Where N is allowed to be any integer >= 0.
	 */
	public void emitPushValue(int value, int numDuplicates) {
		if (numDuplicates < 0) {
			throw new IllegalArgumentException("numDuplicates must be >= 0");
		}

		switch (numDuplicates) {
			case 0:
				break;
			case 1:
				emitPushValue(value);
				break;
			case 2:
				emitPushValue(value);
				instrs.add(Machine.DUP);
				break;
			case 3:
				emitPushValue(value);
				instrs.add(Machine.DUP);
				instrs.add(Machine.DUP);
				break;

			default:
				emitPushValue(value);
				emitPushValue(numDuplicates);
				instrs.add(Machine.DUPN);
		}
	}

	/*
	 * Emit instructions to push a single TRUE or FALSE on to the stack.
	 */
	public void emitPushBoolValue(boolean value) {
		if (value) {
			emitPushValue(Machine.MACHINE_TRUE);
		} else {
			emitPushValue(Machine.MACHINE_FALSE);
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
				emitPushValue(numToPop);
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
	 * Emit an unconditional branch instruction. Assumes address is already on
	 * the stack.
	 */
	public void emitBranch() {
		instrs.add(Machine.BR);
	}

	/*
	 * Emit instructions that push a branch address and unconditional branch
	 * instruction on the stack. Return index of branch address for later
	 * patching.
	 */
	public short emitBranchToUnknown() {
		// Insert branch location
		short brLocation = (short)(instrs.size() + 1);
		emitPushValue(Machine.UNDEFINED);

		// Insert branch instruction
		emitBranch();

		return brLocation;
	}

	/*
	 * Emit instructions that push a branch address and branch-if-false
	 * instruction on the stack. Return index of branch address for later
	 * patching.
	 */
	public short emitBranchIfFalseToUnknown() {
		// Insert branch location
		short brLocation = (short)(instrs.size() + 1);
		emitPushValue(Machine.UNDEFINED);

		// Insert branch instruction
		instrs.add(Machine.BF);

		return brLocation;
	}

	/*
	 * Emit instructions to print a single character to the screen.
	 */
	public void emitPrintChar(char charVal) {
		emitPushValue((short)charVal);
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
	 * Emit instructions to print an integer to the screen.
	 */
	public void emitPrintInt() {
		instrs.add(Machine.PRINTI);
	}

	/*
	 * Emit instructions to print a newline to the screen.
	 */
	public void emitPrintSkip() {
		emitPrintChar('\n');
	}

	/*
	 * Emit instructions to perform an addition.
	 */
	public void emitAdd() {
		instrs.add(Machine.ADD);
	}

	/*
	 * Emit instructions to perform an addition.
	 */
	public void emitSubtract() {
		instrs.add(Machine.SUB);
	}

	/*
	 * Emit instructions to perform an addition.
	 */
	public void emitMultiply() {
		instrs.add(Machine.MUL);
	}

	/*
	 * Emit instructions to perform an addition.
	 */
	public void emitDivide() {
		instrs.add(Machine.DIV);
	}

	/*
	 * Emit instructions to negate an integer.
	 */
	public void emitNegateInt() {
		instrs.add(Machine.NEG);
	}

	/*
	 * Emit instructions to perform an addition.
	 */
	public void emitOr() {
		instrs.add(Machine.OR);
	}

	/*
	 * Emit instructions to check the top two items on the stack for equality.
	 */
	public void emitEquals() {
		instrs.add(Machine.EQ);
	}

	/*
	 * Emit instructions to perform a not operation on the top of the stack.
	 */
	public void emitNot() {
		emitPushValue(Machine.MACHINE_FALSE);
		emitEquals();
	}

	/*
	 * Emit instructions to load a value from memory.
	 */
	public void emitLoad() {
		instrs.add(Machine.LOAD);
	}

	/*
	 * Emit instructions to push the address of the activation record referenced
	 * in the given display register, plus the given offset.
	 */
	public void emitGetAddr(short disp, short offset) {
		instrs.add(Machine.ADDR);
		instrs.add(disp);
		instrs.add(offset);
	}

	/*
	 * Emit instructions to push the address of the given identifier as seen
	 * from the given scope.
	 */
	public void emitGetVarAddr(STScope scope, String name) {
		VarAddress addr = scope.getVarAddress(name);
		emitGetAddr(addr.getLexicalLevel(), addr.getOrderNumber());
	}

	/*
	 * Emit instructions to load the current value of the identifier with the
	 * given name, as seen from the given scope.
	 */
	public void emitLoadVar(STScope scope, String name) {
		emitGetVarAddr(scope, name);
		emitLoad();
	}

	/*
	 * Emit instructions to store the value at the top of the stack at the
	 * memory address indicated by the value right below it on the stack.
	 */
	public void emitStore() {
		instrs.add(Machine.STORE);
	}

	/*
	 * Emit instructions to set the display register for the given lexical
	 * level to whatever is currently on the top of the stack.
	 */
	public void emitSetDisplay(short lexicalLevel) {
		instrs.add(Machine.SETD);
		instrs.add(lexicalLevel);
	}

	/*
	 * Emit instructions to set the display register for the given lexical
	 * level to the current stack pointer.
	 */
	public void emitSetDisplayToStackPointer(short lexicalLevel) {
		instrs.add(Machine.PUSHMT);
		emitSetDisplay(lexicalLevel);
	}

	/*
	 * Emit instructions to push an activation record on to the stack. Return
	 * the index in the instrs array where the return value is, to be patched
	 * later.
	 */
	public short emitActivationRecord(ActivationRecord ar, short returnAddress) {
		activationRecords.push(ar);

		// Set display
		short lexicalLevel = ar.getLexicalLevel();
		emitSetDisplayToStackPointer(lexicalLevel);

		// Return value if applicable
		if (ar.hasReturnValue()) {
			emitPushValue(Machine.UNDEFINED);
		}

		// Return address
		emitPushValue(returnAddress);
		short returnAddrIndex = (short)(instrs.size() - 1);

		// Space for block mark and local vars
		int blockMark = ar.getNumWordsToAllocateForBlockMark();
		int localStorage = ar.getNumWordsToAllocateForLocalStorage();
		emitPushValue(Machine.UNDEFINED, blockMark + localStorage);

		return returnAddrIndex;
	}

	/*
	 * Emit instructions to push an activation record on to the stack. Return
	 * the index in the instrs array where the return value is, to be patched
	 * later.
	 */
	public short emitActivationRecord(ActivationRecord ar, int returnAddress) {
		return emitActivationRecord(ar, (short)returnAddress);
	}

	/*
	 * Emit instructions to push an activation record on to the stack. Return
	 * the index in the instrs array where the return value is, to be patched
	 * later.
	 */
	public short emitActivationRecord(ActivationRecord ar) {
		return emitActivationRecord(ar, Machine.UNDEFINED);
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

	/*
	 * Update the value at the given index to be the offset to the current end
	 * of the instruction list.
	 */
	public void fixForwardBranchToCurrentLocation(short index) {
		instrs.set(index, (short)instrs.size());
	}

	/*
	 * Removes the last emitted instruction if it's a LOAD. Throws an exception
	 * otherwise.
	 */
	public void removeLastEmittedLoad() {
		// Check if the last instruction is a LOAD
		int lastIndex = instrs.size() - 1;
		short instr = instrs.get(lastIndex);
		if (instr != Machine.LOAD) {
			throw new UnsupportedOperationException("Last instruction is not a LOAD");
		}

		// Remove this instruction
		instrs.remove(lastIndex);
	}
}
