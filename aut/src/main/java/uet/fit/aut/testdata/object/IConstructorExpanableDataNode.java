package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.ICommonFunctionNode;

public interface IConstructorExpanableDataNode {
    ICommonFunctionNode getSelectedConstructor();

    void chooseConstructor(String constructor) throws Exception;

}
