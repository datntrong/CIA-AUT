package uet.fit.aut.autogen.cfg.object;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import uet.fit.aut.parser.obj.IFunctionNode;

/**
 * Represent the function call flag node of CFG
 *
 * @author lamnt
 */
public abstract class FunctionCallFlagCfgNode extends FlagCfgNode {

	protected IASTFunctionCallExpression expr;
	protected IFunctionNode function;

	public IASTFunctionCallExpression getExpr() {
		return expr;
	}

	public void setExpr(IASTFunctionCallExpression expr) {
		this.expr = expr;
	}

	public IFunctionNode getFunction() {
		return function;
	}

	public void setFunction(IFunctionNode function) {
		this.function = function;
	}
}
