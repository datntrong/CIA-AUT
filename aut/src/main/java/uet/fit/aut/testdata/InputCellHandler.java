package uet.fit.aut.testdata;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.autogen.testdatagen.RandomInputGeneration;
import uet.fit.aut.boundary.DataSizeModel;
import uet.fit.aut.boundary.PrimitiveBound;
import uet.fit.aut.env.Environment;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.NumberOfCallNode;
import uet.fit.aut.testcase.IDataTestItem;
import uet.fit.aut.env.Environment;
import uet.fit.aut.parser.obj.CloneVariableNode;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testcase.IDataTestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.testdata.gen.InitialTreeGen;
import uet.fit.aut.testdata.gen.TreeExpander;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;
import uet.fit.aut.testdata.object.stl.STLArrayDataNode;
import uet.fit.aut.testdata.object.stl.SmartPointerDataNode;
import uet.fit.aut.testdata.object.stl.StdFunctionDataNode;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputCellHandler implements IInputCellHandler {

    public final static Logger logger = LoggerFactory.getLogger(InputCellHandler.class);

    // store the template type to real type in template function, e.g, "T"->"int"
    // key: template type
    // value: real type
    private Map<String, String> realTypeMapping;
    private boolean inAutoGenMode = false;
    private IDataTestItem testCase;

    public InputCellHandler() {

    }

    private void clearValue(ValueDataNode dataNode) throws Exception {
        if (dataNode instanceof NormalDataNode) {
            ((NormalDataNode) dataNode).setValue(null);
        } else if (dataNode instanceof EnumDataNode) {
            ((EnumDataNode) dataNode).setValue(null);
        } else if (dataNode instanceof UnionDataNode) {
            ((UnionDataNode) dataNode).setField(null);
            dataNode.getChildren().clear();
        } else if (dataNode instanceof SubClassDataNode) {
            ((SubClassDataNode) dataNode).chooseConstructor((String) null);
        } else if (dataNode instanceof ClassDataNode) {
            ((ClassDataNode) dataNode).setSubClass((String) null);
        } else if (dataNode instanceof OneDimensionDataNode) {
            OneDimensionDataNode currentNode = (OneDimensionDataNode) dataNode;
            if (!currentNode.isFixedSize()) {
                currentNode.setSize(ArrayDataNode.UNDEFINED_SIZE);
                currentNode.setSizeIsSet(false);
                currentNode.getChildren().clear();
            }
        } else if (dataNode instanceof PointerDataNode) {
            PointerDataNode currentNode = (PointerDataNode) dataNode;
            currentNode.setAllocatedSize(PointerDataNode.NULL_VALUE);
            currentNode.setSizeIsSet(false);
            currentNode.getChildren().clear();

        } else if (dataNode instanceof MultipleDimensionDataNode) {
            MultipleDimensionDataNode currentNode = (MultipleDimensionDataNode) dataNode;
            if (!currentNode.isFixedSize()) {
                currentNode.setSizeOfDimension(0, ArrayDataNode.UNDEFINED_SIZE);
                currentNode.setSizeIsSet(false);
                currentNode.getChildren().clear();
            }

        } else if (dataNode instanceof TemplateSubprogramDataNode) {
            dataNode.getChildren().clear();
            ((TemplateSubprogramDataNode) dataNode).setRealFunctionNode((DefinitionFunctionNode) null);

        } else if (dataNode instanceof SmartPointerDataNode) {
            dataNode.getChildren().clear();
            ((SmartPointerDataNode) dataNode).setSelectedConstructor(null);

        } else if (dataNode instanceof ListBaseDataNode && !(dataNode instanceof STLArrayDataNode)) {
            ListBaseDataNode currentNode = (ListBaseDataNode) dataNode;
            currentNode.setSize(-1);
            currentNode.setSizeIsSet(false);
            currentNode.getChildren().clear();

        } else if (dataNode instanceof FunctionPointerDataNode) {
            FunctionPointerDataNode fpDataNode = (FunctionPointerDataNode) dataNode;
            fpDataNode.setSelectedFunction(null);
        } else if (dataNode instanceof OtherUnresolvedDataNode) {

        } else if (dataNode instanceof VoidPointerDataNode) {
            VoidPointerDataNode voidPtrNode = (VoidPointerDataNode) dataNode;
            voidPtrNode.setReferenceType(null);

        } else if (dataNode instanceof StdFunctionDataNode) {
            ((StdFunctionDataNode) dataNode).setBody("");

        } else
            logger.error("Do not support to enter data for " + dataNode.getClass());
    }

    @Override
    public void commitEdit(ValueDataNode dataNode, @Nullable String newValue) throws Exception {
        if (newValue == null) {
            dataNode.setUseUserCode(false);
            clearValue(dataNode);
            return;
        }

        boolean is_realCommit = true;

        if (dataNode instanceof NormalNumberDataNode  || dataNode instanceof NumberOfCallNode) {
            String type = dataNode.getRealType();
            type = VariableTypeUtils.removeRedundantKeyword(type);
            String normalizedValue = Utils.preprocessorLiteral(newValue);

            if (type.equals("unsigned long long int") || type.equals("unsigned long long")
                    || type.equals("uint64_t") || type.equals("uint_least64_t") || type.equals("uint_fast64_t")) {
                try {
                    // test convert to numberous value
                    Double.parseDouble(normalizedValue);
                    ((NormalNumberDataNode) dataNode).setValue(newValue);
                } catch (NumberFormatException e) {
                    if (isInAutoGenMode())
                        logger.error("Invalid value of " + type, e);
//                    else
//                        UIController.showErrorDialog("Invalid value of " + type, "Test data entering", "Invalid value");
                }
            } else if (VariableTypeUtils.isNumFloat(type)) {
                try {
                    double value = Double.parseDouble(normalizedValue);
                    PrimitiveBound bound = Environment.getBoundOfDataTypes().getBounds().get(type);
                    if (bound != null) {
                        if (value >= Double.parseDouble(bound.getLower())
                                && value <= Double.parseDouble(bound.getUpper())) {
                            ((NormalNumberDataNode) dataNode).setValue(newValue);
                        } else {
                            // nothing to do
                            if (isInAutoGenMode())
                                logger.error("Value " + value + " out of scope " + bound);
//                            else
//                                UIController.showErrorDialog("Value " + value + " out of scope " + bound,
//                                    "Test data entering", "Invalid value");
                        }
                    }
                } catch (Exception e) {
                    if (isInAutoGenMode())
                        logger.error("Invalid value of " + type, e);
//                    else
//                        UIController.showErrorDialog("Invalid value of " + type, "Test data entering", "Invalid value");
                    logger.error("Do not handle when committing " + dataNode.getClass());
                }
            } else if (VariableTypeUtils.isTimet(type) || VariableTypeUtils.isSizet(type)) {
                try {
                    // test convert to numberous value
                    Double.parseDouble(normalizedValue);
                    ((NormalNumberDataNode) dataNode).setValue(newValue);
                } catch (NumberFormatException e) {
                    if (isInAutoGenMode())
                        logger.error("Invalid value of " + type, e);
//                    else
//                        UIController.showErrorDialog("Invalid value of " + type, "Test data entering", "Invalid value");
                }
            } else if (VariableTypeUtils.isBoolBasic(type)) {
                if (newValue.equalsIgnoreCase("true") || newValue.equalsIgnoreCase("false"))
                    ((NormalNumberDataNode) dataNode).setValue(newValue.toLowerCase());
                else {
                    try {
                        long value = Long.parseLong(normalizedValue);
                        PrimitiveBound bound = Environment.getBoundOfDataTypes().getBounds().get(type);
                        if (value >= Long.parseLong(bound.getLower())
                                && value <= Long.parseLong(bound.getUpper())) {
                            if (dataNode instanceof NumberOfCallNode) {
                                ((NumberOfCallNode) dataNode).setValue(newValue);
                                TreeExpander expander = new TreeExpander();
                                expander.setRealTypeMapping(this.realTypeMapping);
                                expander.expandTree(dataNode);
                            } else {
                                if (value != 0)
                                    ((NormalNumberDataNode) dataNode).setValue("true");
                                else
                                    ((NormalNumberDataNode) dataNode).setValue("false");
                            }
                        } else {
                            // nothing to do
                            if (isInAutoGenMode())
                                logger.error("Value " + value + " out of scope " + bound);
//                            else
//                                UIController.showErrorDialog("Value " + value + " out of scope " + bound,
//                                    "Test data entering", "Invalid value");
                        }
                    } catch (Exception e) {
                        if (isInAutoGenMode())
                            logger.error("Invalid value of " + type, e);
//                        else
//                            UIController.showErrorDialog("Invalid value of " + type, "Test data entering", "Invalid value");
                    }
                }
            } else {
                try {
                    long value = Long.parseLong(normalizedValue);
                    PrimitiveBound bound = Environment.getBoundOfDataTypes().getBounds().get(type);
                    if (bound != null) {
                        if (value >= Long.parseLong(bound.getLower())
                                && value <= Long.parseLong(bound.getUpper())) {
                            if (dataNode instanceof NumberOfCallNode) {
                                ((NumberOfCallNode) dataNode).setValue(newValue);
                                TreeExpander expander = new TreeExpander();
                                expander.setRealTypeMapping(this.realTypeMapping);
                                expander.expandTree(dataNode);
                            } else {
                                ((NormalNumberDataNode) dataNode).setValue(newValue);
                            }
                        } else {
                            // nothing to do
                            if (isInAutoGenMode())
                                logger.error("Value " + value + " out of scope " + bound);
//                            else
//                                UIController.showErrorDialog("Value " + value + " out of scope " + bound,
//                                    "Test data entering", "Invalid value");
                        }
                    }
                } catch (Exception e) {
                    if (isInAutoGenMode())
                        logger.error("Wrong input for " + dataNode.getName() + "; unit = " + IdMapping.getInstance().getOrCreate(dataNode.getUnit().getName()), e);
//                    else
//                        UIController.showErrorDialog("Wrong input for " + dataNode.getName() + "; unit = " + dataNode.getUnit().getName(), "Test data entering", "Invalid value");
                }
            }
        } else if (dataNode instanceof NormalCharacterDataNode) {
            // CASE: Type character
            if (newValue.startsWith(NormalCharacterDataNode.VISIBLE_CHARACTER_PREFIX)
                    && newValue.endsWith(NormalCharacterDataNode.VISIBLE_CHARACTER_PREFIX)) {
                String character = newValue.substring(1, newValue.length() - 1);
                String ascii = NormalCharacterDataNode.getCharacterToACSIIMapping().get(character);
                if (ascii != null)
                    ((NormalCharacterDataNode) dataNode).setValue(ascii);
                else {
//                    if (!isInAutoGenMode()) {
//                        UIController.showErrorDialog("You type wrong character for the type " + dataNode.getRawType()
//                                        + " in src " + dataNode.getUnit().getName() +
//                                        NormalCharacterDataNode.RULE
//                                , "Wrong input of character", "Fail");
//                    }
                    logger.error("Do not handle when the length of text > 1 for character parameter");
                }
            } else {
                try {
                    // CASE: Type ascii
                    String normalizedValue = Utils.preprocessorLiteral(newValue);
                    long value = Long.parseLong(normalizedValue);
                    DataSizeModel dataSizeModel = Environment.getBoundOfDataTypes().getBounds();
                    String type = dataNode.getRealType();
                    PrimitiveBound bound = dataSizeModel.get(type);
                    if (bound == null)
                        bound = dataSizeModel.get(type.replace("std::", "").trim());
                    if (bound == null) {
                        if (isInAutoGenMode())
                            logger.error("You type wrong character for the type " + dataNode.getRawType()
                                    + " in src " + dataNode.getUnit().getName() +
                                    NormalCharacterDataNode.RULE);
//                        else
//                            UIController.showErrorDialog("You type wrong character for the type " + dataNode.getRawType()
//                                        + " in src " + dataNode.getUnit().getName() +
//                                        NormalCharacterDataNode.RULE
//                                , "Wrong input of character", "Fail");

                    } else if (value <= bound.getUpperAsLong() && value >= bound.getLowerAsLong()) {
                        ((NormalCharacterDataNode) dataNode).setValue(newValue);

                    } else {
                        if (isInAutoGenMode())
                            logger.error("Value " + newValue + " is out of bound " + dataNode.getRawType()
                                    + "[" + bound.getLowerAsLong() + "," + bound.getUpperAsLong() + "]"
                                    + " in src " + dataNode.getUnit().getName() +
                                    NormalCharacterDataNode.RULE);
//                        else
//                            UIController.showErrorDialog("Value " + newValue + " is out of bound " + dataNode.getRawType()
//                                        + "[" + bound.getLowerAsLong() + "," + bound.getUpperAsLong() + "]"
//                                        + " in src " + dataNode.getUnit().getName() +
//                                        NormalCharacterDataNode.RULE
//                                , "Wrong input of character", "Fail");
                    }
                } catch (Exception e) {
//                    if (!isInAutoGenMode()) {
//                        UIController.showErrorDialog("You type wrong character for the type " + dataNode.getRawType()
//                                + " in src " + dataNode.getUnit().getName() +
//                                NormalCharacterDataNode.RULE, "Wrong input of character", "Fail");
//                    }
                    logger.error("Do not handle when the length of text > 1 for character parameter");
                }
            }
        } else if (dataNode instanceof NormalStringDataNode) {
            try {
                long lengthOfString = Long.parseLong(newValue);

                if (lengthOfString < 0)
                    throw new Exception("Negative length of string");

                ((NormalStringDataNode) dataNode).setAllocatedSize(lengthOfString);
                TreeExpander expander = new TreeExpander();
                expander.setRealTypeMapping(this.realTypeMapping);
                expander.expandTree(dataNode);
            } catch (Exception e) {
                e.printStackTrace();
//                if (!isInAutoGenMode())
//                    UIController.showErrorDialog("Length of a string must be >=0 and is an integer", "Wrong length of string", "Invalid length");
            }

        } else if (dataNode instanceof EnumDataNode) {
            // enum oke
            ((EnumDataNode) dataNode).setValue(newValue);
            ((EnumDataNode) dataNode).setValueIsSet(true);

        } else if (dataNode instanceof UnionDataNode) {
            // union oke
            // expand tree với thuộc tính được chọn ở combobox
            ((UnionDataNode) dataNode).setField(newValue);
            new TreeExpander().expandStructureNodeOnDataTree(dataNode, newValue);

        } else if (dataNode instanceof StructDataNode) {
            // do nothing

        } else if (dataNode instanceof SubClassDataNode) {
            // subclass (cũng là class) oke
            ((SubClassDataNode) dataNode).chooseConstructor(newValue);
            new TreeExpander().expandTree(dataNode);

        } else if (dataNode instanceof ClassDataNode) {
            // class oke
            ((ClassDataNode) dataNode).setSubClass(newValue);

        } else if (dataNode instanceof OneDimensionDataNode) {
            //array cua normal data. oke
            int size = Integer.parseInt(newValue);
            OneDimensionDataNode currentNode = (OneDimensionDataNode) dataNode;
            if (!currentNode.isFixedSize()) {
                currentNode.setSize(size);
                currentNode.setSizeIsSet(true);

                TreeExpander expander = new TreeExpander();
                expander.setRealTypeMapping(this.realTypeMapping);
                expander.expandTree(dataNode);
            }

        } else if (dataNode instanceof PointerDataNode) {
            // con trỏ coi như array
            int size = Integer.parseInt(newValue);
            PointerDataNode currentNode = (PointerDataNode) dataNode;
            currentNode.setAllocatedSize(size);
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.setRealTypeMapping(this.realTypeMapping);
            expander.expandTree(dataNode);

        } else if (dataNode instanceof MultipleDimensionDataNode) {
            int sizeA = Integer.parseInt(newValue);
            MultipleDimensionDataNode currentNode = (MultipleDimensionDataNode) dataNode;
            if (!currentNode.isFixedSize()) {
                currentNode.setSizeOfDimension(0, sizeA);
                currentNode.setSizeIsSet(true);

                TreeExpander expander = new TreeExpander();
                expander.expandTree(dataNode);
            }

        } else if (dataNode instanceof TemplateSubprogramDataNode) {
            dataNode.getChildren().clear();
            ((TemplateSubprogramDataNode) dataNode).setRealFunctionNode(newValue);
//            ((TemplateDataNode) dataNode).generateArgumentsAndReturnVariable();

        } else if (dataNode instanceof SmartPointerDataNode) {
            dataNode.getChildren().clear();
            ((SmartPointerDataNode) dataNode).chooseConstructor(newValue);
            new TreeExpander().expandTree(dataNode);

        } else if (dataNode instanceof ListBaseDataNode && !(dataNode instanceof STLArrayDataNode)) {
            //array cua normal data. oke
            int size = Integer.parseInt(newValue);
            ListBaseDataNode currentNode = (ListBaseDataNode) dataNode;
            currentNode.setSize(size);
            currentNode.setSizeIsSet(true);

            TreeExpander expander = new TreeExpander();
            expander.expandTree(dataNode);

        } else if (dataNode instanceof FunctionPointerDataNode) {
            FunctionPointerDataNode fpDataNode = (FunctionPointerDataNode) dataNode;
            if (newValue.equals("NULL")) {
                fpDataNode.setSelectedFunction(null);
            } else {
                fpDataNode.getPossibleFunctions()
                        .stream()
                        .filter(f -> f.getName().equals(newValue))
                        .findFirst()
                        .ifPresent(f -> {
                            if (f instanceof ICommonFunctionNode) {
                                commitSelectedReference(fpDataNode, (ICommonFunctionNode) f);
                            }
                        });
            }
        } else if (dataNode instanceof QTDataNode) {
            QTDataNode qtDataNode = ((QTDataNode) dataNode);
            final String temp = "int " + newValue;
            IASTNode astTemp = Utils.convertToIAST(temp);
            try {

                IASTSimpleDeclaration declaration = null;

                if (astTemp instanceof IASTSimpleDeclaration)
                    declaration = (IASTSimpleDeclaration) astTemp;
                else if (astTemp instanceof IASTDeclarationStatement) {
                    declaration = (IASTSimpleDeclaration) ((IASTDeclarationStatement) astTemp).getDeclaration();
                }
                
                ICPPASTFunctionDeclarator declarator = (ICPPASTFunctionDeclarator) declaration.getDeclarators()[0];

                QTDataNode.QTConstructorNode constructorNode = qtDataNode.getConstructorNodes().stream()
                        .filter(c -> c.getName().equals(newValue))
                        .findFirst()
                        .orElse(null);
                qtDataNode.setSelectedConstructor(constructorNode);

                for (int i = 0; i < declarator.getParameters().length; i++) {
                    VariableNode variableNode = new VariableNode();
                    IASTParameterDeclaration pd = declarator.getParameters()[i];
                    variableNode.setAST(pd);
                    variableNode.setParent(testCase.getFunctionNode());
                    if (variableNode.getName().isEmpty())
                        variableNode.setName("Param" + i);
                    new InitialTreeGen().genInitialTree(variableNode, constructorNode);
                }
            } catch (Exception e) {
                logger.error("Error", e);
            }
            logger.debug("OtherUnresolvedDataNode");

        } else if (dataNode instanceof OtherUnresolvedDataNode) {
            logger.debug("OtherUnresolvedDataNode");
//            ((OtherUnresolvedDataNode) dataNode).setUserCode(newValue);

        } else if (dataNode instanceof VoidPointerDataNode) {
            VoidPointerDataNode voidPtrNode = (VoidPointerDataNode) dataNode;
            commitVoidPtrInputMethod(voidPtrNode, newValue);
            if (voidPtrNode.getReferenceType() == null) {
                is_realCommit = false;
            }

        } else if (dataNode instanceof StdFunctionDataNode) {
            if (newValue.isEmpty()) {
                ((StdFunctionDataNode) dataNode).setBody(newValue);
            } else {
                ((StdFunctionDataNode) dataNode).setBody(String.format("return %s;", newValue));
            }

        } else if (dataNode instanceof StdFunctionDataNode) {
            if (newValue.isEmpty()) {
                ((StdFunctionDataNode) dataNode).setBody(newValue);
            } else {
                ((StdFunctionDataNode) dataNode).setBody(String.format("return %s;", newValue));
            }

        } else
            logger.error("Do not support to enter data for " + dataNode.getClass());

        if (is_realCommit) {
            dataNode.setUseUserCode(false);
        }
    }

    private void commitVoidPtrInputMethod(VoidPointerDataNode dataNode, String inputMethod) {
        if (isInAutoGenMode()) {
            VoidPtrTypeChooser controller = new VoidPtrTypeChooser();

            final String DELIMITER_BETWEEN_ATTRIBUTE = ",";
            final String DELIMITER_BETWEEN_KEY_AND_VALUE = "=";
            String[] elements = inputMethod.split(DELIMITER_BETWEEN_ATTRIBUTE);
            Map<String, String> elementMap = new HashMap<>();
            for (String element : elements) {
                elementMap.put(
                        element.split(DELIMITER_BETWEEN_KEY_AND_VALUE)[0],
                        element.split(DELIMITER_BETWEEN_KEY_AND_VALUE)[1]
                );
            }

            String category = elementMap.get(RandomInputGeneration.VOID_POINTER____SELECTED_CATEGORY);

            if (category.equals(RandomInputGeneration.OPTION_VOID_POINTER_PRIMITIVE_TYPES)
                    || category.equals(RandomInputGeneration.OPTION_VOID_POINTER_STRUCTURE_TYPES)) {
                String coreType = elementMap.get(RandomInputGeneration.VOID_POINTER____SELECTED_CORE_TYPE);
                List<INode> possibleNodes = controller.loadContent(testCase.getFunctionNode(), category);

                int level = Integer.parseInt(elementMap.get(RandomInputGeneration.VOID_POINTER____POINTER_LEVEL));
                for (INode possibleNode : possibleNodes) {
                    if (possibleNode.getName().equals(coreType)) {
                        controller.chooseANode(possibleNode, testCase, dataNode, level);
                        break;
                    }
                }
            }
        }
//        else {
//            if (inputMethod.equals(RandomInputGeneration.OPTION_VOID_POINTER_PRIMITIVE_TYPES)
//                    || inputMethod.equals(RandomInputGeneration.OPTION_VOID_POINTER_STRUCTURE_TYPES)) {
//                ChooseRealTypeController controller = ChooseRealTypeController.getInstance(dataNode);
//                if (controller != null && controller.getStage() != null) {
//                    controller.setTestCase(testCase);
//                    controller.loadContent(inputMethod);
//                    Stage window = controller.getStage();
//                    window.setResizable(false);
//                    window.initModality(Modality.WINDOW_MODAL);
////                    window.initOwner(UIController.getPrimaryStage());
//                    window.showAndWait();
//                }
//            }
//        }
    }

    public void commitSelectedReference(FunctionPointerDataNode fpDataNode, ICommonFunctionNode f) {
        fpDataNode.setSelectedFunction(f);
        if (testCase != null) {
            //TODO: relative
//            String filePath = Utils.getSourcecodeFile(f).getAbsolutePath();
//            String proPath = Environment.getInstance().getProjectConfig().getProPath();
            //TODO: fix
//            String cloneFilePath = ProjectClone.getClonedFilePath(proPath, filePath);
//            if (!new File(cloneFilePath).exists())
//                cloneFilePath = filePath;
//            testCase.putOrUpdateDataNodeIncludes(fpDataNode, cloneFilePath);
        }
    }

    public void setTestCase(IDataTestItem testCase) {
        this.testCase = testCase;
    }

    public IDataTestItem getTestCase() {
        return testCase;
    }

    public void setRealTypeMapping(Map<String, String> realTypeMapping) {
        this.realTypeMapping = realTypeMapping;
    }

    private String toSize(String size) {
        if (size.equals("0") || size.equals("-1"))
            return "<<Size: NULL>>";
        else
            return "<<Size: " + size + ">>";
    }

    public void setInAutoGenMode(boolean inAutoGenMode) {
        this.inAutoGenMode = inAutoGenMode;
    }

    public boolean isInAutoGenMode() {
        return inAutoGenMode;
    }
}
