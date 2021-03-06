package compiler488.ast.expn;

import compiler488.ast.PrettyPrinter;
import compiler488.ast.SourceCoord;

/**
 * The common features of binary expressions.
 */
public abstract class BinaryExpn extends Expn {
	/** Left operand of the binary operator. */
	protected Expn left;

	/** Right operand of the binary operator. */
	protected Expn right;

	/** Symbol of the operator.
	 *
	 * <p>Must be a <code>static final</code> constant defined in a subclass of
	 * BinaryExpn.</p>
	 */
	protected String opSymbol;

	protected BinaryExpn(String opSymbol, Expn left, Expn right, SourceCoord sourceCoord) {
		super(sourceCoord);

		this.opSymbol = opSymbol;
		left.setParentNode(this);
		this.left = left;
		right.setParentNode(this);
		this.right = right;
	}

	public String getOpSymbol() {
		return opSymbol;
	}

	public Expn getLeft() {
		return left;
	}

	public Expn getRight() {
		return right;
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.print("(");
		left.prettyPrint(p);
		p.print(") " + opSymbol + " (");
		right.prettyPrint(p);
		p.print(")");
	}
}
