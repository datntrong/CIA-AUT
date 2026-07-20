package uet.fit.aut.autogen.testdatagen;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.BranchInCFG;
import uet.fit.aut.autogen.cfg.object.CaseCfgNode;
import uet.fit.aut.autogen.cfg.object.ConditionCfgNode;
import uet.fit.aut.autogen.cfg.object.EndFunctionCallFlagCfgNode;
import uet.fit.aut.autogen.cfg.object.ICfgNode;
import uet.fit.aut.autogen.cfg.object.NormalCfgNode;
import uet.fit.aut.autogen.cfg.object.SwitchCfgNode;
import uet.fit.aut.autogen.testdatagen.se.IPathConstraints;
import uet.fit.aut.autogen.testdatagen.se.ISymbolicExecution;
import uet.fit.aut.autogen.testdatagen.se.Parameters;
import uet.fit.aut.autogen.testdatagen.se.PathConstraint;
import uet.fit.aut.autogen.testdatagen.se.PathConstraints;
import uet.fit.aut.autogen.testdatagen.se.solver.RunZ3OnCMD;
import uet.fit.aut.autogen.testdatagen.se.solver.SmtLibGeneration;
import uet.fit.aut.autogen.testdatagen.testdatainit.BasicTypeRandom;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class CFDSAutomatedTestdataGeneration extends ConcolicAutomatedTestdataGeneration {

    private final static AUTLogger logger = AUTLogger.get(CFDSAutomatedTestdataGeneration.class);

    private static final int DEFAULT_CFG_NODE_WEIGHT = 0;
    private static final int DEFAULT_CONDITION_WEIGHT = 1;
    private static final int MAX_WEIGHT = 999;

    private BranchInCFG unvisitedBranch = null;
    private final Stack<Pair<ICfgNode, ICfgNode>> edgeMax = new Stack<>();
    private ICfgNode unvisitedNode = null; // node cannot resolve in first path

    public CFDSAutomatedTestdataGeneration(ICommonFunctionNode fn, String coverageType) {
        super(fn, coverageType);
    }

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
                    List<ICfgNode> unvisitedInstructions = currentCFG.getUnvisitedStatements();
                    logger.debug("All node unvisited: " + unvisitedInstructions);

                    ICfgNode unvisitedInstruction = unvisitedInstructions.get(
                            new BasicTypeRandom().generateInt(0, unvisitedInstructions.size() - 1));

                    if (unvisitedNode != null)
                        unvisitedInstruction = unvisitedNode;

                    logger.debug("Choose unvisited stm \"" + unvisitedInstruction + "\"");

                    // find a shortest test path through unvisited instructions
                    List<ICfgNode> shortestTestpath = findShortestTestpath(unvisitedInstruction, null, currentCFG, 0);
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

                    if (unvisitedBranch == null) unvisitedBranch = selectedBranch;
                    ICfgNode unvisitedInstructions = unvisitedBranch.getEnd();
                    ICfgNode unvisitedStart = unvisitedBranch.getStart();

                    logger.debug("Choose unvisited branches \"" + unvisitedStart + "\" -> \"" + unvisitedInstructions + "\"");

                    // find a shortest test path through unvisited instructions
                    List<ICfgNode> shortestTestpath = findShortestTestpath(unvisitedStart, unvisitedInstructions, currentCFG, 0);
                    logger.debug("Shortest test path: " + shortestTestpath);
                    return shortestTestpath;
                }
            }
        }

        return new ArrayList<>();
    }

    private List<ICfgNode> findShortestTestpath(ICfgNode target, ICfgNode end, ICFG currentCFG, int randTimes) {
        List<ICfgNode> shortestPath = new ArrayList<>();
        Map<String, ICfgNode> mapping = new HashMap<>();

        Graph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (ICfgNode node : currentCFG.getExpandedNodes()) {
            String name = toDescription(node);
            graph.addVertex(name);

            mapping.put(name, node);
        }

        constructGraphEdges(graph, currentCFG);

        // Find shortest paths
        logger.debug("Shortest path from begin node of CFG to " + target + " :");
        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        ICfgNode beginNode = currentCFG.getBeginNode();
        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultWeightedEdge> iPaths = dijkstraAlg.getPaths(toDescription(beginNode));
        org.jgrapht.GraphPath<String, DefaultWeightedEdge> shortestTestPath = iPaths.getPath(toDescription(target));

        for (String str : shortestTestPath.getVertexList()) {
            ICfgNode n = mapping.get(str);
            shortestPath.add(n);
        }
        if (end != null && shortestPath.get(shortestPath.size() - 1) != end)
            shortestPath.add(end);

        //random num of iterate for loop
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

//    private static List<Integer> findPeerLoops(List<ICfgNode> shortestPath, int start, int end){
//        List<Integer> indexOfPeerForNodes = new ArrayList<>();
//        int trackLoop = 0;
//        for (int i = start; i < end; i++){
//            if (shortestPath.get(i) instanceof AdditionalScopeCfgNode){
//                if (shortestPath.get(i).getContent().equals("{"))
//                    trackLoop++;
//                else trackLoop--;
//            }
//            if (shortestPath.get(i) instanceof ConditionForCfgNode && trackLoop == 1)
//                indexOfPeerForNodes.add(i);
//        }
//        return indexOfPeerForNodes;
//    }
//
//    private static List<ICfgNode> findNestedPath(List<ICfgNode> shortestPath, List<ICfgNode> tmpShortestPath, DijkstraShortestPath dijkstraAlg, Map<String,
//            ICfgNode> mapping, int currentIndexInShortestPath, int end, int endIdx, int numOfRandLoop){
//        List<ICfgNode> nestedTestPath = new ArrayList<>();
//
//        for (int j = currentIndexInShortestPath; j < endIdx; j++)
//            if (shortestPath.get(j) instanceof AdditionalScopeCfgNode
//                    && shortestPath.get(j).getContent().equals("{")){
//                for (int ii = currentIndexInShortestPath - 1; ii <= j; ii++){
//                    tmpShortestPath.add(shortestPath.get(ii));
//                }
//                for (int k = j; k < end; k++){
//                    if (shortestPath.get(k) instanceof AdditionalScopeCfgNode
//                            && shortestPath.get(k).getContent().equals("}")){
//                        nestedTestPath =  randomIterateForLoop(dijkstraAlg, mapping, shortestPath, j, k, numOfRandLoop);
//                    } else {
//                        nestedTestPath =  randomIterateForLoop(dijkstraAlg, mapping, shortestPath, j, end, numOfRandLoop);
//                    }
//                }
//            }
//        return nestedTestPath;
//    }
//
//    private static List<ICfgNode> randomIterateForLoop(DijkstraShortestPath dijkstraAlg, Map<String,
//                                                    ICfgNode> mapping, List<ICfgNode> shortestPath, int start, int end, int randTimes) {
//        int numOfRandLoop;
//        if (randTimes != 0)
//            numOfRandLoop = randTimes;
//        else
//            numOfRandLoop = 4;
//        // find all peer for loop node
//        List<Integer> indexOfPeerForNodes;
//        indexOfPeerForNodes = findPeerLoops(shortestPath, start, end);
//
//        // initiate tmpShortestPath
//        List<ICfgNode> tmpShortestPath = new ArrayList<>();
//        if (indexOfPeerForNodes.size() > 0) {
//            for (int i = start; i < indexOfPeerForNodes.get(0) - 2; i++){
//                tmpShortestPath.add(shortestPath.get(i));
//            }
//        }
//        // with each for loop
//        for (int idx = 0; idx < indexOfPeerForNodes.size(); idx++){
//            int currentIndexInShortestPath = indexOfPeerForNodes.get(idx);
//
//            // check if nested for loop
//            int endIdx;
//            if (idx < indexOfPeerForNodes.size() - 1)
//                endIdx = indexOfPeerForNodes.get(idx + 1) - 2;
//            else
//                endIdx = end;
//            List<ICfgNode> nestedTestPath = new ArrayList<>();
//            nestedTestPath = findNestedPath(shortestPath, tmpShortestPath, dijkstraAlg, mapping, currentIndexInShortestPath, end, endIdx, numOfRandLoop);
//
//            if (nestedTestPath.size() > 0)
//                tmpShortestPath.addAll(nestedTestPath);
//            else {
//                // find path to iterate through for loop
//                ICfgNode tmpNode = null;
//
//                for (int i = currentIndexInShortestPath - 1; i <= endIdx; i++){
//                    tmpNode = shortestPath.get(i);
//                    tmpShortestPath.add(tmpNode);
//                    if (tmpNode instanceof ConditionForCfgNode) {
//                        ICfgNode targetNode = tmpNode;
////                if (i != shortestPath.size() - 1) {
////                    ICfgNode beginNode = shortestPath.get(i+1);
//                        ICfgNode beginNode = targetNode.getTrueNode();
//                        // find additional path
//                        List<ICfgNode> shortestPath2 = new ArrayList<>();
//                        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultEdge> iPaths2 = dijkstraAlg.getPaths(toDescription(beginNode));
//                        org.jgrapht.GraphPath<String, DefaultEdge> shortestTestPath2 = iPaths2.getPath(toDescription(targetNode));
//                        if (shortestTestPath2 != null){
//                            for (String str : shortestTestPath2.getVertexList()) {
//                                ICfgNode n = mapping.get(str);
//                                shortestPath2.add(n);
//                            }
//                        }
//                        // insert additional path into shortestPath
//                        for (int j = 0; j < numOfRandLoop; j++) {
//                            tmpShortestPath.addAll(shortestPath2);
//                        }
////                }
//                    }
//                }
//            }
//        }
//        return tmpShortestPath;
//    }

    private void constructGraphEdges(Graph<String, DefaultWeightedEdge> graph, ICFG currentCFG) {
        for (ICfgNode node : currentCFG.getExpandedNodes()) {
            if (node instanceof SwitchCfgNode) {
                for (ICfgNode casenode : ((SwitchCfgNode) node).getCases()) {
                    Pair<ICfgNode, ICfgNode> edge = new Pair<>(node, casenode);
                    int caseWeight = DEFAULT_CONDITION_WEIGHT;
                    if (edgeMax.search(edge) != -1) {
                        caseWeight = MAX_WEIGHT;
                    }
                    addEdge(graph, node, casenode, caseWeight);
                }
            } else {
                int trueNodeWeight = DEFAULT_CFG_NODE_WEIGHT;
                int falseNodeWeight = DEFAULT_CFG_NODE_WEIGHT;
                if (node instanceof ConditionCfgNode) {
                    trueNodeWeight = DEFAULT_CONDITION_WEIGHT;
                    falseNodeWeight = DEFAULT_CONDITION_WEIGHT;
                }
                ICfgNode trueNode = node.getTrueNode();
                ICfgNode falseNode = node.getFalseNode();
                Pair<ICfgNode, ICfgNode> trueNodeEdge = new Pair<>(node, trueNode);
                if (edgeMax.search(trueNodeEdge) != -1) {
                    trueNodeWeight = MAX_WEIGHT;
                }
                Pair<ICfgNode, ICfgNode> falseNodeEdge = new Pair<>(node, falseNode);
                if (edgeMax.search(falseNodeEdge) != -1) {
                    falseNodeWeight = MAX_WEIGHT;
                }

                if (trueNode != null && falseNode != null) {
                    if (!trueNode.equals(falseNode)) {
                        addEdge(graph, node, trueNode, trueNodeWeight);
                        addEdge(graph, node, falseNode, falseNodeWeight);
                    } else
                        addEdge(graph, node, trueNode, trueNodeWeight);
                } else if (trueNode != null) {
                    addEdge(graph, node, trueNode, trueNodeWeight);
                } else if (falseNode != null) {
                    addEdge(graph, node, falseNode, falseNodeWeight);
                }

            }
        }
    }

    private static void addEdge(Graph<String, DefaultWeightedEdge> graph, ICfgNode from, ICfgNode to, int weight) {
        if (to instanceof NormalCfgNode && !((NormalCfgNode) to).getSubCFGs().isEmpty()) {
            Collection<ICFG> subCFGs = ((NormalCfgNode) to).getSubCFGs().values();
            ICfgNode prevNode = from;
            for (ICFG subCFG : subCFGs) {
                ICfgNode newTo = subCFG.getBeginNode();
                graph.addEdge(toDescription(prevNode), toDescription(newTo));
                graph.setEdgeWeight(toDescription(prevNode), toDescription(newTo), weight);

                for (ICfgNode cfgNode: subCFG.getAllNodes()) {
                    if (cfgNode instanceof EndFunctionCallFlagCfgNode){
                        prevNode = cfgNode;
                        break;
                    }
                }

//                prevNode = subCFG.getAllNodes().get(subCFG.getAllNodes().size() - 1);
            }

            graph.addEdge(toDescription(prevNode), toDescription(to));
            graph.setEdgeWeight(toDescription(prevNode), toDescription(to), weight);
        } else {
            graph.addEdge(toDescription(from), toDescription(to));
            graph.setEdgeWeight(toDescription(from), toDescription(to), weight);
            if (from instanceof ConditionCfgNode) System.out.print("Condition: ");
            System.out.println(toDescription(from) + "->>>" + toDescription(to) + ": " + weight);
        }

    }

    private static final String NO_SOLUTION_TAG = "unsat";

    @Override
    protected void onZ3Solution(ISymbolicExecution se, Parameters parameters, String solution) {
        if (solution.contains(NO_SOLUTION_TAG)) {
            handleUnsolvedConstraints(se, parameters);
            List<ICfgNode> testpath = se.getTestpath().cast();
            unvisitedNode = testpath.get(testpath.size() - 1);
        } else {
            if (unvisitedNode != null) {
                while (!edgeMax.isEmpty()) {
                    edgeMax.pop();
                }
                unvisitedNode = null;
            }

            if (unvisitedBranch != null) {
                while (!edgeMax.isEmpty()) {
                    edgeMax.pop();
                }
                unvisitedBranch = null;
            }
        }
    }

    private void handleUnsolvedConstraints(ISymbolicExecution se, Parameters parameters) {
        IPathConstraints constraints = se.getConstraints();
        PathConstraint errorConstraint = ((PathConstraints) constraints).get(0);
        while (constraints.size() > 1) {
            errorConstraint = ((PathConstraints) constraints).get(0);
            ((PathConstraints) constraints).remove(0);
            try {
                //smt-lib2
                SmtLibGeneration smt = new SmtLibGeneration(parameters, (List<PathConstraint>) se.getNormalizedPathConstraints(), fn, se.getNewVariables());
                smt.generate();
                logger.debug("SMT-LIB file:\n" + smt.getSmtLibContent());
                String constraintFile = generateRandomConstraintPath();
                logger.debug("constraintFile: " + constraintFile);
                Utils.writeContentToFile(smt.getSmtLibContent(), constraintFile);
                // solve
                String z3Path = Z3_PATH;
                RunZ3OnCMD z3Runner = new RunZ3OnCMD(z3Path, constraintFile);
                z3Runner.execute();
                if (!(z3Runner.getSolution().contains(NO_SOLUTION_TAG))) {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // error constraint
        logger.debug("Error:\n" + errorConstraint);
        ICfgNode errCfgNode = errorConstraint.getCfgNode();
        if (errCfgNode instanceof CaseCfgNode) {
            edgeMax.push(new Pair<>(errCfgNode.getParent(), errCfgNode));
        } else {
            ICfgNode defaultNodeOfEC = errCfgNode.getDefaultNode();
            if (defaultNodeOfEC != null) {
                edgeMax.push(new Pair<>(defaultNodeOfEC, defaultNodeOfEC.getFalseNode()));
            } else {
                edgeMax.push(new Pair<>(errCfgNode, errCfgNode.getTrueNode()));
            }
        }

    }
}
