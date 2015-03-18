package compiler488.ast.decl;

import compiler488.ast.ASTVisitor;
import compiler488.ast.PrettyPrinter;
import compiler488.ast.SourceCoord;
import compiler488.symbol.SymbolTable.SymbolKind;

/**
 * Holds the declaration part of an array.
 */
public class ArrayDeclPart extends DeclarationPart {
	/** The lower bound of dimension 1. */
	private Integer lb1;

	/** The upper bound of dimension 1. */
	private Integer ub1;

	/** The lower bound of dimension 2 (if any.) */
	private Integer lb2 = null;

	/** The upper bound of dimension 2 (if any.) */
	private Integer ub2 = null;

	/** True iff this is an 2D array */
	private Boolean isTwoDimensional = false;

	public ArrayDeclPart(String name, Integer lb1, Integer ub1, SourceCoord sourceCoord) {
		super(name, sourceCoord);

		this.lb1 = lb1;
		this.ub1 = ub1;

		this.isTwoDimensional = false;
		this.lb2 = null;
		this.ub2 = null;
	}

	public ArrayDeclPart(String name, Integer lb1, Integer ub1, Integer lb2, Integer ub2, SourceCoord sourceCoord) {
		this(name, lb1, ub1, sourceCoord);

		this.isTwoDimensional = true;
		this.lb2 = lb2;
		this.ub2 = ub2;
	}

	public ArrayDeclPart(String name, Integer[] dim1, SourceCoord sourceCoord) {
		this(name, dim1[0], dim1[1], sourceCoord);
	}

	public ArrayDeclPart(String name, Integer[] dim1, Integer[] dim2, SourceCoord sourceCoord) {
		this(name, dim1[0], dim1[1], dim2[0], dim2[1], sourceCoord);
	}

	/**
	 * Returns a string that describes the array.
	 */
	@Override
	public String toString() {
		return name + "[" + lb1 + ".." + ub1 +
			   ( isTwoDimensional ?  ", " + lb2 + ".." + ub2 : "" )
			   + "]";
	}

	@Override
	public SymbolKind getKind() {
		return SymbolKind.ARRAY;
	}

	public int getSize() {
		int size = (this.ub1 - this.lb1 + 1);
		if (this.isTwoDimensional) {
			size *= (this.ub2 - this.lb2 + 1);
		}

		return size;
	}

	public Integer getLowerBoundary1() {
		return lb1;
	}

	public Integer getUpperBoundary1() {
		return ub1;
	}

	public Integer getLowerBoundary2() {
		// Confirm correct use
		assert isTwoDimensional;

		return lb2;
	}

	public Integer getUpperBoundary2() {
		// Confirm correct use
		assert isTwoDimensional;

		return ub2;
	}

	public boolean isTwoDimensional() {
		return isTwoDimensional;
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.print(name + "[" + lb1 + ".." + ub1);

		if (isTwoDimensional) {
			p.print(", " + lb2 + ".." + ub2);
		}

		p.print("]");
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.enterVisit(this);
		visitor.exitVisit(this);
	}
}
