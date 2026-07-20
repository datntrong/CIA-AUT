package uet.fit.aut.execution.result_trace;

public interface IResultTrace {
    String getExpected();
    String getActual();
    String getMessage();
    String getExpectedName();
    String getActualName();
    String getUserCode();
    String getTag();
}
