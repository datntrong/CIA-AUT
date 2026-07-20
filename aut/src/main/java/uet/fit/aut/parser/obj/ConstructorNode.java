package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a constructor
 *
 * @author DucAnh
 */
public class ConstructorNode extends AbstractFunctionNode implements IExplicitlyFunction {

    @Override
    public List<IVariableNode> getArguments() {
        if (this.arguments == null || this.arguments.size() == 0) {
            this.arguments = new ArrayList<>();

            for (INode child : getChildren())
                if (child instanceof VariableNode) {
                    this.arguments.add((IVariableNode) child);
                }
        }
        return this.arguments;
    }

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
