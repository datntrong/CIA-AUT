package uet.fit.aut.util.toString;

import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.Node;

public class DependencyTreeDisplayer extends ToString {

	public DependencyTreeDisplayer(INode root) {
		super(root);
	}

	private void displayTree(INode n, int level) {
		if (n == null)
			return;
		else {
			// TODO: un comment 37 - 41
//			if (n instanceof AttributeOfStructureVariableNode)
//				treeInString += genTab(level) + "[" + n.getClass().getSimpleName() + "] " + n.toString() + "; type = " + ((AttributeOfStructureVariableNode) n).getRawType() + "\n";
//			else if (n instanceof VariableNode || n instanceof FunctionNode || n instanceof TypedefDeclaration)
//				treeInString += genTab(level) + "[" + n.getClass().getSimpleName() + "] " + n.toString() + "\n";
//			else
				treeInString += genTab(level) + "[" + n.getClass().getSimpleName() + "] " + n.getNewType() + "\n";

			treeInString += genTab(level) + n.getAbsolutePath() + "\n";
			for (Dependency d : n.getDependencies())
				if (d.getStartArrow().equals(n))
					treeInString += genTab(level + 1) + "[" + d.getClass().getSimpleName() + "]"
							+ d.getEndArrow().getAbsolutePath() + "\n";

		}
		for (Object child : n.getChildren()) {
			displayTree((Node) child, ++level);
			level--;
		}

	}

	@Override
	public String toString(INode n) {
		displayTree(n, 0);
		return treeInString;
	}
}

