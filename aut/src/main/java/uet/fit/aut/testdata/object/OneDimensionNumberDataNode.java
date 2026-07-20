package uet.fit.aut.testdata.object;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OneDimensionNumberDataNode extends OneDimensionDataNode {

	@Override
	public String getInputForGoogleTest(boolean isDeclared) throws Exception {
		String declaration = "";

		if (isUseUserCode()) {
			if (isPassingVariable() && !isDeclared)
				return SpecialCharacter.EMPTY;
			else
				return getUserCodeContent();
		}

		if (!isPassingVariable() || (isPassingVariable() && isDeclared)) {
			declaration = SpecialCharacter.LINE_BREAK;

			String type = VariableTypeUtils.deleteStorageClassesExceptConst(this.getRawType());
			type = VariableTypeUtils.deleteReferenceOperator(type);
			String coreType = type.replaceAll("\\[.*\\]", "");
			if (!isExternal()) {
				List<String> indexes = Utils.getIndexOfArray(type);

				if (indexes.size() > 0) {
					String dimension = "";
					for (String index : indexes)
						if (index.length() == 0)
							dimension += Utils.asIndex(this.getSize());
						else
							dimension += Utils.asIndex(index);

					if (this.getParent() instanceof StructureDataNode)
						declaration = "";
					else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
//						String firstIndex = indexes.get(0);
//						if (!firstIndex.equals("-1") && !firstIndex.isEmpty()) {
						declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
								this.getVirtualName(), dimension);
//						}
					} else if (isVoidPointerValue()) {
						declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
								this.getVirtualName(), dimension);
					} else
						declaration = String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
								this.getVirtualName(), dimension);
				} else if (this.getParent() instanceof StructureDataNode) {
					declaration = "";

				} else {
					declaration = String.format("%s %s[%s]" + SpecialCharacter.END_OF_STATEMENT, coreType,
							this.getVirtualName(), this.getSize());
				}
				declaration += SpecialCharacter.END_OF_STATEMENT;
			} else {
				declaration += "\n/* is global variable */\n";
			}
		}

		return declaration + super.getInputForGoogleTest(isDeclared);
	}
}
