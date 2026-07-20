package uet.fit.aut.coverage.basicpath;

import uet.fit.aut.autogen.cfg.object.ICfgNode;

import java.util.ArrayList;
import java.util.List;

public class BasicPath extends ArrayList<ICfgNode> {

    private boolean visited = false;

    public BasicPath(List<ICfgNode> list) {
        super(list);
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
}
