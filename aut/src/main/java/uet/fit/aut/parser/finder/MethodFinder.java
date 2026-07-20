package uet.fit.aut.parser.finder;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.dependency.DefinitionDependency;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.DefinitionFunctionNodeCondition;
import uet.fit.aut.search.condition.StaticAbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.StaticDefinitionFunctionNodeCondition;
import uet.fit.aut.search.condition.StructurevsTypedefCondition;
import uet.fit.aut.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Find method
 *
 * @author Lamnt
 */
public class MethodFinder {

    private static final Logger logger = LoggerFactory.getLogger(MethodFinder.class);

//    public boolean ignoreArgsLength = false;
//    public boolean ignoreArgsType = false;

    private static final List<String> BASIC_TYPES = VariableTypeUtils.getAllBasicFieldNames(
                    VariableTypeUtils.BASIC.NUMBER.class,
                    VariableTypeUtils.BASIC.STDINT.class,
                    VariableTypeUtils.BASIC.CHARACTER.class
    );

    private final ConcurrentMap<String, List<INode>> cache = new ConcurrentHashMap<>();

    /**
     * Node in the structure that contains the searched function
     */
    private final INode context;

    private final List<IFunctionNode> completeFunctions;
    private final List<DefinitionFunctionNode> onlyDefinedFunctions;

    private final List<IFunctionNode> staticCompletedFunctions;
    private final List<DefinitionFunctionNode> staticUncompletedFunctions;

    public MethodFinder(INode context) {
        this.context = context;
        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();

        completeFunctions = Search.searchInSpace(spaces, new AbstractFunctionNodeCondition());
        onlyDefinedFunctions = Search.searchInSpace(spaces, new DefinitionFunctionNodeCondition());

        INode root = Utils.getRoot(context);
        staticCompletedFunctions = Search.searchNodes(root, new StaticAbstractFunctionNodeCondition());
        staticUncompletedFunctions = Search.searchNodes(root, new StaticDefinitionFunctionNodeCondition());
    }

    public MethodFinder(INode context, int iterator) {
        this(context);
        this.iterator = iterator;
    }

//    public List<INode> find(String simpleFunctionName) {
//        List<INode> output = new ArrayList<>();
//        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();
//
//        for (Level l : spaces)
//            for (INode n : l) {
//
//                List<INode> completeFunctions = Search.searchNodes(n, new AbstractFunctionNodeCondition());
//                for (INode function : completeFunctions) {
//                    String name = getFunctionSimpleName(function);
//
//                    if (isSameName(name, simpleFunctionName))
//                        output.add(function);
//                }
//
//                List<INode> onlyDefinedFunction = Search.searchNodes(n, new DefinitionFunctionNodeCondition());
//                for (INode function : onlyDefinedFunction) {
//                    String name = getFunctionSimpleName(function);
//
//                    if (isSameName(name, simpleFunctionName))
//                        output.add(function);
//                }
//            }
//        return output;
//    }

    public List<INode> find(String simpleFunctionName, int nParamater) {
//        if (ignoreArgsLength)
//            return find(simpleFunctionName);

        List<INode> output = new ArrayList<>();

        List<INode> listInCache = cache.get(simpleFunctionName);

        if (listInCache == null) {
            listInCache = new ArrayList<>();
            cache.put(simpleFunctionName, listInCache);
        }

        for (INode funcInCache : listInCache) {
            if (funcInCache instanceof ICommonFunctionNode) {
                if (nParamater == ((ICommonFunctionNode) funcInCache).getArguments().size())
                    output.add(funcInCache);
            }
        }

        if (!output.isEmpty())
            return output;

        // STEP 1: completed functions in space
        for (IFunctionNode function : completeFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (nParamater == function.getArguments().size())
                    output.add(function);
        }

        // STEP 2: uncompleted function in spaces
        for (DefinitionFunctionNode function : onlyDefinedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (nParamater == function.getArguments().size()) {
                    // find completed function
                    boolean foundComplete = false;
                    for (Dependency d : function.getDependencies()) {
                        if (d instanceof DefinitionDependency && d.getStartArrow().equals(function)) {
                            output.add(d.getEndArrow());
                            foundComplete = true;
                            break;
                        }
                    }

                    if (!foundComplete)
                        output.add(function);
                }
        }

        // STEP 3: static function

        for (IFunctionNode function : staticCompletedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (nParamater == function.getArguments().size())
                    output.add(function);
        }

        for (DefinitionFunctionNode function : staticUncompletedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (nParamater == function.getArguments().size()) {
                    // find completed function
                    boolean foundComplete = false;
                    for (Dependency d : function.getDependencies()) {
                        if (d instanceof DefinitionDependency && d.getStartArrow().equals(function)) {
                            output.add(d.getEndArrow());
                            foundComplete = true;
                            break;
                        }
                    }

                    if (!foundComplete)
                        output.add(function);
                }
        }

        cache.put(simpleFunctionName, new ArrayList<>(output));

        return output;
    }

    private INode find(String simpleFunctionName, IASTInitializerClause[] params) {
//        if (ignoreArgsType) {
//            List<INode> result = find(simpleFunctionName, params.length);
//            return (result.isEmpty() ? null : result.get(0));
//        }
//        if (ignoreArgsLength) {
//            List<INode> result = find(simpleFunctionName);
//            return result.isEmpty() ? null : result.get(0);
//        }

        // STEP 1: completed functions in space
        for (IFunctionNode function : completeFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (isSameDeclare(params, function.getArguments()))
                    return function;
        }

        // STEP 2: uncompleted function in spaces
        for (DefinitionFunctionNode function : onlyDefinedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (isSameDeclare(params, function.getArguments())) {
                    // find completed function
                    for (Dependency d : function.getDependencies()) {
                        if (d instanceof DefinitionDependency && d.getStartArrow().equals(function)) {
                            return d.getEndArrow();
                        }
                    }
                    return function;
                }
        }

        // STEP 3: static function
        for (IFunctionNode function : staticCompletedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (isSameDeclare(params, function.getArguments()))
                    return function;
        }

        for (DefinitionFunctionNode function : staticUncompletedFunctions) {
            String name = getFunctionSimpleName(function);

            if (isSameName(name, simpleFunctionName))
                if (isSameDeclare(params, function.getArguments())) {
                    // find completed function
                    for (Dependency d : function.getDependencies()) {
                        if (d instanceof DefinitionDependency && d.getStartArrow().equals(function)) {
                            return d.getEndArrow();
                        }
                    }
                    return function;
                }
        }

        return null;
    }

    private boolean isSameName(String n1, String n2) {
        String[] l1 = n1.split(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
        String[] l2 = n2.split(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);

        int i1 = l1.length - 1;
        int i2 = l2.length - 1;

        while (i1 >= 0 && i2 >= 0) {
            l1[i1] = VariableTypeUtils.removeRedundantKeyword(l1[i1]);
            l2[i2] = VariableTypeUtils.removeRedundantKeyword(l2[i2]);

            if (l1[i1].equals("auto") || l2[i2].equals("auto")) {
                // do nothing
            } else if (!l1[i1].equals(l2[i2])) {
                return false;
            }

            i1--;
            i2--;
        }

        return true;
    }

    public INode find(ICPPASTNewExpression newExpr) {
        try {
            IASTTypeId typeId = newExpr.getTypeId();
            IASTNamedTypeSpecifier specifier = ((IASTNamedTypeSpecifier) typeId.getDeclSpecifier());
            String funcName;
            String qualifiedName = specifier.getName().toString();
            String className = qualifiedName;
            if (specifier.getName() instanceof ICPPASTQualifiedName) {
                className = specifier.getName().getLastName().toString();
            }
            funcName = qualifiedName + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + className;
            IASTInitializerClause[] args = ((ICPPASTConstructorInitializer) newExpr.getInitializer()).getArguments();
            return cacheOrFind(funcName, args);
        } catch (Exception ex) {
            logger.error("Don't support to find function corresponding to " + newExpr.getRawSignature());
            return null;
        }
    }

    public INode find(IASTFunctionCallExpression expression) {
        String funcName = getFunctionSimpleName(expression);

        if (funcName != null) {
            IASTInitializerClause[] args = expression.getArguments();
            return cacheOrFind(funcName, args);
        }

        return null;
    }

    private INode cacheOrFind(String funcName, IASTInitializerClause[] args) {
        List<INode> listInCache = cache.get(funcName);

        if (listInCache == null) {
            listInCache = new ArrayList<>();
            cache.put(funcName, listInCache);
        }

        for (INode funcInCache : listInCache) {
            if (funcInCache instanceof ICommonFunctionNode) {
                if (isSameDeclare(args, ((ICommonFunctionNode) funcInCache).getArguments()))
                    return funcInCache;
            }
        }

        INode result = find(funcName, args);
        if (result != null) {
            listInCache.add(result);
            return result;
        }

        return null;
    }

    private String getFunctionSimpleName(IASTFunctionCallExpression expr) {
        IASTExpression ex = expr.getFunctionNameExpression();
        String funcName = null;

        while (ex instanceof IASTUnaryExpression
                && ((IASTUnaryExpression) ex).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
            ex = ((IASTUnaryExpression) ex).getOperand();
        }

        if (ex instanceof IASTBinaryExpression
                && ((IASTBinaryExpression) ex).getOperator() == IASTBinaryExpression.op_greaterThan
                && ex.getRawSignature().contains("<") && ex.getRawSignature().contains(">")) {

            // handle macro expansion
            if (ex.getFileLocation() == null) {
                IASTExpression operand = ((IASTBinaryExpression) ex).getOperand1();
                if (operand instanceof IASTIdExpression) {
                    funcName = ((IASTIdExpression) operand).getName().toString();
                }
            } else {
                funcName = ex.getRawSignature();
            }

        } else if (ex instanceof IASTFieldReference) {
            String type = new NewTypeResolver(context).exec(((IASTFieldReference) ex).getFieldOwner());

            if (type == null)
                return null;

            // Delete 2 times
            type = VariableTypeUtils.deleteStorageClasses(type);
            type = VariableTypeUtils.deleteStorageClasses(type);

            funcName = type.replaceAll("[ *]", "")
                    + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS
                    + ((IASTFieldReference) ex).getFieldName().toString();

            VariableSearchingSpace space = new VariableSearchingSpace(context);
            List<INode> possibleTypeNode = Search.searchInSpace(space.getSpaces(), new StructurevsTypedefCondition(), type);
            INode typeNode = null;
            if (!possibleTypeNode.isEmpty()) {
                typeNode = possibleTypeNode.get(0);
                if (possibleTypeNode.size() > 1)
                    logger.debug("Multiple structure node found " + type);
            }

            if (typeNode instanceof StructureNode) {
                INode parent = typeNode.getParent();
                // TODO: structure define in another file
                while (parent != null) {
                    // structure define in a namespace
                    if (parent instanceof NamespaceNode || parent instanceof StructureNode) {
                        String namespace = parent.getName();
                        if (!(parent instanceof ClassNode && ((ClassNode) parent).isTemplate()
                                && funcName.startsWith(namespace)))
                            funcName = namespace + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + funcName;
                    }

                    parent = parent.getParent();
                }
            }
        } else if (ex instanceof IASTIdExpression) {
            funcName = ((IASTIdExpression) ex).getName().toString();
        }

        if (funcName != null)
            funcName = TemplateUtils.deleteTemplateParameters(funcName);

        return funcName;
    }

    private String getFunctionSimpleName(INode function) {
        String name = null;

        if (function instanceof ICommonFunctionNode)
            name = ((ICommonFunctionNode) function).getSingleSimpleName();


        INode realParent = function.getParent();

        if (function instanceof AbstractFunctionNode) {
            INode tmpRealParent = ((AbstractFunctionNode) function).getRealParent();
            if (tmpRealParent != null)
                realParent = tmpRealParent;
        }

        if (realParent instanceof StructureNode || realParent instanceof NamespaceNode) {
            String parent = realParent.getName();
            INode parentNode = realParent.getParent();
            while (parentNode != null && !(parentNode instanceof ISourcecodeFileNode)) {
                if (parentNode instanceof StructureNode || parentNode instanceof NamespaceNode) {
                    parent = parentNode.getName() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + parent;
                }
                parentNode = parentNode.getParent();
            }
            name = parent + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + name;
        }

//        /*
//         * Method call to a method in a same class
//         */
//        if (!name.contains(parent.toString()) && !parent.toString().endsWith(root.getName()))
//            name = parent + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + name;

        return name;
    }

    private boolean isSameDeclare(IASTInitializerClause[] params, List<IVariableNode> args) {
        /*
         * function(void) case
         */
        if (params.length == 0 && args.size() == 1)
            if (getArgType(args.get(0)).equals(VariableTypeUtils.VOID_TYPE.VOID))
                return true;

        if (args.stream().noneMatch(v -> v.getDefaultValue() != null) && params.length != args.size())
            return false;

        if (!args.isEmpty() && args.get(0).getParent() instanceof ICommonFunctionNode) {
            ICommonFunctionNode f = (ICommonFunctionNode) args.get(0).getParent();
            if (f.isTemplate())
                return true;
        }

        for (int i = 0; i < params.length; i++) {
            if (!isSameType(params[i], args.get(i)))
                return false;
        }

        return true;
    }

    // todo: extend class??
    private boolean isSameType(IASTInitializerClause param, IVariableNode arg) {
        String paramType = getParamType(param);

        VariableNode tempVarNode = new VariableNode();
        IASTNode ast = Utils.convertToIAST(String.format("%s %s;", paramType, arg.getName()));
        if (ast instanceof IASTDeclarationStatement)
            ast = ((IASTDeclarationStatement) ast).getDeclaration();
        if (ast instanceof IASTParameterDeclaration || ast instanceof IASTSimpleDeclaration) {
            tempVarNode.setAST(ast);
            tempVarNode.setAbsolutePath(arg.getAbsolutePath());
            tempVarNode.setParent(arg.getParent());
            paramType = tempVarNode.getRealType();
            paramType = deleteKeyWord(paramType);
        }

        String argType = arg.getRealType();
        argType = deleteKeyWord(argType);

        // Check Template function with template arguments
        if (paramType != null && arg.getParent() instanceof ICommonFunctionNode) {
            ICommonFunctionNode called = (ICommonFunctionNode) arg.getParent();

            if (called.isTemplate()) {
                String[] parameters = TemplateUtils.getTemplateParameters(called);
                if (arg.getCorrespondingNode() == null) {
                    paramType = deleteKeyWord(paramType);
                    paramType = paramType.replace(VariableTypeUtils.REFERENCE, "");
                    paramType = paramType.replaceAll(IRegex.POINTER, "[]");

                    String rawType = deleteKeyWord(arg.getRawType());
                    rawType = rawType.replaceAll(IRegex.POINTER, "[]");
                    rawType = rawType.replace(VariableTypeUtils.REFERENCE, "");
                    rawType = rawType.replace("[]", "\\[\\]");

                    assert parameters != null;
                    for (String paramater : parameters)
                        rawType = rawType.replace(paramater, ".+");

                    return paramType.matches(rawType);
                }
            }
        }

        if (argType.equals(paramType) /*|| paramType.equals(deleteKeyWord(arg.getFullType()))*/)
            return true;

        // Casting case
        return BASIC_TYPES.contains(paramType) && BASIC_TYPES.contains(argType);
    }

    private String getArgType(IVariableNode arg) {
        /*
         * TODO: giar dinhj laf ko typedef
         */
        String fullCoreType = arg.getFullType();
        if (fullCoreType.startsWith(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS))
            fullCoreType = fullCoreType.substring(2);

        String coreType = arg.getCoreType();

        String argType = arg.getRealType();

        argType = argType.replace(coreType, fullCoreType);

        argType = deleteKeyWord(argType);

        return argType;
    }

    private int iterator = 0;

    private String getParamType(IASTInitializerClause param) {
        if (!(param instanceof IASTExpression))
            return null;

        NewTypeResolver typeResolver = new NewTypeResolver(context, iterator);
        typeResolver.shouldSolveByParent = false;
        String paramType = typeResolver.exec(param);
        iterator++;

        if (paramType != null)
            paramType = deleteKeyWord(paramType);

        return paramType;
    }

    public String deleteKeyWord(String type) {
        type = VariableTypeUtils.deleteUnionKeyword(type);
        type = VariableTypeUtils.deleteStructKeyword(type);
        type = VariableTypeUtils.deleteStorageClasses(type);
        type = VariableTypeUtils.deleteSizeFromArray(type);
        type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);
        type = VariableTypeUtils.deleteReferenceOperator(type);
        type = type.replaceAll(" \\*", "*")
                .replaceAll(" \\[]", "*").replaceAll("\\[]", "*");

        return type;
    }
}
