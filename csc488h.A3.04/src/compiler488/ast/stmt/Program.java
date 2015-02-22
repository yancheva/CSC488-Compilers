package compiler488.ast.stmt;

import compiler488.ast.ASTVisitor;
import compiler488.ast.PrettyPrinter;

/**
 * Placeholder for the scope that is the entire program
 */
public class Program extends Scope {
    public Program(Scope scope) {
        super(scope.getBody());
    }

    @Override
    public void prettyPrint(PrettyPrinter p) {
        super.prettyPrint(p);
        p.println("");
    }
    
    @Override
	public void accept(ASTVisitor visitor) {
    	
    	// S00: start program scope
		visitor.visit(this);
		
		super.accept(visitor);
		
		// S01: end program scope
		visitor.visit(this);
	}
}
