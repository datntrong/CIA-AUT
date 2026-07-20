package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTPointer;
import uet.fit.aut.parser.dependency.CTypeDependencyGeneration;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.GetterDependency;
import uet.fit.aut.parser.dependency.SetterDependency;
import uet.fit.aut.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent attribute of class/struct/union/enum, arguments of a function,
 * global variables
 *
 * @author DucAnh
 */
public class VariableNode extends CustomASTNode<IASTNode> implements IVariableNode {

    /**
     * true if the variable node is analyzed type dependency generation before
     */
    protected boolean typeDependencyState = false;

    /**
     * The corresponding node of the given type
     */
    protected INode correspondingNode;

    /**
     * For example: "const int* a" ---> raw type  = "const int*"
     * Raw type
     */
    protected String rawType = "";

    /**
     * Remove storage class, &, *, [] from raw type
     *
     *Two cases:
     * + Case 1: Array, pointer, list, vector, stack, v.v. ---> get raw type of element
     * Ex: "const std::vector<int>" ---> coretype = "int"
     * + Case 2: primitive type ---> primitive type
     *
     * For example: "const int* a" ---> raw type  = "int"
     */
    protected String coreType = "";

    /**
     * <pre> typedef void* XXX</pre>
     * <p>
     * void test(const XXX a){...}
     * <p>
     * rawtype = "const XXX", but realRawtype = "const void*"
     */
    protected String realType;

    protected int levelOfPointer = 0;

    protected boolean isReference = false;

    protected int visibility = 0;

    protected String defaultValue;
    private String reducedRawType;
    private boolean isPrivate;

    @Override
    public IASTDeclarator getASTDecName() {
        IASTNode ast = getAST();

        if (ast instanceof IASTSimpleDeclaration)
            return ((IASTSimpleDeclaration) ast).getDeclarators()[0];

        if (ast instanceof IASTParameterDeclaration)
            return ((IASTParameterDeclaration) ast).getDeclarator();

        return null;
    }

    @Override
    public IASTDeclSpecifier getASTType() {
        IASTNode ast = getAST();

        // TODO: macro
//        if (ast instanceof IASTFunctionStyleMacroParameter) {
//            ast = Utils.convertToIAST("__MACRO_UNDEFINE_TYPE__ test");
//
//            if (ast instanceof IASTDeclarationStatement)
//                ast = ((IASTDeclarationStatement) ast).getDeclaration();
//        }

        if (ast instanceof IASTSimpleDeclaration)
            return ((IASTSimpleDeclaration) ast).getDeclSpecifier();

        if (ast instanceof IASTParameterDeclaration)
            return ((IASTParameterDeclaration) ast).getDeclSpecifier();

        return null;
    }

    @Override
    public boolean isPrivate() {
        return visibility == ICPPASTVisibilityLabel.v_private;
    }

    @Override
    public int getSizeOfArray() {
        // only for one dimension array
        try {
            return Utils.toInt(Utils.getIndexOfArray(getRawType()).get(0));
        } catch (Exception ex) {
            try {
                IASTDeclarator declarator = getASTDecName();
                String size = ((IASTArrayDeclarator) declarator)
                        .getArrayModifiers()[0]
                        .getConstantExpression()
                        .getRawSignature();
                return Integer.parseInt(size);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    @Override
    public String getCoreType() {
        return coreType;
    }

    @Override
    public void setCoreType(String coreType) {
        this.coreType = coreType;
    }

    @Override
    public IFunctionNode getGetterNode() {
        for (Dependency d : getDependencies())
            if (d instanceof GetterDependency)
                return (IFunctionNode) d.getEndArrow();
        return null;
    }

    @Override
    public int getLevelOfPointer() {
        return levelOfPointer;
    }

    @Override
    public void setLevelOfPointer(int levelOfPointer) {
        this.levelOfPointer = levelOfPointer;
    }

    @Override
    public String getFullType() {
        StringBuilder prefixPath = new StringBuilder();

        INode currentVar = getCorrespondingNode();

        String realType = getRealType();
        realType = VariableTypeUtils.removeRedundantKeyword(realType);

        if (currentVar instanceof AvailableTypeNode)
            realType = ((AvailableTypeNode) currentVar).getType();
        else if (currentVar instanceof IVariableNode)
            realType = ((IVariableNode) currentVar).getCoreType();
        else if (TemplateUtils.isTemplateClass(getRawType())) {
            realType = getRawType();
        }

//        if (currentVar != null && !currentVar.getDependencies().isEmpty()) {
//            INode finalCurrentVar = currentVar;
//            Dependency aliasD = currentVar.getDependencies().stream()
//                    .filter(d -> d instanceof AliasDependency
//                            && d.getEndArrow().equals(finalCurrentVar)
//                    ).findFirst()
//                    .orElse(null);
//            if (aliasD != null) {
//                realType = getCoreType();
//                currentVar = aliasD.getStartArrow();
//            }
//        }

        if (VariableTypeUtils.isBasic(realType) || VariableTypeUtils.isOneDimensionBasic(realType)
                || VariableTypeUtils.isTwoDimensionBasic(realType) || VariableTypeUtils.isOneLevelBasic(realType)
                || VariableTypeUtils.isTwoLevelBasic(realType) || VariableTypeUtilsForStd.isSTL(realType))
            prefixPath.append(coreType);
        else if (currentVar == null) {
            prefixPath.append(realType);
        } else  {
            INode originalVar = currentVar;

            if (currentVar instanceof AliasDeclaration) {
                prefixPath = new StringBuilder(currentVar.getNewType());
                currentVar = currentVar.getParent();
            }

            while ((currentVar instanceof StructureNode || currentVar instanceof NamespaceNode)) {
                if (prefixPath.length() > 0)
                    prefixPath.insert(0,
                            currentVar.getNewType() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
                else
                    prefixPath = new StringBuilder(currentVar.getNewType());
                currentVar = currentVar.getParent();
            }

            /*
             * Add :: in case the scope if file level
             */
            if (originalVar instanceof StructureNode)
                if (!prefixPath.toString().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS))
                    prefixPath.insert(0, SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);

            /*
             * Add template argument <core type> in case template class
             */
            if (TemplateUtils.isTemplate(getRawType())) {
                int openPos = getRawType().indexOf(TemplateUtils.OPEN_TEMPLATE_ARG);
                int closePos = getRawType().lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1;
                String templateParameter = getRawType().substring(openPos, closePos);
                prefixPath.append(templateParameter);
            }
        }

        String fullType = prefixPath.toString();
        if (fullType.startsWith(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS))
            fullType = fullType.substring(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS.length());

        return fullType;
    }

    @Override
    public INode resolveCoreType() {
        if (correspondingNode == null) {
            CTypeDependencyGeneration gen = new CTypeDependencyGeneration();
            gen.setAddToTreeAutomatically(false);
            gen.dependencyGeneration(this);
            correspondingNode = gen.getCorrespondingNode();
        }
        return correspondingNode;
    }

    @Override
    public String getRawType() {
        return rawType;
    }

    @Override
    public void setRawType(String rawType) {
        this.rawType = rawType;
    }

    /**
     * Get real raw type.
     *
     * For example:
     *
     * Raw type: "const XXX*", but "typedef Student XXX",
     * real raw type: "Student*"
     */
    @Override
    public String getRealType() {
//        if (realType == null)
            realType = VariableTypeUtils.getRealType(this);
        return realType;
    }

    @Override
    public String getReducedRawType() {
        return VariableTypeUtils.removeRedundantKeyword(rawType);
    }

    @Override
    public void setReducedRawType(String reducedRawType) {
        this.reducedRawType = reducedRawType;
    }

    @Override
    public IFunctionNode getSetterNode() {
        for (Dependency d : getDependencies())
            if (d instanceof SetterDependency)
                return (IFunctionNode) d.getEndArrow();
        return null;
    }

    @Override
    public int getVisibility() {
        return visibility;
    }

    @Override
    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }
    
    @Override
    public boolean isReference() {
        return isReference;
    }

    @Override
    public void setReference(boolean isReference) {
        this.isReference = isReference;
    }

    /**
     * Get real type of variable.
     * <p>
     * Ex: "typedex XXX int; XXX a" -------------> type of a is "int", not "XXX"
     * <p>
     * Note:
     * <p>
     * <p>
     *
     * <pre>
     * typedef int int_t; // declares int_t to be an alias for the type int typedef
     * char char_t, *char_p, (*fp)(void); // declares char_t to be an alias for char
     * // char_p to be an alias for char* // fp to be an alias for char(*)(void)
     *
     * <pre>
     *
     * @return
     */
    @Override
    public INode getCorrespondingNode() {
        if (correspondingNode == null && !isTypeDependencyState()) {
            CTypeDependencyGeneration gen = new CTypeDependencyGeneration();
            gen.setAddToTreeAutomatically(false);
            gen.dependencyGeneration(this);
            correspondingNode = gen.getCorrespondingNode();
        }
        return correspondingNode;
    }

    @Override
    public void setAST(IASTNode aST) {
        this.AST = aST;

        /*
         * set name of variable
         */
        String name;
        List<IASTDeclarator> declarators = new ArrayList<>();
        if (getAST() instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration astNode = (IASTSimpleDeclaration) getAST();
            name = astNode.getDeclarators()[0].getName().toString();

            for (IASTDeclarator declarator : ((IASTSimpleDeclaration) getAST()).getDeclarators())
                if (declarator != null)
                    declarators.add(declarator);

        } else if (getAST() instanceof IASTParameterDeclaration) {
            name = ((IASTParameterDeclaration) getAST()).getDeclarator().getName().toString();
            declarators.add(((IASTParameterDeclaration) getAST()).getDeclarator());
        } else {
            // never happen?
            name = getAST().getRawSignature();
        }
        setName(name.trim());

        // break cond
        if (declarators.size() != 1)
            return;

        /*
         * get the level of pointer & reference
         */
        IASTDeclarator firstDeclarator = declarators.get(0);
        int pointerLv = 0;
        for (IASTNode operator : firstDeclarator.getPointerOperators()) {
            if (operator instanceof ICPPASTReferenceOperator)
                setReference(true);
            else if (operator instanceof IASTPointer)
                pointerLv++;
        }
        setLevelOfPointer(pointerLv);

        /*
         * set raw type of variable
         */
        IASTDeclSpecifier astType = getASTType();
        String rawType = ASTVisualizer.toString(astType, firstDeclarator);
        rawType = rawType
                .replaceAll("\\s*\\*\\s*", "*")
                // "int [ 3]" -> "int[3]"
                .replaceAll("\\s*\\[\\s*", "[")
                .replaceAll("\\s*\\]\\s*", "]")
                // "std::vector<  int >" --> "std::vector<int>"
                .replaceAll("\\s*<\\s*", "<")
                .replaceAll("\\s*>\\s*", ">")
                // "std :: vector<int>" --> "std::vector<int>"
                .replaceAll("\\s*::\\s*", "::")
                // "int & " --> "int&"
                .replaceAll("\\s*\\&\\s*", "&")
                .replaceAll("\\s+", " ")
                .replace(";", "")
                .trim();
        this.setRawType(rawType);

        // set default value
        if (firstDeclarator.getInitializer() != null) {
            IASTInitializer initializer = firstDeclarator.getInitializer();
            if (initializer instanceof IASTEqualsInitializer) {
                IASTInitializerClause clause = ((IASTEqualsInitializer) initializer).getInitializerClause();
                this.defaultValue = ASTVisualizer.toString(clause);
            } else if (initializer instanceof IASTInitializerList) {
                this.defaultValue = ASTVisualizer.toString((IASTInitializerClause) initializer);
            }
        }

        /*
         * set core type
         */
        // Ex: "Set* xxx"
        if (astType != null)
            if (firstDeclarator.getChildren().length > 0) {
                if (firstDeclarator.getChildren()[0] instanceof CPPASTPointer) {
                    // rawtype = "void[*]+"
                    if (rawType.matches(IRegex.VOID_PTR_MULTI_LEVEL)) {
                        setCoreType("void*");
                    } else {
                        // rawtype = "<structure>*"
                        String coreType = ASTVisualizer.toString(astType);
                        coreType = VariableTypeUtils.removeRedundantKeyword(coreType);
                        setCoreType(coreType);
                    }
                } else if (firstDeclarator instanceof IASTArrayDeclarator){
                    // rawType = "int[3]", declarator = "a[3], declSpecifier = "int"
                    String coreType = ASTVisualizer.toString(astType);
                    coreType = VariableTypeUtils.removeRedundantKeyword(coreType);
                    setCoreType(coreType);
                } else {
                    // rawtype = "<structure>"
                    String coreType = ASTVisualizer.toString(astType);
                    coreType = VariableTypeUtils.removeRedundantKeyword(coreType);
                    setCoreType(coreType);
                }
            } else {
                String coreType = VariableTypeUtils.removeRedundantKeyword(rawType);
                coreType = VariableTypeUtils.deletePointerOperator(coreType);

                // Handle "std::vector<IndividualStore::hoaqua>" ----> "IndividualStore::hoaqua"
                if (getASTType() instanceof IASTNamedTypeSpecifier && TemplateUtils.isTemplateClass(rawType))
                    coreType = coreType.substring(coreType.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG) + 1,
                            coreType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG));
                setCoreType(coreType.trim());
            }
    }

    @Override
    public String toString() {
        return getNewType();
    }

    @Override
    public boolean isExtern() {
        if (getAST() instanceof IASTSimpleDeclaration) {
            IASTDeclSpecifier declSpecifier = ((IASTSimpleDeclaration) getAST()).getDeclSpecifier();

            return declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_extern;
        }
        return false;
    }

    @Override
    public IASTInitializer getInitializer() {
        if (getAST() instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration ast = (IASTSimpleDeclaration) getAST();
            IASTDeclarator[] declarators = ast.getDeclarators();

            if (declarators.length > 0) {
                IASTDeclarator firstDeclarator = declarators[0];
                return firstDeclarator.getInitializer();
            }
        }
        return null;
    }

    @Override
    public String getAbsolutePath() {
        // Exist absolutePath before
        if (this.getParent() == null || this.getParent().getAbsolutePath() == null
                || this.getParent().getAbsolutePath().isEmpty()) {
            return absolutePath;
        }
        // Generate first time
        else {
            String absolutePath = this.getParent().getAbsolutePath() + File.separator + this.getName();
            setAbsolutePath(absolutePath);
        }

        return absolutePath;
    }

    @Override
    public IVariableNode clone() {
        IVariableNode clone = new CloneVariableNode();
        clone.setAbsolutePath(getAbsolutePath());
        clone.setChildren(getChildren());
        clone.setDependencies(getDependencies());
        clone.setName(getName());
        clone.setRawType(getRawType());
        clone.setParent(getParent());
        clone.setCorrespondingNode(this.getCorrespondingNode());

        if (getAST() != null)
            clone.setAST(getAST());
        else {
            clone.setRawType(getRawType());
            clone.setCoreType(getCoreType());
        }

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariableNode) {
            VariableNode objCast = (VariableNode) obj;
            return (objCast.getRawType().equals(getRawType()) && objCast.getName().equals(getName()));
        } else
            return false;
    }

    public boolean isTypeDependencyState() {
        return typeDependencyState;
    }

    public void setTypeDependencyState(boolean typeDependencyState) {
        this.typeDependencyState = typeDependencyState;
    }

    public void setCorrespondingNode(INode correspondingNode) {
        this.correspondingNode = correspondingNode;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isConst() {
        return getRawType().contains("const ")
                || (getASTType() != null && getASTType().isConst())
                || (getASTType() instanceof ICPPASTDeclSpecifier
                && ((ICPPASTDeclSpecifier) getASTType()).isConstexpr());
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
