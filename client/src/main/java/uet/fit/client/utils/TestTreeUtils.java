package uet.fit.client.utils;

import uet.fit.dto.test.CreateTestDTO;
import uet.fit.dto.test.data.EditableTestNode;
import uet.fit.dto.test.data.GlobalTestNode;
import uet.fit.dto.test.data.HaveTypeTestNode;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.LabelTestNode;
import uet.fit.dto.test.data.StubSubprogramTestNode;
import uet.fit.dto.test.data.SubprogramTestNode;
import uet.fit.dto.test.data.SutTestNode;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.dto.test.data.UnitTestNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestTreeUtils {

	public static SutTestNode findSubprogramUnderTest(ITestNode node) {
		ITestNode parent = node;

		while (parent != null) {
			if (parent instanceof SutTestNode)
				return (SutTestNode) parent;

			if (parent instanceof CreateTestDTO)
				break;

			parent = parent.getParent();
		}

		if (parent != null) {
			for (ITestNode child : parent.getChildren()) {
				if (child instanceof UnitTestNode) {
					for (ITestNode sub : child.getChildren()) {
						if (sub instanceof SutTestNode) {
							return (SutTestNode) sub;
						}
					}
				}
			}
		}

		return null;
	}

	public static ITestNode getActualValue(ITestNode node) {
		ITestNode actualValue = null;
		List<String> traceNames = new ArrayList<>();

		ITestNode parent = node;

		boolean isInputArgument = false;
		boolean isGlobal = false;

		while (parent != null) {
			if (parent instanceof UnitTestNode)
				break;

			if (parent instanceof GlobalTestNode) {
				isGlobal = true;
				break;
			}

			if (parent instanceof SutTestNode) {
				isInputArgument = true;
				break;
			}

			if (parent instanceof LabelTestNode && parent.getTitle().equals("<<STATIC>>")) {
				isInputArgument = true;
				parent = parent.getParent();
				break;
			} else
				traceNames.add(0, parent.getTitle());

			parent = parent.getParent();
		}

		if (isInputArgument || isGlobal) {
			boolean found = false;

			String name = traceNames.remove(0);

			Collection<ITestNode> actualParams;

			if (isInputArgument)
				actualParams = ((SutTestNode) parent).getExpectedMap().keySet();
			else
				actualParams = ((GlobalTestNode) parent).getExpectedMap().keySet();

			for (ITestNode child : actualParams) {
				if (child.getTitle().equals(name)) {
					actualValue = child;
					found = true;
					break;
				}
			}

			if (found) {
				while (!traceNames.isEmpty()) {
					found = false;
					name = traceNames.remove(0);

					for (ITestNode child : actualValue.getChildren()) {
						if (child.getTitle().equals(name)) {
							actualValue = child;
							found = true;
							break;
						}
					}

					if (!found) {
						actualValue = null;
						break;
					}
				}
			}
		}

		return actualValue;
	}

	public static boolean isExpected(ITestNode node) {
		ITestNode parent = node.getParent();

		if (parent == null)
			return false;

		if (!(parent instanceof HaveTypeTestNode || parent instanceof SubprogramTestNode || parent instanceof GlobalTestNode))
			return false;

		if (node instanceof SutTestNode || node instanceof StubSubprogramTestNode)
			return false;

		if (parent instanceof SutTestNode) {
			SutTestNode sutTestNode = (SutTestNode) parent;
			return sutTestNode.getExpectNodes().contains(node)
					|| node.getTitle().equals("return");
		} else if (parent instanceof GlobalTestNode) {
			GlobalTestNode globalTestNode = (GlobalTestNode) parent;
			return globalTestNode.getExpectNodes().contains(node);
		}
		// stub
		else if (parent.getParent() != null
				&& parent.getParent().getParent() instanceof StubSubprogramTestNode) {
			return !node.getTitle().equals("return");
		} else if (parent instanceof StubSubprogramTestNode) {
			return true;
		}

		return isExpected(parent);
	}

	public static HaveTypeTestNode getExpectedValue(ITestNode node) {
		HaveTypeTestNode expectedNode = null;
		List<String> traceNames = new ArrayList<>();

		ITestNode parent = node;

		boolean isInputArgument = false;
		boolean isGlobal = false;

		while (parent != null) {
			if (parent instanceof UnitTestNode)
				break;

			if (parent instanceof GlobalTestNode) {
				isGlobal = true;
				break;
			}

			if (parent instanceof SutTestNode) {
				isInputArgument = true;
				break;
			}

			if (parent instanceof LabelTestNode && parent.getTitle().equals("<<STATIC>>")) {
				isInputArgument = true;
				parent = parent.getParent();
				break;
			} else
				traceNames.add(0, parent.getTitle());

			parent = parent.getParent();
		}

		if (isInputArgument || isGlobal) {
			boolean found = false;

			String name = traceNames.remove(0);

			Collection<ITestNode> expectedParams;

			if (isInputArgument)
				expectedParams = ((SutTestNode) parent).getExpectNodes();
			else
				expectedParams = ((GlobalTestNode) parent).getExpectNodes();

			for (ITestNode child : expectedParams) {
				if (child.getTitle().equals(name) && child instanceof HaveTypeTestNode) {
					expectedNode = (HaveTypeTestNode) child;
					found = true;
					break;
				}
			}

			if (found) {
				while (!traceNames.isEmpty()) {
					found = false;
					name = traceNames.remove(0);

					for (ITestNode child : expectedNode.getChildren()) {
						if (child instanceof HaveTypeTestNode && child.getTitle().equals(name)) {
							expectedNode = (HaveTypeTestNode) child;
							found = true;
							break;
						}
					}

					if (!found) {
						expectedNode = null;
						break;
					}
				}
			}
		}

		return expectedNode;
	}

	public static String getTestCaseId(ITestNode node) {
		if (node instanceof TestDataDTO) {
			return ((TestDataDTO) node).getId();
		} else {
			return getTestCaseId(node.getParent());
		}
	}

	public static boolean isStubFunction(ITestNode value) {
		return value instanceof SubprogramTestNode
				&& value.getParent() instanceof EditableTestNode
				&& value.getParent().getParent() instanceof StubSubprogramTestNode
				&& ((StubSubprogramTestNode) value.getParent().getParent()).isStub();
	}

	public static boolean isStubRelated(ITestNode node) {
		if (node == null)
			return false;

		if (node instanceof StubSubprogramTestNode)
			return true;

		return isStubRelated(node.getParent());
	}

	public static boolean isGlobalVariable(ITestNode dataNode) {
		return dataNode.getParent() instanceof GlobalTestNode;
	}
}
