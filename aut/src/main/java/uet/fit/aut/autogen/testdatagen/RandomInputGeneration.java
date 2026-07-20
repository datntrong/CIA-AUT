package uet.fit.aut.autogen.testdatagen;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTypeId;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.autogen.testdatagen.randomgeneration.BasicTypeRandom;
import uet.fit.aut.boundary.MultiplePrimitiveBound;
import uet.fit.aut.boundary.PointerOrArrayBound;
import uet.fit.aut.boundary.PrimitiveBound;
import uet.fit.aut.boundary.UndefinedBound;
import uet.fit.aut.config.IFunctionConfig;
import uet.fit.aut.config.IFunctionConfigBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.IProjectLoader;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.SizeOfArrayOrPointerDependency;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.Search2;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.aut.testdata.gen.type.PointerTypeInitiation;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.testdata.object.stl.*;
import uet.fit.aut.usercode.usercodetestdata.UserCodeTestDataManager;
import uet.fit.aut.usercode.usercodetestdata.UserCodeValue;
import uet.fit.aut.util.*;

import java.io.File;
import java.util.*;

/**
 * Example:
 * <p>
 * Class: {r1=Rectangle(int,int), r1.a=4, r1.b=9, r2=Triangle()}
 * <p>
 * Pointer class:
 * <p>
 * Struct:
 */
public class RandomInputGeneration {

    public static final String OPTION_VOID_POINTER_PRIMITIVE_TYPES = "Primitive Types";
    public static final String OPTION_VOID_POINTER_STRUCTURE_TYPES = "Structure Types";
    public static final String OPTION_VOID_POINTER_USER_CODE = "User Code";

    private final static Logger logger = LoggerFactory.getLogger(RandomInputGeneration.class);

    private ICommonFunctionNode functionNode;
    private int depth = 0;
    private String additionalHeader = SpecialCharacter.EMPTY;

    private ConstructorNode selectedConstructor;

    private TestPrototype selectedPrototype;

    private static RandomInputGeneration instance;

    // store the template type to real type in template function, e.g, "T"->"int"
    // key: template type/void*
    // value: real type
    private Map<String, String> realTypeMapping = new HashMap<>();
    /**
     * The maximum depth, especially to avoid infinite expansion in linked list
     * <p>
     * Ex: x: depth = 1; x.a: depth = 2; x.x.a: depth = 3
     */
    private final int MAX_DEPTH = 5;

    /**
     * The limit of size in array expansion.
     * If the size of an array is too high, the performance might become poor.
     */
    private final int LIMIT_ARRAY_AND_POINTER_SIZE = 20;

    private Map<String, INode> selectedNodesInVoidPointer = new HashMap<>();
    private HashMap<String, List<String>> userCode = new HashMap<>();

    public static final String DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT = "_";
    public static final String DIMENSIONAL_STARTING = "[";
    public static final String DIMENSIONAL_END = "]";
    public static final String DELIMITER_BETWEEN_STRUCT_INSTANCE_AND_ATTRIBUTE = ".";
    public static final String DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT = "_";
    private RootDataNode root;

    public static RandomInputGeneration getInstance() {
        if (instance == null ) {
            instance = new RandomInputGeneration();
        }
            return instance;
    }

    public void setUserCode(HashMap<String, List<String>> userCode) {
        this.userCode = userCode;
    }

    protected List<RandomValue> constructRandomInput(IVariableNode argument, IFunctionConfig functionConfig,
                                                     String prefixName) {
        if (prefixName == null)
            prefixName = SpecialCharacter.EMPTY;
        List<IVariableNode> vars = new ArrayList<>();
        vars.add(argument);
        return new ArrayList<>(constructRandomInput(vars, functionConfig, prefixName));
    }

    IVariableNode originalCurrentVarNode = null;

    protected List<RandomValue> constructRandomInput(List<IVariableNode> arguments, IFunctionConfig functionConfig,
                                                     String prefixName) {
        List<RandomValue> input = new ArrayList<>();
        for (IVariableNode argument : arguments) {
            boolean isRandomValue = true;
            if (!userCode.isEmpty()) {
                String content = UserCodeTestDataManager.getFirstContentForArgument(userCode, argument.getName());//lấy usercode đau tiên
                if (!content.equals("")) {
                    isRandomValue = false;
                    handleUserCode(content, input, argument, prefixName);
                }
            }

            if (isRandomValue){
                //radomvalue
                if (depth == 0)
                    originalCurrentVarNode = argument;
                else if (depth > MAX_DEPTH) {
                    logger.debug("Interrupt " + IdMapping.getInstance().getOrCreate(argument.getName()) + " due to reach the maximum of test chain (MAX_DEPTH = " + MAX_DEPTH + ")");
                    continue;
                }
                depth++;
                try {
                    logger.debug("Analyze " + prefixName + IdMapping.getInstance().getOrCreate(argument.getName()));

                    // normalize type
                    String type = argument.getRealType();
                    type = VariableTypeUtils.deleteStorageClasses(type);
                    type = VariableTypeUtils.deleteReferenceOperator(type);

                    handleTypes(type, input, argument, prefixName, functionConfig);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    depth--;
                }
            }
        }
        return input;
    }

    protected void handleUserCode(String content, List<RandomValue> input, IVariableNode argument,
            String prefixName) {

        String nameUsedInExpansion = prefixName + argument.getName();
        input.add(new UserCodeValue(nameUsedInExpansion,content));

    }

    protected void handleTypes(String type, List<RandomValue> input, IVariableNode argument,
                               String prefixName, IFunctionConfig functionConfig) {
        // Boolean
        if (VariableTypeUtils.isBoolBasic(type)) {
            logger.debug(type + ": isBoolBasic");
            handleBool(input, prefixName, argument, functionConfig);

        }
        // Character
        else if (VariableTypeUtils.isChBasic(type)) {
            logger.debug(type + ": isChBasic");
            handleChBasic(input, prefixName, argument, functionConfig);

        }
        // Number
        else if (VariableTypeUtils.isNumBasic(type)) {
            logger.debug(type + ": isNumBasic");
            handleNumBasic(input, prefixName, argument, type, functionConfig);

        } //TODO
//        // Template
//        else if (TemplateUtils.isTemplateTypeDefinedByUser(type, functionConfig.getFunctionNode())) {
//            logger.debug(type + ": isTemplateTypeDefinedByUser");
//            handleTemplateTypeDefinedByUser(input, prefixName, argument, type, functionConfig);
//
//        }
        // Structure
        else if (VariableTypeUtils.isStructureSimple(type)) { // ok
            logger.debug(type + ": isStructureSimple");
            handleStructureSimple(input, prefixName, argument, type, functionConfig);

        }
        // Std
        else if (VariableTypeUtilsForStd.isStdVectorBasic(type)) {
            logger.debug(type + ": isStdVectorBasic");
            handleStdVector(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdListBasic(type)) {
            logger.debug(type + ": isStdListBasic");
            handleStdList(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdSetBasic(type)) {
            logger.debug(type + ": isStdSetBasic");
            handleStdSet(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdStackBasic(type)) {
            logger.debug(type + ": isStdStackBasic");
            handleStdStack(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdQueueBasic(type)) {
            logger.debug(type + ": isStdSQueueBasic");
            handleStdQueue(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdPairBasic(type)) {
            logger.debug(type + ": isStd Pair Basic");
            handleStdPair(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isStdMapBasic(type)) {
            logger.debug(type + ": isStd Map Basic");
            handleStdMap(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtilsForStd.isSharedPtr(type)) {
            logger.debug(type + ": isSharedPtr");
            handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*shared_ptr\\s*<",
                    SharedPtrDataNode.class.getName());

        } else if (VariableTypeUtilsForStd.isUniquePtr(type)) {
            logger.debug(type + ": isUniquePtr");
            handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*unique_ptr\\s*<",
                    UniquePtrDataNode.class.getName());

        } else if (VariableTypeUtilsForStd.isWeakPtr(type)) {
            logger.debug(type + ": isWeakPtr");
            handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*weak_ptr\\s*<",
                    WeakPtrDataNode.class.getName());

        } else if (VariableTypeUtilsForStd.isAutoPtr(type)) {
            logger.debug(type + ": isAutoPtr");
            handleSmartPointer(input, prefixName, argument, type, functionConfig, "std\\s*::\\s*auto_ptr\\s*<",
                    AutoPtrDataNode.class.getName());

        } else if (VariableTypeUtilsForStd.isFunction(type)) {
            logger.debug(type + ": isLambda");
            handleLambda(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtils.isFunctionPointer(type)) {
            logger.debug(type + ": is function pointer");

//            ISourcecodeFileNode uut = Utils.getSourcecodeFile(functionNode);
//            if (uut.getName().endsWith(IProjectLoader.C_FILE_SYMBOL))
                handleFunctionPointerForC(input, prefixName, argument, type, functionConfig);
//            else {
//                handleFunctionPointerForCpp(input, prefixName, argument, type, functionConfig);
//            }
        }

        // Multi-dimension
        else if (VariableTypeUtils.isNumMultiDimension(type)
                || VariableTypeUtils.isChMultiDimension(type)
                || VariableTypeUtilsForStd.isStdVectorMultiDimension(type)
                || VariableTypeUtilsForStd.isStdListMultiDimension(type)
                || VariableTypeUtilsForStd.isStdStackMultiDimension(type)
                || VariableTypeUtilsForStd.isStdSetMultiDimension(type)
                || VariableTypeUtilsForStd.isStdQueueMultiDimension(type)
                || VariableTypeUtilsForStd.isStdPairMultiDimension(type)
                || VariableTypeUtilsForStd.isStdMapMultiDimension(type)
                || VariableTypeUtils.isBoolMultiDimension(type)
                || VariableTypeUtils.isStructureMultiDimension(type)
                || VariableTypeUtils.isStrMultiDimension(type)) {
            logger.debug(type + ": is multi dimension");
            handleMultiDimensionalArray(input, prefixName, argument, type, functionConfig,
                    "\\s*\\[[a-zA-Z0-9_\\s]*\\]\\s*$", SpecialCharacter.EMPTY);
        }
        // Multi-level
        else if (VariableTypeUtils.isNumMultiLevel(type)
                || VariableTypeUtils.isChMultiLevel(type)

                || VariableTypeUtilsForStd.isStdVectorMultiLevel(type)
                || VariableTypeUtilsForStd.isStdListMultiLevel(type)
                || VariableTypeUtilsForStd.isStdStackMultiLevel(type)
                || VariableTypeUtilsForStd.isStdSetMultiLevel(type)
                || VariableTypeUtilsForStd.isStdQueueMultiLevel(type)
                || VariableTypeUtilsForStd.isStdPairMultiLevel(type)
                || VariableTypeUtilsForStd.isStdMapMultiLevel(type)

                || VariableTypeUtils.isBoolMultiLevel(type)
                || VariableTypeUtils.isStructureMultiLevel(type)
                || VariableTypeUtils.isStrMultiLevel(type)) {
            logger.debug(type + ": is multi level");
            handleMultiLevelPointer(input, prefixName, argument, type, functionConfig,
                    IRegex.POINTER, SpecialCharacter.EMPTY);

        } else if (VariableTypeUtils.isStrBasic(type)) {
            logger.debug(type + ": isString");
            handleString(input, prefixName, argument, type, functionConfig);

        } else if (VariableTypeUtils.isVoidPointer(type)) {
            logger.debug(type + ": isVoidPointer");

            ISourcecodeFileNode uut = Utils.getSourcecodeFile(functionNode);
            if (uut.getName().endsWith(IProjectLoader.C_FILE_SYMBOL))
                handleVoidPointerForC(input, prefixName, argument, type, functionConfig);
            else
                handleVoidPointerForCpp(input, prefixName, argument, type, functionConfig);
        } else if (VariableTypeUtils.isPointerMultiDimension(type)) {
            handleMultiDimensionalArray(input, prefixName, argument, type, functionConfig,
                    "\\s*\\[[a-zA-Z0-9_\\s]*\\]\\s*$", SpecialCharacter.EMPTY);
        } if (VariableTypeUtils.isQT(type) && QTDataNode.ENABLE) {
            handleQT(input, prefixName, argument, type, functionConfig);
        } else {
            logger.debug("Do not support to generate a random value of " + type);
        }
    }

    private void handleFunctionPointerForCpp(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        // TODO: support later
    }

    private void handleFunctionPointerForC(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        if (isDefinedType(argument.getName()))
            handleFunctionPointerForC_definedType(input, prefixName, argument, type, functionConfig);
        else
            handleFunctionPointerForC_undefinedType(input, prefixName, argument, type, functionConfig);
    }

    private void handleFunctionPointerForC_definedType(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + IdMapping.getInstance().getOrCreate(argument.getName()) + "\"");
        String value = realTypeMapping.get(argument.getName());
        String name = prefixName + argument.getName();
        input.add(new RandomValueForAssignment(name, value));
    }

    private void handleFunctionPointerForC_undefinedType(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + IdMapping.getInstance().getOrCreate(argument.getName()) + "\"");

        if (realTypeMapping.containsKey(argument.getName()))
            handleFunctionPointerForC_definedType(input, prefixName, argument, type, functionConfig);
        else {
            INode typeNode = argument.getCorrespondingNode();

            if (typeNode instanceof FunctionPointerTypeNode) {
                List<INode> possibleRefs = Search.searchAllMatchFunctions(functionNode, (FunctionPointerTypeNode) typeNode);
                // todo: handle if possibleRefs is empty
                if (!possibleRefs.isEmpty()) {

                    INode selectedRef = possibleRefs.get(new Random().nextInt(possibleRefs.size()));

                    String value = selectedRef.getName();
                    String name = prefixName + argument.getName();
                    input.add(new RandomValueForAssignment(name, value));

//                    String filePath = Utils.getSourcecodeFile(selectedRef).getAbsolutePath();
//                    String cloneFilePath = ProjectClone.getClonedFilePath(filePath);
//                    if (!new File(cloneFilePath).exists())
//                        cloneFilePath = filePath;
//                    String includeStm = String.format("#include \"%s\"", cloneFilePath);
//                    additionalHeader += includeStm;
                } else {
                    boolean useDefault = new Random().nextBoolean();
                    if (useDefault) {
                        String value = FunctionPointerDataNode.DEFAULT;
                        String name = prefixName + argument.getName();
                        input.add(new RandomValueForAssignment(name, value));
                    }
                }
            }
        }
    }

    boolean isDefinedType(String varName) {
        for (String genericVarName : realTypeMapping.keySet())
            if (genericVarName.equals(varName)) {
                return true;
            }
        return false;
    }

    private void handleVoidPointerForC(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        if (isDefinedType(argument.getName()))
            handleVoidPointerForC_definedType(input, prefixName, argument, type, functionConfig);
        else
            handleVoidPointerForC_autoType(input, prefixName, argument, type, functionConfig);
    }

    private void handleVoidPointerForC_autoType(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
//        realTypeMapping = new VoidPtrTypeResolver(functionNode).getTypeMap();
        handleVoidPointerForC_definedType(input, prefixName, argument, type, functionConfig);
    }

    private void handleVoidPointerForC_undefinedType(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + IdMapping.getInstance().getOrCreate(argument.getName()) + "\"");

        // choose category
        String categories[] = new String[]{
                OPTION_VOID_POINTER_PRIMITIVE_TYPES,
                OPTION_VOID_POINTER_STRUCTURE_TYPES,
                OPTION_VOID_POINTER_USER_CODE
        };
        int selectedCategory = new RandomDataGenerator().nextInt(0, categories.length - 1);
        logger.debug("Choose \"" + categories[selectedCategory] + "\"");

        // choose core type
        INode selectedCoreTypeNode = null;
        String selectedCoreType = SpecialCharacter.EMPTY;
        switch (categories[selectedCategory]) {
            case OPTION_VOID_POINTER_PRIMITIVE_TYPES: {
                List<INode> allPrimitiveNodes = VariableTypeUtils.getAllPrimitiveTypeNodes();
//                selectedCoreTypeNode = allPrimitiveNodes.get(new RandomDataGenerator().nextInt(0, allPrimitiveNodes.size() - 1));
//                selectedCoreType = selectedCoreTypeNode.getName();

                /**
                 * Choose "int"
                 */
                selectedCoreType = VariableTypeUtils.BASIC.NUMBER.INTEGER.INT;
                for (INode n : allPrimitiveNodes) {
                    if (n.getName().equals(selectedCoreType)) {
                        selectedCoreTypeNode = n;
                        break;
                    }
                }

                selectedNodesInVoidPointer.put(selectedCoreType, selectedCoreTypeNode);
                break;
            }
            case OPTION_VOID_POINTER_STRUCTURE_TYPES: {
                List<INode> allStructureNodes = VariableTypeUtils.getAllStructureNodes(functionNode);
                selectedCoreTypeNode = allStructureNodes.get(new RandomDataGenerator().nextInt(0, allStructureNodes.size() - 1));
                selectedCoreType = selectedCoreTypeNode.getName();

                // for testing - begin
//                for (INode n: allStructureNodes)
//                    if (n.getName().equals("_ListEntry")) {
//                        selectedCoreType = n.getName();
//                        selectedCoreTypeNode = n;
//                    }
                // for testing - end
                selectedNodesInVoidPointer.put(selectedCoreType, selectedCoreTypeNode);
                break;
            }
        }

        // generate level of pointer
//        final PrimitiveBound pointerLevelBound = functionConfig.getBoundOfPointer();
//        final int maxLevel = (int) pointerLevelBound.getUpperAsLong();
//        int minLevel = (int) pointerLevelBound.getLowerAsLong();
//        if (minLevel == 0)
//            minLevel = 1;
        int size = new RandomDataGenerator().nextInt(1, 1);

        StringBuilder level = new StringBuilder();
        for (int i = 0; i < size; i++) {
            level.append("*");
        }

        input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                String.format("%s=%s,%s=%s,%s=%s",
                        VOID_POINTER____SELECTED_CORE_TYPE, selectedCoreType,
                        VOID_POINTER____SELECTED_CATEGORY, categories[selectedCategory],
                        VOID_POINTER____POINTER_LEVEL, level.length())));

        // generate value for elements
        IVariableNode var = new VariableNode();
        String tmpType = String.format("%s%s", selectedCoreType, level.toString());
        var.setRawType(tmpType);
        var.setCoreType(selectedCoreType);
        var.setParent(argument);
        var.setName(prefixName + argument.getName() + "_" + NAME_REFERENCE);
        input.addAll(constructRandomInput(var, functionConfig, SpecialCharacter.EMPTY));
    }

    private final static String NAME_REFERENCE = "value";

    private void handleVoidPointerForC_definedType(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + IdMapping.getInstance().getOrCreate(argument.getName()) + "\"");

        // get the configured selected type
        String selectedType = SpecialCharacter.EMPTY;
        for (String genericVarName : realTypeMapping.keySet()) {
            if (genericVarName.equals(argument.getName())) {
                selectedType = realTypeMapping.get(genericVarName);
            }
        }

        if (selectedType == null || selectedType.length() == 0) {
            handleVoidPointerForC_undefinedType(input, prefixName, argument, type, functionConfig);
            return;
        }
        //
        String selectedCoreType = selectedType.replaceAll("(\\*)+", SpecialCharacter.EMPTY).trim();
        //

        INode selectedNode = null;
        String category = null;

        // check whether the selected type is primitive
        List<INode> allPrimitiveNodes = VariableTypeUtils.getAllPrimitiveTypeNodes();
        for (INode node : allPrimitiveNodes)
            if (node.getName().equals(selectedCoreType)) {
                selectedNode = node;
                category = OPTION_VOID_POINTER_PRIMITIVE_TYPES;
                break;
            }

        // check whether the selected type is structure
        if (selectedNode == null) {
            selectedCoreType = VariableTypeUtils.removeRedundantKeyword(selectedCoreType);
            List<INode> allStructureNodes = VariableTypeUtils.getAllStructureNodes(functionNode);
            for (INode node : allStructureNodes)
                if (node.getName().equals(selectedCoreType)) {
                    selectedNode = node;
                    category = OPTION_VOID_POINTER_STRUCTURE_TYPES;
                    break;
                }
        }

        if (selectedNode == null)
            return;

        selectedNodesInVoidPointer.put(selectedNode.getName(), selectedNode);

        // generate level of pointer
        String tmp = selectedType;
        String level = SpecialCharacter.EMPTY;
        while (tmp.endsWith("*")) {
            level += "*";
            tmp = tmp.substring(0, tmp.length() - 1);
        }

        input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                String.format("%s=%s,%s=%s,%s=%s",
                        VOID_POINTER____SELECTED_CORE_TYPE, selectedCoreType,
                        VOID_POINTER____SELECTED_CATEGORY, category,
                        VOID_POINTER____POINTER_LEVEL, level.length())));

        // generate value for elements
        IVariableNode var = new VariableNode();
        String tmpType = String.format("%s%s", selectedCoreType, level);
        var.setRawType(tmpType);
        var.setCoreType(selectedCoreType);
        var.setParent(argument);
        var.setName(prefixName + argument.getName() + "_" + NAME_REFERENCE);
        input.addAll(constructRandomInput(var, functionConfig, SpecialCharacter.EMPTY));
    }

    private void handleVoidPointerForCpp(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("Generating a random initialization of type = \"" + type + "\", name = \"" + IdMapping.getInstance().getOrCreate(argument.getName()) + "\"");
        final int PRIMITIVE_TYPE = 0;
        final int POINTER_TYPE = 1;
        final int STD_SET = 2;
        final int STD_LIST = 3;
        final int STD_MAP = 4;
        final int STD_QUEUE = 5;
        final int STD_STACK = 6;
        final int STD_VECTOR = 7;
        final int STD_PAIR = 8;


        // get all basic types
        List<Class> possibleClasses = new ArrayList<>();
        possibleClasses.add(VariableTypeUtils.BASIC.STRING.class);
        possibleClasses.add(VariableTypeUtils.BASIC.NUMBER.class);
        possibleClasses.add(VariableTypeUtils.BASIC.CHARACTER.class);
        possibleClasses.add(VariableTypeUtils.BASIC.BOOLEAN.class);
        Class selectedClass = possibleClasses.get(new Random().nextInt(possibleClasses.size()));
        logger.debug("selectedClass = " + selectedClass);
        List<String> possibleTypes = VariableTypeUtils.getAllBasicFieldNames(selectedClass);
        logger.debug("possibleTypes = " + possibleTypes);

        // generate complex type
        int ran = PRIMITIVE_TYPE; // default for both C project and Cpp project
        //TODO:
//        if (Environment.getInstance().getCompiler().isGPlusPlusCommand())
            ran = new Random().nextInt(9);
        switch (ran) {
            case PRIMITIVE_TYPE: {
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        // a random value
                        "void* " + argument.getName() + " = " +
                                new RandomDataGenerator().nextInt(-999999, 999999) + ";"));
                break;
            }

            case POINTER_TYPE: {
                String selectedType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        "void* " + argument.getName() + " = new " + selectedType + "[" + functionConfig.getBoundOfArray().getLower() + "];"));
                break;
            }
            case STD_LIST: {
                String selectedType = "list";
                additionalHeader += "#include <list>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_MAP: {
                String selectedType = "map";
                additionalHeader += "#include <map>;";
                String coreType1 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                String coreType2 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* p = new std::map<int, float>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s, %s>[%s];",
                                argument.getName(), selectedType, coreType1, coreType2, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_SET: {
                String selectedType = "set";
                additionalHeader += "#include <set>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: std::map<int, float>* p = new std::map<int, float>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_QUEUE: {
                String selectedType = "queue";
                additionalHeader += "#include <queue>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* p = new std::map<int, float>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_STACK: {
                String selectedType = "stack";
                additionalHeader += "#include <stack>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::stack<char>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_VECTOR: {
                String selectedType = "vector";
                additionalHeader += "#include <vector>;";
                String coreType = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::vector<char>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s>[%s];",
                                argument.getName(), selectedType, coreType, functionConfig.getBoundOfArray().getLower())));
                break;
            }
            case STD_PAIR: {
                String selectedType = "pair";
                additionalHeader += "#include <utility>;";
                String coreType1 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                String coreType2 = possibleTypes.get(new Random().nextInt(possibleTypes.size()));
                // Ex: void* v = new std::vector<char, char>[5];
                input.add(new RandomValueForAssignment(prefixName + argument.getName(),
                        String.format("void* %s = new std::%s<%s, %s>[%s];",
                                argument.getName(), selectedType, coreType1, coreType2, functionConfig.getBoundOfArray().getLower())));
                break;
            }
        }
    }

    private void handleTemplateTypeDefinedByUser(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        // generate random value
        if (this.selectedPrototype != null && this.selectedPrototype.getRootDataNode() != null) {
            SubprogramNode subprogramNode = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode());

            // map template type and real type
            List<IDataNode> args = Search2.findArgumentNodes(root);
            for (IDataNode arg : args) {
                if (arg instanceof ValueDataNode) {
                    realTypeMapping.put(arg.getName(), ((ValueDataNode) arg).getRawType());
                }
            }

            //
            List<IVariableNode> vars = new ArrayList<>();
            for (IDataNode child : subprogramNode.getChildren())
                if (!(child.getName().equals(INameRule.RETURN_VARIABLE_NAME_PREFIX)))
                    if (child instanceof ValueDataNode)
                        if (argument.getName().equals(child.getName())) {
                            IVariableNode variableNode = new TmpVariableNode();
                            variableNode.setName(argument.getName());
                            variableNode.setRawType(realTypeMapping.get(argument.getName()));
                            variableNode.setParent(argument.getParent());

                            vars.add(variableNode);
                        }

            input.addAll(constructRandomInput(vars, functionConfig, prefixName));
        }
    }

    private String arrayElement(long index) {
        return DIMENSIONAL_STARTING + index + DIMENSIONAL_END;
    }

    public static String arraySize(String arrayName, long size) {
//        return "sizeof(" + arrayName + ")=" + size;
        return size + SpecialCharacter.EMPTY;
    }

    private long getFirstSizeOfArray(String varName, String type, IFunctionConfig functionConfig) {
        // get size
        int level = PointerTypeInitiation.getLevel(type);
        if (level == 0)
            // is an array
            level = Utils.getIndexOfArray(type).size();

        long size = 0;
        IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(varName);
        if (bound != null) {
            if (bound instanceof PointerOrArrayBound) {
                List<String> indexes = ((PointerOrArrayBound) bound).getIndexes();
                String currentIndex;
                if (indexes.size() >= level)
                    currentIndex = indexes.get(indexes.size() - level);
                else
                    currentIndex = indexes.get(0);
                long lower = 0;
                long upper = 0;
                if (currentIndex.contains(IFunctionConfigBound.RANGE_DELIMITER)) {
                    lower = Long.parseLong(currentIndex.split(IFunctionConfigBound.RANGE_DELIMITER)[0]);
                    upper = Long.parseLong(currentIndex.split(IFunctionConfigBound.RANGE_DELIMITER)[1]);
                } else {
                    lower = Long.parseLong(currentIndex);
                    upper = Long.parseLong(currentIndex);
                }
                size = BasicTypeRandom.random(lower, upper);
            } else {
                if (type.endsWith("*")) {
                    size = BasicTypeRandom.random(functionConfig.getBoundOfPointer().getLowerAsLong(),
                            functionConfig.getBoundOfPointer().getUpperAsLong());
                } else {
                    List<String> indexes = Utils.getIndexOfArray(type);
                    if (indexes.size() >= 1 && Utils.toInt(indexes.get(0)) != Utils.UNDEFINED_TO_INT)
                        size = Utils.toInt(indexes.get(0)); // return the first size
                    else
                        size = BasicTypeRandom.random(functionConfig.getBoundOfArray().getLowerAsLong(),
                                functionConfig.getBoundOfArray().getUpperAsLong());
                }
            }
        } else {
            List<String> indexes = Utils.getIndexOfArray(type);
            if (indexes.size() >= 1 && Utils.toInt(indexes.get(0)) != Utils.UNDEFINED_TO_INT)
                size = Utils.toInt(indexes.get(0)); // return the first size
            else
                size = BasicTypeRandom.random(functionConfig.getBoundOfArray().getLowerAsLong(),
                        functionConfig.getBoundOfArray().getUpperAsLong());
        }
        return size;
    }

    private long getFirstSizeOfPointer(IFunctionConfig functionConfig) {
        return BasicTypeRandom.random(
                functionConfig.getBoundOfPointer().getLowerAsLong(),
                functionConfig.getBoundOfPointer().getUpperAsLong());
    }

    private void handleString(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("type " + type + " : handleString");

        long size = getFirstSizeOfPointer(functionConfig);
        if (size == 0) // string size >= 1
            size = 1;

        //
        String stringValue = arraySize(prefixName + argument.getName(), size);
        input.add(new RandomValueForSizeOf(prefixName + argument.getName(), stringValue));

        //
        for (int i = 0; i < size; i++) {
            String value = SpecialCharacter.EMPTY;
            String configName = getConfigName(argument);
            IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
            if (bound == null)
                bound = functionConfig.getBoundOfOtherCharacterVars();

            String lower = IFunctionConfigBound.MIN_VARIABLE_TYPE, upper = IFunctionConfigBound.MAX_VARIABLE_TYPE;
            if (bound instanceof PrimitiveBound) {
                lower = ((PrimitiveBound) bound).getLower();
                upper = ((PrimitiveBound) bound).getUpper();
            }

            String elementType = NormalStringDataNode.getStringToCharacterTypeMap().get(type.replace(" ", SpecialCharacter.EMPTY));
            if (elementType != null && elementType.length() > 0) {
                long ascii = BasicTypeRandom.generateInt(
                        lower,
                        upper,
                        elementType);
                value = ascii + SpecialCharacter.EMPTY;

                String nameUsedInExpansion = prefixName + argument.getName() + "[" + i + "]";
                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
            }
        }
    }

    private void handleStructureSimple(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("type " + type + " : handleStructureSimple");
        INode correspondingNode = selectedNodesInVoidPointer.get(argument.getCoreType());
        if (correspondingNode == null)
            correspondingNode = argument.getCorrespondingNode();

        if (correspondingNode instanceof IEmptyStructureNode)
            correspondingNode = ((IEmptyStructureNode) correspondingNode).getDefinition();

        if (correspondingNode instanceof StructNode) {
            logger.debug("Defined in " + IdMapping.getInstance().getOrCreate(correspondingNode.getAbsolutePath()));
//            input.add(new RandomValueForAssignment(prefixName + argument.getName(), correspondingNode.getName()));
            input.addAll(constructRandomInput(((StructNode) correspondingNode).getAttributes(),
                    functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_STRUCT_INSTANCE_AND_ATTRIBUTE));

        } else if (correspondingNode instanceof ClassNode) {
            String nameUsedInExpansion = prefixName + argument.getName();
            logger.debug("Defined in " + IdMapping.getInstance().getOrCreate(correspondingNode.getAbsolutePath()));
            if (this.selectedConstructor == null) {
                // choose an inheritance class or this class
                List<INode> derivedNodes = ((ClassNode) correspondingNode).getDerivedNodes();
                derivedNodes.removeIf(n -> !(n instanceof ClassNode) || ((ClassNode) n).isAbstract());
                int randomDerivedNodes = new Random().nextInt(derivedNodes.size());
                INode derivedNode = derivedNodes.get(randomDerivedNodes);
                input.add(new RandomValueForAssignment(nameUsedInExpansion, derivedNode.getName()));
                // choose a constructor (instance) or among constructors
                List<ICommonFunctionNode> constructors = ((ClassNode) derivedNodes.get(randomDerivedNodes)).getPublicConstructors();
                logger.debug("There are " + constructors.size() + " candidate constructors");
                String value = "";

                if (constructors.size() > 0) {
                    int randomConstructor = new Random().nextInt(constructors.size());
                    value = constructors.get(randomConstructor).getName();

                    logger.debug("Choose constructor " + IdMapping.getInstance().getOrCreate(value));

                    input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                    input.addAll(constructRandomInput(constructors.get(randomConstructor).getArguments(),
                            functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
                } else {
                    List<ICommonFunctionNode> instances = ((ClassNode) derivedNodes.get(randomDerivedNodes)).getInstanceMethods();
                    int randomInstance = new Random().nextInt(instances.size());
                    value = instances.get(randomInstance).getName();
                    logger.debug("Choose constructor " + IdMapping.getInstance().getOrCreate(value));

                    input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                    input.addAll(constructRandomInput(instances.get(randomInstance).getArguments(),
                            functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
                }
            } else {
                String value = this.selectedConstructor.getName();
                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                List<IVariableNode> arguments = this.selectedConstructor.getArguments();
                this.selectedConstructor = null; // reset
                input.addAll(constructRandomInput(arguments,
                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            }

        } else if (correspondingNode instanceof EnumNode) {
            List<String> possibleValues = ((EnumNode) correspondingNode).getAllNameEnumItems();
            String chosenValue = possibleValues.get(new Random().nextInt(possibleValues.size()));
            input.add(new RandomValueForAssignment(prefixName + argument, chosenValue));

        } else if (correspondingNode instanceof UnionNode) {
            // choose a random attribue in union
            List<INode> possibleValues = correspondingNode.getChildren();
            IVariableNode chosenValue = (IVariableNode) possibleValues.get(new Random().nextInt(possibleValues.size()));
            input.add(new RandomValueForAssignment(prefixName + argument, chosenValue.getName()));

            // generate value for attribute
            TmpVariableNode tmpvar = new TmpVariableNode();
            String insideType = chosenValue.getRawType();
            tmpvar.setRawType(insideType);
            tmpvar.setName(chosenValue.getName());
            tmpvar.setCoreType(insideType);
            tmpvar.setParent(argument.getParent());
            tmpvar.setAbsolutePath(argument.getAbsolutePath());

            input.addAll(constructRandomInput(tmpvar,
                    functionConfig, prefixName + argument.getName() + "."));
        } else {
            logger.debug("not found definition of " + type);
        }
    }

    private void handleChBasic(List<RandomValue> input, String prefixName, IVariableNode argument, IFunctionConfig functionConfig) {
        String configName = getConfigName(argument);
        IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
        if (bound == null)
            bound = functionConfig.getBoundOfOtherCharacterVars();

        if (bound instanceof PrimitiveBound) {
            String lower = ((PrimitiveBound) bound).getLower();
            String upper = ((PrimitiveBound) bound).getUpper();
            long ascii = BasicTypeRandom.generateInt(lower, upper, argument.getRawType());

            String nameUsedInExpansion = prefixName + argument.getName();

            String value = ascii + SpecialCharacter.EMPTY;

            input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
        } else if (bound instanceof MultiplePrimitiveBound) {
            long ascii = generateIntMultipleBound((MultiplePrimitiveBound) bound, argument.getRawType());

            String nameUsedInExpansion = prefixName + argument.getName();

            String value = ascii + SpecialCharacter.EMPTY;

            input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
        }
    }

    private void handleNumBasic(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        if (VariableTypeUtils.isNumBasicFloat(type)) {
            String nameUsedInExpansion = prefixName + argument.getName();
            String configName = getConfigName(argument);
            IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);
            if (bound == null)
                bound = functionConfig.getBoundOfOtherNumberVars();

            if (bound instanceof PrimitiveBound) {
                String lower = ((PrimitiveBound) bound).getLower();
                String upper = ((PrimitiveBound) bound).getUpper();
                String value = BasicTypeRandom.generateFloat(lower, upper) + SpecialCharacter.EMPTY;
                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
            } else if (bound instanceof MultiplePrimitiveBound) {
                String value = generateFloatMultipleBound((MultiplePrimitiveBound) bound) + SpecialCharacter.EMPTY;
                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
            }
        } else {
            // integer type
            boolean isSizeOfArrayOrPointer = false;
            // two cases happen: (1) this variable is size of array/pointer, (2) this variable is not size of array/pointer
            if (argument.getParent() instanceof FunctionNode)
                for (Dependency d : argument.getDependencies())
                    if (d instanceof SizeOfArrayOrPointerDependency) {
                        isSizeOfArrayOrPointer = true;
                        String nameUsedInExpansion = prefixName + argument.getName();
                        String value = functionConfig.getBoundOfArray().getLower() + SpecialCharacter.EMPTY;
                        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                        break;
                    }
            if (!isSizeOfArrayOrPointer) {
                String nameUsedInExpansion = prefixName + argument.getName();

                String configName = getConfigName(argument);
                IFunctionConfigBound bound = functionConfig.getBoundOfArgumentsAndGlobalVariables().get(configName);

                // when we do not know the bound of variable
                if (bound == null || bound instanceof UndefinedBound)
                    bound = functionConfig.getBoundOfOtherNumberVars();

                if (bound instanceof PrimitiveBound) {
                    String lower = ((PrimitiveBound) bound).getLower();
                    String upper = ((PrimitiveBound) bound).getUpper();
                    String value = BasicTypeRandom.generateInt(lower, upper, argument.getRawType()) + SpecialCharacter.EMPTY;
                    input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                } else if (bound instanceof MultiplePrimitiveBound) {
                    String value = generateIntMultipleBound((MultiplePrimitiveBound) bound, argument.getRawType()) + SpecialCharacter.EMPTY;
                    input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                }
            }
        }
    }

    private void handleBool(List<RandomValue> input, String prefixName, IVariableNode argument, IFunctionConfig functionConfig) {
        String nameUsedInExpansion = prefixName + argument.getName();
        long value = BasicTypeRandom.generateInt("0", "1", argument.getRawType());
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value + SpecialCharacter.EMPTY));
    }


    private List<Long> generateAListOfRandomNumbers(long realSize, long limit) {
        List<Long> randomNumbers = new ArrayList<>();

        if (limit < realSize) {
            // just select some random indexes to initialize values
            for (int i = 0; i < limit; i++)
                randomNumbers.add(BasicTypeRandom.random(0, realSize));
        } else {
            for (long i = 0; i < realSize; i++)
                randomNumbers.add(i);
        }
        return randomNumbers;
    }

    private void handleMultiDimensionalArray(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig,
                                             String regrex, String replacement) {
        long size = getFirstSizeOfArray(originalCurrentVarNode.getName(), type, functionConfig);

        logger.debug("Size = " + size);
        String nameUsedInExpansion = prefixName + argument.getName();
        String value = arraySize(prefixName + argument.getName(), size);
        input.add(new RandomValueForSizeOf(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        List<Long> randomNumbers = generateAListOfRandomNumbers(size, LIMIT_ARRAY_AND_POINTER_SIZE);
        for (Long index : randomNumbers) {
//        for (int index = 0; index < size; index++)
            IVariableNode tmpvar = new TmpVariableNode();
            // Ex: "a[3][5]" -> "a[3]"
            String insideType = type.replaceAll(regrex, replacement);
            tmpvar.setRawType(insideType);
            tmpvar.setCoreType(insideType);
            tmpvar.setParent(argument.getParent());
            tmpvar.setAbsolutePath(argument.getAbsolutePath());
            tmpvar.setName(arrayElement(index));
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefixName + argument.getName()));
    }

    private void handleMultiLevelPointer(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig,
                                         String regrex, String replacement) {
        if (depth >= MAX_DEPTH) {
            logger.debug("Assign pointer to null");
            String nameUsedInExpansion = prefixName + argument.getName();
            String value = arraySize(prefixName + argument.getName(), PointerDataNode.NULL_VALUE);
            input.add(new RandomValueForSizeOf(nameUsedInExpansion, value));

        } else {
            long size = getFirstSizeOfArray(originalCurrentVarNode.getName(), type, functionConfig);
            if (size == 0) {
                logger.debug("Assign pointer to null");
                String nameUsedInExpansion = prefixName + argument.getName();
                String value = arraySize(prefixName + argument.getName(), PointerDataNode.NULL_VALUE);
                input.add(new RandomValueForSizeOf(nameUsedInExpansion, value));
            } else {
                logger.debug("Size = " + size);
                String nameUsedInExpansion = prefixName + argument.getName();
                String value = arraySize(prefixName + argument.getName(), size);
                input.add(new RandomValueForSizeOf(nameUsedInExpansion, value));
            }

            List<IVariableNode> list = new ArrayList<>();
            List<Long> randomNumbers = generateAListOfRandomNumbers(size, LIMIT_ARRAY_AND_POINTER_SIZE);
            for (Long index : randomNumbers) {
//        for (int index = 0; index < size; index++)
                IVariableNode tmpvar = new TmpVariableNode();
                // Ex: "a[3][5]" -> "a[3]"
                // TODO: Lamnt fix replace all -> replace first
                String insideType = type.replaceFirst(regrex, replacement);
                tmpvar.setRawType(insideType);
                tmpvar.setCoreType(type.replaceAll(regrex, replacement));
                tmpvar.setParent(argument.getParent());
                tmpvar.setAbsolutePath(argument.getAbsolutePath());
                tmpvar.setName(arrayElement(index));
                list.add(tmpvar);
            }

            input.addAll(constructRandomInput(list,
                    functionNode.getFunctionConfig(),
                    prefixName + argument.getName()));
        }
    }

    private void handleStdStack(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::stack is "top"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "top");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdSet(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::set is "begin"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "begin");
            else if (i == size - 1)
                // the virtual name of the last element in std::set is "end"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "end");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdList(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::list is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "front");
            else if (i == size - 1)
                // the virtual name of the last element in std::list is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "back");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdVector(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);
            tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);
            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdQueue(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            String insideType = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
            tmpvar.setRawType(insideType);

            if (i == 0)
                // the virtual name of the first element in std::queue is "front"
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "front");
            else
                tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);

            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleStdMap(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        long size = BasicTypeRandom.generateInt(getFunctionNode().getFunctionConfig().getBoundOfArray().getLowerAsLong()
                , getFunctionNode().getFunctionConfig().getBoundOfArray().getUpperAsLong());
        String nameUsedInExpansion = prefix + var.getName();
        String value = size + SpecialCharacter.EMPTY;
        input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

        List<IVariableNode> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            IVariableNode tmpvar = new TmpVariableNode();
            // Ex: "map<int, int>" -> "pair<int,int>"
            String insideType = type.replaceFirst("\\bmap\\b\\s*<", "pair<");
            tmpvar.setRawType(insideType);
            tmpvar.setName(DELIMITER_BETWEEN_STD_OBJECT_AND_ELEMENT + "element" + i);
            tmpvar.setCoreType(insideType);
            tmpvar.setParent(var.getParent());
            tmpvar.setAbsolutePath(var.getAbsolutePath());
            list.add(tmpvar);
        }

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleLambda(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        int start = type.indexOf(TemplateUtils.OPEN_TEMPLATE_ARG) + 1;
        int end = type.lastIndexOf("(");
        String returnType = type.substring(start, end);

        handleTypes(returnType, input, var, prefix, functionConfig);
    }

    private void handleSmartPointer(List<RandomValue> input, String prefixName, IVariableNode argument, String type,
                                    IFunctionConfig functionConfig,
                                    String regex, String smartPointerObject) {
        if (this.selectedConstructor == null) {
            String templateType = TemplateUtils.getTemplateArguments(type)[0];
            String nameUsedInExpansion = prefixName + argument.getName();
            try {
                SmartPointerDataNode smartPointerDataNode = (SmartPointerDataNode) Class.forName(smartPointerObject).newInstance();

                int randIndex = new Random().nextInt(smartPointerDataNode.getConstructors().length);
                String value = smartPointerDataNode.getConstructors()[randIndex];
                logger.debug("Choose constructor " + value);
                value = value.replaceAll("\\bT\\b", templateType);

                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
                smartPointerDataNode.setRawType("auto");
                smartPointerDataNode.setRealType("auto");

                smartPointerDataNode.chooseConstructor(value);
                ICommonFunctionNode selectedConstructorAST = smartPointerDataNode.getSelectedConstructor();
                List<IVariableNode> arguments = new ArrayList<>();
                for (INode node : selectedConstructorAST.getChildren())
                    if (node instanceof IVariableNode) {
                        node.setAbsolutePath(functionConfig.getFunctionNode().getAbsolutePath() + File.separator + node.getName());
                        arguments.add((IVariableNode) node);
                    }
                input.addAll(constructRandomInput(arguments,
                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            String nameUsedInExpansion = prefixName + argument.getName();
            String value = this.selectedConstructor.getName();
            input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
            List<IVariableNode> arguments = this.selectedConstructor.getArguments();
            this.selectedConstructor = null; // reset
            input.addAll(constructRandomInput(arguments,
                    functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
        }
    }

    private void handleStdPair(List<RandomValue> input, String prefix, IVariableNode var, String type, IFunctionConfig functionConfig) {
        List<IVariableNode> list = new ArrayList<>();

        // Ex: "std::pair<int, float>" -> "int, float"
        IASTNode ast = Utils.convertToIAST(type + " a;");

        if (!(ast instanceof CPPASTDeclarationStatement))
            return;

        IASTNode qualifiedName = ast;
        while (!(qualifiedName instanceof CPPASTQualifiedName) && !(qualifiedName instanceof CPPASTTemplateId)) {
            qualifiedName = qualifiedName.getChildren()[0];
        }

        CPPASTTemplateId pair = null;
        if (qualifiedName instanceof CPPASTQualifiedName) {
            if (qualifiedName.getChildren()[0].getRawSignature().equals("std")) {
                pair = (CPPASTTemplateId) qualifiedName.getChildren()[1];
            } else {
                pair = (CPPASTTemplateId) qualifiedName.getChildren()[0];
            }
        } else if (qualifiedName instanceof CPPASTTemplateId)
            pair = (CPPASTTemplateId) qualifiedName;

        CPPASTTypeId astTypeA = (CPPASTTypeId) pair.getChildren()[1];
        CPPASTTypeId astTypeB = (CPPASTTypeId) pair.getChildren()[2];

        // first type
        String typeA = astTypeA.getRawSignature();
        IVariableNode tmpvarA = new TmpVariableNode();
        tmpvarA.setRawType(typeA);
        tmpvarA.setName(".first");
        tmpvarA.setCoreType(typeA);
        tmpvarA.setParent(var.getParent());
        tmpvarA.setAbsolutePath(var.getAbsolutePath());
        list.add(tmpvarA);
        logger.debug("Type A = " + typeA);
        // second type
        String typeB = astTypeB.getRawSignature();
        IVariableNode tmpvarB = new TmpVariableNode();
        tmpvarB.setRawType(typeB);
        tmpvarB.setName(".second");
        tmpvarB.setCoreType(typeB);
        tmpvarB.setParent(var.getParent());
        tmpvarB.setAbsolutePath(var.getAbsolutePath());
        list.add(tmpvarB);
        logger.debug("Type B = " + typeB);

        input.addAll(constructRandomInput(list,
                functionNode.getFunctionConfig(),
                prefix + var.getName()));
    }

    private void handleQT(List<RandomValue> input, String prefixName, IVariableNode argument, String type, IFunctionConfig functionConfig) {
        logger.debug("type " + type + " : handleQT");
        String nameUsedInExpansion = prefixName + argument.getName();
        if (this.selectedConstructor == null) {
            // choose a constructor (instance) or among constructors
            QTDataNode qtNode = (QTDataNode) QTDataNode.fromJson(TemplateUtils.deleteTemplateParameters(type));
            List<QTDataNode.QTConstructorNode> constructors = qtNode.getConstructorNodes();

            logger.debug("There are " + constructors.size() + " candidate constructors");

            String value = "";

            if (constructors.size() > 0) {
                for (QTDataNode.QTConstructorNode constructorNode : constructors) {
                    if (constructorNode.getParams().size() == 0) {
                        value = constructorNode.getName();
                        break;
                    }
                }

                logger.debug("Choose constructor " + IdMapping.getInstance().getOrCreate(value));

                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));

            } else {
//                List<ICommonFunctionNode> instances = ((ClassNode) derivedNodes.get(randomDerivedNodes)).getInstanceMethods();
//                int randomInstance = new Random().nextInt(instances.size());
//                value = instances.get(randomInstance).getName();
//                logger.debug("Choose constructor " + IdMapping.getInstance().getOrCreate(value));
//
//                input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
//                input.addAll(constructRandomInput(instances.get(randomInstance).getArguments(),
//                        functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
            }
        } else {
            String value = this.selectedConstructor.getName();
            input.add(new RandomValueForAssignment(nameUsedInExpansion, value));
            List<IVariableNode> arguments = this.selectedConstructor.getArguments();
            this.selectedConstructor = null; // reset
            input.addAll(constructRandomInput(arguments,
                    functionConfig, prefixName + argument.getName() + DELIMITER_BETWEEN_CONSTRUCTOR_OF_STRUCTURE_AND_ARGUMENT));
        }

    }

    protected Map<String, String> getFunctionPointerMappingInPrototype(TestPrototype selectedPrototype) {
        if (selectedPrototype == null)
            return new HashMap<>();
        Map<String, String> functionPointerMapping = new HashMap<>();
        SubprogramNode sut = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode());
        for (IDataNode child : sut.getChildren()) {
            if (child.getName().equals("RETURN")) {
                // ignore
            } else {
                if (child instanceof FunctionPointerDataNode) {
                    String nameselectedFunction = ((FunctionPointerDataNode) child).getSelectedFunction().getName();
                    functionPointerMapping.put(child.getName(), nameselectedFunction);
                }
            }
        }
        return functionPointerMapping;
    }

    protected Map<String, String> getVoidPointerMappingInPrototype(TestPrototype selectedPrototype) {
        if (selectedPrototype == null)
            return new HashMap<>();
        Map<String, String> voidPointerMapping = new HashMap<>();
        SubprogramNode sut = Search2.findSubprogramUnderTest(selectedPrototype.getRootDataNode());
        assert sut != null;
        for (IDataNode child : sut.getChildren()) {
            if (child.getName().equals("RETURN")) {
                // ignore
            } else {
                if (child instanceof VoidPointerDataNode && child.getChildren().size() == 1) {
                    ValueDataNode firstChild = (ValueDataNode) child.getChildren().get(0);
                    IVariableNode var = firstChild.getCorrespondingVar();
                    voidPointerMapping.put(child.getName(), var.getRealType());
                }
            }
        }
        return voidPointerMapping;
    }

    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    public static Map<String, String> getRandomValuesUsedInExpansion(List<RandomValue> randomValues) {
        Map<String, String> output = new HashMap<>();
        for (RandomValue randomValue : randomValues) {
            output.put(randomValue.getNameUsedInExpansion(), randomValue.getValue());
        }
        return output;
    }

    public static Map<String, String> getRandomValuesUsedInCode(List<RandomValue> randomValues) {
        Map<String, String> output = new HashMap<>();
        for (RandomValue randomValue : randomValues) {
            output.put(randomValue.getNameUsedToUpdateValue(), randomValue.getValue());
        }
        return output;
    }

    public String getAdditionalHeader() {
        return additionalHeader;
    }

    public void setAdditionalHeader(String additionalHeader) {
        this.additionalHeader = additionalHeader;
    }

    public void setRoot(RootDataNode root) {
        this.root = root;
    }

    public RootDataNode getRoot() {
        return root;
    }

    public void setSelectedPrototype(TestPrototype selectedPrototype) {
        this.selectedPrototype = selectedPrototype;
        realTypeMapping.putAll(getVoidPointerMappingInPrototype(selectedPrototype));
        realTypeMapping.putAll(getFunctionPointerMappingInPrototype(selectedPrototype));
    }

    public TestPrototype getSelectedPrototype() {
        return selectedPrototype;
    }

    public Map<String, String> getRealTypeMapping() {
        return realTypeMapping;
    }

    public void setRealTypeMapping(Map<String, String> realTypeMapping) {
        this.realTypeMapping = realTypeMapping;
    }

    public ConstructorNode getSelectedConstructor() {
        return selectedConstructor;
    }

    public void setSelectedConstructor(ConstructorNode selectedConstructor) {
        this.selectedConstructor = selectedConstructor;
    }

    public Map<String, INode> getSelectedNodesInVoidPointer() {
        return selectedNodesInVoidPointer;
    }

    public void setSelectedNodesInVoidPointer(Map<String, INode> selectedNodesInVoidPointer) {
        this.selectedNodesInVoidPointer = selectedNodesInVoidPointer;
    }

    private long generateIntMultipleBound(MultiplePrimitiveBound bounds, String type) {
        int selectedBoundIdx = new Random().nextInt(bounds.size());
        PrimitiveBound selectedBound = bounds.get(selectedBoundIdx);
        String lower = selectedBound.getLower();
        String upper = selectedBound.getUpper();
        return BasicTypeRandom.generateInt(lower, upper, type);
    }

    private double generateFloatMultipleBound(MultiplePrimitiveBound bounds) {
        int selectedBoundIdx = new Random().nextInt(bounds.size());
        PrimitiveBound selectedBound = bounds.get(selectedBoundIdx);
        String lower = selectedBound.getLower();
        String upper = selectedBound.getUpper();
        return BasicTypeRandom.generateFloat(lower, upper);
    }

    private String getConfigName(IVariableNode variableNode) {
        return variableNode.getName();
    }

    public static final String VOID_POINTER____SELECTED_CORE_TYPE = "selectedCoreType";
    public static final String VOID_POINTER____SELECTED_CATEGORY = "selectedCategory";
    public static final String VOID_POINTER____POINTER_LEVEL = "level";
}
