package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search2;
import uet.fit.aut.testdata.gen.module.subtree.InitialArgTreeGen;
import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubprogramNode extends ValueDataNode {

    protected INode functionNode;

    protected Map<ValueDataNode, ValueDataNode> inputToExpectedOutputMap = new HashMap<>();

    public SubprogramNode() {

    }

    public Map<ValueDataNode, ValueDataNode> getInputToExpectedOutputMap() {
        return inputToExpectedOutputMap;
    }

    public SubprogramNode(INode fn) {
        setFunctionNode(fn);
    }

    public void initInputToExpectedOutputMap() {
        inputToExpectedOutputMap.clear();

        /*
          create initial tree
         */
        try {
            ICommonFunctionNode castFunctionNode = (ICommonFunctionNode) functionNode;
            RootDataNode root = new RootDataNode();
            root.setFunctionNode(castFunctionNode);
            InitialArgTreeGen dataTreeGen = new InitialArgTreeGen();
            List<ValueDataNode> actualNodes = Search2.searchParameterNodes(this);
            for (INode child : castFunctionNode.getChildren()) {
                if (child instanceof VariableNode) {
                    ValueDataNode expected = dataTreeGen.genInitialTree((VariableNode) child, root);
                    for (ValueDataNode actual : actualNodes) {
                        if (actual.getCorrespondingVar() == expected.getCorrespondingVar()) {
                            inputToExpectedOutputMap.put(actual, expected);
                            expected.setParent(actual.getParent());
                            expected.setExternal(false);
                            actualNodes.remove(actual);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Collection<ValueDataNode> getParamExpectedOuputs() {
        return inputToExpectedOutputMap.values();
    }

    public boolean putParamExpectedOutputs(ValueDataNode expectedOuput) {
        if (expectedOuput.getName().equals("RETURN"))
            return false;

        List<ValueDataNode> parameterNodes = Search2.searchParameterNodes(this);
        ValueDataNode input = parameterNodes.stream()
                .filter(child -> child.getCorrespondingVar() == expectedOuput.getCorrespondingVar())
                .findFirst()
                .orElse(null);

        if (input != null) {
            inputToExpectedOutputMap.remove(input);
            inputToExpectedOutputMap.put(input, expectedOuput);
            return true;
        }

        return false;
    }

    public boolean checkIsValidParamExpectedOuputs() {
        for (IDataNode input : getChildren()) {
            if (! input.getName().equals("RETURN")) {
                ValueDataNode eo = getExpectedOuput((ValueDataNode) input);
                if (eo == null) return false;
            }
        }

        return true;
    }

    public INode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(INode functionNode) {
        this.functionNode = functionNode;

        if (functionNode == null)
            return;

        setName(functionNode.getName());

        String type = ((ICommonFunctionNode) functionNode).getReturnType();
        String realType = type;
        VariableNode returnVarNode = Search2.getReturnVarNode((ICommonFunctionNode) functionNode);

        if (returnVarNode != null) {
            setCorrespondingVar(returnVarNode);
            type = returnVarNode.getRawType();
            realType = returnVarNode.getRealType();
        }

        type = VariableTypeUtils.deleteStorageClassesExceptConst(type);
        type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);

        realType = VariableTypeUtils.deleteStorageClassesExceptConst(realType);
        realType = VariableTypeUtils.deleteVirtualAndInlineKeyword(realType);

        setRawType(type);
        setRealType(realType);
    }

    public ValueDataNode getExpectedOuput(ValueDataNode inputValue) {
        ValueDataNode eo = inputToExpectedOutputMap.get(inputValue);
        return eo;
    }

    public String getDisplayName() {
        String prefixPath = "";

        INode originalNode = getFunctionNode();

        if (originalNode instanceof ICommonFunctionNode) {
            prefixPath = ((ICommonFunctionNode) originalNode).getSingleSimpleName();

            if (isLibrary())
                return prefixPath.replace(SourceConstant.STUB_PREFIX, SpecialCharacter.EMPTY);

            INode currentNode = originalNode.getParent();

            if (originalNode instanceof AbstractFunctionNode) {
                INode realParent = ((AbstractFunctionNode) originalNode).getRealParent();
                if (realParent != null)
                    currentNode = realParent;
            }

            while ((currentNode instanceof StructureNode || currentNode instanceof NamespaceNode)) {
                prefixPath = currentNode.getNewType() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + prefixPath;
                currentNode = currentNode.getParent();
            }
        }

        return prefixPath;
    }

    private boolean isLibrary() {
        if (functionNode == null)
            return false;

//        if (Environment.getInstance().getSystemLibraryRoot() == null)
//            return false;

        INode root = functionNode.getParent();

        if (root == null)
            return false;

        root = root.getParent();

        return false;
    }

    public boolean isStubable() {
        if (this instanceof ConstructorDataNode)
            return false;

        if (getRoot() instanceof RootDataNode) {
            NodeType type = ((RootDataNode) getRoot()).getLevel();

            if (type == NodeType.STUB || type == NodeType.SBF)
                return true;
        }

        if (getParent() instanceof UnitNode) {
            UnitNode unit = getUnit();

            return unit instanceof StubUnitNode;
        }

        return false;
    }

    public boolean isSut() {
        return getParent() instanceof UnitUnderTestNode;
    }

    public boolean isStub() {
        return !getChildren().isEmpty();
    }
}
