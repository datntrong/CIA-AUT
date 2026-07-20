package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.ExtendDependency;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NamespaceNode extends CustomASTNode<ICPPASTNamespaceDefinition> implements ISourceNavigable {

    protected List<List<INode>> extendPaths = new ArrayList<>();

    @Override
    public String getNewType() {
        return getAST().getName().toString();
    }

    @Override
    public IASTFileLocation getNodeLocation() {
        return getAST().getName().getFileLocation();
    }

    @Override
    public File getSourceFile() {
        return new File(getAST().getContainingFilename());
    }

    public List<List<INode>> getExtendPaths() {
        ArrayList<INode> path = new ArrayList<>();
        this.getExtendPaths(this, path);
        return extendPaths;
    }

    public List<INode> getExtendNodes() {
        List<INode> extendedNode = new ArrayList<>();
        for (Dependency d : getDependencies())
            if (d instanceof ExtendDependency && d.getStartArrow().equals(this))
                extendedNode.add(d.getEndArrow());
        return extendedNode;
    }

    private void getExtendPaths(INode n, ArrayList<INode> path) {
        path.add(n);
        List<INode> extendedNodes = new ArrayList<>();
        if (n instanceof NamespaceNode)
            extendedNodes = ((NamespaceNode) n).getExtendNodes();
        else if (n instanceof StructOrClassNode)
            extendedNodes = ((StructOrClassNode) n).getExtendNodes();

        if (extendedNodes.size() > 0)
            for (INode child : extendedNodes)
                this.getExtendPaths(child, path);
        else
            extendPaths.add((List<INode>) path.clone());
        path.remove(path.size() - 1);
    }
}
