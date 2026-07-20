package uet.fit.aut.testcase;

import uet.fit.aut.util.SpecialCharacter;

import java.time.LocalDateTime;

public abstract class TestItem implements ITestItem {
    // name of test case
    private String name;

    // the path of the test case
    private String path;

    private LocalDateTime creationDateTime, lastModifiedTime;

    protected TestItem() {
        creationDateTime = LocalDateTime.now();
        lastModifiedTime = creationDateTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = removeSpecialCharacter(name);
    }

    @Override
    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    @Override
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = removeSysPathInName(path);
    }

    protected static String removeSysPathInName(String path) {
        // name could not have File.separator
        return path.replaceAll("operator\\s*/", "operator_division");
    }

    public static String removeSpecialCharacter(String name) {
        return name
                .replace("+", "plus")
                .replace("-", "minus")
                .replace("*", "mul")
                .replace("/", "div")
                .replace("%", "mod")
                .replace("=", "equal")
                .replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE)
                .replaceAll("[_]+", SpecialCharacter.UNDERSCORE);
    }
}
