package uet.fit.aut.testdata.gen.module.type;

import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.MacroDefinitionNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.MacroDefinitionNodeCondition;
import uet.fit.aut.testdata.gen.module.TreeExpander;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.IRegex;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.io.File;
import java.util.List;

/**
 * Khoi tao bien dau vao la mang mot chieu
 */
public class OneDimensionTypeInitiation extends AbstractTypeInitiation {
    public OneDimensionTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        String coreType = VariableTypeUtils.getSimpleRealType(vParent)
                .replaceAll(IRegex.ARRAY_INDEX, "");

        OneDimensionDataNode child;
        if (VariableTypeUtils.isPointer(coreType))
            child = new OneDimensionPointerDataNode();
        else if (VariableTypeUtils.isCh(coreType))
            child = new OneDimensionCharacterDataNode();
        else if (VariableTypeUtils.isNum(coreType))
            child = new OneDimensionNumberDataNode();
        else if (VariableTypeUtils.isStr(coreType))
            child = new OneDimensionStringDataNode();
        else
            child = new OneDimensionStructureDataNode();

        child.setParent(nParent);
        child.setRawType(vParent.getRawType());
        child.setRealType(vParent.getRealType());
        child.setName(vParent.getNewType());
        child.setCorrespondingVar(vParent);

        try {
            setSizeOf(child);
        } catch (Exception ex) {
            child.setSize(-1);
            child.setFixedSize(false);
        }

        if (vParent instanceof ExternalVariableNode)
            child.setExternel(true);

        nParent.addChild(child);
        return  child;
    }

    /**
     * Set size of the One Dimension Data Node
     *
     * @param node
     * @throws Exception
     */
    private void setSizeOf(OneDimensionDataNode node) throws Exception {
        int size = -1;

        String type = node.getRawType();
        String expr = "a" + type.substring(type.lastIndexOf('['));
        IASTNode astArray = Utils.convertToIAST(expr);

        if (astArray instanceof IASTArraySubscriptExpression) {
            IASTInitializerClause astArraySize = ((IASTArraySubscriptExpression) astArray).getArgument();
            if (astArraySize instanceof IASTLiteralExpression) {
                size = Integer.parseInt(astArraySize.getRawSignature());

            } else if (astArraySize instanceof IASTIdExpression) {
                String name = astArraySize.getRawSignature();
                VariableSearchingSpace searchingSpace = new VariableSearchingSpace(vParent);
                List<Level> space = searchingSpace.getSpaces();
                String path = File.separator + name;
                List<INode> macroNodes = Search.searchInSpace(space, new MacroDefinitionNodeCondition(), path);
                if (!macroNodes.isEmpty()) {
                    MacroDefinitionNode macroNode = (MacroDefinitionNode) macroNodes.get(0);
                    size = Integer.parseInt(macroNode.getOldType());
                }
            }
        }

        node.setSize(size);

        if (size > 0) {
            node.setFixedSize(true);
            new TreeExpander().expandTree(node);
        }
    }

}
