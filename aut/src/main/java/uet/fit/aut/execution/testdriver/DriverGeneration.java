package uet.fit.aut.execution.testdriver;

import uet.fit.aut.util.SpecialCharacter;

public abstract class DriverGeneration implements IDriverGeneration {

    protected String testDriver = SpecialCharacter.EMPTY;

    protected abstract String getTestDriverTemplate();

    @Override
    public String getTestDriver() {
        return testDriver;
    }

    @Override
    public String toString() {
        return "DriverGeneration: " + testDriver;
    }
}
