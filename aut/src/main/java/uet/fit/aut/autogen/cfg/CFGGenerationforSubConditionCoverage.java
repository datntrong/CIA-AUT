package uet.fit.aut.autogen.cfg;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import uet.fit.aut.autogen.cfg.object.*;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.util.Utils;

/**
 * Generate control flow graph from source code for sub-condition coverage
 *
 * @author DucAnh
 */
public class CFGGenerationforSubConditionCoverage implements ICFGGeneration {

	private IFunctionNode functionNode;

	public CFGGenerationforSubConditionCoverage(IFunctionNode fn) {
		this.functionNode = fn;
	}

	@Override
	public ICFG generateCFG() {
		return parse(functionNode);
	}

	private ICFG parse(IFunctionNode functionNode) {
		ICFG cfg = new CFGGenerationforBranchvsStatementvsBasispathCoverage(functionNode).generateCFG();
		cfg.setIdforAllNodes();
		boolean containComplexConditions = true;
		while (containComplexConditions) {
			containComplexConditions = false;

			for (ICfgNode cfgNode : cfg.getAllNodes())
				if (cfgNode instanceof ConditionCfgNode && isComplexCondition((ConditionCfgNode) cfgNode)) {
					createGraphForComplexCondition((ConditionCfgNode) cfgNode, cfg);
					containComplexConditions = true;
					break;
				}
		}
		return cfg;
	}

	private void createGraphForComplexCondition(ConditionCfgNode complexConditionNode, ICFG cfg) {
		IASTNode ast = Utils.shortenAstNode(complexConditionNode.getAst());
		if (ast instanceof IASTBinaryExpression) {
			IASTBinaryExpression astBin = (IASTBinaryExpression) ast;

			IASTExpression left = astBin.getOperand1();
			IASTExpression right = astBin.getOperand2();

			ConditionCfgNode leftNode = null;
			ConditionCfgNode rightNode = null;

			switch (astBin.getOperator()) {
			case IASTBinaryExpression.op_logicalAnd:
			case IASTBinaryExpression.op_logicalOr:
				if (complexConditionNode instanceof ConditionDoCfgNode
						|| complexConditionNode instanceof ConditionElementDoCfgNode) {
					leftNode = new ConditionElementDoCfgNode(Utils.shortenAstNode(left));
					rightNode = new ConditionElementDoCfgNode(Utils.shortenAstNode(right));

				} else if (complexConditionNode instanceof ConditionIfCfgNode
						|| complexConditionNode instanceof ConditionElementIfCfgNode) {
					leftNode = new ConditionElementIfCfgNode(Utils.shortenAstNode(left));
					rightNode = new ConditionElementIfCfgNode(Utils.shortenAstNode(right));

				} else if (complexConditionNode instanceof ConditionForCfgNode
						|| complexConditionNode instanceof ConditionElementForCfgNode) {
					leftNode = new ConditionElementForCfgNode(Utils.shortenAstNode(left));
					rightNode = new ConditionElementForCfgNode(Utils.shortenAstNode(right));

				} else if (complexConditionNode instanceof ConditionWhileCfgNode
						|| complexConditionNode instanceof ConditionElementWhileCfgNode) {
					leftNode = new ConditionElementWhileCfgNode(Utils.shortenAstNode(left));
					rightNode = new ConditionElementWhileCfgNode(Utils.shortenAstNode(right));
				}
			}

			// Create new node
			if (leftNode != null && rightNode != null) {
				ICfgNode parent = complexConditionNode.getParent();
				ICfgNode falseBranch = complexConditionNode.getFalseNode();
				ICfgNode trueBranch = complexConditionNode.getTrueNode();

				switch (astBin.getOperator()) {
				case IASTBinaryExpression.op_logicalAnd:
					leftNode.setParent(parent);
					leftNode.setTrue(rightNode);
					leftNode.setFalse(falseBranch);

					rightNode.setParent(leftNode);
					rightNode.setTrue(trueBranch);
					rightNode.setFalse(falseBranch);
					break;

				case IASTBinaryExpression.op_logicalOr:
					leftNode.setParent(parent);
					leftNode.setTrue(trueBranch);
					leftNode.setFalse(rightNode);

					rightNode.setParent(leftNode);
					rightNode.setTrue(trueBranch);
					rightNode.setFalse(falseBranch);
					break;
				}

				// Update all nodes
				leftNode.setId(cfg.getMaxId() + 1);
				rightNode.setId(cfg.getMaxId() + 2);
				cfg.getAllNodes().add(leftNode);
				cfg.getAllNodes().add(rightNode);
				cfg.getAllNodes().remove(complexConditionNode);

				for (ICfgNode node : cfg.getAllNodes())
					if (node instanceof ScopeCfgNode) {
						ScopeCfgNode castNode = (ScopeCfgNode) node;
						if (castNode.getFalseNode().equals(complexConditionNode)
								|| castNode.getTrueNode().equals(complexConditionNode))
							node.setBranch(leftNode);

					} else if (node instanceof NormalCfgNode) {

						if (node instanceof ConditionCfgNode) {
							ConditionCfgNode castNode = (ConditionCfgNode) node;

							if (castNode.getTrueNode().equals(complexConditionNode))
								castNode.setTrue(leftNode);
							else if (castNode.getFalseNode().equals(complexConditionNode))
								castNode.setFalse(leftNode);

						} else if (node instanceof SimpleCfgNode) {
							SimpleCfgNode castNode = (SimpleCfgNode) node;
							if (castNode.getFalseNode().equals(complexConditionNode)
									|| castNode.getTrueNode().equals(complexConditionNode))
								node.setBranch(leftNode);
						}
					}
			}
		}
	}

	private boolean isComplexCondition(ConditionCfgNode cfgNode) {
		boolean isComplexCon = false;

		IASTNode ast = cfgNode.getAst();
		if (ast != null) {
			ast = Utils.shortenAstNode(ast);
			if (ast instanceof IASTBinaryExpression) {
				IASTBinaryExpression astBinary = (IASTBinaryExpression) ast;
				switch (astBinary.getOperator()) {
				case IASTBinaryExpression.op_logicalAnd:
				case IASTBinaryExpression.op_logicalOr:
					isComplexCon = true;
					break;
				default:
					break;
				}
			}
		}
		return isComplexCon;
	}

	@Override
	public IFunctionNode getFunctionNode() {
		return functionNode;
	}

	@Override
	public void setFunctionNode(IFunctionNode functionNode) {
		this.functionNode = functionNode;
	}
}
