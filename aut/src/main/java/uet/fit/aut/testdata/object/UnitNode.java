package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.INode;

public abstract class UnitNode extends DataNode {

    private INode sourceNode;

    public UnitNode() {

    }

    public UnitNode(INode source) {
        setSourceNode(source);
    }

    public void setSourceNode(INode sourceNode) {
        if (sourceNode == null)
            return;

        this.sourceNode = sourceNode;

        String fileName = sourceNode.getName();
        setName(fileName);
    }

    public INode getSourceNode() {
        return sourceNode;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }
}
