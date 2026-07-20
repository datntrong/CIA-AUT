package uet.fit.aut.autogen.testdatagen.se;

import uet.fit.aut.parser.obj.IVariableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the paramaters of a function including the arguments + external
 * variables
 *
 * @author ducanhnguyen
 */
public class Parameters extends ArrayList<IVariableNode> {

    /**
     *
     */
    private static final long serialVersionUID = -2583457982870539611L;

    public Parameters() {
    }

    public <T extends List<IVariableNode>> Parameters(T parameters) {
        this.addAll(parameters);
    }

}
