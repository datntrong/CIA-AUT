package uet.fit.aut.testcase;

import uet.fit.aut.parser.funcdetail.FunctionDetailTree;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.search.Search2;
import uet.fit.aut.testdata.DataTree;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.GlobalRootDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.IUserCodeNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.UnitUnderTestNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.usercode.UserCodeManager;
import uet.fit.aut.usercode.objects.AbstractUserCode;
import uet.fit.aut.usercode.objects.ParameterUserCode;
import uet.fit.aut.usercode.objects.UsedParameterUserCode;
import uet.fit.aut.util.NodeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represent a single test case
 */
public class TestCase extends AbstractTestCase implements IDataTestItem {

    private RootDataNode rootDataNode;

    private ICommonFunctionNode functionNode;

    // map input to expected output of global variables
    private final Map<ValueDataNode, ValueDataNode> globalInputExpOutputMap = new HashMap<>();

    // map data node to include paths of the data node
    private final Map<DataNode, List<String>> additionalIncludePathsMap = new HashMap<>();

    public TestCase(ICommonFunctionNode functionNode, String name) {
        super();
        setName(name);
        FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
        DataTree dataTree = new DataTree(functionDetailTree);
        rootDataNode = dataTree.getRoot();
        setFunctionNode(functionNode);
        setId(UUID.randomUUID().toString());
    }

    public Map<ValueDataNode, ValueDataNode> getGlobalInputExpOutputMap() {
        return globalInputExpOutputMap;
    }

    // is only called when create new testcase
    public void initGlobalInputExpOutputMap() {
        try {
            globalInputExpOutputMap.clear();
            FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
            DataTree dataTree = new DataTree(functionDetailTree);
            RootDataNode root = dataTree.getRoot();
//            RootDataNode root = getRootDataNode();

            // children of the global data node of the root are used to be expected output values
            RootDataNode newGlobalDataNode = getGlobalDataNode(root);
            GlobalRootDataNode globalDataNode = getGlobalDataNode(rootDataNode);
            if (newGlobalDataNode != null && globalDataNode != null) {
                mapGlobalInputToExpOutput(globalDataNode.getChildren(), newGlobalDataNode.getChildren());
                globalDataNode.setGlobalInputExpOutputMap(globalInputExpOutputMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private GlobalRootDataNode getGlobalDataNode(RootDataNode root) {
        for (IDataNode uut : root.getChildren()) {
            if (uut instanceof UnitUnderTestNode) {
                for (IDataNode dataNode : uut.getChildren()) {
                    if (dataNode instanceof GlobalRootDataNode
                            && ((RootDataNode) dataNode).getLevel().equals(NodeType.GLOBAL)) {
                        return (GlobalRootDataNode) dataNode;
                    }
                }
            }
        }
        return null;
    }

    private void mapGlobalInputToExpOutput(List<IDataNode> inputs, List<IDataNode> expOutputs) {
        if (inputs != null) {
            for (IDataNode inputValue : inputs) {
                for (IDataNode expectedOutput : expOutputs) {
                    if (expectedOutput.getName().equals(inputValue.getName())) {
                        ((ValueDataNode) expectedOutput).setExternal(false);
                        expectedOutput.setParent(inputValue.getParent());
                        globalInputExpOutputMap.put((ValueDataNode) inputValue, (ValueDataNode) expectedOutput);
                    }
                }
            }
        }
    }

    public RootDataNode getRootDataNode() {
        return rootDataNode;
    }

    public void setRootDataNode(RootDataNode rootDataNode) {
        this.rootDataNode = rootDataNode;
    }

    public boolean initParameterExpectedOutputs() {
        if (rootDataNode != null) {
            SubprogramNode sut = Search2.findSubprogramUnderTest(rootDataNode);
            if (sut != null) {
                // init parameter expected output datanodes
                sut.initInputToExpectedOutputMap();
                return true;
            }
        }

        return false;
    }

    /**
     * put or update data node and is include paths (used by user code) to map
     *
     * @param dataNode data node that use user code
     */
    public void putOrUpdateDataNodeIncludes(DataNode dataNode) {
        if (dataNode instanceof IUserCodeNode) {
            AbstractUserCode uc = ((IUserCodeNode) dataNode).getUserCode();
            if (uc instanceof UsedParameterUserCode) {
                UsedParameterUserCode userCode = (UsedParameterUserCode) uc;
                List<String> includePaths = new ArrayList<>();
                if (userCode.getType().equals(UsedParameterUserCode.TYPE_CODE)) {
                    includePaths.addAll(userCode.getIncludePaths());
                } else if (userCode.getType().equals(UsedParameterUserCode.TYPE_REFERENCE)) {
                    ParameterUserCode reference = UserCodeManager.getInstance()
                            .getParamUserCodeById(userCode.getId());
                    includePaths.addAll(reference.getIncludePaths());
                }

                if (additionalIncludePathsMap.containsKey(dataNode)) {
                    List<String> paths = additionalIncludePathsMap.get(dataNode);
                    paths.clear();
                    paths.addAll(includePaths);
                } else {
                    additionalIncludePathsMap.put(dataNode, includePaths);
                }
            } else {
                additionalIncludePathsMap.remove(dataNode);
            }
        }
    }

    public void putOrUpdateDataNodeIncludes(DataNode dataNode, String includePath) {
        if (dataNode instanceof IUserCodeNode) {
            additionalIncludePathsMap.remove(dataNode);
            List<String> paths = new ArrayList<>();
            paths.add(includePath);
            additionalIncludePathsMap.put(dataNode, paths);
        }
    }

    public ICommonFunctionNode getFunctionNode() {
        if (functionNode == null) {
            if (rootDataNode != null)
                functionNode = rootDataNode.getFunctionNode();
        }
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    @Override
    public void setStatus(String status) {
        super.setStatus(status);
        if (status.equals(STATUS_NA)) {
            deleteOldData();
        }
    }

    /**
     * get all additional include headers used by user codes of test case
     */
    public List<String> getAdditionalIncludes() {
        // add from additionalIncludePathsMap
        List<String> allPaths = new ArrayList<>();
        for (Collection<String> collection : additionalIncludePathsMap.values()) {
            allPaths.addAll(collection);
        }

        // add from test case user code include paths
        allPaths.addAll(getTestCaseUserCode().getIncludePaths());

        return allPaths.stream().distinct().collect(Collectors.toList());
    }

    public Map<DataNode, List<String>> getAdditionalIncludePathsMap() {
        return additionalIncludePathsMap;
    }
}
