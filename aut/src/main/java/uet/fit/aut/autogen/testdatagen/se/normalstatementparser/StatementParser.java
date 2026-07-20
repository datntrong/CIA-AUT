package uet.fit.aut.autogen.testdatagen.se.normalstatementparser;

import uet.fit.aut.autogen.testdatagen.se.memory.FunctionCallTable;
import uet.fit.aut.autogen.testdatagen.se.memory.VariableNodeTable;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * The top abstract class used to parse statement
 *
 * @author ducanhnguyen
 */
public abstract class StatementParser {
    /**
     * Parse the statement
     *
     * @param ast   the AST of the statement
     * @param table table of variables
     * @param callTable table of function calls
     * @throws Exception
     */
    public abstract void parse(IASTNode ast, VariableNodeTable table, FunctionCallTable callTable) throws Exception;
}
