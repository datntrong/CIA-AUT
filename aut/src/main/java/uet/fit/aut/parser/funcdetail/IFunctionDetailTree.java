package uet.fit.aut.parser.funcdetail;

import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.RootNode;
import uet.fit.aut.util.NodeType;

/**
 * Cay Function Detail Tree co. Ex:
 * |
 * + GLOBAL
 * |--- g_var
 * |--- g_student
 * |
 * + UUT
 * |--- testSimple(Student stu)
 * |
 * + STUB
 * |--- testStudent(Student, int x)
 * |--- testStubFunc(int x)
 * |
 * + DONT STUB
 * |--- anotherTest(Student stu)
 *
 * @author TungLam
 */
public interface IFunctionDetailTree {

    /**
     * Stub mot subprogram
     *
     * @param fn subprogram can stub
     * @throws Exception
     */
    void stub(INode fn) throws Exception;

    /**
     * Huy stub mot subprogram
     *
     * @param fn subprogram can huy stub
     * @throws Exception
     */
    void dontStub(INode fn) throws Exception;

    /**
     * Kiem tra mot subprogram co duoc stub hay khong?
     *
     * @param fn subprogram can kiem tra
     * @return true - fn duoc stub | false - fn khong duoc stub
     * @throws Exception
     */
    boolean isStub(INode fn) throws Exception;

    /**
     * Lay root cua nhanh (sub tree).
     *
     * @param type kieu cua nhanh
     *             Ex: GLOBAL, STUB, UUT, DONT STUB
     * @return root tuong ung
     */
    RootNode getSubTreeRoot(NodeType type);

    /**
     * Lay unit under test
     *
     * @return unit under test
     */
    ICommonFunctionNode getUUT();
}
