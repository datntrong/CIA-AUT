package uet.fit.aut.execution.result_trace;

import uet.fit.aut.coverage.function_call.ConstructorCall;
import uet.fit.aut.coverage.function_call.FunctionCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.EnumNode;
import uet.fit.aut.parser.obj.IFunctionPointerTypeNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.Search2;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testdata.comparable.AssertMethod;
import uet.fit.aut.testdata.object.EnumDataNode;
import uet.fit.aut.testdata.object.FunctionPointerDataNode;
import uet.fit.aut.testdata.object.GlobalRootDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.TestPathUtils;
import uet.fit.aut.util.Utils;

import java.util.List;

public class AssertResultImporter {

	private static final Logger logger = LoggerFactory.getLogger(AssertResultImporter.class);

	/**
	 * List of assertion from execution result trace
	 */
	protected List<IResultTrace> traces;

	protected TestCase testCase;

	public AssertResultImporter(TestCase testCase) {
		this.testCase = testCase;
		this.traces = ResultTrace.load(testCase);
	}

	public AssertionResult generate() {
		/*
		 * Result PASS/ALL
		 */
		AssertionResult results = new AssertionResult();

		if (traces != null && !traces.isEmpty()) {
			String testPath = Utils.readFileContent(testCase.getTestPathFile());
			String[] lines = testPath.split("\\R");
			List<FunctionCall> calledFunctions = TestPathUtils.traceFunctionCall(lines);

			int skip = 0;
			String firstLine = lines[0];
			if (firstLine.startsWith(TestPathUtils.SKIP_TAG))
				skip = Integer.parseInt(firstLine.substring(TestPathUtils.SKIP_TAG.length()));

			List<SubprogramNode> subprograms = Search2
					.searchNodes(testCase.getRootDataNode(), SubprogramNode.class);

			INode root = Utils.getRoot(testCase.getFunctionNode());

			for (int i = 0; i < calledFunctions.size(); i++) {
				FunctionCall current = calledFunctions.get(i);
				FunctionCall.Position position = current.getCategory();

				if (position.equals(FunctionCall.Position.FIRST))
					continue;

				SubprogramNode subprogram = findSubprogram(root, subprograms, current);

				int fcalls = i + skip + 1;

				if (subprogram != null && Utils.getSourcecodeFile(subprogram.getFunctionNode()) != null) {
					for (IResultTrace trace : traces) {
						String fcallTag = "aut function calls: " + fcalls;

						if (trace.getTag().contains(fcallTag)) {
							ValueDataNode correspondingNode = findNode(subprogram, trace);
							if (correspondingNode == null && position == FunctionCall.Position.LAST) {
								GlobalRootDataNode globalRoot = Search2.findGlobalRoot(testCase.getRootDataNode());
								correspondingNode = findNode(globalRoot, trace);
							}

							if (correspondingNode == null) {
								logger.error("Can't find corresponding node of " + trace.getMessage());
								continue;
							}

							boolean match = isMatch(correspondingNode, trace);
							if (match)
								results.increasePass();

							results.increaseTotal();
						}
					}
				}
			}
		}

		return results;
	}

	protected boolean isMatch(ValueDataNode node, IResultTrace trace) {
		String actual = trace.getActual();
		String expected = trace.getExpected();

		if (node instanceof EnumDataNode) {
			INode typeNode = node.getCorrespondingType();
			if (typeNode instanceof EnumNode) {
				EnumNode enumNode = (EnumNode) typeNode;
				for (String name : enumNode.getAllNameEnumItems()) {
					String val = enumNode.getValueOfEnumItem(name);
					if (val.equals(actual)) {
						actual = name;
					}
					if (val.equals(expected)) {
						expected = name;
					}
				}
			}
		}

		return AssertMethod.isMatch(node.getAssertMethod(), actual, expected);
	}

	private ValueDataNode findNode(IDataNode node, IResultTrace resultTrace) {
		if (node instanceof ValueDataNode) {
			String expectedName = node.getVirtualName();

			if (node instanceof FunctionPointerDataNode && node.getName().isEmpty()) {
				IFunctionPointerTypeNode typeNode = ((FunctionPointerDataNode) node).getCorrespondingType();
				String functionName = typeNode.getFunctionName();
				expectedName = node.getVirtualName() + functionName;
			}

			if (node instanceof ListBaseDataNode)
				expectedName += ".size()";

			String expectedOutputRegex = "\\Q" + SourceConstant.EXPECTED_OUTPUT + "\\E";
			expectedName = expectedName.replaceFirst(expectedOutputRegex, SourceConstant.ACTUAL_OUTPUT);

			if ((resultTrace.getExpectedName().equals(expectedName)
					|| resultTrace.getActualName().equals(expectedName))) {
				return (ValueDataNode) node;
			}
		}

		for (IDataNode child : node.getChildren()) {
			ValueDataNode find = findNode(child, resultTrace);
			if (find != null)
				return find;
		}

		if (node instanceof SubprogramNode) {
			for (IDataNode child : ((SubprogramNode) node).getParamExpectedOuputs()) {
				ValueDataNode find = findNode(child, resultTrace);
				if (find != null)
					return find;
			}
		}

		if (node instanceof GlobalRootDataNode) {
			for (IDataNode child : ((GlobalRootDataNode) node).getGlobalInputExpOutputMap().values()) {
				ValueDataNode find = findNode(child, resultTrace);
				if (find != null)
					return find;
			}
		}

		return null;
	}

	private SubprogramNode findSubprogram(INode root, List<SubprogramNode> subprograms, FunctionCall call) {
		for (SubprogramNode subprogram : subprograms) {
			if (subprogram != null) {
				INode functionNode = subprogram.getFunctionNode();
				String functionPath = PathUtils.normalize(functionNode.getAbsolutePath());
				String callPath = PathUtils.normalize(call.getAbsolutePath());
				if (call instanceof ConstructorCall) {
					if (functionPath.equals(callPath)
							&& subprogram.getPathFromRoot().equals(((ConstructorCall) call).getParameterPath()))
						return subprogram;
				} else {
					if (functionPath.equals(callPath))
						return subprogram;
				}
			}
		}

		try {
			INode called = searchFunctionNodeByPath(root, call.getAbsolutePath());
			return new SubprogramNode(called);
		} catch (Exception ex) {
			logger.error("Can't find function corresponding to " + call.getAbsolutePath(), ex);
		}

		return null;
	}

	private INode searchFunctionNodeByPath(INode root, String path) {
		return Search.searchNodes(root, new AbstractFunctionNodeCondition(), path).get(0);
	}
}
