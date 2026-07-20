package uet.fit.aut.autogen.cfg;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import uet.fit.aut.autogen.cfg.object.BeginFunctionCallFlagCfgNode;
import uet.fit.aut.autogen.cfg.object.EndFunctionCallFlagCfgNode;
import uet.fit.aut.autogen.cfg.object.NormalCfgNode;
import uet.fit.aut.parser.obj.IFunctionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate (called/nested) control flow graph
 * from source code for statement/branch coverage
 *
 * @author Lamnt
 */
public class NestedCFGGenerationforBranchvsStatementvsBasispathCoverage extends CFGGenerationforBranchvsStatementvsBasispathCoverage
        implements INestedCFGGeneration {

    private IASTFunctionCallExpression expr;
    private List<IFunctionNode> previousCalls = new ArrayList<>();

    public NestedCFGGenerationforBranchvsStatementvsBasispathCoverage(IFunctionNode normalizedFunction) {
        super(normalizedFunction);
    }

    public void setPreviousCalls(List<IFunctionNode> previousCalls) {
        this.previousCalls = previousCalls;
    }

    public List<IFunctionNode> getPreviousCalls() {
        return previousCalls;
    }

    @Override
    protected void preprocessor(IFunctionNode fn) {
        BeginFunctionCallFlagCfgNode BEGIN = new BeginFunctionCallFlagCfgNode();
        BEGIN.setFunction(fn);
        BEGIN.setExpr(expr);

        EndFunctionCallFlagCfgNode END = new EndFunctionCallFlagCfgNode();
        END.setFunction(fn);
        END.setBeginNode(BEGIN);
        END.setExpr(expr);

        this.END = END;
        this.BEGIN = BEGIN;
    }

    @Override
    protected void attachSubCFG(IFunctionNode called, IASTFunctionCallExpression expr,
                                NormalCfgNode cfgNode) throws Exception {
        if (!previousCalls.contains(called)) {
            INestedCFGGeneration cfgGen =
                    new NestedCFGGenerationforBranchvsStatementvsBasispathCoverage(called);

            cfgGen.getPreviousCalls().addAll(previousCalls);
            cfgGen.getPreviousCalls().add(functionNode);
            cfgGen.setExpression(expr);

            ICFG cfg = cfgGen.generateCFG();

            cfgNode.getSubCFGs().put(expr, cfg);
        }
    }

    @Override
    public void setExpression(IASTFunctionCallExpression expr) {
        this.expr = expr;
    }

}
