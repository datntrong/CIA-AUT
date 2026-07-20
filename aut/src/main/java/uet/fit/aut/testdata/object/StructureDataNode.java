package uet.fit.aut.testdata.object;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.util.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Represent structure variable such as class, struct, etc.
 *
 * @author DucAnh
 */
public abstract class StructureDataNode extends ValueDataNode {
	@Override
	public Set<String> getAdditionalSources() {
		HashSet<String> set = new HashSet<>();
		INode coreType = getCorrespondingType();
		if (coreType != null) {
			ISourcecodeFileNode sourcecodeFileNode = Utils.getSourcecodeFile(coreType);
			if (sourcecodeFileNode != null) {
				String sourceFile = sourcecodeFileNode.getAbsolutePath();
				set.add(sourceFile);
			}
		}

		set.addAll(super.getAdditionalSources());
		return set;
	}
}
