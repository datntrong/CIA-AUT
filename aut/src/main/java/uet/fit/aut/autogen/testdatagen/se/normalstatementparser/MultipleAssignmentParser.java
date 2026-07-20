package uet.fit.aut.autogen.testdatagen.se.normalstatementparser;

import uet.fit.aut.autogen.cfg.FunctionCallVisitor;
import uet.fit.aut.autogen.testdatagen.se.ExpressionRewriterUtils;
import uet.fit.aut.autogen.testdatagen.se.memory.FunctionCallTable;
import uet.fit.aut.autogen.testdatagen.se.memory.PhysicalCell;
import uet.fit.aut.autogen.testdatagen.se.memory.VariableNodeTable;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.util.ASTUtils;
import uet.fit.aut.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.util.List;

/**
 * Parse multiple assignments, e.g., "x=y=z+1"
 *
 * @author ducanhnguyen
 */
public class MultipleAssignmentParser extends BinaryAssignmentParser {

    @Override
    public void parse(IASTNode ast, VariableNodeTable table, FunctionCallTable callTable) throws Exception {
        ast = Utils.shortenAstNode(ast);
        if (ast instanceof IASTBinaryExpression) {
            List<String> expressions = ASTUtils.getAllExpressionsInBinaryExpression((IASTBinaryExpression) ast);
            int last = expressions.size() - 1;

            String finalExpression = expressions.get(last);
            finalExpression = ExpressionRewriterUtils.rewrite(table, finalExpression);

            IASTNode finalExprAst = Utils.convertToIAST(finalExpression);
            FunctionCallVisitor visitor = new FunctionCallVisitor((IFunctionNode) table.getFunctionNode());
            finalExprAst.accept(visitor);
            for (IASTFunctionCallExpression expr : visitor.getCallMap().keySet()) {
                String varName = callTable.get(expr);
                if (varName != null) {
                    String regex = "\\Q" + expr.getRawSignature() + "\\E";
                    finalExpression = finalExpression.replaceFirst(regex, varName);
                    callTable.remove(expr);
                }
            }

			/*
             * All variable corresponding to expressions, except the final
			 * expression, is assigned to the final expression
			 */
            for (int i = 0; i < last; i++) {
                String currentExpression = expressions.get(i);
                PhysicalCell cell = table.findPhysicalCellByName(currentExpression);

                if (cell != null)
                    cell.setValue(finalExpression);
            }
        }
    }

}
