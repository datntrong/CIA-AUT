package uet.fit.aut.autogen.testdatagen.coverage;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.testpath.ITestpathInCFG;

/**
 * Update visited statement in CFG
 *
 * @author ducanhnguyen
 */
public interface ICFGUpdater {
	/**
	 * Update visited nodes in CFG
	 */
	void updateVisitedNodes();

	ICFG getCfg();

	ITestpathInCFG getUpdatedCFGNodes();
}