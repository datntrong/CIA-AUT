package uet.fit.aut.autogen.cfg.testpath;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.*;
import uet.fit.aut.autogen.instrument.FunctionInstrumentationForStatementvsBranch_Markerv2;
import uet.fit.aut.autogen.testdata.object.StatementInTestpath_Mark;
import uet.fit.aut.autogen.testdata.object.TestpathString_Marker;
import uet.fit.aut.autogen.testdatagen.ConcolicAutomatedTestdataGeneration;
import uet.fit.aut.autogen.testdatagen.coverage.CFGUpdaterv2;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.util.CFGUtils;
import uet.fit.aut.util.TestPathUtils;
import uet.fit.aut.util.Utils;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static uet.fit.aut.autogen.testdatagen.DirectedAutomatedTestdataGeneration.constructGraphEdges;

/**
 * Ex:
 * Stack<ICfgNode> testpath = new TestPathRetriever(testcase).get();
 */
public class TestPathRetriever {

    private Stack<ICfgNode> testPath;
    private Map<String, ICfgNode> mapping;
    private Graph<String, DefaultEdge> graph;

    public TestPathRetriever(TestCase testCase) {
        try {
            ICommonFunctionNode sut = testCase.getFunctionNode();
            //TODO
//            String cov = Environment.getInstance().getTypeofCoverage();
            String cov = "STATEMENT";

            ICFG currentCFG = null;
            if (sut instanceof MacroFunctionNode) {
                IFunctionNode tmpFunctionNode = ((MacroFunctionNode) sut).getCorrespondingFunctionNode();
                currentCFG = CFGUtils.createCFG(tmpFunctionNode, cov);
                currentCFG.setFunctionNode(tmpFunctionNode);
            } else if (sut instanceof AbstractFunctionNode) {
                currentCFG = CFGUtils.createCFG((IFunctionNode) sut, cov);
                currentCFG.setFunctionNode((IFunctionNode) sut);
            }

            String testPathFile = testCase.getTestPathFile();
            if (currentCFG != null && testPathFile != null && new File(testPathFile).exists()) {
                mapping = new HashMap<>();
                graph = new DefaultDirectedGraph<>(DefaultEdge.class);

                for (ICfgNode node : currentCFG.getExpandedNodes()) {
                    String name = ConcolicAutomatedTestdataGeneration.toDescription(node);
                    graph.addVertex(name);
                    mapping.put(name, node);
                }

                constructGraphEdges(graph, currentCFG);

                String testPathContent = Utils.readFileContent(testPathFile);
                String[] lines = testPathContent.split("\\R");
                TestpathString_Marker testPathMarker = new TestpathString_Marker();
                testPathMarker.setEncodedTestpath(lines);

                CFGUpdaterv2 cfgUpdaterv2 = new CFGUpdaterv2(testPathMarker, currentCFG);
                cfgUpdaterv2.updateVisitedNodes();

                testPath = traverse(currentCFG, testPathMarker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Stack<ICfgNode> get() {
        return testPath;
    }

    private Stack<ICfgNode> traverse(ICFG cfg, TestpathString_Marker marker) {
        Stack<ICfgNode> stack = new Stack<>();

        ICfgNode begin = cfg.getAllNodes().stream()
                .filter(n -> n instanceof BeginFlagCfgNode)
                .findFirst()
                .orElse(null);

        if (begin != null) {
            stack.push(begin);

            Map<String, ICfgNode> cache = new HashMap<>();

            for (StatementInTestpath_Mark line : marker.getStandardTestpathByAllProperties()) {
                if (line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Markerv2.START_OFFSET_IN_FUNCTION) != null) {

                    String offset = line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Markerv2.START_OFFSET_IN_FUNCTION).getValue();

                    ICfgNode node;

                    if (cache.containsKey(offset)) {
                        node = cache.get(offset);
                    } else {
                        node = cfg.getAllNodes().stream()
                                .filter(n -> {
                                    if (n.getAstLocation() != null) {
                                        String offsetInCFG = n.getAstLocation().getNodeOffset()
                                                - cfg.getFunctionNode().getAST().getFileLocation().getNodeOffset() + "";
                                        return offset.equals(offsetInCFG);
                                    }
                                    return false;
                                })
                                .findFirst()
                                .orElse(null);
                        if (node != null) {
                            cache.put(offset, node);
                        }
                    }

                    if (node != null) {
                        push(stack, node);
                    }
                }
            }

            if (marker.getEncodedTestpath().contains(TestPathUtils.END_TAG)) {
                ICfgNode exit = cfg.getAllNodes().stream()
                        .filter(n -> n instanceof EndFlagCfgNode)
                        .findFirst()
                        .orElse(null);

                if (exit != null) {
                    push(stack, exit);
                }
            }
        }

        return stack;
    }

    private void push(Stack<ICfgNode> stack, ICfgNode node) {
        ICfgNode top = stack.peek();
        if (isNextBy(top, node)) {
            stack.push(node);
        } else {
            List<ICfgNode> subPath = findPathBetween(top, node);
            for (int i = 1; i < subPath.size(); i++) {
                stack.push(subPath.get(i));
            }
        }
    }

    private List<ICfgNode> findPathBetween(ICfgNode from, ICfgNode to) {
        List<ICfgNode> shortestPath = new ArrayList<>();

        // Find shortest paths
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultEdge> iPaths = dijkstraAlg.getPaths(ConcolicAutomatedTestdataGeneration.toDescription(from));
        org.jgrapht.GraphPath<String, DefaultEdge> shortestTestPath = iPaths.getPath(ConcolicAutomatedTestdataGeneration.toDescription(to));

        if (shortestTestPath != null){
            for (String str : shortestTestPath.getVertexList()) {
                ICfgNode n = mapping.get(str);
                shortestPath.add(n);
            }
        }

        return shortestPath;
    }

    private boolean isNextBy(ICfgNode from, ICfgNode to) {
        if (from instanceof ConditionCfgNode) {
            return from.getFalseNode() == to || from.getTrueNode() == to;
        } else if (from instanceof SwitchCfgNode) {
            return ((SwitchCfgNode) from).getCases().contains(to);
        } else {
            return from.getFalseNode() == to || from.getTrueNode() == to;
        }
    }
}
