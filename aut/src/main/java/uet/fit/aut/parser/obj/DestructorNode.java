package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;

public class DestructorNode extends AbstractFunctionNode implements IExplicitlyFunction {
	@Override
	public boolean isDefault() {
		if (getAST() instanceof ICPPASTFunctionDefinition) {
			return ((ICPPASTFunctionDefinition) getAST()).isDefaulted();
		}

		return false;
	}

	@Override
	public boolean isDelete() {
		if (getAST() instanceof ICPPASTFunctionDefinition) {
			return ((ICPPASTFunctionDefinition) getAST()).isDeleted();
		}

		return false;
	}
}
