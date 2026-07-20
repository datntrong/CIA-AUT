package uet.fit.aut.search;

import uet.fit.aut.parser.obj.INode;

public abstract class SearchCondition implements ISearchCondition {

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.fit.utils.search.ISearchCondition#isSatisfiable(com.fit.tree.object.
	 * INode)
	 */
	@Override
	public abstract boolean isSatisfiable(INode n);
}
