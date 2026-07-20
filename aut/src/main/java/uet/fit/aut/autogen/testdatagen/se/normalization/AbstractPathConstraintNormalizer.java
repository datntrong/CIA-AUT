package uet.fit.aut.autogen.testdatagen.se.normalization;

import uet.fit.aut.autogen.testdatagen.se.memory.IVariableNodeTable;
import uet.fit.aut.parser.normalizer.AbstractNormalizer;

/**
 * Normalize path constraint
 *
 * @author DucAnh
 */
public abstract class AbstractPathConstraintNormalizer extends AbstractNormalizer implements uet.fit.aut.autogen.testdatagen.se.normalization.IPathConstraintNormalizer {

    /**
     * Table of variables
     */
    protected IVariableNodeTable tableMapping;

    public IVariableNodeTable getTableMapping() {
        return tableMapping;
    }

    public void setTableMapping(IVariableNodeTable tableMapping) {
        this.tableMapping = tableMapping;
    }

}
