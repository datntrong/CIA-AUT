package uet.fit.aut.coverage.function_call;

import java.util.Objects;

public class FunctionCall {

    private Position category;

    private String absolutePath;

    private int index;

    private int iterator;

    public Position getCategory() {
        return category;
    }

    public void setCategory(Position category) {
        this.category = category;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionCall call = (FunctionCall) o;
        return category == call.category &&
                Objects.equals(absolutePath, call.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, absolutePath);
    }

    public int getIterator() {
        return iterator;
    }

    public void setIterator(int iterator) {
        this.iterator = iterator;
    }

    public enum Position {
        FIRST,
        LAST,
        MIDDLE,
        UNKNOWN
    }
}