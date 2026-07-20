package uet.fit.dto.env;

import java.util.List;

/**
 * Represent a node in project navigator tree pane
 */
public interface INavigableNode {

	/**
	 * @return node title
	 */
	String getTitle();

	List<? extends INavigableNode> getChildren();
}
