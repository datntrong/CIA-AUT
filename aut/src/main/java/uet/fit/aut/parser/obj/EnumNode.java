package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTEnumerator;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent enum declaration
 * <p>
 * <p>
 * 
 * <pre>
 * |    enum Color {	RED, 	GREEN, 	BLUE};: CPPASTSimpleDeclaration
 * |       enum Color {	RED, 	GREEN, 	BLUE}: CPPASTEnumerationSpecifier
 * |          Color: CPPASTName
 * |          RED: CPPASTEnumerator
 * |             RED: CPPASTName
 * |          GREEN: CPPASTEnumerator
 * |             GREEN: CPPASTName
 * |          BLUE: CPPASTEnumerator
 * |             BLUE: CPPASTName
 * </pre>
 *
 * @author ducanhnguyen
 */
public class EnumNode extends StructureNode {

	public static final int NAME_ENUM_ITEM = 0;
	public static final int VALUE_ENUM_ITEM = 1;

	@Override
	public String getNewType() {
		String name = ((IASTEnumerationSpecifier) getAST().getDeclSpecifier()).getName().toString();
		return VariableTypeUtils.deleteEnumkeyword(name);
	}

	/**
	 * Get all name defined in the declaration of enum. For example, enum "Color {
	 * RED=10, GREEN=40, BLUE}" -----> "RED", "GREEN", "BLUE"
	 *
	 * @return
	 */
	public List<String> getAllNameEnumItems() {
		List<String> values = new ArrayList<>();
		IASTNode CPPASTEnumerationSpecifier = getAST().getChildren()[0];
		final int START_INDEX_ENUM_VALUE = 1;

		for (int i = START_INDEX_ENUM_VALUE; i < CPPASTEnumerationSpecifier.getChildren().length; i++) {
			IASTNode enumItem = CPPASTEnumerationSpecifier.getChildren()[i];

			if (enumItem instanceof CPPASTEnumerator)

				if (enumItem.getChildren().length == 2) {
					IASTNode nameEnumItem = enumItem.getChildren()[0];

					values.add(nameEnumItem.getRawSignature());

				} else if (enumItem.getChildren().length == 1)
					values.add(enumItem.getRawSignature());
		}

		return values;
	}

	/**
	 * Get all pairs of (name, value) that are defined in the declaration of enum.
	 * For example, enum "Color { RED=10, GREEN=40, BLUE}" -----> "(RED,10)",
	 * "(GREEN,40)", "BLUE"
	 *
	 * @return
	 */
	public List<String[]> getAllEnumItems() {
		List<String[]> values = new ArrayList<>();
		IASTNode CPPASTEnumerationSpecifier = getAST().getChildren()[0];

		final int START_INDEX_ENUM_VALUE = 1;

		for (int i = START_INDEX_ENUM_VALUE; i < CPPASTEnumerationSpecifier.getChildren().length; i++) {
			IASTNode enumItem = CPPASTEnumerationSpecifier.getChildren()[i];

			if (enumItem instanceof CPPASTEnumerator) {
				if (enumItem.getChildren().length == 2) {
					IASTNode nameEnumItem = enumItem.getChildren()[0];
					IASTNode valueEnumItem = enumItem.getChildren()[1];

					values.add(new String[]{nameEnumItem.getRawSignature(), valueEnumItem.getRawSignature()});

				} else if (enumItem.getChildren().length == 1) {
					String strValue = "0";
					if (!values.isEmpty()) {
						String[] lastItem = values.get(values.size() - 1);
						String normalizedVal = Utils.preprocessorLiteral(lastItem[VALUE_ENUM_ITEM]);
						int lastItemVal = Integer.parseInt(normalizedVal);
						strValue = String.valueOf(lastItemVal + 1);
					}
					values.add(new String[]{enumItem.getRawSignature(), strValue});
				}
			}
		}

		return values;
	}

	/**
	 * Get value of enum item. Ex: enum "Color { RED=10, GREEN=40, BLUE}", item
	 * "RED" has value "10"
	 *
	 * @param nameEnumItem
	 * @return
	 */
	public String getValueOfEnumItem(String nameEnumItem) {
		List<String[]> enumItems = getAllEnumItems();

		for (String[] enumItem : enumItems)

			if (enumItem[EnumNode.NAME_ENUM_ITEM].equals(nameEnumItem))
				return enumItem[EnumNode.VALUE_ENUM_ITEM];

		return nameEnumItem;
	}

	@Override
	public IASTFileLocation getNodeLocation() {
		return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier()).getName().getFileLocation();
	}

	@Override
	public File getSourceFile() {
		return new File(getAST().getContainingFilename());
	}
}
