package uet.fit.aut.util;

import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.*;

import java.util.List;

public class FunctionPointerUtils {

    public static boolean match(IFunctionPointerTypeNode tn, ICommonFunctionNode fn) {
        // compare paramaters
        String[] argumentTypes = tn.getArgumentTypes();
        int size = argumentTypes.length;

        if (size != fn.getArguments().size())
            return false;

        VariableNode[] arguments = new VariableNode[size];

        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = new VariableNode();
            arguments[i].setRawType(argumentTypes[i]);

            String fpType = arguments[i].getRealType();
            fpType = VariableTypeUtils.deleteStorageClassesExceptConst(fpType);

            IVariableNode fnVar = fn.getArguments().get(i);
            String fnType = fnVar.getRealType();
            fnType = VariableTypeUtils.deleteStorageClassesExceptConst(fnType);
            if (!fpType.equals(fnType)) {
                return false;
            }
        }

        // compare return type
        INode fileNode = Utils.getSourcecodeFile(tn);

        List<Level> space = new VariableSearchingSpace(fileNode).getSpaces();

        String fpReturnType = VariableTypeUtils.getRealType(space, tn.getReturnType());
        fpReturnType = VariableTypeUtils.deleteStorageClassesExceptConst(fpReturnType);

        String fnReturnType = VariableTypeUtils.getRealType(space, fn.getReturnType());
        fnReturnType = VariableTypeUtils.deleteStorageClassesExceptConst(fnReturnType);

        return fnReturnType.equals(fpReturnType);
    }

    public static DefinitionFunctionNode toFunctionNode(FunctionPointerTypeNode tn) {
        DefinitionFunctionNode fn = null;

        StringBuilder stmBuilder = new StringBuilder();
        stmBuilder.append(tn.getReturnType())
                .append(SpecialCharacter.SPACE)
                .append(tn.getFunctionName())
                .append("(");
        for (String argType : tn.getArgumentTypes())
            stmBuilder.append(argType).append(",");
        stmBuilder.append(")");
        String declarationStm = stmBuilder.toString().replace(",)", ")");

        IASTNode ast = Utils.convertToIAST(declarationStm);
        if (ast instanceof IASTDeclarationStatement) {
            ast = ((IASTDeclarationStatement) ast).getDeclaration();
            if (ast instanceof CPPASTSimpleDeclaration) {
                fn = new DefinitionFunctionNode();
                fn.setAST((CPPASTSimpleDeclaration) ast);
            }
        }

        return fn;
    }
}
