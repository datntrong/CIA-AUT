package uet.fit.aut.testdata.object;

import uet.fit.aut.autogen.testdatagen.IAutoStubGeneration;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search2;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.testdata.Iterator;
import uet.fit.aut.testdata.comparable.*;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;
import uet.fit.aut.testdata.object.stl.PairDataNode;
import uet.fit.aut.testdata.object.stl.STLArrayDataNode;
import uet.fit.aut.usercode.objects.AbstractUserCode;
import uet.fit.aut.usercode.objects.AssertUserCode;
import uet.fit.aut.usercode.objects.UsedParameterUserCode;
import uet.fit.aut.util.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static uet.fit.aut.autogen.testdatagen.IAutoStubGeneration.FUNCTION_CALL_POSTFIX;
import static uet.fit.aut.autogen.testdatagen.se.SymbolicExecution.getFullName;

/**
 * Represent a variable node in the <b>variable tree</b>, example: class,
 * struct, array item, etc.
 *
 * @author DucAnh
 */
public abstract class ValueDataNode extends DataNode implements IValueDataNode, IUserCodeNode {

    protected AbstractUserCode userCode = null;
    protected String userCodeContent = null;
    protected boolean useUserCode = false;

    /**
     * The type of variable. Ex: const int&
     */
    private String rawType = SpecialCharacter.EMPTY;

    private String realType = SpecialCharacter.EMPTY;

    /**
     * The node contains the definition of type's variable. For example, the type of
     * variable is "Student". This instance returns the node that defines "Student"
     * (class Student{char* name; ...}).
     */
    private VariableNode correspondingVar = null;

    /**
     * global variable
     */
    private boolean external = false;

    private List<Iterator> iterators;

    private String assertMethod;

    private AssertUserCode assertUserCode;
    private boolean externel;

    public ValueDataNode() {
        iterators = new ArrayList<>();
        iterators.add(new Iterator(this));
    }

    public AssertUserCode getAssertUserCode() {
        return assertUserCode;
    }

    public void setAssertUserCode(AssertUserCode assertUserCode) {
        this.assertUserCode = assertUserCode;
    }

    public INode getCorrespondingType() {
        VariableNode correspondingVar = getCorrespondingVar();

        if (correspondingVar == null)
            return null;

        return getCorrespondingVar().getCorrespondingNode();
    }

    public boolean haveValue() {
        for (IDataNode child : getChildren()) {
            if (child instanceof ValueDataNode && ((ValueDataNode) child).haveValue())
                return true;
        }

        return false;
    }

    public void setAssertMethod(String assertMethod) {
        this.assertMethod = assertMethod;
    }

    public String getAssertMethod() {
        return assertMethod;
    }

    @Override
    public VariableNode getCorrespondingVar() {
        return this.correspondingVar;
    }

    @Override
    public void setCorrespondingVar(VariableNode correspondingVar) {
        this.correspondingVar = correspondingVar;
    }

    protected String getExportExeResultStm(String actualName, String expectedName) {
        return String.format(DriverConstant.ASSERT + "(\"%s\", %s, \"%s\", %s);",
                actualName, actualName, expectedName, expectedName);
    }

    @Override
    public String getAssertion() {
        StringBuilder assertion = new StringBuilder();

        for (IDataNode child : this.getChildren()) {
            if (child instanceof ValueDataNode) {
                String childAssertion = ((ValueDataNode) child).getAssertion() + SpecialCharacter.LINE_BREAK;
                assertion.append(childAssertion);
            }
        }

        return assertion.toString();
    }

    protected String getActualName() {
        String actualName = getVirtualName();

        String expectedOutputRegex = "\\Q" + SourceConstant.EXPECTED_OUTPUT + "\\E";
        actualName = actualName.replaceFirst(expectedOutputRegex, SourceConstant.ACTUAL_OUTPUT);

        String expectedPrefixRegex = "\\Q" + SourceConstant.EXPECTED_PREFIX + "\\E";
        actualName = actualName.replaceFirst(expectedPrefixRegex, SpecialCharacter.EMPTY);

        String stubPrefixRegex = "\\Q" + SourceConstant.STUB_PREFIX + "\\E";
        actualName = actualName.replaceFirst(stubPrefixRegex, SpecialCharacter.EMPTY);

        String globalRegex = "\\Q" + SourceConstant.GLOBAL_PREFIX + "\\E";
        actualName = actualName.replaceFirst(globalRegex, SpecialCharacter.EMPTY);

        return actualName;
    }

    @Override
    public String getRawType() {
        return this.rawType;
    }

    @Override
    public void setRawType(String rawType) {
        rawType = VariableTypeUtils.deleteStorageClassesExceptConst(rawType);
        this.rawType = rawType;
    }

    @Override
    public String getRealType() {
        return realType;
    }

    @Override
    public void setRealType(String realType) {
        realType = VariableTypeUtils.deleteStorageClassesExceptConst(realType);
        this.realType = realType;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean _external) {
        external = _external;
    }

    @Override
    public boolean isArrayElement() {
        IDataNode parent = getParent();

        if (!(parent instanceof ValueDataNode))
            return false;

        if (parent instanceof ArrayDataNode || parent instanceof PointerDataNode || parent instanceof STLArrayDataNode)
            return true;

        if (this instanceof SubClassDataNode && parent instanceof ClassDataNode)
            return ((ClassDataNode) parent).isArrayElement();

        return false;
    }

    @Override
    public boolean isElementInString() {
        IDataNode parent = getParent();

        if (!(parent instanceof ValueDataNode))
            return false;

        return parent instanceof NormalStringDataNode;
    }

    @Override
    public boolean isSTLListBaseElement() {
        return this.getParent() != null && this.getParent() instanceof ListBaseDataNode;
    }

    @Override
    public boolean isAttribute() {
        if (this instanceof SubClassDataNode) {
            return getParent().getParent() instanceof StructureDataNode || getParent().getParent() instanceof PairDataNode;
        } else if (this instanceof SubprogramNode) {
            if (this instanceof ConstructorDataNode)
                return getParent().getParent().getParent() instanceof StructureDataNode
                        || getParent().getParent().getParent() instanceof PairDataNode;
            else
                return false;
        } else
            return this.getParent() instanceof StructureDataNode || getParent() instanceof PairDataNode;
    }

    public boolean isVoidPointerValue() {
        return getParent() instanceof VoidPointerDataNode;
    }

    public boolean isExpected() {
        IDataNode parent = getParent();

        if (parent == null)
            return false;

        if (!(parent instanceof ValueDataNode))
            return false;

        if (this instanceof SubprogramNode && !(this instanceof ConstructorDataNode))
            return false;

        if (parent instanceof SubprogramNode && !(parent instanceof ConstructorDataNode)) {
            IDataNode grandParent = parent.getParent();

            boolean isReturnVar = getName().equals("RETURN");

            // case test function in sbf unit (<<SBF>>) or stub subprogram (<<STUB>>)
            if (grandParent instanceof RootDataNode)
                return !isReturnVar;
            if (grandParent instanceof UnitNode) {
                boolean isStubUnit = grandParent instanceof StubUnitNode;
                return ((isStubUnit && !isReturnVar) || (!isStubUnit && isReturnVar));
//                        && Search2.getExpectedValue(this) != null;
            }
        }

        return ((ValueDataNode) parent).isExpected();
    }

    @Override
    public boolean isInConstructor() {
        return this.getParent() instanceof ConstructorDataNode;
    }

    @Override
    public boolean isPassingVariable() {
        return this.getParent() != null && !isSutExpectedValue()
                && (/*this.getParent() instanceof RootDataNode || */this.getParent() instanceof SubprogramNode)
                && !(this.getParent() instanceof ConstructorDataNode);
    }

    public boolean isStubArgument() {
        if (this instanceof SubprogramNode)
            return false;

        IDataNode grandParent = parent.getParent();

        if (grandParent instanceof StubUnitNode)
            return true;

        if (grandParent instanceof RootDataNode) {
            NodeType rootType = ((RootDataNode) grandParent).getLevel();

            return rootType == NodeType.STUB || rootType == NodeType.SBF;
        }

        return false;
    }

    public boolean isInstance() {
        if (isGlobalExpectedValue())
            return false;

        if (this instanceof ClassDataNode || this instanceof StructDataNode)
            if (correspondingVar instanceof InstanceVariableNode)
                return true;

        if (this instanceof SubClassDataNode) {
            return ((ClassDataNode) getParent()).isInstance();
        }

        return false;
    }

    public void setUserCodeContent(String userCodeContent) {
        this.userCodeContent = userCodeContent;
    }


    protected String getUserCodeContent() {
//        if (userCode instanceof  AbstractUserCode) {
//            if (userCode instanceof UsedParameterUserCode) {
//                UsedParameterUserCode usedUserCode = (UsedParameterUserCode) userCode;
//                if (usedUserCode.getType().equals(UsedParameterUserCode.TYPE_REFERENCE)) {
//                    ParameterUserCode reference = UserCodeManager.getInstance().getParamUserCodeById(userCode.getId());
//                    return reference.getContent() + SpecialCharacter.LINE_BREAK;
//                }
//            }
//            // ..
//            // UUID.randomUUID().toString();
//
//            return userCode.getContent() + SpecialCharacter.LINE_BREAK;
//        }

        return userCodeContent;
    }

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return getUserCodeContent();
        }

        return super.getInputForGoogleTest(isDeclared);
    }

    public boolean isSupportUserCode() {
        return !(this instanceof SubprogramNode);
    }

    public boolean isHaveExpectedValue() {
        // subprogram under test case
        if (this instanceof SubprogramNode && !(this instanceof ConstructorDataNode)) {
            IDataNode parent = getParent();
            IDataNode grandParent = parent.getParent();

            return parent instanceof StubUnitNode
                    && grandParent instanceof RootDataNode && ((RootDataNode) grandParent).getLevel() == NodeType.ROOT;
        }

        if (getCorrespondingVar() instanceof ReturnVariableNode || !(getParent() instanceof ValueDataNode))
            return false;

        return ((ValueDataNode) getParent()).isHaveExpectedValue();
    }

    public void setVirtualName() {
        if (this.virtualName != null)
            return;

        String virtualName;
        IDataNode parent = getParent();

        if (isExternal()) {
            IVariableNode variable = getCorrespondingVar();
            if (variable instanceof StaticVariableNode)
                virtualName = ((StaticVariableNode) variable).getInstrument();
            else if (variable instanceof InstanceVariableNode)
                virtualName = getName();
            else
                virtualName = getDisplayName();
        }
        // parameter case
        else if (isPassingVariable()) {
            virtualName = getName();
        }
        // subprogram case
        else if (this instanceof IterationSubprogramNode){
            INode iNode = ((IterationSubprogramNode) this).getFunctionNode();
            virtualName = iNode.getName();
            if(iNode instanceof ICommonFunctionNode) virtualName  = getFullName((ICommonFunctionNode) iNode);
            virtualName = Utils.deleteBracesAndParams(virtualName);
            virtualName = virtualName + SpecialCharacter.UNDERSCORE + FUNCTION_CALL_POSTFIX + ((IterationSubprogramNode) this).getIndex();
        }
        else if (this instanceof ConstructorDataNode) {
            virtualName = getParent().getVirtualName();
        }
        else if (this instanceof SubprogramNode) {
            String name = this.getDisplayName();
            name = name.replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, SpecialCharacter.REPLACE_FOR_STRUCTURE);
            virtualName = Utils.deleteBracesAndParams(name + IAutoStubGeneration.AUTO_STUB_POSTFIX);
        }
        // subclass data node
        else if (this instanceof SubClassDataNode) {
            virtualName = parent.getVirtualName();
        }
        else if (this instanceof NumberOfCallNode) {
            virtualName = "NONE_VALUE";
        }
        // virtual name depend on parent's virtual name
        else if (isArrayElement() || isElementInString()) {
            String elementIndex = VariableTypeUtils.getElementIndex(getName());

            String parentVirtualName = parent.getVirtualName();

            if (parent instanceof ValueDataNode && ((ValueDataNode) parent).isArrayElement())
                parentVirtualName = parentVirtualName
                        .substring(0, parentVirtualName.lastIndexOf(SpecialCharacter.OPEN_SQUARE_BRACE));

            virtualName = parentVirtualName + elementIndex;
        } else if (isAttribute()) {
            virtualName = parent.getVirtualName() + SpecialCharacter.DOT + getName();

            if (parent.getVirtualName().startsWith(SourceConstant.INSTANCE_VARIABLE))
                virtualName = parent.getVirtualName() + SpecialCharacter.POINT_TO + getName();
        }
//		else if (getParent() instanceof VoidPointerDataNode) {
//            String parentPrefix = parent.getVituralName();
//            virtualName = parentPrefix + SpecialCharacter.UNDERSCORE_CHAR + getName();
//        }
        // other data node
        else {
            String parentPrefix = parent.getVirtualName();
            virtualName = getName();

            if (!parentPrefix.equals(NON_VALUE))
                virtualName = parentPrefix + SpecialCharacter.UNDERSCORE_CHAR + virtualName;

            virtualName = virtualName.replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);
            virtualName = virtualName.replaceAll("[^\\w_]", SpecialCharacter.EMPTY);
        }

        // expected output
        if (name.equals("RETURN")) {
            UnitNode unit = getUnit();
            if (unit != null && !(unit instanceof StubUnitNode))
                virtualName = SourceConstant.EXPECTED_OUTPUT;
        }

        if (isStubArgument()) {
//            String normalizeSubprogramName = parent.getDisplayNameInParameterTree()
//                    .replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
            virtualName = SourceConstant.STUB_PREFIX /*+ normalizeSubprogramName*/ + virtualName;
        }

        if (isSutExpectedArgument()) {
            VariableNode v = getCorrespondingVar();
            if (v instanceof StaticVariableNode) {
                StaticVariableNode staticVar = (StaticVariableNode) v;
                virtualName = SourceConstant.EXPECTED_PREFIX + staticVar.getInstrument();
            } else
                virtualName = SourceConstant.EXPECTED_PREFIX + virtualName;
        } else if (isGlobalExpectedValue()) {
            virtualName = SourceConstant.EXPECTED_PREFIX + SourceConstant.GLOBAL_PREFIX + virtualName;
        }

        if (this instanceof NullPointerDataNode)
            virtualName = "nullptr_t";

        setVirtualName(virtualName);
    }

    public boolean isGlobalExpectedValue() {
        GlobalRootDataNode globalRoot = Search2.findGlobalRoot(getTestCaseRoot());

        assert globalRoot != null;
        return (globalRoot.getGlobalInputExpOutputMap().containsValue(this));
    }

    public boolean isSutExpectedValue() {
        IDataNode parent = getParent();

        if (!(parent instanceof ValueDataNode))
            return false;

        if (parent instanceof SubprogramNode) {
            SubprogramNode sut = Search2
                    .findSubprogramUnderTest(((SubprogramNode) parent).getTestCaseRoot());

            if (sut == parent) {
                if (sut.getParamExpectedOuputs().contains(this))
                    return true;
            }
        }

        return ((ValueDataNode) parent).isSutExpectedValue();
    }

    public boolean isSutExpectedArgument() {
//        IDataNode parent = getParent();
//
//        if (!(parent instanceof ValueDataNode))
//            return false;
//
//        if (parent instanceof SubprogramNode) {
            SubprogramNode sut = Search2.findSubprogramUnderTest(getTestCaseRoot());
//
            if (sut != null) {
                return sut.getParamExpectedOuputs().contains(this);
            }
//        }

        return false;
    }

    public String getDisplayName() {
        String prefixPath = null;

        if (getName().startsWith(SourceConstant.INSTANCE_VARIABLE))
            return getRawType() + " Instance";

        prefixPath = getName() + "";

        INode originalVar = getCorrespondingVar();

        if (originalVar instanceof ReturnVariableNode) {
            prefixPath = "return";
        }

        if (originalVar instanceof ExternalVariableNode) {
            INode currentVar = originalVar.getParent();

            while ((currentVar instanceof StructureNode || currentVar instanceof NamespaceNode)) {
                prefixPath = currentVar.getNewType() + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + prefixPath;
                currentVar = currentVar.getParent();
            }
        }

        for (int i = 0; i < iterators.size(); i++) {
            Iterator iterator = iterators.get(i);

            if (iterator.getDataNode() == this) {
                if (i != 0 || iterator.getRepeat() != Iterator.FILL_ALL) {
                    prefixPath += String.format(" [%s]", iterator.getDisplayName());
                }

                break;
            }
        }

        prefixPath = prefixPath.replaceFirst("^RETURN\\b", "return");

        return prefixPath;
    }

    public List<Iterator> getIterators() {
        return iterators;
    }

    public void setIterators(List<Iterator> iterators) {
        this.iterators = iterators;
    }

    public Iterator getCorrespondingIterator() {
        if (isStubArgument()) {
            return iterators.stream().filter(i -> i.getDataNode() == this).findFirst().orElse(null);
        } else {
            if (parent instanceof ValueDataNode) {
                return ((ValueDataNode) parent).getCorrespondingIterator();
            } else
                return null;
        }
    }

    public String[] getAllSupportedAssertMethod() {
        List<String> supportedMethod = new ArrayList<>();

        if (!isVoidPointerValue() && !(this instanceof SubprogramNode)) {
            supportedMethod.add(SpecialCharacter.EMPTY);

            if (this instanceof IEqualityComparable) {
                supportedMethod.add(AssertMethod.ASSERT_EQUAL);
                supportedMethod.add(AssertMethod.ASSERT_NOT_EQUAL);
            }
            if (this instanceof IValueComparable) {
                supportedMethod.add(AssertMethod.ASSERT_LOWER);
                supportedMethod.add(AssertMethod.ASSERT_GREATER);
                supportedMethod.add(AssertMethod.ASSERT_LOWER_OR_EQUAL);
                supportedMethod.add(AssertMethod.ASSERT_GREATER_OR_EQUAL);
            }
            if (this instanceof IBooleanComparable) {
                supportedMethod.add(AssertMethod.ASSERT_TRUE);
                supportedMethod.add(AssertMethod.ASSERT_FALSE);
            }
            if (this instanceof INullableComparable) {
                supportedMethod.add(AssertMethod.ASSERT_NULL);
                supportedMethod.add(AssertMethod.ASSERT_NOT_NULL);
            }

            supportedMethod.add(AssertMethod.USER_CODE);
        }

        return supportedMethod.toArray(new String[0]);
    }

    @Override
    public ValueDataNode clone() {
        ValueDataNode clone = null;

        try {
            clone = getClass().getDeclaredConstructor().newInstance();
            clone.setName(getName() + "");
            clone.setParent(getParent());
            clone.setRawType(getRawType() + "");
            clone.setRealType(getRealType() + "");
            clone.setCorrespondingVar(getCorrespondingVar());
            clone.setExternal(isExternal());
            clone.setIterators(iterators);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return clone;
    }

    // tmp implement IUserCodeNode
    public String getContextPath() {
        UnitNode unitNode = getUnit();
        String filePath;

        if (unitNode != null) {
            filePath = unitNode.getSourceNode().getAbsolutePath();
        } else {
            String[] pathItems = getCorrespondingVar().getAbsolutePath().split(File.separator);
            filePath = pathItems[1];
        }

        return filePath;
    }

    /**
     * @return temporary file path where archive user code file
     */
    public String getTemporaryPath() {
        return "";
    }

    /**
     * @return initial user code with only declaration
     */
    public String generateInitialUserCode() {
        String input = "";

        String typeVar = getRawType();

        if (isExternal())
            typeVar = "";

        // generate the statement
        if (this.isPassingVariable()) {
            input += typeVar + " " + getVirtualName() + " = ";

        } else if (this.isAttribute()) {
            input += getVirtualName() + " = ";

        } else if (this.isArrayElement()) {
            input += getVirtualName() + " = ";

        } else if (isSTLListBaseElement()) {
            input += typeVar + " " + getVirtualName() + " = ";

        } else if (this.isInConstructor()) {
            input += typeVar + " " + getVirtualName() + " = ";

        } else if (this.isSutExpectedValue()) {
            input += typeVar + " " + getVirtualName() + " = ";

        } else if (this.isVoidPointerValue()) {
            input += typeVar + " " + getVirtualName() + " = ";

        }

        return input;
    }

    public void setUserCode(AbstractUserCode userCode) {
        this.userCode = userCode;
    }

    public AbstractUserCode getUserCode() {
        if (userCode == null) {
            userCode = new UsedParameterUserCode();
            ((UsedParameterUserCode) userCode).setType(UsedParameterUserCode.TYPE_CODE);
            userCode.setContent(generateInitialUserCode() + DEFAULT_USER_CODE);
        }

        return userCode;
    }

    public void setUseUserCode(boolean useUserCode) {
        this.useUserCode = useUserCode;
        if (!useUserCode) {
            userCode = null;
        }
    }

    public boolean isUseUserCode() {
        return useUserCode;
    }

    public String getUserCodeDisplayText() {
        if (isUseUserCode()) return "USER CODE";
        return "";
    }

    public void setExternel(boolean _externelVariable) {
        externel = _externelVariable;
    }
    // tmp implement IUserCodeNode

    public boolean isExternel() {
        return external;
    }
}
