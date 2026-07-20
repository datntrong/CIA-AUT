package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.*;
import uet.fit.aut.parser.vardect.RelatedExternalVariableDetecter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uet.fit.aut.util.NodeType.GLOBAL;

/**
 * Represent the root of global the variable tree
 *
 * @author lamnt
 */
public class GlobalRootDataNode extends RootDataNode {

	// map input to expected output of global variables
    // only used for GLOBAL level
	private Map<ValueDataNode, ValueDataNode> globalInputExpOutputMap;

	private boolean hide = true;

	private List<IVariableNode> relatedVariables;

	private StructureNode structureNode;

	public GlobalRootDataNode() {
		level = GLOBAL;
		globalInputExpOutputMap = new HashMap<>();
	}

	public void setFunctionNode(ICommonFunctionNode functionNode) {
		super.setFunctionNode(functionNode);

		if (functionNode instanceof MacroFunctionNode)
			functionNode = ((MacroFunctionNode) functionNode).getCorrespondingFunctionNode();

		RelatedExternalVariableDetecter detector = new RelatedExternalVariableDetecter((IFunctionNode) functionNode);
		relatedVariables = detector.findVariables();

		INode parent = ((IFunctionNode) functionNode).getRealParent();
		if (parent == null)
			parent = functionNode.getParent();

		if (parent instanceof StructureNode) {
			this.structureNode = (StructureNode) parent;
		}
	}

	public boolean isRelatedVariable(IVariableNode v) {
		if (structureNode != null && v instanceof InstanceVariableNode) {
			if (v.getCorrespondingNode() == structureNode)
				return true;
		}

		if (relatedVariables != null)
			return relatedVariables.contains(v);
		else
			return false;
	}

	public Map<ValueDataNode, ValueDataNode> getGlobalInputExpOutputMap() {
		return globalInputExpOutputMap;
	}

	public void setGlobalInputExpOutputMap(Map<ValueDataNode, ValueDataNode> globalInputExpOutputMap) {
		this.globalInputExpOutputMap = globalInputExpOutputMap;
	}

	public boolean putGlobalExpectedOutput(ValueDataNode expectedOuput) {
		ValueDataNode input = null;
		for (IDataNode child : getChildren()) {
			if (((ValueDataNode) child).getCorrespondingVar().getAbsolutePath().equals(expectedOuput.getCorrespondingVar().getAbsolutePath())) {
				input = (ValueDataNode) child;
				break;
			}
		}

		if (input != null) {
			globalInputExpOutputMap.remove(input);
			globalInputExpOutputMap.put(input, expectedOuput);
			return true;
		}

		return false;
	}

	public boolean isShowRelated() {
		return hide;
	}

	public void setShowRelated(boolean hide) {
		this.hide = hide;
	}

	public List<IVariableNode> getRelatedVariables() {
		return relatedVariables;
	}
}
