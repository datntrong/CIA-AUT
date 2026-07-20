package uet.fit.aut.boundary;

import uet.fit.aut.config.IFunctionConfigBound;

public class UndefinedBound implements IFunctionConfigBound {
    public static final String UNDEFINED = "N/A";

    public UndefinedBound() {
    }

    public String show() {
        return UNDEFINED;
    }
}
