package uet.fit.aut.parser.funcdetail;

import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.FunctionCallDependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;
import uet.fit.aut.parser.dependency.RealParentDependency;
import uet.fit.aut.parser.dependency.TypeDependency;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.SearchCondition;
import uet.fit.aut.search.condition.ClassNodeCondition;
import uet.fit.aut.search.condition.GlobalVariableNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.search.condition.StructNodeCondition;
import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FuncDetailTreeGeneration implements IFuncDetailTreeGeneration {
    private final static Logger logger = LoggerFactory.getLogger(FuncDetailTreeGeneration.class);

    private final INode root;

    public FuncDetailTreeGeneration(RootNode root, ICommonFunctionNode fn) {
        this.root = Utils.getRoot(fn);
        generateTree(root, fn);
    }

    @Override
    public void generateTree(RootNode root, ICommonFunctionNode fn) {
        logger.debug("generateGlobalSubTree");
        generateGlobalSubTree(root, fn);

        logger.debug("generateUUTSubTree");
        generateUUTSubTree(root, fn);

        logger.debug("generateStubSubTree");
        generateStubSubTree(root, fn);
    }

    List<INode> includeNodes = new ArrayList<>();

    private boolean isSystemUnit(INode unit) {
        List<INode> sources = Search.searchNodes(root, new SourcecodeFileNodeCondition());
        return !sources.contains(unit);
    }

    private List<Node> getAllIncludedNodes(INode n) {
        List<Node> output = new ArrayList<>();

        if (n != null) {
            try {
                for (Dependency child : n.getDependencies()) {
                    if (child instanceof IncludeHeaderDependency) {
                        if (child.getStartArrow().equals(n)) {
                            includeNodes.add(n);

                            INode end = child.getEndArrow();
                            if (!includeNodes.contains(end) && !isSystemUnit(end)) {
                                output.add((Node) end);
                                /*
                                 * In case recursive include
                                 */
                                output.addAll(getAllIncludedNodes(end));
                            }
                        }
                    }
                }
            } catch (StackOverflowError e) {
                e.printStackTrace();
            }
        }

        return output;
    }


    @Override
    public void generateGlobalSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode globalRoot = new RootNode(NodeType.GLOBAL);

        /*
         * Them cac bien global co trong unit
         */
        INode unit = Utils.getSourcecodeFile(fn);
        List<IVariableNode> globalVariables = Search.searchNodes(unit, new GlobalVariableNodeCondition());

        List<Node> includedNodes = getAllIncludedNodes(unit);
        for (Node node : includedNodes) {
            List<IVariableNode> includedGlobalVariables = Search.searchNodes(node, new GlobalVariableNodeCondition());
            includedGlobalVariables.forEach(global -> {
                if (!globalVariables.contains(global))
                    globalVariables.add(global);
            });
        }

        for (IVariableNode node : globalVariables) {
            if (node instanceof ExternalVariableNode) {
                if (!node.isConst() && !(getRealParent(node) instanceof StructureNode))
                    globalRoot.addElement(node);
            }
        }

        /*
         * Them instance trong truong hop test method cua class
         */
//        if (!Environment.getInstance().isC()) {
            List<INode> instances = searchAllInstances(unit);
            if (fn instanceof IFunctionNode) {
                INode realParent = ((IFunctionNode) fn).getRealParent();
                if (realParent == null)
                    fn.getParent();
                if (realParent instanceof StructOrClassNode && !instances.contains(realParent)) {
                    instances.add(realParent);
                }
            }

            for (INode instance : instances) {
                InstanceVariableNode instanceVarNode = generateInstance(instance);
                globalRoot.addElement(instanceVarNode);
            }
//        }

        root.addElement(globalRoot);
    }

    private INode getRealParent(IVariableNode node) {
        INode parent = node.getParent();
        for (Dependency d : node.getDependencies()) {
            if (d instanceof RealParentDependency) {
                parent = d.getEndArrow();
                break;
            }
        }
        return parent;
    }

    private InstanceVariableNode generateInstance(INode correspondingType) {
        InstanceVariableNode instance = new InstanceVariableNode();
        String type = Search.getScopeQualifier(correspondingType);

        if (correspondingType instanceof ClassNode && ((ClassNode) correspondingType).isTemplate()) {
            String[] templateParams = TemplateUtils.getTemplateParameters(correspondingType);
            if (templateParams != null) {
                type += TemplateUtils.OPEN_TEMPLATE_ARG;

                for (String param : templateParams)
                    type += param + ", ";

                type += TemplateUtils.CLOSE_TEMPLATE_ARG;
                type = type.replace(", >", ">");
            }
        }

        instance.setCoreType(type);
        instance.setRawType(type);

        String instanceVarName = SourceConstant.getInstanceName(correspondingType);
        instance.setName(instanceVarName);

        instance.setParent(correspondingType);

        instance.setCorrespondingNode(correspondingType);
        TypeDependency d = new TypeDependency(instance, correspondingType);
        instance.getDependencies().add(d);
        correspondingType.getDependencies().add(d);

        return instance;
    }

    private List<INode> searchAllInstances(INode unit) {
        List<INode> instances;

        List<SearchCondition> conditions = new ArrayList<>();
        conditions.add(new StructNodeCondition());
        conditions.add(new ClassNodeCondition());
        instances = Search.searchNodes(unit, conditions);

        instances.removeIf(instance -> {
            if (((StructOrClassNode) instance).getVisibility() != ICPPASTVisibilityLabel.v_public)
                return true;

            return instance instanceof IEmptyStructureNode;
        });

        return instances;
    }

    @Override
    public void generateUUTSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode uutRoot = new RootNode(NodeType.UUT);
        uutRoot.addElement(fn);
        root.addElement(uutRoot);
    }

    @Override
    public void generateStubSubTree(RootNode root, ICommonFunctionNode fn) {
        RootNode dontStubRoot = new RootNode(NodeType.DONT_STUB);
        RootNode stubRoot = new RootNode(NodeType.STUB);

        for (Dependency d : fn.getDependencies()) {
            if (d instanceof FunctionCallDependency && ((FunctionCallDependency) d).fromNode(fn)) {
                INode referNode = d.getEndArrow();
                stubRoot.addElement(referNode);
            }
        }

        root.addElement(dontStubRoot);
        root.addElement(stubRoot);
    }
}
