package uet.fit.aut.testdata.comparable;

public interface IEqualityComparable extends IComparable {
    String assertEqual(String expected, String actual);
    String assertNotEqual(String expected, String actual);
}
