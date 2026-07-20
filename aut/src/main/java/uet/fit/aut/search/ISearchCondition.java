package uet.fit.aut.search;

import uet.fit.aut.parser.obj.INode;

public interface ISearchCondition {

	boolean isSatisfiable(INode n);

}
