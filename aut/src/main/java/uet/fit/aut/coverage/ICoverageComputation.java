package uet.fit.aut.coverage;

import uet.fit.aut.parser.obj.INode;

public interface ICoverageComputation {

	void compute();

	void setCoverage(String coverage);

	void setConsideredSourcecodeNode(INode consideredSourcecodeNode);

	void setTestpathContent(String testpathContent);
}