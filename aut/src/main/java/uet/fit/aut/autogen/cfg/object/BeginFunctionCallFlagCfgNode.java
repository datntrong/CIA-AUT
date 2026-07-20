package uet.fit.aut.autogen.cfg.object;

/**
 * Represent the function call flag node of CFG
 *
 * @author lamnt
 */
public class BeginFunctionCallFlagCfgNode extends FunctionCallFlagCfgNode {

	public BeginFunctionCallFlagCfgNode() {
		setContent(BeginFlagCfgNode.BEGIN_FLAG);
	}
}
