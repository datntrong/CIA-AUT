package uet.fit.aut.autogen.testdatagen.coverage;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.ConditionCfgNode;
import uet.fit.aut.autogen.cfg.object.ICfgNode;
import uet.fit.aut.autogen.cfg.testpath.FullTestpath;
import uet.fit.aut.autogen.cfg.testpath.ITestpathInCFG;
import uet.fit.aut.autogen.maker.StatementInTestpath_Mark;
import uet.fit.aut.autogen.maker.TestpathString_Marker;
import uet.fit.aut.coverage.basicpath.BasicPath;
import uet.fit.aut.instrument.FunctionInstrumentationForStatementvsBranch_Marker;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.TestPathUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Update the visited state of CFG.
 *
 */
public class CFGUpdater implements ICFGUpdater {

    private final TestpathString_Marker testpath;
    private final ICFG cfg;

    public CFGUpdater(TestpathString_Marker testpath, ICFG cfg) {
        this.testpath = testpath;
        this.cfg = cfg;
    }

    @Override
    public void updateVisitedNodes() {
        // find all nodes corresponding to statements or conditions
        Set<String> visitedOffsets = getAllVisitedNodesByItsOffset(testpath);

        // update the visited state of nodes
        updateStateOfVisitedNodeByItsOffset(cfg, visitedOffsets);

        // create a chain of visited statement in order
        updateVisitedStateOfBranches(testpath, cfg);

        updateVisitedStateOfBasicPath(testpath, cfg);
    }

    private void updateVisitedStateOfBasicPath(TestpathString_Marker testpath, ICFG cfg) {
        String[] testpathItems = testpath.getEncodedTestpath().split(ITestpathInCFG.SEPARATE_BETWEEN_NODES);

        List<String> visitedOffsets = new ArrayList<>();

        for (int i = 0; i < testpathItems.length; i++) {
            String item = testpathItems[i];
            if (item.contains(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION))
            {
                StatementInTestpath_Mark line = TestpathString_Marker.lineExtractor(item);
                if (line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION) != null &&
                        line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION) != null) {
                    // is statement or condition
                    visitedOffsets.add(line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION).getValue());
                }
            }
            if (i != 0 && (item.startsWith(TestPathUtils.BEGIN_TAG) || i == testpathItems.length - 1)) {
                markVisitedBasicPath(visitedOffsets, cfg);
                visitedOffsets = new ArrayList<>();
            }
        }
    }

    private void markVisitedBasicPath(List<String> visitedOffsets, ICFG cfg) {
        List<ICfgNode> visitedNodes = new ArrayList<>();

        for (ICfgNode node : cfg.getAllNodes()) {
            if (node.getAstLocation() != null) {
                String offsetInCFG = node.getAstLocation().getNodeOffset() - cfg.getFunctionNode().getAST().getFileLocation().getNodeOffset() + "";
                if (visitedOffsets.contains(offsetInCFG)) {
                    visitedNodes.add(node);
                }
            }
        }

        for (BasicPath basicPath : cfg.getAllBasicPaths()) {
            List<ICfgNode> clone = new ArrayList<>(visitedNodes);

            boolean isVisited = true;

            for (ICfgNode node : basicPath) {
                if (node.getAstLocation() != null) {
                    if (!visitedNodes.contains(node)) {
                        isVisited = false;
                        break;
                    } else {
                        clone.remove(node);
                    }
                }
            }

            if (isVisited && clone.isEmpty())
                basicPath.setVisited(true);
        }
    }

    private void updateVisitedStateOfBranches(TestpathString_Marker testpath, ICFG cfg) {
        String visitedStatementInStr = " ";
        for (String offset : testpath.getStandardTestpathByProperty(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION))
            visitedStatementInStr += offset + " ";
//        logger.debug("visitedStatementInStr = " + visitedStatementInStr);

        int offsetOfFunctionInSourcecodeFile = cfg.getFunctionNode().getAST().getFileLocation().getNodeOffset();
        for (ICfgNode visitedNode : cfg.getVisitedStatements()) {
            if (visitedNode instanceof ConditionCfgNode) { // condition
                // analyze the true branch
                ICfgNode trueBranchNode = visitedNode.getTrueNode();
                boolean isUpdatedAsVisited = updateTheStateOfBranches(trueBranchNode, visitedNode, offsetOfFunctionInSourcecodeFile, visitedStatementInStr);
                if (isUpdatedAsVisited)
                    ((ConditionCfgNode) visitedNode).setVisitedTrueBranch(true);

                // analyze the false branch
                ICfgNode falseBranchNode = visitedNode.getFalseNode();
                isUpdatedAsVisited = updateTheStateOfBranches(falseBranchNode, visitedNode, offsetOfFunctionInSourcecodeFile, visitedStatementInStr);
                if (isUpdatedAsVisited)
                    ((ConditionCfgNode) visitedNode).setVisitedFalseBranch(true);
            }
        }
    }

    private Set<String> getAllVisitedNodesByItsOffset(TestpathString_Marker testpath) {
        Set<String> visitedOffsets = new HashSet<>();
        for (StatementInTestpath_Mark line : testpath.getStandardTestpathByAllProperties()) {
            if (line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION) != null &&
                    line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION) != null) {
                // is statement or condition
                String path = line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.FUNCTION_ADDRESS).getValue();
                path = PathUtils.normalize(path);
                String targetPath = PathUtils.normalize(cfg.getFunctionNode().getAbsolutePath());
                if (path.equals(targetPath)) {
                    visitedOffsets.add(line.getPropertyByName(FunctionInstrumentationForStatementvsBranch_Marker.START_OFFSET_IN_FUNCTION).getValue());
                }
            }
        }
        return visitedOffsets;
    }

    private void updateStateOfVisitedNodeByItsOffset(ICFG cfg, Set<String> visitedOffsets) {
        List<ICfgNode> nodes = cfg.getAllNodes();
        for (ICfgNode node : nodes)
            if (node.getAstLocation() != null) {
                String offsetInCFG = node.getAstLocation().getNodeOffset() - cfg.getFunctionNode().getAST().getFileLocation().getNodeOffset() + "";
                if (visitedOffsets.contains(offsetInCFG)) {
                    node.setVisit(true);
                }
            }
     }

    private boolean updateTheStateOfBranches(ICfgNode branchNode, ICfgNode visitedNode,
                                             int offsetOfFunctionInSourcecodeFile, String visitedStatementInStr) {
        if (visitedNode instanceof ConditionCfgNode && branchNode != null) {
            boolean isUpdated = false;
            List<ICfgNode> flagNodes = new ArrayList<>();

            int offsetVisitedNode = visitedNode.getAstLocation().getNodeOffset();

            // ignore nodes corresponding to flag
            while (branchNode != null && branchNode.isSpecialCfgNode()) {
                flagNodes.add(branchNode);
                branchNode = branchNode.getTrueNode();  // for these type of nodes, true node and false node are the same
            }

            if (branchNode == null){
                // the current node point to the end node of cfg
                for (ICfgNode flagNode : flagNodes)
                    flagNode.setVisit(true);
                return true;

            } else {
                // make a comparison to check whether the branch node is visited
                String comparison = " " + (offsetVisitedNode - offsetOfFunctionInSourcecodeFile) + " " +
                        (branchNode.getAstLocation().getNodeOffset() - offsetOfFunctionInSourcecodeFile) + " ";
                if (visitedStatementInStr.contains(comparison)) {
                    isUpdated = true;
                    branchNode.setVisit(true);
//                    logger.debug("updated the branch " + comparison);
                    // update the flag nodes between the condition node and the normal nodes in its checked branch
                    for (ICfgNode flagNode : flagNodes)
                        flagNode.setVisit(true);
                    return isUpdated;
                } else
                    return false;
            }
        } else
            return false;
    }

    @Override
    public ICFG getCfg() {
        return cfg;
    }

    @Override
    public ITestpathInCFG getUpdatedCFGNodes() {
        ITestpathInCFG updatedCFGNodes = new FullTestpath();
        for (ICfgNode node : getCfg().getVisitedStatements()) {
            updatedCFGNodes.getAllCfgNodes().add(node);
        }
        return updatedCFGNodes;
    }
}
