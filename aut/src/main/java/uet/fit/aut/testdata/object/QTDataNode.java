package uet.fit.aut.testdata.object;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;
import uet.fit.config.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QTDataNode extends ValueDataNode {

	public static final boolean ENABLE = false;

	@Expose
	private List<QTConstructorNode> constructorNodes = new ArrayList<>();

	@Expose
	private QTConstructorNode selectedConstructor;

	public QTDataNode() {

	}

	public static ValueDataNode fromJson(String name) {

//		String typeDir = FolderConfig.load().getEnvironment() + File.separator + "TestApp16" + File.separator + "qtConstructors" + File.separator + name;
		String typeDir = Config.getHomePath() + File.separator + "qtConstructors" + File.separator + name;
		String json = Utils.readFileContent(typeDir);

		ValueDataNode dataNode;
		dataNode = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
				.fromJson(json, QTDataNode.class);

		if (dataNode == null)
			dataNode = new OtherUnresolvedDataNode();

		return dataNode;
	}

	public void setConstructorNodes(List<QTConstructorNode> constructorNodes) {
		this.constructorNodes = constructorNodes;
	}

	public void setSelectedConstructor(QTConstructorNode selectedConstructor) {
		children.clear();
		children.add(selectedConstructor);
		selectedConstructor.setParent(this);
		this.selectedConstructor = selectedConstructor;
	}

	public List<QTConstructorNode> getConstructorNodes() {
		return constructorNodes;
	}

	public QTConstructorNode getSelectedConstructor() {
		return selectedConstructor;
	}

	public static class QTConstructorNode extends ValueDataNode {

		@Expose
		private String name;

		@Expose
		private List<ParamNode> params = new ArrayList<>();

		public String getName() {
			return name;
		}

		public QTConstructorNode() {
		}

		public QTConstructorNode(String name, List<ParamNode> params) {
			this.name = name;
			this.params = params;
		}

		public void setParams(List<ParamNode> params) {
			this.params = params;
		}

		private String getConstructorArgumentsInputForGoogleTest() {
			StringBuilder input = new StringBuilder();
			input.append("(");

			if (getChildren().size() > 0) {
				for (IDataNode parameter : getChildren()) {
					input.append(parameter.getVirtualName()).append(",");
				}
			}

			input.append(")");

			input = new StringBuilder(input.toString().replace(",)", ")"));

			return input.toString();
		}

		@Override
		public String getInputForGoogleTest(boolean isDeclared) throws Exception {
			String input = "";
			if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
				input = super.getInputForGoogleTest(isDeclared);

				String argumentInput = getConstructorArgumentsInputForGoogleTest();
				ValueDataNode qtDataNode = (ValueDataNode) getParent();
				ValueDataNode pointerNode = null;
				IDataNode grandParent = qtDataNode.getParent();
				if (grandParent instanceof PointerDataNode)
					pointerNode = (PointerDataNode) grandParent;

				String realType = qtDataNode.getRawType();
				realType = VariableTypeUtils.deleteStorageClassesExceptConst(realType);
				realType = VariableTypeUtils.deleteReferenceOperator(realType);
				realType = VariableTypeUtils.deleteStorageClassesExceptConst(realType);
				realType = VariableTypeUtils.deleteStructKeyword(realType);
				realType = VariableTypeUtils.deleteUnionKeyword(realType);

				String name = qtDataNode.getVirtualName();

				// TODO: lấy ra cha và ông
				// cha QTDataNode -> lấy ra type của qt
				// ông là pointerDataNode
				// new
				// <type> <name> = (new) <type><argumentInput>

				// can not use new
				if (pointerNode != null) {
					input += realType + name + " = new " + realType
							+ argumentInput + SpecialCharacter.END_OF_STATEMENT;
				} else if (qtDataNode.isArrayElement() || qtDataNode.isAttribute()) {
					input += name + " = " + realType
								+ argumentInput + SpecialCharacter.END_OF_STATEMENT;
				} else {
					input += realType + " " + name + " = " + realType
							+ argumentInput + SpecialCharacter.END_OF_STATEMENT;
				}
			}

			return input;
		}

		public List<ParamNode> getParams() {
			return params;
		}
	}

	public static class ParamNode {

		@Expose
		private String name;

		@Expose
		private String type;

		@Expose
		private boolean isTemplate;

		public ParamNode(String name, String type, boolean isTemplate) {
			this.name = name;
			this.type = type;
			this.isTemplate = isTemplate;
		}
	}
}
