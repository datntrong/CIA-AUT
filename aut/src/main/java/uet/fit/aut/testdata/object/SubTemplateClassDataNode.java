package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent real class variable
 */
public class SubTemplateClassDataNode extends SubClassDataNode {
    /**
     * Lay tat ca cac constructor cua mot class
     *
     * @return list cac constructor cua class
     */
    public List<ICommonFunctionNode> getConstructorsOnlyInCurrentClass() {
        List<ICommonFunctionNode> constructors = new ArrayList<>();

        INode correspondingNode = getCorrespondingType();

        if (correspondingNode instanceof ClassNode) {
            if (((ClassNode) correspondingNode).isTemplate())
                correspondingNode = correspondingNode.getChildren().get(0);

            ClassNode correspondingClass = (ClassNode) correspondingNode;
            constructors.addAll(correspondingClass.getPublicConstructors());
        }

        return constructors;
    }
}