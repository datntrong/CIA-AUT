package uet.fit.aut.parser.obj;

import java.io.File;

/**
 * Created by DucToan on 14/07/2017.
 */
public class SpecialUnionTypedefNode extends UnionNode {
    public SpecialUnionTypedefNode() {
        super();
    }

    @Override
    public String getNewType() {
        return getAST().getDeclarators()[0].getName().toString();
    }

    @Override
    public String getAbsolutePath() {
        return getParent().getAbsolutePath() + File.separator + getNewType();
    }
}
