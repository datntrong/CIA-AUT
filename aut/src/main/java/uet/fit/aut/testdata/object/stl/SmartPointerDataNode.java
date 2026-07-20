package uet.fit.aut.testdata.object.stl;


import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.IConstructorExpanableDataNode;
import uet.fit.aut.util.*;

public abstract class SmartPointerDataNode extends STLDataNode implements IConstructorExpanableDataNode  {
    @Override
    public ICommonFunctionNode getSelectedConstructor() {
        return selectedConstructor;
    }

    private ICommonFunctionNode selectedConstructor;

    public String getTemplateArgument() {
        return arguments.get(0);
    }

    public abstract String[] getConstructors();

//    @Override
//    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
//        return "/* Not support to assert smart pointer */";
//    }

    public String[] getConstructorsWithTemplateArgument() {
        String[] constructors = getConstructors();

        String[] result = new String[constructors.length];

        String argument = getTemplateArgument();
        argument = argument.replaceAll(IRegex.ARRAY_INDEX, IRegex.POINTER);

        for (int i = 0; i < constructors.length; i++) {
            String constructor = constructors[i].replaceAll("\\bT\\b", argument);
            result[i] = constructor;
        }

        return result;
    }

//    public DefinitionFunctionNode[] generateConstructorDefinitions() {
//        String[] constructors = getConstructors();
//
//        DefinitionFunctionNode[] result = new DefinitionFunctionNode[constructors.length];
//
//        for (int i = 0; i < constructors.length; i++) {
//            String statement = getType() + SpecialCharacter.SPACE + constructors[i];
//            statement = statement.replaceAll("\\bT\\b", getTemplateArgument());
//            IASTNode astNode = Utils.convertToIAST(statement);
//
//            if (astNode instanceof IASTDeclarationStatement) {
//                astNode = ((IASTDeclarationStatement) astNode).getDeclaration();
//            }
//
//            if (astNode instanceof CPPASTSimpleDeclaration) {
//                DefinitionFunctionNode functionNode = new DefinitionFunctionNode();
//                functionNode.setAST((CPPASTSimpleDeclaration) astNode);
//                functionNode.setName(functionNode.getNewType());
//                result[i] = functionNode;
//            }
//        }
//
//        return result;
//    }


    public void setSelectedConstructor(ICommonFunctionNode selectedConstructor) {
        this.selectedConstructor = selectedConstructor;
    }

    public void chooseConstructor(String constructor) throws Exception {
        String returnType = getRealType();
        returnType = VariableTypeUtils.removeRedundantKeyword(returnType);
        returnType = TemplateUtils.deleteTemplateParameters(returnType);

        IASTDeclarationStatement statement = ASTUtils.generateDeclarationStatement(returnType, constructor);
        IASTDeclaration declaration = statement.getDeclaration();

        if (declaration instanceof CPPASTSimpleDeclaration) {
            DefinitionFunctionNode functionNode = new DefinitionFunctionNode();

            VariableNode variableNode = getCorrespondingVar();
            if (variableNode != null) {
                functionNode.setAbsolutePath(variableNode.getAbsolutePath());
                String decl = declaration.getRawSignature().replace(SpecialCharacter.END_OF_STATEMENT, SpecialCharacter.EMPTY);
                INode existConstructor = variableNode.getChildren().stream().filter(c -> c instanceof DefinitionFunctionNode
                        && ((DefinitionFunctionNode) c).getAST().getRawSignature().startsWith(decl))
                        .findFirst()
                        .orElse(null);

                if (existConstructor == null) {
                    variableNode.getChildren().add(functionNode);
                    functionNode.setParent(variableNode);
                }
            }

            functionNode.setAST((CPPASTSimpleDeclaration) declaration);
            functionNode.setName(functionNode.getNewType());


//            for (INode child : functionNode.getArguments()) {
//                String path = getCorrespondingVar().getAbsolutePath() + File.separator + child.getName();
//                child.setAbsolutePath(path);
//            }

            selectedConstructor = functionNode;
        }
    }

//    public void setConstructor(DefinitionFunctionNode constructor) throws Exception {
//        for (IVariableNode node : constructor.getArguments()) {
//            ValueDataNode child = new InitialArgTreeGen().genInitialTree((VariableNode) node, this);
//
//            String path = getCorrespondingVar().getAbsolutePath() + File.separator + child.getName();
//
//            child.getCorrespondingVar().setAbsolutePath(path);
//        }
//    }
}
