package uet.fit.aut.autogen.testdatagen;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.*;
import uet.fit.aut.autogen.testdatagen.testdatainit.BasicTypeRandom;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.testcase.ITestCase;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedAutomatedTestdataGeneration extends ConcolicAutomatedTestdataGeneration {
    private final static AUTLogger logger = AUTLogger.get(DirectedAutomatedTestdataGeneration.class);

    public DirectedAutomatedTestdataGeneration(ICommonFunctionNode fn, String coverageType) {
        super(fn, coverageType);
    }

    @Override
    protected String getTestCaseNamePrefix(ICommonFunctionNode fn) {
        return fn.getSimpleName() + ITestCase.POSTFIX_TESTCASE_BY_DIRECTED_METHOD;
    }

    @Override
    protected List<ICfgNode> getTestPathByCoverage(ICFG currentCFG, String coverageType) {
        currentCFG.setIdforAllNodes();

        switch (coverageType) {
            case CoverageType.STATEMENT: {
                logger.debug("Visited stm = " + currentCFG.getVisitedStatements().size());
                int nStm = currentCFG.getVisitedStatements().size() + currentCFG.getUnvisitedStatements().size();
                logger.debug("Total stm = " + nStm);

                if (currentCFG.getUnvisitedStatements().size() == 0) {
                    maximizeCov = true;
                    return new ArrayList<>();
                } else {
                    List <ICfgNode> unvisitedInstructions = currentCFG.getUnvisitedStatements();
                    ICfgNode unvisitedInstruction = unvisitedInstructions.get(
                            new BasicTypeRandom().generateInt(0, unvisitedInstructions.size() - 1));
                    logger.debug("Choose unvisited stm \"" + unvisitedInstruction + "\"");

                    // find a shortest test path through unvisited instructions
                    List<ICfgNode> shortestTestpath = findShortestTestpath(unvisitedInstruction, currentCFG, 0);
                    logger.debug("Shortest test path: " + shortestTestpath);

                    return shortestTestpath;
                }
            }

            case CoverageType.STATEMENT_AND_BRANCH:
            case CoverageType.STATEMENT_AND_MCDC:
            case CoverageType.BASIS_PATH:
            case CoverageType.BRANCH:
            case CoverageType.MCDC: {
                logger.debug("Visited branches = " + currentCFG.getVisitedBranches().size());
                int nStm = currentCFG.getVisitedBranches().size() + currentCFG.getUnvisitedBranches().size();
                logger.debug("Total branches = " + nStm);

                if (currentCFG.getUnvisitedBranches().size() == 0) {
                    maximizeCov = true;
                    return new ArrayList<>();
                } else {
                    BranchInCFG selectedBranch = currentCFG.getUnvisitedBranches().get(
                            new BasicTypeRandom().generateInt(0, currentCFG.getUnvisitedBranches().size() - 1));
                    ICfgNode unvisitedInstructions = selectedBranch.getEnd();
                    logger.debug("Choose unvisited branches \"" + selectedBranch.getStart() + "\" -> \"" + selectedBranch.getEnd() + "\"");

                    // find a shortest test path through unvisited instructions
                    List<ICfgNode> shortestTestpath = findShortestTestpath(unvisitedInstructions, currentCFG, 0);
                    logger.debug("Shortest test path: " + shortestTestpath);

                    return shortestTestpath;
                }
            }
        }
        return new ArrayList<>();
    }

    public List<ICfgNode> findShortestTestpath(ICfgNode target, ICFG currentCFG, int randTimes) {
        List<ICfgNode> shortestPath = new ArrayList<>();
        Map<String, ICfgNode> mapping = new HashMap<>();

        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (ICfgNode node : currentCFG.getExpandedNodes()) {
            String name = toDescription(node);
            graph.addVertex(name);

            mapping.put(name, node);
        }

        constructGraphEdges(graph, currentCFG);

        // Find shortest paths
        logger.debug("Shortest path from begin node of CFG to " + target + " :");
        DijkstraShortestPath dijkstraAlg = new DijkstraShortestPath<>(graph);
        ICfgNode beginNode = currentCFG.getBeginNode();
        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultEdge> iPaths = dijkstraAlg.getPaths(toDescription(beginNode));
        org.jgrapht.GraphPath<String, DefaultEdge> shortestTestPath = iPaths.getPath(toDescription(target));

        for (String str : shortestTestPath.getVertexList()) {
            ICfgNode n = mapping.get(str);
            shortestPath.add(n);
        }

        // random the num of iterate for loop
        if (haveLoop(shortestPath)) {
            shortestPath = completePathForFixedLoop(shortestPath, dijkstraAlg, mapping);
        }
        logger.debug("shortestPath = " + shortestPath);

        // Normalize path
        List<ICfgNode> normalizedShortestPath = normalizePath(shortestPath);
        logger.debug("normalizedShortestPath = " + normalizedShortestPath);

        return normalizedShortestPath;
    }

    public static void constructGraphEdges(Graph<String, DefaultEdge> graph, ICFG currentCFG) {
        for (ICfgNode node : currentCFG.getExpandedNodes()) {
            if (node instanceof SwitchCfgNode) {
                for (ICfgNode casenode : ((SwitchCfgNode) node).getCases()) {
                    addEdge(graph, node, casenode);
                }
            } else {
                ICfgNode trueNode = node.getTrueNode();
                ICfgNode falseNode = node.getFalseNode();

                if (trueNode != null && falseNode != null) {
                    if (!trueNode.equals(falseNode)) {
                        addEdge(graph, node, trueNode);
                        addEdge(graph, node, falseNode);
                    } else
                        addEdge(graph, node, trueNode);
                } else if (trueNode != null) {
                    addEdge(graph, node, trueNode);
                } else if (falseNode != null) {
                    addEdge(graph, node, falseNode);
                }
            }

        }
    }

    private static void addEdge(Graph<String, DefaultEdge> graph, ICfgNode from, ICfgNode to) {
        if (to instanceof NormalCfgNode && !((NormalCfgNode) to).getSubCFGs().isEmpty()) {
            Collection<ICFG> subCFGs = ((NormalCfgNode) to).getSubCFGs().values();
            ICfgNode prevNode = from;
            for (ICFG subCFG : subCFGs) {
                ICfgNode newTo = subCFG.getBeginNode();
                graph.addEdge(toDescription(prevNode), toDescription(newTo));
                prevNode = subCFG.getAllNodes().get(subCFG.getAllNodes().size() - 1);
            }
            graph.addEdge(toDescription(prevNode), toDescription(to));
        } else {
            graph.addEdge(toDescription(from), toDescription(to));
        }
    }
}