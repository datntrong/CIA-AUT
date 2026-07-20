package uet.fit.aut.instrument;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIfStatement;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.StaticVariableNode;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static uet.fit.aut.instrument.ProjectClone.MAIN_REFACTOR_NAME;
import static uet.fit.aut.instrument.ProjectClone.MAIN_REGEX;

/**
 * Instrument function for constructor, destructor, normal function (not include macro function)
 * <p>
 * Extend the previous instrumentation function by adding extra information
 * (e.g., the line of statements) to markers. <br/>
 * Ex: int a = 0; ----instrument-----> mark("line 12:int a = 0"); int a = 0;
 * <p>
 * <br/>
 *
 * @author DucAnh
 */
public class FunctionInstrumentationForAllCoverages extends AbstractFunctionInstrumentation {

    protected FunctionInstrumentationForAllCoverages() {
        prefix = "aut_static";
    }

    public FunctionInstrumentationForAllCoverages(IASTFunctionDefinition astFunctionNode, IFunctionNode functionNode) {
        this.functionNode = functionNode;

        this.prefix = "aut_static_" + functionNode.getNewType()
                .replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE)
                .replaceAll("_+", SpecialCharacter.UNDERSCORE);

        if (astFunctionNode != null && astFunctionNode.getFileLocation() != null) {
            this.astFunctionNode = astFunctionNode;
        }
    }

    private int scope = 0;

    private final VariableList varList = new VariableList();

    private final String prefix;

    @Override
    public String generateInstrumentedFunction() {
        try {
            return instrumentUnConstexprFunction();

        } catch (Exception e) {
            e.printStackTrace();
            // return the original function without instrumentation
            return astFunctionNode.getRawSignature();
        }
    }

    private String instrumentUnConstexprFunction() {
        IASTFunctionDefinition astFunctionNode = this.astFunctionNode;
        int functionOffset = astFunctionNode.getFileLocation().getNodeOffset();
        IASTCompoundStatement astBody = (IASTCompoundStatement) astFunctionNode.getBody();
        int bodyOffset = astBody.getFileLocation().getNodeOffset();
        String prototype = astFunctionNode.getRawSignature().substring(0, bodyOffset - functionOffset);
        prototype = prototype.replaceAll(MAIN_REGEX, MAIN_REFACTOR_NAME);
        String body = parseBlock(astBody, null, "");
        String mapStaticToGlobal = mapStaticToGlobal();
        return mapStaticToGlobal + prototype + body;
    }

    private String mapStaticToGlobal() {
        StringBuilder b = new StringBuilder();

        if (STATIC_REFACTOR) {
            for (StaticVariableNode v : functionNode.getStaticVariables()) {
                String declaration = v.getAST().getRawSignature();
                String origin = v.getName();
                String instrument = v.getInstrument();
                declaration = declaration.replaceAll("\\b\\Q" + origin + "\\E\\b", instrument)
                        .replaceAll("^static\\s+", SpecialCharacter.EMPTY)
                        .replaceAll("\\s+static\\s+", " ");

                if (!declaration.endsWith(SpecialCharacter.END_OF_STATEMENT))
                    declaration += SpecialCharacter.END_OF_STATEMENT;
                declaration += SpecialCharacter.LINE_BREAK;

                b.append(declaration);
            }
        }

        return b.toString();
    }

//    private String parseMemberInits(IASTFunctionDefinition astFunctionNode) {
//        StringBuilder builder = new StringBuilder();
//
//        if (astFunctionNode instanceof ICPPASTFunctionDefinition) {
//            ICPPASTConstructorChainInitializer[] memberInits =
//                    ((ICPPASTFunctionDefinition) astFunctionNode).getMemberInitializers();
//
//            if (memberInits.length > 0) {
//                builder.append(" : ");
//                for (int i = 0; i < memberInits.length; i++) {
//                    builder.append(memberInits[i].getRawSignature());
//                    if (i != memberInits.length - 1)
//                        builder.append(", ");
//                    else
//                        builder.append(SpecialCharacter.SPACE);
//                }
//            }
//        }
//
//        return builder.toString();
//    }

    protected String addExtraCall(IASTStatement stm, String extra, String margin) {
        if (extra != null)
            extra = putInMark(extra, true);

        if (stm instanceof IASTCompoundStatement)
            return parseBlock((IASTCompoundStatement) stm, extra, margin);
        else {
            String inside = margin + SpecialCharacter.TAB;

            return SpecialCharacter.OPEN_BRACE + SpecialCharacter.LINE_BREAK + inside /*+ inside*/ +
                    parseStatement(stm, inside) + SpecialCharacter.LINE_BREAK + margin +
                    SpecialCharacter.CLOSE_BRACE;
        }
    }

    protected String parseBlock(IASTCompoundStatement block, String extra, String margin) {
        scope++;

        if (block == null)
            return "";
        StringBuilder b = new StringBuilder("{" + SpecialCharacter.LINE_BREAK);
        String inside = margin + SpecialCharacter.TAB;
        if (extra != null)
            b.append(inside).append(extra).append(SpecialCharacter.LINE_BREAK);

        for (IASTStatement stm : block.getStatements()) {
            String content = stm.getRawSignature();

            if (stm instanceof IASTProblemHolder) {
                if (!content.equals(")")) {
                    // ignore
                    b.append("/* Cant instrument this following code */\n");
                    b.append(content);
                }
            } else if (isMacroExpansion(stm)) {
                b.append(inside)
                        .append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, false, false), true)) // add markers
                        .append(content)
                        .append(SpecialCharacter.LINE_BREAK);
            } else  {
                b.append(inside)
                        .append(parseStatement(stm, inside))
                        .append(SpecialCharacter.LINE_BREAK);
            }
        }

        b.append(margin).append(SpecialCharacter.CLOSE_BRACE);

        if (block.getStatements().length > 0 && !b.toString().contains(SpecialCharacter.END_OF_STATEMENT)) {
            b.append(SpecialCharacter.END_OF_STATEMENT);
        }

        varList.removeAtScope(scope);
        scope--;

        return b.toString();
    }

    private boolean isMacroExpansion(IASTStatement stm) {
        return Arrays.stream(stm.getNodeLocations())
                .filter(Objects::nonNull)
                .anyMatch(l -> l instanceof IASTMacroExpansionLocation);
    }

    private boolean isAssign(IASTNode astNode) {
        while (astNode instanceof IASTUnaryExpression && ((IASTUnaryExpression) astNode).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
            astNode = ((IASTUnaryExpression) astNode).getOperand();
        }

        if (astNode instanceof IASTBinaryExpression) {
            return ((IASTBinaryExpression) astNode).getOperator() == IASTBinaryExpression.op_assign;
        }

        return false;
    }

    protected String parseStatement(IASTStatement stm, String margin) {
        StringBuilder b = new StringBuilder();

        if (stm instanceof IASTCompoundStatement)
            b.append(parseBlock((IASTCompoundStatement) stm, null, margin));

        else {
            if (isMacroExpansion(stm)) {
                b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, false, false), true)) // add markers
                        .append(stm.getRawSignature());
            } else if (stm instanceof IASTLabelStatement) {
                IASTStatement nestedStm = ((IASTLabelStatement) stm).getNestedStatement();
                String origin = stm.getRawSignature();
                String markNestedStm = parseStatement(nestedStm, SpecialCharacter.EMPTY);
                String markStm = origin.replace(nestedStm.getRawSignature(), markNestedStm);
                b.append(markStm);

            } else if (stm instanceof IASTIfStatement) {
                IASTIfStatement astIf = (IASTIfStatement) stm;
                IASTStatement astElse = astIf.getElseClause();
                IASTNode cond = astIf.getConditionExpression();
                IASTNode decla = ((CPPASTIfStatement) stm).getConditionDeclaration();

                if (cond != null) {
                    b.append("if (")
                            .append(putInMark(addContentOfMarkFunction(cond, astFunctionNode, functionPath, true, false), false))
                            .append(" && (").append(createMarkForSubCondition(cond)).append(")) ");
                } else if (decla != null) {
                    b.append(putInMark(addContentOfMarkFunction(decla, astFunctionNode, functionPath, true, true), false)).append(";");
                    String declaStr = refactorStatic(decla.getRawSignature());
                    b.append("if (").append(declaStr).append(")");
                }

                b.append(addExtraCall(astIf.getThenClause(), null, margin));

                if (astElse != null) {
                    b.append(SpecialCharacter.LINE_BREAK).append(margin).append("else ");
                    b.append(addExtraCall(astElse, null, margin));
                }

            } else if (stm instanceof IASTForStatement) {
                IASTForStatement astFor = (IASTForStatement) stm;

                // Add marker for initialization
                IASTStatement astInit = astFor.getInitializerStatement();
                if (!(astInit instanceof IASTNullStatement)) {
                    b.append(putInMark(addContentOfMarkFunction(astInit, astFunctionNode, functionPath, false, false), true));
                }

                String init = refactorStatic(getShortenContent(astInit));
                b.append("for (").append(init);
                // Add marker for condition
                IASTExpression astCond = (IASTExpression) Utils.shortenAstNode(astFor.getConditionExpression());
                if (astCond != null) {
                    //b.append(SpecialCharacter.LINE_BREAK).append("\t\t\t");
                    b.append(putInMark(addContentOfMarkFunction(astCond, astFunctionNode, functionPath, true, false), false)).append(" && ")
                            .append(createMarkForSubCondition(astCond)).append(";");
                } else
                    b.append(";");

                // Add marker for increment
                IASTExpression astIter = astFor.getIterationExpression();
                if (astIter != null) {
                    String iter = refactorStatic(getShortenContent(astIter));
                    //b.append(SpecialCharacter.LINE_BREAK).append("\t\t\t");
                    b.append("({")
                            .append(putInMark(addContentOfMarkFunction(astIter, astFunctionNode, functionPath, false, false), false))
                            .append(";")
                            .append(iter)
                            .append(";})");
                }
                b.append(") ");

                // For loop: no condition
                if (astCond == null)
                    b.append(parseStatement(astFor.getBody(), margin));
                else
                    b.append(addExtraCall(astFor.getBody(), null, margin));

            } else if (stm instanceof IASTWhileStatement) {
                IASTWhileStatement astWhile = (IASTWhileStatement) stm;
                IASTNode cond = astWhile.getCondition();

                b.append("while (")
                        .append(putInMark(addContentOfMarkFunction(cond, astFunctionNode, functionPath, true, false), false))
                        .append(" && (").append(createMarkForSubCondition(cond)).append(")) ");

                b.append(addExtraCall(astWhile.getBody(), null, margin));

            } else if (stm instanceof IASTDoStatement) {
                IASTDoStatement astDo = (IASTDoStatement) stm;
                IASTNode cond = astDo.getCondition();

                boolean isAssign = isAssign(cond);

                b.append("do ").append(addExtraCall(astDo.getBody(), null, margin)).append(SpecialCharacter.LINE_BREAK)
                        .append(margin).append("while (");

                if (!isAssign) {
                    b.append(putInMark(addContentOfMarkFunction(cond, astFunctionNode, functionPath, true, false), false))
                            .append(" && (");
                }

                b.append(createMarkForSubCondition(cond));

                if (!isAssign) {
                    b.append(")");
                }

                b.append(");");

            } else if (stm instanceof ICPPASTTryBlockStatement) {
                ICPPASTTryBlockStatement astTry = (ICPPASTTryBlockStatement) stm;

                b.append(DriverConstant.MARK + "(\"start try;\");");

                b.append(SpecialCharacter.LINE_BREAK).append(margin).append("try ");
                b.append(addExtraCall(astTry.getTryBody(), null, margin));

                for (ICPPASTCatchHandler catcher : astTry.getCatchHandlers()) {
                    b.append(SpecialCharacter.LINE_BREAK).append(margin).append("catch (");

                    String exception = catcher.isCatchAll() ? "..." : getShortenContent(catcher.getDeclaration());
                    exception = refactorStatic(exception);
                    b.append(exception).append(") ");

                    b.append(addExtraCall(catcher.getCatchBody(), null, margin)); // TODO: old extra = exception
                }

                b.append(SpecialCharacter.LINE_BREAK).append(margin).append(DriverConstant.MARK + "(\"end catch;\");");

            } else if (stm instanceof IASTBreakStatement || stm instanceof IASTContinueStatement) {
                b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, false, false), true));
                String content = refactorStatic(getShortenContent(stm));
                b.append(content);

            } else if (stm instanceof IASTReturnStatement) {
                b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, false, false), true));
                String content = refactorStatic(getShortenContent(stm));
                b.append(content);
            } else if (stm instanceof IASTSwitchStatement) {
                IASTNode ce = ((IASTSwitchStatement) stm).getControllerExpression();
                String content = getShortenContent(ce);
                b.append(putInMark(addContentOfMarkFunction(ce, astFunctionNode, functionPath, false, false), true))
                        .append("switch(")
                        .append(content)
                        .append(")")
                        .append(addExtraCall(((IASTSwitchStatement) stm).getBody(), null, margin));
            } else if (stm instanceof IASTCaseStatement) {
                String content = stm.getRawSignature();
                IASTNode buffer = stm.getParent();
                while (!(buffer instanceof IASTSwitchStatement)) {
                    buffer = buffer.getParent();
                }
                String switchParam = getShortenContent(((IASTSwitchStatement) buffer).getControllerExpression());
                IASTNode thisCase = ((IASTCaseStatement) stm).getExpression();
                b.append(content)
                        .append(" if(")
                        .append(switchParam).append(" == ").append(thisCase.getRawSignature())
                        .append(")")
                        .append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, true, true), true));
            } else if (stm instanceof IASTDefaultStatement) {
                b.append(getShortenContent(stm));

                IASTNode buffer = stm.getParent();
                while (!(buffer instanceof IASTSwitchStatement)) {
                    buffer = buffer.getParent();
                }
                IASTSwitchStatement switchStm = (IASTSwitchStatement) buffer;
                String controlValue = switchStm.getControllerExpression().getRawSignature();
                List<String> caseValues = new ArrayList<>();
                if (switchStm.getBody() instanceof IASTCompoundStatement) {
                    IASTStatement[] statements = ((IASTCompoundStatement) switchStm.getBody()).getStatements();
                    for (IASTStatement statement : statements) {
                        if (statement instanceof IASTCaseStatement) {
                            String caseValue = ((IASTCaseStatement) statement).getExpression().getRawSignature();
                            caseValues.add(caseValue);
                        }
                    }
                }

                String defaultContent = Utils.generateContentForDefaultCase(controlValue, caseValues);

                b.append(" if(").append(defaultContent).append(")")
                        .append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, true, true), true));

            } else if (stm instanceof ICPPASTRangeBasedForStatement) {
                ICPPASTRangeBasedForStatement astFor = (ICPPASTRangeBasedForStatement) stm;

                // Add marker for initialization
                IASTDeclaration declaration = astFor.getDeclaration();
                if (!(declaration instanceof IASTNullStatement)) {
                    b.append(putInMark(addContentOfMarkFunction(declaration, astFunctionNode, functionPath, false, false), true));
                }

                String init = refactorStatic(getShortenContent(declaration));
                b.append("for (").append(init);

                // Add marker for init clause
                IASTInitializerClause initClause = astFor.getInitializerClause();
                String range = refactorStatic(getShortenContent(initClause));
                b.append(" : ").append(range).append(") ");

                // Add marker for body
                String markInitClause = addContentOfMarkFunction(initClause, astFunctionNode, functionPath, false, false);
                b.append(addExtraCall(astFor.getBody(), markInitClause, margin));

            } else {
                String raw = getShortenContent(stm);
                boolean isStatic = false;

                if (stm instanceof IASTDeclarationStatement) {
                    IASTDeclaration declaration = ((IASTDeclarationStatement) stm).getDeclaration();
                    if (declaration instanceof IASTSimpleDeclaration) {
                        IASTSimpleDeclaration simpleDeclaration = (IASTSimpleDeclaration) declaration;
                        IASTDeclSpecifier declSpec = simpleDeclaration.getDeclSpecifier();
                        isStatic = declSpec.getStorageClass() == IASTDeclSpecifier.sc_static && !declSpec.isConst();

                        for (IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
                            String name = declarator.getName().getRawSignature();
                            VariableEntry e = new VariableEntry(name, scope);
                            e.setStatic(isStatic);
                            varList.add(e);
                        }
                    }
                }

                if (isStatic && STATIC_REFACTOR) {
                    raw = String.format("/* instrument variable %s */", raw);
                } else {
                    raw = refactorStatic(raw);
                }

                b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath, false, false), true));// add markers
                b.append(raw);
            }
        }

        return b.toString();
    }

    private final boolean STATIC_REFACTOR = false;

    private String refactorStatic(String stm) {
        if (STATIC_REFACTOR) {
            List<VariableEntry> statics = varList.stream()
                    .filter(v -> v.isStatic)
                    .collect(Collectors.toList());

            for (VariableEntry v : statics) {
                if (varList.getNearest(v.getName()) == v) {
                    String instrument = prefix + v.getName();
                    stm = stm.replaceAll("\\b\\Q" + v.getName() + "\\E\\b", instrument);
                }
            }
        }

        return stm;
    }

    private String createMarkForSubCondition(IASTNode astCon) {
        StringBuilder tempStr = new StringBuilder();
        astCon = Utils.shortenAstNode(astCon);
        if (isCondition(astCon)) {
            if (astCon instanceof IASTBinaryExpression) {
                int operator = ((IASTBinaryExpression) astCon).getOperator();

                switch (operator) {
                    case IASTBinaryExpression.op_greaterEqual:
                    case IASTBinaryExpression.op_greaterThan:
                    case IASTBinaryExpression.op_lessEqual:
                    case IASTBinaryExpression.op_lessThan:
                        tempStr.append("	(").append(astCon)
                                .append("&&").append(Utils.shortenAstNode(astCon).getRawSignature()).append(")");
                        break;

                    case IASTBinaryExpression.op_logicalAnd:
                    case IASTBinaryExpression.op_logicalOr:
                        IASTExpression operand1 = ((IASTBinaryExpression) astCon).getOperand1();
                        IASTExpression operand2 = ((IASTBinaryExpression) astCon).getOperand2();

                        tempStr.append("(").append(createMarkForSubCondition(operand1)).append(")")
                                .append(operator == IASTBinaryExpression.op_logicalAnd ? "	&&" : "	||").append("(").append(createMarkForSubCondition(operand2)).append(")");
                        break;
                }
            } else {
                // unary expression
                tempStr.append(DriverConstant.MARK + "(\"")
                        .append(addContentOfMarkFunction(astCon, astFunctionNode, functionPath, false, true)).
                        append("\")&&").
                        append(astCon.getRawSignature());
            }
        } else {
            if (isAssign(astCon)) {
                IASTBinaryExpression astBin = (IASTBinaryExpression) astCon;
                String operand1 = astBin.getOperand1().getRawSignature();
                String operand2 = astBin.getOperand2().getRawSignature();
                tempStr.append("(")
                        .append(operand1)
                        .append("=")
                        .append(operand2)
                        .append(") && ")
                        .append(DriverConstant.MARK + "(\"")
                        .append(addContentOfMarkFunction(astCon, astFunctionNode, functionPath, false, true)).
                        append("\")");

            } else {
                tempStr.append(DriverConstant.MARK + "(\"")
                        .append(addContentOfMarkFunction(astCon, astFunctionNode, functionPath, false, true)).
                        append("\")&&").
                        append(astCon.getRawSignature());
            }
        }

        return refactorStatic(tempStr.toString());
    }

    private static class VariableList extends ArrayList<VariableEntry> {

        public void removeAtScope(int scope) {
            removeIf(v -> v.getScope() == scope);
        }

        public VariableEntry getNearest(String name) {
            return stream()
                    .sorted(Comparator.reverseOrder())
                    .filter(v -> v.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static class VariableEntry implements Comparable<VariableEntry> {

        private final String name;

        private final int scope;

        private boolean isStatic;

        public VariableEntry(String name, int scope) {
            this.scope = scope;
            this.name = name;
        }

        public void setStatic(boolean aStatic) {
            isStatic = aStatic;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public String getName() {
            return name;
        }

        public int getScope() {
            return scope;
        }

        @Override
        public int compareTo(VariableEntry o) {
            return Integer.compare(scope, o.scope);
        }
    }

}
