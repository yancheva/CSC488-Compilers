package compiler488.ast.decl;

import compiler488.ast.ASTVisitable;
import compiler488.ast.ASTVisitor;
import compiler488.ast.BaseAST;

/**
 * The common features of declarations' parts.
 */
public abstract class DeclarationPart extends BaseAST implements ASTVisitable {
    /** The name of the thing being declared. */
    protected String name;

    public DeclarationPart(String name) {
        super();

        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
	public void accept(ASTVisitor visitor) {
		visitor.visit(this);
	}
}
