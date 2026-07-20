package uet.fit.aut.autogen.cfg;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTIfStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import uet.fit.aut.autogen.cfg.object.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generate control flow graph from source code for statement/branch coverage
 *
 * @author DucAnh
 */
public class CFGGenerationforBranchvsStatementvsBasispathCoverage implements ICFGGeneration {

    private final static Logger logger = LoggerFactory.getLogger(CFGGenerationforBranchvsStatementvsBasispathCoverage.class);

    /**
     * Represent the begin node of CFG
     */
    protected ICfgNode BEGIN;

    /**
     * Represent the end node of CFG
     */
    protected ICfgNode END;

    protected Map<String, MarkFlagCfgNode> labels;

    protected IFunctionNode functionNode;

    public CFGGenerationforBranchvsStatementvsBasispathCoverage() {
    }

    public CFGGenerationforBranchvsStatementvsBasispathCoverage(IFunctionNode normalizedFunction) {
        functionNode = normalizedFunction;
    }

    @Override
    public ICFG generateCFG() {
        ICFG cfg = parse(functionNode);
        setParentAgain(cfg);
        cfg.setFunctionNode(functionNode);
        return cfg;
    }

    /**
     * For fix bugs. Reason: Some scope nodes ("{", "}") do not have its
     * parents.
     *
     * @param cfg
     * @return
     */
    private void setParentAgain(ICFG cfg) {
        for (ICfgNode cfgNode : cfg.getAllNodes()) {
            if (cfgNode instanceof ReturnNode) {
                // prevent
            } else {
                if (cfgNode.getTrueNode() != null)
                    cfgNode.getTrueNode().setParent(cfgNode);

                if (cfgNode.getFalseNode() != null)
                    cfgNode.getFalseNode().setParent(cfgNode);
            }
        }
    }

    private boolean isThrowStatement(IASTStatement stm) {
        if (stm instanceof IASTExpressionStatement) {
            IASTExpression ex = ((IASTExpressionStatement) stm).getExpression();

            if (ex instanceof IASTUnaryExpression)
                return ((IASTUnaryExpression) ex).getOperator() == IASTUnaryExpression.op_throw;
            else
                return false;
        } else
            return false;
    }

    private IASTExpression joinCaseSwitch(IASTExpression cond, ArrayList<IASTCaseStatement> cases) {
        INodeFactory fac = cond.getTranslationUnit().getASTNodeFactory();
        IASTExpression build = fac.newBinaryExpression(IASTBinaryExpression.op_equals,
                cond.copy(CopyStyle.withLocations), cases.get(0).getExpression().copy(CopyStyle.withLocations));

        for (int i = 1; i < cases.size(); i++) {
            IASTExpression build2 = fac.newBinaryExpression(IASTBinaryExpression.op_equals,
                    cond.copy(CopyStyle.withLocations), cases.get(i).getExpression().copy(CopyStyle.withLocations));
            build = fac.newBinaryExpression(IASTBinaryExpression.op_logicalOr, build, build2);
        }
        return build;
    }

    /**
     * Get next statement that is not temporary statement
     */
    private ICfgNode nextConcrete(ICfgNode stm) {
        while (stm instanceof ForwardCfgNode) {
            if (stm.getAstLocation() != null)
                break;
            stm = stm.getTrueNode();
        }
        return stm;
    }

    /**
     * Shorten expression by removing unnecessary "(" and ")"
     */
    private IASTExpression normalize(IASTExpression ex) {
        return (IASTExpression) Utils.shortenAstNode(ex);
    }

    private boolean notNull(IASTNode node) {
        return node != null && !(node instanceof IASTNullStatement);
    }

    private void linkStatement(ICfgNode root, List<ICfgNode> stmList) {
        if (root == null || root.isVisited())
            return;
        root.setVisit(true);
        stmList.add(root);

        if (root.isMultipleTarget()) {
            for (ICfgNode target : root.getListTarget())
                linkStatement(target, stmList);
            return;
        }

        ICfgNode stmTrue = nextConcrete(root.getTrueNode());
        root.setTrue(stmTrue);

        ICfgNode stmFalse = nextConcrete(root.getFalseNode());
        root.setFalse(stmFalse);

        linkStatement(stmTrue, stmList);
        linkStatement(stmFalse, stmList);
    }

    /**
     * Generate begin & end node
     *
     * @param fn function node
     */
    protected void preprocessor(IFunctionNode fn) {
        BEGIN = new BeginFlagCfgNode();
        END = new EndFlagCfgNode();
        ((EndFlagCfgNode) END).setBeginNode((BeginFlagCfgNode) BEGIN);
    }

    private ICFG parse(IFunctionNode fn) {
        preprocessor(fn);

        labels = new HashMap<>();

        visitBlock((IASTCompoundStatement) fn.getAST().getBody(), BEGIN, END, null, null, END, BEGIN);

        ArrayList<ICfgNode> stmList = new ArrayList<>();
        linkStatement(BEGIN, stmList);

        stmList.removeIf(n -> n instanceof ForwardCfgNode);

        for (ICfgNode node : stmList) {
            if (node.getTrueNode() instanceof ForwardCfgNode && node.getTrueNode().getAstLocation() != null) {
                ICfgNode trueNode = nextConcrete(node.getTrueNode(), stmList);
                node.setTrue(trueNode);
            }
            if (node.getFalseNode() instanceof ForwardCfgNode && node.getFalseNode().getAstLocation() != null) {
                ICfgNode falseNode = nextConcrete(node.getFalseNode(), stmList);
                node.setFalse(falseNode);
            }
        }

        for (ICfgNode stm : stmList)
            stm.setParent(nextConcrete(stm.getParent()));

        return new CFG(stmList);
    }

    private ICfgNode nextConcrete(ICfgNode cfgNode, ArrayList<ICfgNode> stmList) {

        return stmList.stream()
                .filter(n -> n.getAstLocation() != null)
                .filter(n -> {
                    int curSO = n.getAstLocation().getNodeOffset();
                    int curEO = n.getAstLocation().getNodeOffset() + n.getAstLocation().getNodeLength();
                    int forwardSO = cfgNode.getAstLocation().getNodeOffset();
                    int forwardEO = cfgNode.getAstLocation().getNodeOffset() + cfgNode.getAstLocation().getNodeLength();
                    return curSO >= forwardSO && curEO <= forwardEO;
                })
                .findFirst()
                .orElse(null);
    }

    private void visitBlock(IASTCompoundStatement block, ICfgNode begin, ICfgNode end, ICfgNode _break,
                            ICfgNode _continue, ICfgNode _throw, ICfgNode parent) {

        if (block == null)
            return;

        IASTStatement[] children = block.getStatements();

        if (children.length == 0) {
            begin.setBranch(end);
            return;
        }

        ICfgNode scopeIn = ScopeCfgNode.newOpenScope();
        ICfgNode scopeOut = ScopeCfgNode.newCloseScope(end);
        ICfgNode[] points = new CfgNode[children.length + 1];
        begin.setBranch(scopeIn);

        // Táº¡o cÃ¡c Ä‘iá»ƒm ná»‘i trung gian
        points[0] = scopeIn;
        for (int i = 1; i < children.length; i++)
            points[i] = new ForwardCfgNode();
        points[children.length] = scopeOut;

        for (int i = 0; i < children.length; i++)
            visitStatement(children[i], points[i], points[i + 1], _break, _continue, _throw, parent);
    }

    private void visitDeclaraInControlCondition(IASTDeclaration cond, ICfgNode begin, ICfgNode endTrue, ICfgNode endFalse,
                                                ICfgNode parent, int flag) {

        NormalCfgNode stmCond = null;
        switch (flag) {
            case DO_FLAG:
                stmCond = new ConditionDoCfgNode(cond);
                break;
            case FOR_FLAG:
                stmCond = new ConditionForCfgNode(cond);
                break;
            case WHILE_FLAG:
                stmCond = new ConditionWhileCfgNode(cond);
                break;
            case IF_FLAG:
                stmCond = new ConditionIfCfgNode(cond);
                break;
        }

        if (stmCond == null)
            return;

        visitFunctionCall(cond, stmCond);

        begin.setBranch(stmCond);
        stmCond.setTrue(endTrue);
        stmCond.setFalse(endFalse);
        stmCond.setParent(parent);
    }

    private void visitCondition(IASTExpression cond, ICfgNode begin, ICfgNode endTrue, ICfgNode endFalse,
                                ICfgNode parent, int flag) {

        // normalize condition ast
        cond = normalize(cond);
        cond = normalize(cond);

        NormalCfgNode stmCond = null;
        switch (flag) {
            case DO_FLAG:
                stmCond = new ConditionDoCfgNode(cond);
                break;
            case FOR_FLAG:
                stmCond = new ConditionForCfgNode(cond);
                break;
            case WHILE_FLAG:
                stmCond = new ConditionWhileCfgNode(cond);
                break;
            case IF_FLAG:
                stmCond = new ConditionIfCfgNode(cond);
                break;
        }

        if (stmCond == null)
            return;

        visitFunctionCall(cond, stmCond);

        begin.setBranch(stmCond);
        stmCond.setTrue(endTrue);
        stmCond.setFalse(endFalse);
        stmCond.setParent(parent);
        //TODO setContent if empty
        if (stmCond.getContent().isEmpty()) {
            IASTBinaryExpression ast = (IASTBinaryExpression) stmCond.getAst();
            String content = ast.getOperand1().getRawSignature() + " == " + ast.getOperand2().getRawSignature();
            IASTNode newAst = Utils.convertToIAST(content);
            stmCond.setAst(newAst);
            stmCond.setContent(content);

        }
    }

    private void visitFunctionCall(IASTNode ast, NormalCfgNode cfgNode) {
//        FunctionCallVisitor visitor = new FunctionCallVisitor(functionNode);
//        ast.accept(visitor);
//        FunctionCallVisitor.CallMap callMap = visitor.getCallMap();
//
//        for (Map.Entry<IASTFunctionCallExpression, IFunctionNode> entry : callMap.entrySet()) {
//            try {
//                IFunctionNode called = entry.getValue();
//                IASTFunctionCallExpression expr = entry.getKey();
//                attachSubCFG(called, expr, cfgNode);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
    }

    protected void attachSubCFG(IFunctionNode called, IASTFunctionCallExpression expr,
                                NormalCfgNode cfgNode) throws Exception {
        INestedCFGGeneration cfgGen =
                new NestedCFGGenerationforBranchvsStatementvsBasispathCoverage(called);

        cfgGen.getPreviousCalls().add(functionNode);

        cfgGen.setExpression(expr);

        ICFG cfg = cfgGen.generateCFG();

        cfgNode.getSubCFGs().put(expr, cfg);
    }

    private void visitSimpleStatement(IASTStatement stm, ICfgNode begin, ICfgNode end, ICfgNode _throw,
                                      ICfgNode parent) {

        ICfgNode normal;

        if (isThrowStatement(stm)) {
            normal = new ThrowCfgNode(stm);
            normal.setBranch(_throw);

        } else if (stm instanceof IASTReturnStatement) {
            normal = new ReturnNode(stm);
            parseReturnStatement(normal, stm, END, BEGIN);

        } else if (stm.getRawSignature().matches("exit\\s*\\(.*") || stm.getRawSignature().matches("abort\\s*\\(.*")) {
            normal = new SimpleCfgNode(stm);
            normal.setBranch(END);

        } else {
            normal = new SimpleCfgNode(stm);
            parseSimpleStatement((SimpleCfgNode) normal, stm, end, BEGIN);
            // normal.setBranch(end);
        }

        begin.setBranch(normal);
        normal.setParent(parent);
    }

    private void parseReturnStatement(ICfgNode returnNode, IASTStatement stm, ICfgNode endOfCFG,
                                      ICfgNode beginningOfCfg) {
        // Get all function calls
        List<CPPASTFunctionCallExpression> functionCalls = new ArrayList<>();
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public int visit(IASTExpression statement) {
                if (statement instanceof CPPASTFunctionCallExpression) {
                    functionCalls.add((CPPASTFunctionCallExpression) statement);
                    return ASTVisitor.PROCESS_SKIP;
                } else
                    return ASTVisitor.PROCESS_CONTINUE;
            }
        };
        visitor.shouldVisitExpressions = true;
        stm.accept(visitor);

        // Create link from statement to the called function
        boolean foundRecursive = false;
        for (CPPASTFunctionCallExpression functionCall : functionCalls) {
            String name = functionCall.getChildren()[0].getRawSignature();
            if (functionNode.getName().startsWith(name)) {
                returnNode.setBranch(beginningOfCfg.getTrueNode());
                foundRecursive = true;
                break;
            }
        }

        if (!foundRecursive) {
            returnNode.setBranch(endOfCFG);

            if (returnNode instanceof NormalCfgNode) {
                visitFunctionCall(stm, (NormalCfgNode) returnNode);
            }
        }
    }

    private void parseSimpleStatement(SimpleCfgNode simpleNode, IASTStatement stm, ICfgNode nextStatement,
                                      ICfgNode beginningOfCfg) {
        // Get all function calls
        List<CPPASTFunctionCallExpression> functionCalls = new ArrayList<>();
        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public int visit(IASTExpression statement) {
                if (statement instanceof CPPASTFunctionCallExpression) {
                    functionCalls.add((CPPASTFunctionCallExpression) statement);
                    return ASTVisitor.PROCESS_SKIP;
                } else
                    return ASTVisitor.PROCESS_CONTINUE;
            }
        };
        visitor.shouldVisitExpressions = true;
        stm.accept(visitor);

        // Create link from statement to the called function
        boolean foundRecursive = false;
        for (CPPASTFunctionCallExpression functionCall : functionCalls) {
            String name = functionCall.getChildren()[0].getRawSignature();
            if (functionNode.getName().startsWith(name)) {
                simpleNode.setTrue(beginningOfCfg.getTrueNode());
                simpleNode.setFalse(nextStatement);
                foundRecursive = true;
                break;
            }
        }
        if (!foundRecursive) {
            //*****
            simpleNode.setBranch(nextStatement);
            visitFunctionCall(stm, simpleNode);
        }
    }

    private void visitStatement(IASTStatement stm, ICfgNode begin, ICfgNode end, ICfgNode _break, ICfgNode _continue,
                                ICfgNode _throw, ICfgNode parent) {
        if (stm instanceof IASTIfStatement) {
            IASTIfStatement stmIf = (IASTIfStatement) stm;

            IASTExpression astCond = stmIf.getConditionExpression();

            // Sometimes, statement "if(A){...}" can not detect that A is a
            // condition. In this case,
            // A is
            // CPPASTSimpleDeclaration
            // Ex: "if (auto spt = gw.lock()){..}"
            IASTDeclaration astDecla = ((ICPPASTIfStatement) stmIf).getConditionDeclaration();

            //
            IASTStatement astThen = stmIf.getThenClause();
            IASTStatement astElse = stmIf.getElseClause();

            ICfgNode afterTrue = new ForwardCfgNode();
            ICfgNode afterFalse = new ForwardCfgNode();

            if (astCond != null)
                visitCondition(astCond, begin, afterTrue, afterFalse, parent, ICFGGeneration.IF_FLAG);
            else if (astDecla != null)
                visitDeclaraInControlCondition(astDecla, begin, afterTrue, afterFalse, parent, ICFGGeneration.IF_FLAG);

            visitStatement(astThen, afterTrue, end, _break, _continue, _throw, begin);

            visitStatement(astElse, afterFalse, end, _break, _continue, _throw, begin);
        } else if (!notNull(stm))
            begin.setBranch(end);

        else if (stm instanceof IASTForStatement) {

            IASTForStatement stmFor = (IASTForStatement) stm;
            IASTStatement astInit = stmFor.getInitializerStatement();
            IASTExpression astCond = stmFor.getConditionExpression();
            IASTExpression astIter = stmFor.getIterationExpression();
            IASTStatement astBody = stmFor.getBody();

            ICfgNode bfInit = new ForwardCfgNode(), bfCond = new ForwardCfgNode(), bfBody = new ForwardCfgNode();

            // Táº¡o scope áº£o cho trÆ°á»�ng há»£p cÃ³ khai bÃ¡o thÃªm biáº¿n
            // cháº¡y
            ICfgNode scopeIn = new AdditionalScopeCfgNode(ScopeCfgNode.SCOPE_OPEN);
            begin.setBranch(scopeIn);
            scopeIn.setParent(begin);
            scopeIn.setBranch(bfInit);
            bfInit.setParent(scopeIn);

            ICfgNode scopeOut = new AdditionalScopeCfgNode(ScopeCfgNode.SCOPE_CLOSE);
            scopeOut.setBranch(end);
            end.setParent(scopeOut);

            _continue = new ForwardCfgNode();// sau khi háº¿t pháº§n thÃ¢n
            // hoáº·c

            ForwardCfgNode bodyParent = new ForwardCfgNode();
            bodyParent.setBranch(bfCond);

            visitStatement(astInit, bfInit, bfCond, scopeOut, _continue, _throw, bodyParent);

            if (notNull(astCond))
                visitCondition(astCond, bfCond, bfBody, scopeOut, parent, ICFGGeneration.FOR_FLAG);
            else {
                bfCond.setBranch(bfBody);
                bodyParent.setBranch(parent);
            }

            visitStatement(astBody, bfBody, _continue, scopeOut, _continue, _throw, bodyParent);

            //
            if (bfCond.getTrueNode().getTrueNode() == _continue)
                throw new RuntimeException(
                        "This FOR statement is infinity." + "No condition or body found: " + stmFor.getRawSignature());

            if (notNull(astIter)) {
                ICfgNode stmIter = new SimpleCfgNode(astIter);
                _continue.setBranch(stmIter);
                stmIter.setBranch(bfCond);
                stmIter.setParent(bodyParent);
            } else
                _continue.setBranch(bfCond);

        } else if (stm instanceof IASTWhileStatement) {
            IASTWhileStatement astWhile = (IASTWhileStatement) stm;
            IASTStatement astBody = astWhile.getBody();

            IASTExpression astCond;
            astCond = astWhile.getCondition();

            // Sometimes, statement "if(A){...}" can not detect that A is a
            // condition. In this case,
            // A is
            // CPPASTSimpleDeclaration
            if (astCond == null) {
                CPPASTSimpleDeclaration decl = (CPPASTSimpleDeclaration) astWhile.getChildren()[0];
                String newStm = decl.getRawSignature() + "==true";
                astCond = (IASTExpression) Utils.convertToIAST(newStm);
            }

            //
            ICfgNode beforeCond = new ForwardCfgNode();
            ICfgNode afterCond = new ForwardCfgNode();

            begin.setBranch(beforeCond);
            visitCondition(astCond, beforeCond, afterCond, end, parent, ICFGGeneration.WHILE_FLAG);

            if (notNull(astBody))
                visitStatement(astBody, afterCond, beforeCond, end, beforeCond, _throw, beforeCond);
            else
                afterCond.setBranch(beforeCond);

        } else if (stm instanceof IASTDoStatement) {
            IASTDoStatement astDo = (IASTDoStatement) stm;
            IASTStatement astBody = astDo.getBody();

            ICfgNode beforeDo = new ForwardCfgNode();
            ICfgNode beforeCond = new ForwardCfgNode();

            begin.setBranch(beforeDo);
            _break = end;
            _continue = beforeCond;

            if (notNull(astBody))
                visitStatement(astBody, beforeDo, beforeCond, _break, _continue, _throw, beforeCond);
            else
                beforeDo.setBranch(beforeCond);

            visitCondition(astDo.getCondition(), beforeCond, beforeDo, _break, parent, ICFGGeneration.DO_FLAG);

        } else if (stm instanceof IASTSwitchStatement) {
            IASTSwitchStatement astSw = (IASTSwitchStatement) stm;
            ICfgNode scopeIn = ScopeCfgNode.newOpenScope();
            ICfgNode scopeOut = ScopeCfgNode.newCloseScope(end);
            begin.setBranch(scopeIn);

            visitSwitch(astSw.getControllerExpression(), (IASTCompoundStatement) astSw.getBody(), scopeIn, scopeOut,
                    _continue, _throw, parent);

        } else if (stm instanceof IASTCaseStatement) {
            CaseCfgNode caseNode = new CaseCfgNode(stm);
            IASTExpression cond = null;
            IASTNode astNode = stm;
            while (astNode != null) {
                if (astNode instanceof IASTSwitchStatement) {
                    cond = ((IASTSwitchStatement) astNode).getControllerExpression();
                    break;
                }
                astNode = astNode.getParent();
            }
            if (cond != null) {
                String normalizedCond = String.format("%s == %s", cond.getRawSignature(), ((IASTCaseStatement) stm).getExpression().getRawSignature());
                IASTNode normalizedASTNode = Utils.convertToIAST(normalizedCond);
                caseNode.setAst(normalizedASTNode);
                caseNode.setAstLocation(stm.getFileLocation());
                if (begin instanceof SwitchCfgNode) {
                    ((SwitchCfgNode) begin).getCases().add(caseNode);
                    caseNode.setParent(begin);
                }
                caseNode.setBranch(_break);
            }
        } else if (stm instanceof IASTDefaultStatement) {
            DefaultCaseCfgNode defaultNode = new DefaultCaseCfgNode(stm);
            if (begin instanceof SwitchCfgNode) {
                ((SwitchCfgNode) begin).getCases().add(defaultNode);
                defaultNode.setParent(begin);
            }
            defaultNode.setBranch(_break);
        } else if (stm instanceof IASTCompoundStatement)
            visitBlock((IASTCompoundStatement) stm, begin, end, _break, _continue, _throw, parent);

        else if (stm instanceof IASTBreakStatement) {
            ICfgNode breakNode = new BreakCfgNode(stm);
            begin.setBranch(breakNode);
            breakNode.setBranch(_break);
        } else if (stm instanceof IASTContinueStatement) {
            ICfgNode continueNode = new ContinueCfgNode(stm);
            begin.setBranch(continueNode);
            continueNode.setBranch(_continue);
        } else if (stm instanceof ICPPASTTryBlockStatement)
            visitTryCatch((ICPPASTTryBlockStatement) stm, begin, end, _break, _continue, _throw, parent);

        else if (stm instanceof IASTLabelStatement) {
            IASTLabelStatement stmLabel = (IASTLabelStatement) stm;
            String name = stmLabel.getName().toString();
            MarkFlagCfgNode ref = labels.get(name);

            if (ref == null) {
                ref = new MarkFlagCfgNode(name + ":");
                labels.put(name, ref);
            }
            ref.setAstLocation(stmLabel.getName().getFileLocation());
            ref.setParent(parent);

            begin.setBranch(ref);
            visitStatement(stmLabel.getNestedStatement(), ref, end, _break, _continue, _throw, ref);

        } else if (stm instanceof IASTGotoStatement) {
            String name = ((IASTGotoStatement) stm).getName().toString();
            MarkFlagCfgNode ref = labels.get(name);

            if (ref == null) {
                ref = new MarkFlagCfgNode(name + ":");
                labels.put(name, ref);
            }
            ICfgNode goToNode = new GoToCfgNode(stm);
            begin.setBranch(goToNode);
            goToNode.setBranch(ref);

        } else
            visitSimpleStatement(stm, begin, end, _throw, parent);
    }

    private void visitSwitch(IASTExpression cond, IASTCompoundStatement body, ICfgNode scopeIn, ICfgNode scopeOut,
                             ICfgNode _continue, ICfgNode _throw, ICfgNode parent) {

        SwitchCfgNode switchNode = new SwitchCfgNode(cond);
        scopeIn.setBranch(switchNode);

        IASTStatement[] children = body.getStatements();

        if (children.length == 0) {
            scopeIn.setBranch(scopeOut);
            return;
        }

        ICfgNode[] points = new CfgNode[children.length + 1];

        // Táº¡o cÃ¡c Ä‘iá»ƒm ná»‘i trung gian
        points[0] = scopeIn;
        for (int i = 1; i < children.length; i++)
            points[i] = new ForwardCfgNode();
        points[children.length] = scopeOut;

        for (int i = 0; i < children.length; i++) {
            IASTStatement stm = children[i];
            if (stm instanceof IASTCaseStatement || stm instanceof IASTDefaultStatement) {
                points[i] = switchNode;
            } else if (points[i - 1] instanceof SwitchCfgNode) {
                points[i] = switchNode.getCases().get(switchNode.getCases().size() - 1);
            }
            if (i + 2 < children.length) {
                if (children[i + 1] instanceof IASTCaseStatement || children[i + 1] instanceof IASTDefaultStatement) {
                    points[i + 1].setAstLocation(children[i + 2].getFileLocation());
                }
            }

            visitStatement(stm, points[i], points[i + 1], scopeOut, _continue, _throw, parent);
        }

        ICfgNode defaultNode = switchNode.getListTarget().stream()
                .filter(n -> n instanceof DefaultCaseCfgNode)
                .findFirst()
                .orElse(null);
        if (defaultNode != null) {
            //set content for default node
            String content = "";
            List<ICfgNode> listTargets = switchNode.getListTarget();
            listTargets = listTargets.stream()
                    .filter(n -> !(n instanceof DefaultCaseCfgNode))
                    .collect(Collectors.toList());
            for (int i = 0; i < listTargets.size(); i++) {
                if (i < listTargets.size() - 1) {
                    content += listTargets.get(i).getContent().replace("==", "!=") + " && ";
                } else {
                    content += listTargets.get(i).getContent().replace("==", "!=");
                }
            }
            defaultNode.setContent(content);
        }
//        ICfgNode prevNode = null;
//        for (IASTStatement stm : body.getStatements()){
//            if (stm instanceof IASTCaseStatement){
//                CaseCfgNode caseNode = new CaseCfgNode(stm);
//                String normalizedCond = String.format("%s == %s", cond.getRawSignature(), ((IASTCaseStatement) stm).getExpression().getRawSignature());
//                IASTNode astNode = Utils.convertToIAST(normalizedCond);
//                caseNode.setAst(astNode);
//                caseNode.setAstLocation(stm.getFileLocation());
//                switchNode.getCases().add(caseNode);
//                prevNode = caseNode;
//            }else if (stm instanceof IASTDefaultStatement){
//                DefaultCaseCfgNode defaultNode = new DefaultCaseCfgNode(stm);
//                defaultNode.setParent(switchNode);
//                switchNode.getCases().add(defaultNode);
//                prevNode = defaultNode;
//            }else {
//                visitStatement(stm, prevNode, end, null, _continue, _throw, parent);
//            }
//        }

//        ArrayList<Pair> caseLink = new ArrayList<>();
//        ArrayList<IASTCaseStatement> cases = new ArrayList<>();
//
//        IASTStatement[] childs = body.getStatements();
//        ICfgNode defaultPoint = null;
//        ICfgNode before = new ForwardCfgNode(), after;
//        int i = 0;
//
//        while (i < childs.length) {
//            IASTStatement stm = childs[i];
//
//            if (stm instanceof IASTCaseStatement){
//                cases.add((IASTCaseStatement) stm);
////                if(!(childs[i-1] instanceof IASTBreakStatement)){
////
////                }
//            }
//
//            else if (stm instanceof IASTDefaultStatement) {
//                cases.clear();
//                while (i + 1 < childs.length && childs[i + 1] instanceof IASTCaseStatement)
//                    i++;
//                defaultPoint = before;
//            } else {
//                // Vá»«a má»›i ra khá»�i má»™t dÃ£y case
//                if (cases.size() > 0) {
//                    caseLink.add(new Pair(cases, before));
//                    cases = new ArrayList<>();
//                }
////                if(i < childs.length-1 && childs[i+1] instanceof IASTCaseStatement && !(stm instanceof IASTBreakStatement)){
//////                    IASTBinaryExpression ast = (IASTBinaryExpression)childs[i+1].getAst();
//////                    String content = ast.getOperand1().getRawSignature()+" == "+ast.getOperand2().getRawSignature();
//////                    stmCond.setContent(content);
////
////                    after = new ConditionIfCfgNode(childs[i+1]);
////                    visitStatement(stm, before, after, end, _continue, _throw, parent);
////                    before = after;
////                }else{
////                    after = new ForwardCfgNode();
////                    visitStatement(stm, before, after, end, _continue, _throw, parent);
////                    before = after;
////                }
//                after = new ForwardCfgNode();
//                visitStatement(stm, before, after, end, _continue, _throw, parent);
//                before = after;
//
//            }
//            i++;
//        }
//        before.setBranch(end);
//
//        // NÃºt trÆ°á»›c khi báº¯t Ä‘áº§u default
//        ICfgNode beforeDefault = new ForwardCfgNode();
//
//        beforeDefault.setBranch(defaultPoint == null ? end : defaultPoint);
//        if (caseLink.size() == 0) {
//            begin.setBranch(beforeDefault);
//            return;
//        }
//
//        ICfgNode[] mid = new CfgNode[caseLink.size() + 1];
//        mid[0] = begin;
//        for (i = 1; i < mid.length - 1; i++)
//            mid[i] = new ForwardCfgNode();
//        mid[i] = beforeDefault;
//
//        SwitchCaseCfgNode switchCaseCfgNode = new SwitchCaseCfgNode(cond);
//        begin.setTrue(switchCaseCfgNode);
//        begin.setFalse(switchCaseCfgNode);
//        mid[0] = switchCaseCfgNode;
//
//        for (i = 0; i < caseLink.size(); i++) {
//            Pair p = caseLink.get(i);
//            IASTExpression join = joinCaseSwitch(cond, p.getCases());
//            visitCondition(join, mid[i], p.getStm(), mid[i + 1], parent, ICFGGeneration.IF_FLAG);
//        }
    }

    private void visitTryCatch(ICPPASTTryBlockStatement stm, ICfgNode begin, ICfgNode end, ICfgNode _break,
                               ICfgNode _continue, ICfgNode _throw, ICfgNode parent) {

        MarkFlagCfgNode startTry = new MarkFlagCfgNode("start try");
        MarkFlagCfgNode endCatch = new MarkFlagCfgNode("end catch");

        begin.setBranch(startTry);
        startTry.setParent(parent);
        endCatch.setParent(startTry);
        endCatch.setBranch(end);
        ICfgNode catchEntry = new ForwardCfgNode();
        visitStatement(stm.getTryBody(), startTry, catchEntry, _break, _continue, catchEntry, startTry);
        catchEntry.setBranch(_throw);

        for (ICPPASTCatchHandler catcher : stm.getCatchHandlers()) {
            ICfgNode label;

            if (catcher.isCatchAll()) {
                label = new MarkFlagCfgNode("catch (...)");
                label.setAstLocation(catcher.getFileLocation());
            } else {
                label = new MarkFlagCfgNode(String.format("catch (%s)", catcher.getDeclaration().getRawSignature()));
                label.setAstLocation(catcher.getDeclaration().getFileLocation());
            }

            label.setParent(startTry);
            catchEntry.setBranch(label);

            if (catcher.isCatchAll()) {
                visitStatement(catcher.getCatchBody(), label, endCatch, _break, _continue, _throw, label);
                break;
            } else {
                ICfgNode labelTrue = new ForwardCfgNode();
                label.setTrue(labelTrue);
                visitStatement(catcher.getCatchBody(), labelTrue, endCatch, _break, _continue, _throw, label);

                catchEntry = new ForwardCfgNode();
                catchEntry.setBranch(_throw);
                label.setFalse(catchEntry);
            }
        }
    }

    @Override
    public IFunctionNode getFunctionNode() {
        return functionNode;

    }

    @Override
    public void setFunctionNode(IFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    class Pair {

        ArrayList<IASTCaseStatement> cases;
        ICfgNode stm;

        public Pair(ArrayList<IASTCaseStatement> cases, ICfgNode stm) {
            this.cases = cases;
            this.stm = stm;
        }

        public Pair() {
        }

        public ArrayList<IASTCaseStatement> getCases() {
            return cases;
        }

        public void setCases(ArrayList<IASTCaseStatement> cases) {
            this.cases = cases;
        }

        public ICfgNode getStm() {
            return stm;
        }

        public void setStm(ICfgNode stm) {
            this.stm = stm;
        }

    }

}
