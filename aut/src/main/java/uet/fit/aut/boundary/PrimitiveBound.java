package uet.fit.aut.boundary;

import com.google.gson.annotations.Expose;
import uet.fit.aut.config.IFunctionConfigBound;

/**
 * Bound of primitive variable
 *
 * @author ducanh
 */
public class PrimitiveBound implements IFunctionConfigBound {

    @Expose
    private String lower;

    @Expose
    private String upper;

    public PrimitiveBound(){
    }

    public PrimitiveBound(long lower, long upper) {
        this.lower = lower + "";
        this.upper = upper + "";
    }

    public PrimitiveBound(String lower, String upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public String getLower() {
        return lower;
    }

    public long getLowerAsLong() {
        return Long.parseLong(getLower());
    }

    public void setLower(String lower) {
        this.lower = lower;
    }

    public String getUpper() {
        return upper;
    }

    public void setUpper(String upper) {
        this.upper = upper;
    }

    public long getUpperAsLong() {
        return Long.parseLong(getUpper());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "[" + lower + ".." + upper + "]";
    }
}
