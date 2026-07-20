package uet.fit.aut.testdata.object;

import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Represent variable as pointer (one level, two level, etc.)
 *
 * @author ducanhnguyen
 */
public class PointerStructureDataNode extends PointerDataNode {
	@Override
	public String getInputForGoogleTest(boolean isDeclared) throws Exception {
		INode coresType = getCorrespondingType();
		if (coresType instanceof ClassNode)
			return getInputForObjectPointer(isDeclared);
		else
			return super.getInputForGoogleTest(isDeclared);
	}

	private String getInputForObjectPointer(boolean isDeclared) throws Exception {
		if (isUseUserCode()) {
			if (isPassingVariable() && !isDeclared)
				return SpecialCharacter.EMPTY;
			else
				return getUserCodeContent();
		}

		String input = "";
		if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
			String type = VariableTypeUtils.deleteStorageClassesExceptConst(getRawType());
			type = VariableTypeUtils.deleteReferenceOperator(type);

			String coreType = "";
			if (getChildren() != null && !getChildren().isEmpty())
				coreType = ((ValueDataNode) getChildren().get(0)).getRawType();
			else {
				int index = type.lastIndexOf('*');
				if (index < 0)
					index = getRealType().lastIndexOf('*');
				coreType = type.substring(0, index).trim();
			}

			if (isExternal())
				type = "";

			String name = getVirtualName();
			int size = getAllocatedSize();

			String newAllocation = String.format("(%s) malloc(sizeof(%s))", type, coreType);
			IDataNode subClassNode = null;
			ConstructorDataNode constructorNode = null;
			String attrInit = "";
			try {
				IDataNode classNode = getChildren().get(0);
				subClassNode = classNode.getChildren().get(0);
				constructorNode = (ConstructorDataNode) subClassNode.getChildren().get(0);
				String argumentInput = constructorNode.getConstructorArgumentsInputForGoogleTest();
				newAllocation = String.format("new %s%s", constructorNode.getRealType(), argumentInput);
				input += constructorNode.generateInputForParameters(false);
				for (IDataNode child : subClassNode.getChildren()) {
					if (!child.equals(constructorNode))
					attrInit += child.getInputForGoogleTest(isDeclared) + SpecialCharacter.LINE_BREAK;
				}
			} catch (Exception ignored) { }

			if (isPassingVariable() || isSTLListBaseElement() || isInConstructor() || isGlobalExpectedValue() || isSutExpectedArgument()) {
				String allocation;

				if (this.isNotNull()) {
					allocation = String.format("%s %s = %s;", type, name, newAllocation);
					allocation += attrInit;
				} else {
					allocation = String.format("%s %s = "
									+ IDataNode.NULL_POINTER_IN_CPP
									+ SpecialCharacter.END_OF_STATEMENT
							, type, name);
				}
				input += allocation;
			} else if (isArrayElement() || isAttribute()) {
				String allocation;

				if (this.isNotNull()) {
					allocation = String.format("%s = %s;", name, newAllocation);
					allocation += attrInit;
				} else
					allocation = String.format("%s = "
									+ IDataNode.NULL_POINTER_IN_CPP
									+ SpecialCharacter.END_OF_STATEMENT
							, name);
				input += allocation;
			} else {
				if (this.isNotNull()) {
					input += String.format("%s = %s;", name, newAllocation);
					input += attrInit;
				} else
					input += name + " = " + IDataNode.NULL_POINTER_IN_CPP + SpecialCharacter.END_OF_STATEMENT;
			}
		}

		return input + SpecialCharacter.LINE_BREAK;
	}

	private String generateNewAllocationStm(String type, String coreType) {
		String newAllocation = String.format("(%s) malloc(sizeof(%s))", type, coreType);
			IDataNode classNode = getChildren().get(0);
			IDataNode subClassNode = classNode.getChildren().get(0);
			ConstructorDataNode constructorNode = (ConstructorDataNode) subClassNode.getChildren().get(0);
			String argumentInput = constructorNode.getConstructorArgumentsInputForGoogleTest();
			newAllocation = String.format("new %s%s", coreType, argumentInput);
		return newAllocation;
	}
}
