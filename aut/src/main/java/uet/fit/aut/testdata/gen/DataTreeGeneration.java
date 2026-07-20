package uet.fit.aut.testdata.gen;

import uet.fit.aut.env.Environment;
import uet.fit.aut.parser.dependency.DefinitionDependency;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.FunctionCallDependency;
import uet.fit.aut.parser.funcdetail.IFunctionDetailTree;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.CppFileNodeCondition;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.testdata.IDataTree;
import uet.fit.aut.testdata.gen.module.subtree.InitialStubTreeGen;
import uet.fit.aut.testdata.gen.module.subtree.InitialStubUnitBranchGen;
import uet.fit.aut.testdata.gen.module.subtree.InitialUUTBranchGen;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uet.fit.aut.util.NodeType.STUB;

/**
 * dựng cây Function Detail Tree của một test case (có UUT, STUB, GLOBAL)
 *
 * @author DucAnh
 */
public class DataTreeGeneration extends AbstractDataTreeGeneration {

    private IFunctionDetailTree functionTree;
    private IDataTree dataTree;

    public DataTreeGeneration() {
    }

    public DataTreeGeneration(IDataTree dataTree, IFunctionDetailTree functionTree)  {
        this.dataTree = dataTree;
        setRoot(dataTree.getRoot());
        setFunctionNode(functionTree.getUUT());
        this.functionTree = functionTree;
    }

    @Override
    public void generateTree() throws Exception {
        root.setFunctionNode(functionNode);

        // generate uut branch
        new InitialUUTBranchGen().generateCompleteTree(root, functionTree);

        // generate other sbfs
//        generateOtherSBFs();

        INode sourceCode = Utils.getSourcecodeFile(functionNode);
        INode projectRoot = Utils.getRoot(sourceCode);
        List<ISourcecodeFileNode> allCppFiles = Search.searchNodes(projectRoot, new CppFileNodeCondition());
        for (ISourcecodeFileNode sbf : allCppFiles) {
            if (!sourceCode.equals(sbf)) {
                List<FunctionNode> functions = Search.searchNodes(sbf, new FunctionNodeCondition());
                new InitialStubUnitBranchGen().generate(root, sbf, functions);
            }
        }

//        // generate stub branch
//        RootDataNode stubRoot = new RootDataNode(STUB);
//        root.addChild(stubRoot);
//        stubRoot.setParent(root);
//        new InitialStubTreeGen().generateCompleteTree(stubRoot, functionTree);
    }

    private void generateOtherSBFs() {
        Map<ISourcecodeFileNode, List<FunctionNode>> map = new HashMap<>();

        for (Dependency d : functionNode.getDependencies()) {
            // call other function
            if (d instanceof FunctionCallDependency && d.getStartArrow().equals(functionNode)) {
                INode end = d.getEndArrow();
                FunctionNode called = null;
                if (end instanceof FunctionNode) {
                    called = (FunctionNode) end;
                } else if (end instanceof DefinitionFunctionNode) {
                    for (Dependency ed : end.getDependencies()) {
                        if (ed instanceof DefinitionDependency
                                && ed.getStartArrow().equals(end)
                                && ed.getEndArrow() instanceof FunctionNode) {
                            called = (FunctionNode) ed.getEndArrow();
                            break;
                        }
                    }
                }
                if (called != null) {
                    ISourcecodeFileNode fileNode = Utils.getSourcecodeFile(called);
                    if (!map.containsKey(fileNode)) {
                        List<FunctionNode> list = new ArrayList<>();
                        map.put(fileNode, list);
                    }
                    map.get(fileNode).add(called);
                }
            }
        }

        Set<ISourcecodeFileNode> allCppFiles = map.keySet();
        INode sourceCode = Utils.getSourcecodeFile(functionNode);
        for (ISourcecodeFileNode sbf : allCppFiles) {
            if (!sourceCode.equals(sbf)) {
                Set<FunctionNode> functionNodes = new HashSet<>(map.get(sbf));
                new InitialStubUnitBranchGen().generate(root, sbf, functionNodes);
            }
        }
    }

    @Override
    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;

        if (dataTree != null)
            dataTree.setFunctionNode(functionNode);
    }
}
