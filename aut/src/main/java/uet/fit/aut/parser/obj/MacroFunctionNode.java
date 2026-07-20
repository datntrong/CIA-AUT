package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDeclarator;
import uet.fit.aut.config.IFunctionConfig;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Instrument a function-like macro
 */
public class MacroFunctionNode extends CustomASTNode<IASTPreprocessorFunctionStyleMacroDefinition> implements ICommonFunctionNode {

    private IFunctionNode correspondingFunctionNode; // a fake function node
    private IFunctionConfig functionConfig;
    private int nAdditionalOffsetInBody = 0;

    private String rewriteName(IASTPreprocessorFunctionStyleMacroDefinition originalMacroNode) {
        String macroContent = originalMacroNode.getRawSignature().replace("\\\n", " \n").replace("\\\r", " \n");
        String newName = macroContent.substring(0, macroContent.indexOf("(")).replaceFirst("#define", "");
        newName = "void" + newName + "(";
        IASTFunctionStyleMacroParameter[] parameters = originalMacroNode.getParameters();
        for (IASTFunctionStyleMacroParameter parameter : parameters) {
            newName += "auto " + parameter.getParameter() + ",";
        }
        newName = newName.substring(0, newName.lastIndexOf(","));
        newName += ")";
        return newName;
    }

    private String rewriteBodyOfMacroFunction(String macroContent) {
        String body = macroContent.substring(macroContent.indexOf(")") + 1);
        String tmpBody = body.trim();
        if (!tmpBody.startsWith("{")) {
            body = "{" + body;
            nAdditionalOffsetInBody++;
        }
        if (!tmpBody.endsWith("}"))
            if (tmpBody.endsWith(";"))
                body += "}";
            else
                body += ";}";
        return body;
    }

    //TODO: convertMacroFunctionToRealFunction

    /**
     * Convert a function-like macro to a real function by modifying its content
     * <p>
     * Ex:
     * "#define my_macro(a) if (a>0) return 1; else return 0;"
     * ------->
     * "void my_macro(auto a) {if (a>0) return 1; else return 0;}"
     *
     * @param originalMacroNode
     * @return
     */
    public IASTFunctionDefinition convertMacroFunctionToRealFunction(IASTPreprocessorFunctionStyleMacroDefinition originalMacroNode) {
        String macroContent = originalMacroNode.getRawSignature().replace(LINE_BREAK_IN_MACRO, " ");
        String originalName = macroContent.substring(0, macroContent.indexOf(")") + 1);

        // rewrite the name of macro
        // Ex: "#define my_macro(a)" --> "void my_macro(auto a)"
        String newName = rewriteName(originalMacroNode);

        // rewrite body
        String newBody = rewriteBodyOfMacroFunction(macroContent);

        // merge new name with new body
        String newContent = newName + newBody;

        // generate new AST of new content
        int nAdditionalOffsetInName = newName.length() - originalName.length() ;
        int startOffset = originalMacroNode.getFileLocation().getNodeOffset() - (nAdditionalOffsetInName + nAdditionalOffsetInBody);
        newContent = Utils.insertSpaceToFunctionContent(originalMacroNode.getFileLocation().getStartingLineNumber(),
                startOffset, newContent);
        IASTTranslationUnit newAST = null;
        try {
            newAST = Utils.getIASTTranslationUnitforCpp(newContent.toCharArray());
            if (newAST.getChildren()[0] instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition newFunctionAST = (IASTFunctionDefinition) newAST.getChildren()[0];

                final boolean[] foundProblem = {false};
                ASTVisitor visitor = new ASTVisitor() {

                    @Override
                    public int visit(IASTProblem name) {
                        foundProblem[0] = true;
                        return ASTVisitor.PROCESS_ABORT;
                    }
                };
                visitor.shouldVisitProblems = true;
                newFunctionAST.accept(visitor);

                if (foundProblem[0]) {
                    throw new Exception("Problem in AST. So ignore this macro function.");
                }

                return newFunctionAST;

            } else
                throw new Exception("Problem in AST. So ignore this macro function.");

//            String source = postProcessor(newFunctionAST);
//            newAST = new SourcecodeFileParser().getIASTTranslationUnit(source.toCharArray());
//            newFunctionAST = (IASTFunctionDefinition) newAST.getChildren()[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String postProcessor(IASTFunctionDefinition ast) {
        IASTFunctionDeclarator declarator = ast.getDeclarator();
        String source = ast.getRawSignature();
        if (declarator instanceof ICPPASTFunctionDeclarator) {
            ICPPASTParameterDeclaration[] params = ((ICPPASTFunctionDeclarator) declarator).getParameters();
            for (ICPPASTParameterDeclaration param : params) {
                IASTNode name = param.getDeclarator().getName();
                source = replaceProblem(name, source);
            }

        } else if (declarator instanceof CASTFunctionDeclarator) {
            IASTParameterDeclaration[] params = ((CASTFunctionDeclarator) declarator).getParameters();

            for (IASTParameterDeclaration param : params) {
                IASTNode name = param.getDeclarator().getName();
                source = replaceProblem(name, source);
            }
        }

        return source;
    }

    private String replaceProblem(IASTNode node, String sourse) {
        String name = node.getRawSignature();
        String regex = "#(\\s*\\Q" + name + "\\E)";
        sourse = sourse.replaceAll(regex, " $1");
        return sourse;
    }


    @Override
    public void setAST(IASTPreprocessorFunctionStyleMacroDefinition aST) {
        super.setAST(aST);

        setName(AST.getName().getBinding().toString());

        for (IASTFunctionStyleMacroParameter parameter : AST.getParameters()) {
            VariableNode arg = new VariableNode();

            arg.setAST(parameter);

            arg.setParent(this);
            getChildren().add(arg);

            arg.setAbsolutePath(getAbsolutePath() + File.separator + arg.getName());
        }
    }

    @Override
    public IFunctionConfig getFunctionConfig() {
        return null;
    }

    @Override
    public void setFunctionConfig(IFunctionConfig functionConfig) {

    }

    @Override
    public List<IVariableNode> getArgumentsAndGlobalVariables() {
        return getArguments();
    }

    @Override
    public List<IVariableNode> getArguments() {
        List<IVariableNode> arguments = new ArrayList<>();

        for (INode child:getChildren())
            if (child instanceof IVariableNode)
                arguments.add((IVariableNode) child);

        return arguments;
    }

    @Override
    public List<IVariableNode> getPassingVariables() {
        List<IVariableNode> passingVariables = new ArrayList<>();

        passingVariables.addAll(getArguments());

        passingVariables.addAll(getExternalVariables());

        return passingVariables;
    }

    @Override
    public String getReturnType() {
        return MACRO_UNDEFINE_TYPE;
    }

    @Override
    public String getSimpleName() {
        return AST.getName().getLastName().getRawSignature();
    }

    @Override
    public String getSingleSimpleName() {
        return AST.getName().getLastName().getRawSignature();
    }

    @Override
    public boolean isTemplate() {
        return false;
    }

    @Override
    public int getVisibility() {
        return ICPPASTVisibilityLabel.v_public;
    }

    @Override
    public void setVisibility(int v) {

    }

    @Override
    public String toString() {
        return AST.toString();
    }

    public IFunctionNode getCorrespondingFunctionNode() {
        if (correspondingFunctionNode == null) {
            try {
                IASTFunctionDefinition iastFunctionDefinition = convertMacroFunctionToRealFunction(getAST());

                FunctionNode functionNode = new FunctionNode();
                functionNode.setAST(iastFunctionDefinition);
                functionNode.setAbsolutePath(this.getAbsolutePath());
                functionNode.setParent(getParent());

                this.correspondingFunctionNode = functionNode;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return correspondingFunctionNode;
    }

    @Override
    public List<IVariableNode> getExternalVariables() {
        return new ArrayList<>();
    }

    @Override
    public boolean isMethod() {
        return false;
    }

    @Override
    public boolean hasVoidPointerArgument() {
        return false;
    }

    @Override
    public boolean hasFunctionPointerArgument() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    public static final String MACRO_UNDEFINE_TYPE = "__MACRO_UNDEFINE_TYPE__";
    public static final String LINE_BREAK_IN_MACRO = "\\";
}
