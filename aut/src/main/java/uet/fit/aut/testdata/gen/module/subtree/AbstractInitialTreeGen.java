package uet.fit.aut.testdata.gen.module.subtree;


import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.FunctionCallParser;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.gen.InitialTreeGen;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.SpecialCharacter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public abstract class AbstractInitialTreeGen implements IInitialSubTreeGen {
    protected final static Logger logger = LoggerFactory.getLogger(AbstractInitialTreeGen.class);

    protected ICommonFunctionNode functionNode;
    protected IDataNode root;

    @Override
    public ValueDataNode genInitialTree(VariableNode vCurrentChild, DataNode nCurrentParent) throws Exception {
        return new InitialTreeGen().genInitialTree(vCurrentChild, nCurrentParent);
    }

    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    public IDataNode getRoot() {
        return root;
    }

    public void setRoot(IDataNode root) {
        this.root = root;
    }

//    public static List<String> filterCalledFunctions(ICommonFunctionNode functionNode) {
//        FunctionCallParser parser = new FunctionCallParser();
//        IASTNode fASTNode = null;
//        if (functionNode instanceof FunctionNode) {
//            fASTNode = ((FunctionNode) functionNode).getAST();
//            fASTNode.accept(parser);
//        }
//        if (fASTNode == null) return null;
//        List<IASTExpression> expressions = parser.getExpressions();
//        List<String> calledFunctionsName = new ArrayList<>();
//        for (IASTExpression exp : expressions) {
//            List<String> funcNames = new ArrayList<>();
//            if (exp instanceof IASTFunctionCallExpression){
//                 funcNames = getFuncNames((IASTFunctionCallExpression) exp);
//            } else if (exp instanceof ICPPASTNewExpression){
//                //TODO
//            }
//            String expName = funcNames.get(funcNames.size() -1);
//            if (!calledFunctionsName.contains(expName)) {
//                calledFunctionsName.add(expName);
//            }
//        }
//        return calledFunctionsName;
//    }

    protected List<ICommonFunctionNode> filterCalledFunctions(ICommonFunctionNode sut, List<ICommonFunctionNode> functionNodes) {
        final List<String> functionNames = new ArrayList<>();

        if (sut instanceof IFunctionNode) {
            IASTNode fASTNode = ((IFunctionNode) sut).getAST();
            FunctionCallParser parser = new FunctionCallParser();
            fASTNode.accept(parser);

            for (IASTExpression expr : parser.getExpressions()) {
                if (expr instanceof IASTFunctionCallExpression) {
//                    String name = ((IASTFunctionCallExpression) expr).getFunctionNameExpression().getRawSignature();
                    List<String> funcNames = getFuncNames((IASTFunctionCallExpression) expr);
                    String name = funcNames.get(funcNames.size() - 1);
                    name = simplifyName(name);
                    functionNames.add(name);
                } else if (expr instanceof ICPPASTNewExpression) {
                    IASTTypeId typeId = ((ICPPASTNewExpression) expr).getTypeId();
                    IASTNamedTypeSpecifier specifier = ((IASTNamedTypeSpecifier) typeId.getDeclSpecifier());
                    String qualifiedName = specifier.getName().toString();
                    String name = simplifyName(qualifiedName);
                    functionNames.add(name);
                }
            }

            for (IASTSimpleDeclaration decl : parser.getUnexpectedCalledFunctions()) {
                String funcName = decl.getDeclarators()[0].getName().toString();
                funcName = simplifyName(funcName);
                functionNames.add(funcName);
            }
        }

        return functionNodes.stream()
                .filter(f -> functionNames.contains(f.getSingleSimpleName()))
                .collect(Collectors.toList());
    }

    private static List<String> getFuncNames(IASTFunctionCallExpression funcExpression) {
        List<String> funcNames = new ArrayList<>();
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public int visit(IASTName name) {
                if (! (name instanceof ICPPASTQualifiedName))
                    funcNames.add(name.getRawSignature());
                return super.visit(name);
            }
        };

        visitor.shouldVisitNames = true;
        funcExpression.getFunctionNameExpression().accept(visitor);
        return funcNames;
    }

    private String simplifyName(String name) {
        String[] segments = name.split(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
        return segments[segments.length - 1];
    }
}
