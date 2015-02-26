package compiler488.ast.expn;

import compiler488.ast.ASTList;
import compiler488.ast.ASTVisitor;
import compiler488.ast.PrettyPrinter;
import compiler488.ast.SourceCoord;
import compiler488.symbol.SymbolTable;
import compiler488.symbol.SymbolTable.SymbolType;

/**
 * Represents a function call with arguments.
 */
public class FunctionCallExpn extends Expn {
	/** The name of the function. */
	private String ident;

	/** The arguments passed to the function. */
	private ASTList<Expn> arguments;

	public FunctionCallExpn(String ident, ASTList<Expn> arguments, SourceCoord sourceCoord) {
		super(sourceCoord);

		this.ident = ident;
		arguments.setParentNode(this);
		this.arguments = arguments;
	}

	public ASTList<Expn> getArguments() {
		return arguments;
	}

	public String getIdent() {
		return ident;
	}

	public void prettyPrint(PrettyPrinter p) {
		p.print(ident);

		if (arguments.size() > 0) {
			p.print("(");
			arguments.prettyPrintCommas(p);
			p.print(")");
		}
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.enterVisit(this);

		if (arguments.size() > 0) {
			arguments.accept(visitor);
		}

		visitor.exitVisit(this);
	}

	@Override
	public SymbolType getExpnType(SymbolTable st) {
		if (this.expnType == null) {
			this.expnType = this.getSTETypeOrUnknown(st, this.ident);
		}
		return this.expnType;
	}
}
