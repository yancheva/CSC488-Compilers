package compiler488.ast.expn;

import compiler488.ast.ASTList;
import compiler488.ast.ASTVisitor;
import compiler488.ast.PrettyPrinter;
import compiler488.ast.SourceCoord;
import compiler488.ast.stmt.Scope;
import compiler488.ast.stmt.Stmt;
import compiler488.semantics.Semantics;
import compiler488.symbol.SymbolTable;
import compiler488.symbol.SymbolTable.SymbolType;

/**
 * Represents the parameters and instructions associated with a function or
 * procedure.
 */
public class AnonFuncExpn extends Expn {
	/** Execute these statements. */
	private ASTList<Stmt> body;

	/** The expression whose value is yielded. */
	private Expn expn;

	public AnonFuncExpn(ASTList<Stmt> body, Expn expn, SourceCoord sourceCoord) {
		super(sourceCoord);

		body.setParentNode(this);
		this.body = body;
		expn.setParentNode(this);
		this.expn = expn;
	}

	public ASTList<Stmt> getBody() {
		return body;
	}

	public Expn getExpn() {
		return expn;
	}

	public void prettyPrint(PrettyPrinter p) {
		p.println("{");
		p.enterBlock();

		body.prettyPrintNewlines(p);

		p.print("yields ");
		expn.prettyPrint(p);
		p.newline();

		p.exitBlock();
		p.println("}");
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.enterVisit(this);

		// S04: start anonymous function scope
		((Semantics)visitor).getSymbolTable().enterScope();
		
		body.accept(visitor);
		expn.accept(visitor);
		
		// S05: end anonymous function scope
		((Semantics)visitor).getSymbolTable().exitScope();
		
		visitor.exitVisit(this);
	}

	/**
	 * getExpnType : recursively returns the type of the expression of the anon func
	 */
	@Override
	public SymbolType getExpnType(SymbolTable st) {
		if (this.expnType == null) {
			this.expnType = this.expn.getExpnType(st);
		}
		return this.expnType;
	}
}
