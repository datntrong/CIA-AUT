package uet.fit.aut.parser.obj;

import uet.fit.aut.util.SpecialCharacter;

/**
 * static variable
 * @author TungLam
 */
public class StaticVariableNode extends VariableNode {

    private String instrument;

    private ICommonFunctionNode context;

    public String getInstrument() {
        return instrument;
    }

    public void setContext(ICommonFunctionNode context) {
        this.context = context;

        String prefix = "aut_static_" + context.getNewType()
                .replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE)
                .replaceAll("_+", SpecialCharacter.UNDERSCORE);

        instrument = prefix + getName();
    }

    public ICommonFunctionNode getContext() {
        return context;
    }
}
