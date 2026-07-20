package uet.fit.aut.testcase;


import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.RootDataNode;

import java.util.List;
import java.util.Map;

public interface IDataTestItem extends ITestItem {

    RootDataNode getRootDataNode();

    void setRootDataNode(RootDataNode rootDataNode);

    ICommonFunctionNode getFunctionNode();

    void setFunctionNode(ICommonFunctionNode functionNode);

    Map<DataNode, List<String>> getAdditionalIncludePathsMap();

    void putOrUpdateDataNodeIncludes(DataNode dataNode);

    void putOrUpdateDataNodeIncludes(DataNode dataNode, String includePath);
}
