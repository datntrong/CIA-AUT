package uet.fit.aut.testdata.gen.module.subtree;

import uet.fit.aut.parser.funcdetail.IFunctionDetailTree;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.Utils;

import java.util.List;

import static uet.fit.aut.util.NodeType.SBF;
import static uet.fit.aut.util.NodeType.STATIC;

public class InitialUUTBranchGen extends AbstractInitialTreeGen {
    @Override
    public void generateCompleteTree(RootDataNode root, IFunctionDetailTree functionTree) throws Exception {
        this.root = root;
        functionNode = root.getFunctionNode();
        INode sourceCode = Utils.getSourcecodeFile(functionNode);

        if (sourceCode instanceof ISourcecodeFileNode) {
            UnitNode unitNode = new UnitUnderTestNode(sourceCode);
//            unitNode.setStubChildren(false);

            root.addChild(unitNode);
            unitNode.setParent(root);

//            if (Environment.getInstance().getSBFs().contains(sourceCode))
            generateSBFBranch(unitNode, sourceCode, functionNode);

            RootDataNode globalRoot = generateGlobalVarBranch(unitNode, functionTree);

            IDataNode sut = new InitialArgTreeGen().generate(unitNode, functionNode);

            if (!functionNode.isTemplate() && !functionNode.isMethod() && functionNode instanceof IFunctionNode) {
                IDataNode staticRoot = generateStaticBranch(sut, (IFunctionNode) functionNode);
                sut.getChildren().add(0, staticRoot);
            }

            if (functionNode instanceof ConstructorNode) {
                sut.getChildren().clear();
                expandInstance((ConstructorNode) functionNode, globalRoot);
            }
        }
    }

    // case test constructor
    private void expandInstance(IFunctionNode sut, RootDataNode globalRoot) throws Exception {
        INode parent = sut.getRealParent() == null ? sut.getParent() : sut.getRealParent();

        for (IDataNode child : globalRoot.getChildren()) {
            if (child instanceof ValueDataNode) {
                ValueDataNode globalVar = (ValueDataNode) child;

                if (globalVar.getCorrespondingVar() instanceof InstanceVariableNode
                        && globalVar.getCorrespondingType().equals(parent)) {

//                    new InputCellHandler().commitEdit(globalVar, globalVar.getCorrespondingType().getName());

                    if (!globalVar.getChildren().isEmpty() && globalVar.getChildren().get(0) instanceof ValueDataNode) {
                        ValueDataNode subclass = (ValueDataNode) globalVar.getChildren().get(0);
//                        new InputCellHandler().commitEdit(subclass, sut.getName());
                    }
                }
            }
        }
    }

    private RootDataNode generateSBFBranch(IDataNode current, INode sourceNode, ICommonFunctionNode sut) {
        RootDataNode sbfRoot = new RootDataNode(SBF);

        List<ICommonFunctionNode> functions = Search.searchNodes(sourceNode, new FunctionNodeCondition());
        functions = filterCalledFunctions(sut, functions);

        for (INode child : functions) {
            if (child instanceof FunctionNode && !child.equals(sut)) {
                FunctionNode functionNode = (FunctionNode) child;
                SubprogramNode subprogramNode = new SubprogramNode(functionNode);

                if (functionNode.isTemplate())
                    subprogramNode = new TemplateSubprogramDataNode(functionNode);

                sbfRoot.addChild(subprogramNode);
                subprogramNode.setParent(sbfRoot);
            }
        }

        current.addChild(sbfRoot);
        sbfRoot.setParent(current);

        return sbfRoot;
    }

    private RootDataNode generateStaticBranch(IDataNode current, IFunctionNode sut) throws Exception {
        logger.debug("generateStaticBranch");
        RootDataNode staticRoot = new RootDataNode(STATIC);

        List<StaticVariableNode> staticVars = sut.getStaticVariables();

        for (StaticVariableNode staticVar : staticVars) {
            ValueDataNode dataNode = genInitialTree(staticVar, staticRoot);
            dataNode.setExternal(true);
        }

        staticRoot.setParent(current);
        staticRoot.setFunctionNode(functionNode);

        return staticRoot;
    }

    private RootDataNode generateGlobalVarBranch(IDataNode current, IFunctionDetailTree functionTree) throws Exception {
        logger.debug("generateGlobalVarBranch");
        GlobalRootDataNode globalVarRoot = new GlobalRootDataNode();
        globalVarRoot.setFunctionNode(functionNode);

        List<INode> globalElements = functionTree.getSubTreeRoot(NodeType.GLOBAL).getElements();

        for (INode globalElement : globalElements) {
            if (globalElement instanceof VariableNode) {
                VariableNode globalVariable = (VariableNode) globalElement;
                if (!globalVariable.isConst() && globalVarRoot.isRelatedVariable(globalVariable)) {
                    ValueDataNode dataNode = genInitialTree(globalVariable, globalVarRoot);
                    dataNode.setExternal(true);
                }
            }
        }

        current.addChild(globalVarRoot);
        globalVarRoot.setParent(current);
        globalVarRoot.setFunctionNode(functionNode);

        return globalVarRoot;
    }
}
