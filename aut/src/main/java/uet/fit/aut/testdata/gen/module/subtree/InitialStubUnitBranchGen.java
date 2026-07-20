package uet.fit.aut.testdata.gen.module.subtree;


import uet.fit.aut.parser.funcdetail.IFunctionDetailTree;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.testdata.object.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class InitialStubUnitBranchGen extends AbstractInitialTreeGen {

    @Override
    public void generateCompleteTree(RootDataNode root, IFunctionDetailTree functionTree) throws Exception {

    }

    public IDataNode generate(IDataNode parent, ISourcecodeFileNode physicalNode, Collection<FunctionNode> functions) {
        if (physicalNode != null) {
            UnitNode unitNode = new StubUnitNode(physicalNode);

            for (INode child : functions) {
                if (child instanceof FunctionNode) {
                    FunctionNode functionNode = (FunctionNode) child;
                    SubprogramNode subprogramNode = new SubprogramNode(functionNode);

                    if (functionNode.isTemplate())
                        subprogramNode = new TemplateSubprogramDataNode(functionNode);

                    unitNode.addChild(subprogramNode);
                    subprogramNode.setParent(unitNode);
                }
            }

            parent.addChild(unitNode);
            unitNode.setParent(parent);

            return unitNode;
        }

        return null;
    }
}
