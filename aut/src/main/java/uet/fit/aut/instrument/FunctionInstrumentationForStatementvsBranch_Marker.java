package uet.fit.aut.instrument;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

/**
 * Extend the previous instrumentation function by adding extra information
 * (e.g., the line of statements) to markers. <br/>
 * Ex: int a = 0; ----instrument-----> mark("line 12:int a = 0"); int a = 0;
 * <p>
 * <br/>
 *
 * @author DucAnh
 */
public class FunctionInstrumentationForStatementvsBranch_Marker extends AbstractFunctionInstrumentation {

    public FunctionInstrumentationForStatementvsBranch_Marker(IASTFunctionDefinition astFunctionNode) {
        this.astFunctionNode = astFunctionNode;
    }

    @Override
    public String generateInstrumentedFunction() {
        return instrument(astFunctionNode);
    }

    protected String addExtraCall(IASTStatement stm, String extra, String margin) {
        if (extra != null)
            extra = putInMark(extra, true);

        if (stm instanceof IASTCompoundStatement)
            return parseBlock((IASTCompoundStatement) stm, extra, margin);
        else {
            String inside = margin + SpecialCharacter.TAB;

            String b = SpecialCharacter.OPEN_BRACE + SpecialCharacter.LINE_BREAK + inside /*+ inside*/ +
                    parseStatement(stm, inside) + SpecialCharacter.LINE_BREAK + margin +
                    SpecialCharacter.CLOSE_BRACE;
            return b;
        }
    }

    protected String instrument(IASTFunctionDefinition astFunctionNode) {
        String b = getShortenContent(astFunctionNode.getDeclSpecifier()) + SpecialCharacter.SPACE +
                getShortenContent(astFunctionNode.getDeclarator()) +
                parseBlock((IASTCompoundStatement) astFunctionNode.getBody(), null, "");
        return b;
    }

    protected String parseBlock(IASTCompoundStatement block, String extra, String margin) {
        StringBuilder b = new StringBuilder("{" + SpecialCharacter.LINE_BREAK);
        String inside = margin + SpecialCharacter.TAB;
//        if (extra != null)
//            b.append(inside);

        for (IASTStatement stm : block.getStatements())
            b.append(inside).append(parseStatement(stm, inside)).append(SpecialCharacter.LINE_BREAK);
                    //.append(SpecialCharacter.LINE_BREAK);

        b.append(margin)
                .append(SpecialCharacter.CLOSE_BRACE);
        return b.toString();
    }

    protected String parseStatement(IASTStatement stm, String margin) {
        StringBuilder b = new StringBuilder();

        if (stm instanceof IASTCompoundStatement)
            b.append(parseBlock((IASTCompoundStatement) stm, null, margin));

        else if (stm instanceof IASTIfStatement) {
            IASTIfStatement astIf = (IASTIfStatement) stm;
            IASTStatement astElse = astIf.getElseClause();
            String cond = getShortenContent(astIf.getConditionExpression());
            b.append("if (").append(putInMark(addDefaultMarkerContentForIf(astIf.getConditionExpression()), false))
                    .append(" && (").append(cond).append(")) ");

            b.append(addExtraCall(astIf.getThenClause(), "", margin));

            if (astElse != null) {
                b.append(SpecialCharacter.LINE_BREAK).append(margin).append("else ");
                b.append(addExtraCall(astElse, "", margin));
            }

        } else if (stm instanceof IASTForStatement) {
            IASTForStatement astFor = (IASTForStatement) stm;

            // Add marker for initialization
            IASTStatement astInit = astFor.getInitializerStatement();
            if (!(astInit instanceof IASTNullStatement)) {
                b.append(putInMark(addContentOfMarkFunction(astInit, astFunctionNode, functionPath), true));
            }

            b.append("for (").append(getShortenContent(astInit));
            // Add marker for condition
            IASTExpression astCond = (IASTExpression) Utils.shortenAstNode(astFor.getConditionExpression());
            if (astCond != null) {
                //b.append(SpecialCharacter.LINE_BREAK).append("\t\t\t");
                b.append(putInMark(addContentOfMarkFunction(astCond, astFunctionNode, functionPath), false)).append(" && ").append(getShortenContent(astCond)).append(";");
            }

            // Add marker for increment
            IASTExpression astIter = astFor.getIterationExpression();
            if (astIter != null) {
                //b.append(SpecialCharacter.LINE_BREAK).append("\t\t\t");
                b.append("({" + putInMark(addContentOfMarkFunction(astIter, astFunctionNode, functionPath), false)).append(";").
                        append(getShortenContent(astIter)).append(";})");
            }
            b.append(") ");

            // For loop: no condition
            if (astCond == null)
                b.append(parseStatement(astFor.getBody(), margin));
            else
                b.append(addExtraCall(astFor.getBody(), "", margin));

        } else if (stm instanceof IASTWhileStatement) {
            IASTWhileStatement astWhile = (IASTWhileStatement) stm;
            String cond = getShortenContent(astWhile.getCondition());

            b.append("while (")
                    .append(putInMark(addContentOfMarkFunction(astWhile.getCondition(), astFunctionNode, functionPath), false))
                    .append(" && (").append(cond).append(")) ");

            b.append(addExtraCall(astWhile.getBody(), "", margin));

        } else if (stm instanceof IASTDoStatement) {
            IASTDoStatement astDo = (IASTDoStatement) stm;
            String cond = getShortenContent(astDo.getCondition());

            b.append("do ").append(addExtraCall(astDo.getBody(), "", margin)).append(SpecialCharacter.LINE_BREAK)
                    .append(margin).append("while (")
                    .append(putInMark(addContentOfMarkFunction(astDo.getCondition(), astFunctionNode, functionPath), false))
                    .append(" && (").append(cond).append("));");

        } else if (stm instanceof ICPPASTTryBlockStatement) {
            ICPPASTTryBlockStatement astTry = (ICPPASTTryBlockStatement) stm;

            b.append(DriverConstant.MARK + "(\"start try;\");");

            b.append(SpecialCharacter.LINE_BREAK).append(margin).append("try ");
            b.append(addExtraCall(astTry.getTryBody(), null, margin));

            for (ICPPASTCatchHandler catcher : astTry.getCatchHandlers()) {
                b.append(SpecialCharacter.LINE_BREAK).append(margin).append("catch (");

                String exception = catcher.isCatchAll() ? "..." : getShortenContent(catcher.getDeclaration());
                b.append(exception).append(") ");

                b.append(addExtraCall(catcher.getCatchBody(), exception, margin));
            }

            b.append(SpecialCharacter.LINE_BREAK).append(margin).append(DriverConstant.MARK + "(\"end catch;\");");

        } else if (stm instanceof IASTBreakStatement || stm instanceof IASTContinueStatement) {
            b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath), true));
            b.append(getShortenContent(stm));

        } else if (stm instanceof IASTReturnStatement) {
            b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath), true));
            b.append(getShortenContent(stm));

        } else {
            String raw = getShortenContent(stm);
            b.append(putInMark(addContentOfMarkFunction(stm, astFunctionNode, functionPath), true));// add markers
            b.append(raw);
        }

        return b.toString();
    }

    protected String addDefaultMarkerContentForIf(IASTNode node) {
        return addContentOfMarkFunction(node, astFunctionNode, functionPath);// + DELIMITER_BETWEEN_PROPERTIES;
    }

    public static final String IN_CONTROL_BLOCK = "control-block";
    public static final String IF_BLOCK = "if";
    public static final String FOR_BLOCK = "for";

    public static final String LINE_NUMBER_IN_FUNCTION = "line-in-function";
    public static final String LINE_NUMBER_OF_BLOCK_IN_FUNCTION = "line-of-blockin-function";
    public static final String OFFSET_IN_FUNCTION = "offset";
    public static final String STATEMENT = "statement";
    public static final String DELIMITER_BETWEEN_PROPERTIES = IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTIES;
    public static final String DELIMITER_BETWEEN_PROPERTY_AND_VALUE = IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTY_AND_VALUE;
    public static final String OPENNING_FUNCTION_SCOPE = "openning-function"; // {true, false}
    public static final String IS_RECURSIVE = "is-recursive"; // {true, false}
    // Beside the executed statements, we also add several additional codes to print
    // out further information
    public static final String ADDITIONAL_BODY_CONTROL_MARKER = "additional-code";
    public static final String SOURROUNDING_MARKER = "surrounding-control-block";
}
