package uet.fit.aut.testdata;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import uet.fit.aut.autogen.testdatagen.RandomInputGeneration;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.testcase.IDataTestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.testdata.gen.TreeExpander;
import uet.fit.aut.testdata.gen.type.PointerTypeInitiation;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.testdata.object.VoidPointerDataNode;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VoidPtrTypeChooser {

    public final static String NAME_REFERENCE = "value";

    private final static Logger logger = LoggerFactory.getLogger(VoidPtrTypeChooser.class);

    public List<INode> loadContent(INode functionNode, String inputMethod) {
        List<INode> realTypes = new ArrayList<>();

        if (inputMethod.equals(RandomInputGeneration.OPTION_VOID_POINTER_PRIMITIVE_TYPES)) {
            realTypes.addAll(VariableTypeUtils.getAllPrimitiveTypeNodes());

        } else if (inputMethod.equals(RandomInputGeneration.OPTION_VOID_POINTER_STRUCTURE_TYPES)) {
            realTypes.addAll(VariableTypeUtils.getAllStructureNodes(functionNode));
        }

        return realTypes;
    }

    public void chooseANode(INode typeNode, IDataTestItem testCase, VoidPointerDataNode dataNode, int level) {
        String coreType = getCoreType(typeNode);

        if (typeNode instanceof StructureNode) {
            // TODO: relative
//            String filePath = Utils.getSourcecodeFile(typeNode).getAbsolutePath();
//            String proPath = Environment.getInstance().getProjectConfig().getProPath();

            // TODO: fix
//            String cloneFilePath = ProjectClone.getClonedFilePath(proPath, filePath);
//            if (!new File(cloneFilePath).exists())
//                cloneFilePath = filePath;
////            String includeStm = String.format("#include \"%s\"", cloneFilePath);
//            if (testCase != null) {
////                testCase.appendAdditionHeader(includeStm);
//                testCase.putOrUpdateDataNodeIncludes(dataNode, cloneFilePath);
//            }
        }

        // backup children of data node
        List<IDataNode> oldChildren = new ArrayList<>(dataNode.getChildren());
        try {
            // build Type
            String referType = buildType(coreType, level);

            // Generate variable node
            VariableNode v = parseStmToGetCorrespondingVar(coreType, level, dataNode);
            v.setCorrespondingNode(typeNode);

            // generate child data node
            dataNode.getChildren().clear();

            ValueDataNode child = new PointerTypeInitiation(v, dataNode).execute();

            if (child != null) {
                child.setName(NAME_REFERENCE);
                dataNode.setReferenceType(referType);
                dataNode.setInputMethod(VoidPointerDataNode.InputMethod.AVAILABLE_TYPES);
                dataNode.setUserCode(null);
                new TreeExpander().expandTree(child);
            } else {
                throw new Exception("Not supported type: " + referType);
            }

        } catch (Exception e) {
            e.printStackTrace();
            dataNode.setChildren(oldChildren);
        }
    }

    protected String getCoreType(INode typeNode) {
        String coreType = typeNode.getName();

        if (typeNode instanceof StructNode && !(typeNode instanceof StructTypedefNode))
            coreType = "struct " + coreType;
        else if (typeNode instanceof EnumNode && !(typeNode instanceof EnumTypedefNode))
            coreType = "enum " + coreType;
        else if (typeNode instanceof UnionNode && !(typeNode instanceof UnionTypedefNode))
            coreType = "union " + coreType;

        return coreType;
    }

    protected String buildType(String coreType, int level) {
        return coreType + "*".repeat(Math.max(0, level));
    }

    protected VariableNode parseStmToGetCorrespondingVar(String coreType, int level, VoidPointerDataNode dataNode) {
        VariableNode v = new VariableNode();

        String type = buildType(coreType, level);
        StringBuilder stmBuilder = new StringBuilder(type);

        if (dataNode != null && dataNode.getName() != null)
            stmBuilder.append(" ").append(NAME_REFERENCE);
        else
            stmBuilder.append(" ").append("tmp");

        String stm = stmBuilder.toString();
        IASTNode ast = Utils.convertToIAST(stm);

        if (ast instanceof IASTDeclarationStatement) {
            IASTDeclaration declaration = ((IASTDeclarationStatement) ast).getDeclaration();
            if (declaration instanceof IASTSimpleDeclaration) {
                v.setAST(declaration);
                if (dataNode != null) {
                    VariableNode parentVar = dataNode.getCorrespondingVar();
                    v.setParent(parentVar);
                    v.setAbsolutePath(parentVar.getAbsolutePath() + File.separator + v.getName());
                }
            } else {
                logger.error("The declaration is not an IASTSimpleDeclaration");
            }
        } else {
            logger.error("The ast is not an IASTDeclarationStatement");
        }
        return v;
    }
}
