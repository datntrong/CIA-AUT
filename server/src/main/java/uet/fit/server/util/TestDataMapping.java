package uet.fit.server.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.NumberOfCallNode;
import uet.fit.aut.parser.obj.UnionNode;
import uet.fit.aut.testdata.object.ClassDataNode;
import uet.fit.aut.testdata.object.ConstructorDataNode;
import uet.fit.aut.testdata.object.EnumDataNode;
import uet.fit.aut.testdata.object.FunctionPointerDataNode;
import uet.fit.aut.testdata.object.GlobalRootDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.IterationSubprogramNode;
import uet.fit.aut.testdata.object.MultipleDimensionDataNode;
import uet.fit.aut.testdata.object.NormalDataNode;
import uet.fit.aut.testdata.object.NullPointerDataNode;
import uet.fit.aut.testdata.object.OneDimensionDataNode;
import uet.fit.aut.testdata.object.PointerDataNode;
import uet.fit.aut.testdata.object.QTDataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.SubClassDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.TemplateSubprogramDataNode;
import uet.fit.aut.testdata.object.UnionDataNode;
import uet.fit.aut.testdata.object.UnitNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.testdata.object.VoidPointerDataNode;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;
import uet.fit.aut.testdata.object.stl.STLArrayDataNode;
import uet.fit.aut.testdata.object.stl.SmartPointerDataNode;
import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.dto.test.data.EditableTestNode;
import uet.fit.dto.test.data.GlobalTestNode;
import uet.fit.dto.test.data.HaveTypeTestNode;
import uet.fit.dto.test.data.HaveValueTestNode;
import uet.fit.dto.test.data.IHaveExpectNode;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.LabelTestNode;
import uet.fit.dto.test.data.StubSubprogramTestNode;
import uet.fit.dto.test.data.SubprogramTestNode;
import uet.fit.dto.test.data.SutTestNode;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.dto.test.data.UnitTestNode;
import uet.fit.server.exception.InvalidTestDataException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uet.fit.aut.autogen.testdatagen.RandomInputGeneration.OPTION_VOID_POINTER_PRIMITIVE_TYPES;
import static uet.fit.aut.autogen.testdatagen.RandomInputGeneration.OPTION_VOID_POINTER_STRUCTURE_TYPES;

public class TestDataMapping {

	@NotNull
	public static TestDataDTO toDTO(@NotNull RootDataNode root) throws Exception {
		TestDataDTO dto = new TestDataDTO();

		if (root.getLevel() != NodeType.ROOT)
			throw new InvalidTestDataException(root.getDisplayName());

		for (IDataNode child : root.getChildren()) {
			ITestNode childTestNode = toTestNode(dto, child);
			if (childTestNode != null) {
				dto.getChildren().add(childTestNode);
			}
		}

		return dto;
	}

	@Nullable
	private static ITestNode toTestNode(@NotNull ITestNode parent, @NotNull IDataNode node) throws Exception {
		ITestNode testNode = null;

		if (node instanceof GlobalRootDataNode) {
			testNode = new GlobalTestNode();
		} else if (node instanceof RootDataNode) {
			testNode = new LabelTestNode();
		} else if (node instanceof UnitNode) {
			testNode = new UnitTestNode();
		} else if (node instanceof ValueDataNode) {
			testNode = toHaveTypeNode((ValueDataNode) node);
		}

		if (testNode != null) {
			String title = node.getDisplayName();
			testNode.setTitle(title);
			testNode.setParent(parent);

			if (node instanceof GlobalRootDataNode) {
				Map<ValueDataNode, ValueDataNode> expectedMap = ((GlobalRootDataNode) node).getGlobalInputExpOutputMap();
				handleExpectRoot(testNode, node, expectedMap);
			} else if (testNode instanceof SutTestNode) {
				Map<ValueDataNode, ValueDataNode> expectedMap = ((SubprogramNode) node).getInputToExpectedOutputMap();
				handleExpectRoot(testNode, node, expectedMap);
			} else {
				for (IDataNode child : node.getChildren()) {
					ITestNode childTestNode = toTestNode(testNode, child);
					if (childTestNode != null) {
						testNode.getChildren().add(childTestNode);
					}
				}
			}
		}

		return testNode;
	}

	private static void handleExpectRoot(ITestNode testNode, IDataNode dataNode,
			Map<ValueDataNode, ValueDataNode> expectedMap) throws Exception {
		for (IDataNode child : dataNode.getChildren()) {
			if (child instanceof ValueDataNode) {
				// map children
				ITestNode inputNode = toTestNode(testNode, child);
				if (inputNode != null) {
					testNode.getChildren().add(inputNode);

					// map expected nodes
					if (expectedMap.containsKey(child)) {
						ITestNode expectNode = toTestNode(testNode, expectedMap.get(child));
						if (expectNode != null) {
							((IHaveExpectNode) testNode).getExpectNodes().add(expectNode);
							((IHaveExpectNode) testNode).getExpectedMap().put(inputNode, expectNode);
						}
					}
				}
			}
		}
	}

	private static @NotNull ITestNode toHaveTypeNode(@NotNull ValueDataNode valueNode) throws Exception {
		ITestNode testNode;

		if (valueNode instanceof NumberOfCallNode) {
			NumberOfCallNode normalNode = (NumberOfCallNode) valueNode;
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue((normalNode.getValue()));

		} else if (valueNode instanceof NormalDataNode) {
			// normal
			NormalDataNode normalNode = (NormalDataNode) valueNode;
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue((normalNode.getValue()));

		} else if (valueNode instanceof EnumDataNode) {
			// enum
			EnumDataNode enumNode = (EnumDataNode) valueNode;

			String value;
			if (enumNode.isSetValue()) {
				value = ((EnumDataNode) valueNode).getValue();
			} else {
				value = "<<Select value>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			String[] choices = enumNode.getAllNameEnumItems()
					.toArray(new String[0]);
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof UnionDataNode) {
			// union
			UnionDataNode unionNode = (UnionDataNode) valueNode;

			String value;
			String field = unionNode.getSelectedField();
			if (field != null) {
				value = field;
			} else {
				value = "<<Select attribute>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			INode node = unionNode.getCorrespondingType();
			if (node instanceof UnionNode) {
				String[] choices = node.getChildren().stream().map(INode::getName)
						.toArray(String[]::new);
				((EditableTestNode) testNode).setChoices(choices);
			}

		} else if (valueNode instanceof SubClassDataNode) {
			// subclass
			SubClassDataNode subClassNode = (SubClassDataNode) valueNode;
			String value;
			if (subClassNode.getSelectedConstructor() != null) {
				// Hiển thị tên constuctor class
				value = subClassNode.getSelectedConstructor().getName();
			} else {
				value = "<<Select constructor>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			String[] choices = subClassNode
					.getConstructorsOnlyInCurrentClass()
					.stream()
					.map(INode::getName)
					.toArray(String[]::new);
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof ClassDataNode) {
			// class
			ClassDataNode classDataNode = (ClassDataNode) valueNode;
			String value;
			if (classDataNode.getSubClass() != null) {
				// Hiển thị tên class
				value = classDataNode.getSubClass().getRawType();
			} else {
				value = "<<Select real class>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			String[] choices = classDataNode
					.getDerivedClass()
					.stream()
					.map(INode::getName)
					.toArray(String[]::new);
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof QTDataNode) {
			// class
			QTDataNode qtDataNode = (QTDataNode) valueNode;
			String value;
			if (qtDataNode.getSelectedConstructor() != null) {
				// Hiển thị tên constructor
				value = qtDataNode.getSelectedConstructor().getName();
			} else {
				value = "<<Select constructor>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			String[] choices = qtDataNode
					.getConstructorNodes()
					.stream()
					.map(QTDataNode.QTConstructorNode::getName)
					.toArray(String[]::new);
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof OneDimensionDataNode) {
			// array
			OneDimensionDataNode arrayNode = (OneDimensionDataNode) valueNode;
			if (arrayNode.isFixedSize()) {
				String value = toSize(String.valueOf(arrayNode.getSize()));
				testNode = new HaveValueTestNode();
				((HaveValueTestNode) testNode).setValue(value);
			} else {
				String value;
				if (arrayNode.isSetSize()) {
					value = toSize(String.valueOf(arrayNode.getSize()));
				} else {
					value = "<<Define size>>";
				}
				testNode = new EditableTestNode();
				((EditableTestNode) testNode).setValue(value);
			}

		} else if (valueNode instanceof PointerDataNode) {
			// con trỏ coi như array
			PointerDataNode arrayNode = (PointerDataNode) valueNode;

			String value;
			if (arrayNode.isSetSize()) {
				value = toSize(String.valueOf(arrayNode.getAllocatedSize()));
			} else {
				value = "<<Define size>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

		} else if (valueNode instanceof MultipleDimensionDataNode) {
			// mảng 2 chiều của int, char
			MultipleDimensionDataNode arrayNode = (MultipleDimensionDataNode) valueNode;

			String value;
			if (arrayNode.isSetSize()) {
				if (arrayNode.getSizeOfDimension(0) > 0) {
					StringBuilder sizesInString = new StringBuilder();
					int lastIdx = arrayNode.getSizes().length - 1;
					for (int i = 0; i < lastIdx; i++)
						sizesInString.append(arrayNode.getSizes()[i]).append(" x ");
					sizesInString.append(arrayNode.getSizes()[lastIdx]);
					value = toSize(sizesInString.toString());
				} else {
					value = toSize("0");
				}
			} else {
				value = "<<Define size>>";
			}

			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

		} else if (valueNode instanceof ListBaseDataNode) {
			ListBaseDataNode vectorNode = (ListBaseDataNode) valueNode;

			String value;
			if (valueNode instanceof STLArrayDataNode) {
				value = toSize(String.valueOf(vectorNode.getSize()));
				testNode = new HaveValueTestNode();
				((HaveValueTestNode) testNode).setValue(value);
			} else {
				if (vectorNode.isSetSize()) {
					value = toSize(String.valueOf(vectorNode.getSize()));
				} else {
					value = "<<Define size>>";
				}
				testNode = new EditableTestNode();
				((EditableTestNode) testNode).setValue(value);
			}

		} else if (valueNode instanceof SmartPointerDataNode) {

			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue("<<Choose constructor>>");

			SmartPointerDataNode smartPtrDataNode = (SmartPointerDataNode) valueNode;
			String[] constructors = smartPtrDataNode.getConstructorsWithTemplateArgument();
			((EditableTestNode) testNode).setChoices(constructors);

		} else if (valueNode instanceof FunctionPointerDataNode) {
			FunctionPointerDataNode cast = (FunctionPointerDataNode) valueNode;

			INode selected = cast.getSelectedFunction();
			String value;
			if (selected != null) {
				value = selected.getName();
			} else {
				value = "<<Choose reference>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			List<String> matches = cast.getPossibleFunctions()
					.stream()
					.map(INode::getName)
					.collect(Collectors.toList());
			matches.add("NULL");

			String[] choices = matches.toArray(new String[0]);
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof VoidPointerDataNode) {
			String realType = ((VoidPointerDataNode) valueNode).getReferenceType();
			String value;
			if (realType != null) {
				value = realType;
			} else {
				value = "<<Choose real type>>";
			}
			testNode = new EditableTestNode();
			((EditableTestNode) testNode).setValue(value);

			String[] choices = new String[] {
					OPTION_VOID_POINTER_PRIMITIVE_TYPES,
					OPTION_VOID_POINTER_STRUCTURE_TYPES
			};
			((EditableTestNode) testNode).setChoices(choices);

		} else if (valueNode instanceof NullPointerDataNode) {
			testNode = new HaveValueTestNode();
			((HaveValueTestNode) testNode).setValue(NullPointerDataNode.NULL_PTR);

		} else if (valueNode instanceof SubprogramNode) {
			SubprogramNode subprogramNode = (SubprogramNode) valueNode;

			// template function
			if (subprogramNode instanceof TemplateSubprogramDataNode) {
				// TODO: code later
				testNode = new SubprogramTestNode();
			}
			// constructor
			else if (subprogramNode instanceof ConstructorDataNode) {
				testNode = new SubprogramTestNode();
			}
			else if (subprogramNode instanceof IterationSubprogramNode) {
				testNode = new SubprogramTestNode();
			}
			// stubable subprogram
			else if (subprogramNode.isStubable()) {
				testNode = new StubSubprogramTestNode();
				((StubSubprogramTestNode) testNode).setStub(subprogramNode.isStub());
			}
			// sut
			else {
				testNode = new SutTestNode();
			}

			((SubprogramTestNode) testNode).setReturnType(subprogramNode.getRealType());
		} else {
			testNode = new HaveTypeTestNode();
		}

		if (testNode instanceof HaveTypeTestNode) {
			HaveTypeTestNode haveTypeNode = (HaveTypeTestNode) testNode;

			String type = valueNode.getRawType();
			type = TemplateUtils.deleteTemplateParameters(type);
			haveTypeNode.setType(type);
			haveTypeNode.setCategory(valueNode.getClass().getSimpleName());

			// first "if" to display "USER CODE" for parameters that use user code
			if (valueNode.isUseUserCode()) {
				haveTypeNode.setUserCode(valueNode.getUserCode().getContent());
			}

			haveTypeNode.setAssertMethod(valueNode.getAssertMethod());

			String[] supportAsserts = valueNode.getAllSupportedAssertMethod();
			if (supportAsserts.length > 0)
				haveTypeNode.setSupportAsserts(supportAsserts);
		}

		return testNode;
	}

	private static String toSize(String size) {
		if (size.equals("0") || size.equals("-1"))
			return "<<Size: NULL>>";
		else
			return "<<Size: " + size + ">>";
	}
}
