package uet.fit.aut.testdata.object;

public class IterationSubprogramNode extends SubprogramNode {
    int index = 0;

    public IterationSubprogramNode() {}

    public IterationSubprogramNode(String name) {
        super.setName(name);
    }

    public int getIndex() {
        //return index;
        return getParent().getChildren().indexOf(this) + 1;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean isStubable() {
        return false;
    }

    @Override
    public String getDisplayName() {
        String name = "call";
        name += getIndex();
        return name;
    }
}
