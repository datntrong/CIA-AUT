package uet.fit.aut.testcase;

import uet.fit.aut.parser.funcdetail.FunctionDetailTree;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testdata.DataTree;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.IUserCodeNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.usercode.UserCodeManager;
import uet.fit.aut.usercode.objects.AbstractUserCode;
import uet.fit.aut.usercode.objects.ParameterUserCode;
import uet.fit.aut.usercode.objects.UsedParameterUserCode;

import java.util.*;
import java.util.stream.Collectors;

public class TestPrototype extends TestItem implements IDataTestItem {

    public static final String PROTOTYPE_SIGNAL = "PROTOTYPE_";

    private RootDataNode rootDataNode;
    private ICommonFunctionNode functionNode;
    // map data node to include paths of the data node
    private final Map<DataNode, List<String>> additionalIncludePathsMap = new HashMap<>();

    public TestPrototype() {
        super();
    }

    public TestPrototype(ICommonFunctionNode functionNode, String name) {
        this();
        setName(name);
        FunctionDetailTree functionDetailTree = new FunctionDetailTree(functionNode);
        DataTree dataTree = new DataTree(functionDetailTree);
        rootDataNode = dataTree.getRoot();
        setFunctionNode(functionNode);
    }

    @Override
    public boolean isPrototypeTestcase() {
        return true;
    }

    @Override
    public RootDataNode getRootDataNode() {
        return rootDataNode;
    }

    @Override
    public void setRootDataNode(RootDataNode rootDataNode) {
        this.rootDataNode = rootDataNode;
    }

    @Override
    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    @Override
    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
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
//        allPaths.addAll(getTestCaseUserCode().getIncludePaths());

        return allPaths.stream().distinct().collect(Collectors.toList());
    }

    public Map<DataNode, List<String>> getAdditionalIncludePathsMap() {
        return additionalIncludePathsMap;
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
}
