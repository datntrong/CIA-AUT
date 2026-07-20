package uet.fit.aut.testdata;

import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.ValueDataNode;

public interface IInputCellHandler {
    void commitEdit(ValueDataNode dataNode, String newValue) throws Exception;
}
