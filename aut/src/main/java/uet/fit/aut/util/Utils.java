package uet.fit.aut.util;

import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.IntegralValue;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblemDeclaration;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.autogen.testdatagen.se.ExpressionRewriterUtils;
import uet.fit.aut.autogen.testdatagen.se.memory.IVariableNodeTable;
import uet.fit.aut.exception.SuffixInputInvalidException;
import uet.fit.aut.logger.Locations;
import uet.fit.aut.parser.obj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.search.Search;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uet.fit.aut.util.SourceConstant.INSTANCE_VARIABLE;
import static uet.fit.aut.util.SourceConstant.getInstanceName;

public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

	public static final int UNDEFINED_TO_INT = -9999;
	public static final float UNDEFINED_TO_DOUBLE = -9999;
	public static boolean containFunction = false;

	private static final List<String> SUFFIX_STRINGS = Arrays.asList("", "u", "l", "ul", "lu", "ll", "ull", "f");

	public static void chmod777(File file) throws IOException {
		Set<PosixFilePermission> perms = Files.readAttributes(file.toPath(), PosixFileAttributes.class).permissions();

		perms.add(PosixFilePermission.OWNER_WRITE);
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_EXECUTE);
		perms.add(PosixFilePermission.GROUP_WRITE);
		perms.add(PosixFilePermission.GROUP_READ);
		perms.add(PosixFilePermission.GROUP_EXECUTE);
		perms.add(PosixFilePermission.OTHERS_WRITE);
		perms.add(PosixFilePermission.OTHERS_READ);
		perms.add(PosixFilePermission.OTHERS_EXECUTE);

		Files.setPosixFilePermissions(file.toPath(), perms);
	}

	/**
	 * Convert a string into regex
	 *
	 * @param str
	 * @return
	 */
	public static String toRegex(String str) {
		str = str.replace("[", "\\[")
				.replace("]", "\\]")
				.replace("(", "\\(")
				.replace(")", "\\)")
				.replace(".", "\\.")
				.replace("+", "\\+")
				.replace("*", "\\*")
				.replace(" ", "\\s*")
				.replace("_", "\\_");

		if (str.isEmpty())
			return str;

		/*
		 * Add bound of word at the beginning
		 */
		if (str.toCharArray()[0] >= 'A' && str.toCharArray()[0] <= 'Z'
				|| str.toCharArray()[0] >= 'a' && str.toCharArray()[0] <= 'z')
			str = "\\b" + str;

		/*
		 * Add bound of word at the end
		 */
		int last = str.toCharArray().length - 1;
		if (str.toCharArray()[last] >= 'A' && str.toCharArray()[last] <= 'Z'
				|| str.toCharArray()[last] >= 'a' && str.toCharArray()[last] <= 'z')
			str += "\\b";
		return str;
	}


	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}

	public static boolean isUnix() {
		return System.getProperty("os.name").toLowerCase().contains("nix")
				|| System.getProperty("os.name").toLowerCase().contains("nux")
				|| System.getProperty("os.name").toLowerCase().contains("aix")
				|| System.getProperty("os.name").toLowerCase().contains("centos");
	}

	public static String toUpperFirstCharacter(String str) {
		StringBuilder output;
		char[] c = str.toCharArray();

		output = new StringBuilder((c[0] + "").toUpperCase());
		for (int i = 1; i < c.length; i++)
			output.append(c[i]);

		return output.toString();
	}

	public static String putInString(String str) {
		return "\"" + str + "\"";
	}

	public static String asIndex(int i) {
		return "[" + i + "]";
	}

	public static String asIndex(String str) {
		return "[" + str + "]";
	}

	public static boolean isSpecialChInVisibleRange(int ASCII) {
		return ASCII == 34 /* nhay kep */ || ASCII == 92 /* gach cheo */
				|| ASCII == 39 /* nhay don */;
	}

	public static boolean isVisibleCh(int ASCII) {
		return ASCII >= 32 && ASCII <= 126;
	}

	public static double toDouble(String str) {
		/*
		  Remove bracket from negative number. Ex: Convert (-2) into -2
		 */
		str = str.replaceAll("\\((" + IRegex.NUMBER_REGEX + ")\\)", "$1");

		/*

		 */
		boolean isNegative = false;
		if (str.startsWith("-")) {
			str = str.substring(1);
			isNegative = true;
		} else if (str.startsWith("+"))
			str = str.substring(1);
		/*

		 */
		double n;
		try {
			n = Double.parseDouble(str);
			if (isNegative)
				n = -n;
		} catch (Exception e) {
			n = Utils.UNDEFINED_TO_DOUBLE;
		}
		return n;
	}

	public static String[] parseIndexesInput(DataNode node, String input) throws Exception {
		final int MAX_INDEX = 50;

		int dimensions;

		if (node instanceof MultipleDimensionDataNode)
			dimensions = ((MultipleDimensionDataNode) node).getDimensions();
		else
			dimensions = 1;

		String[] expandIndexes = new String[dimensions];

		Pattern pattern = Pattern.compile("\\[.*?\\]");
		Matcher matcher = pattern.matcher(input);

		int dim = 0;

		while (matcher.find()) {
			List<String> indexes = new ArrayList<>();
			String[] items = matcher.group().substring(1, matcher.group().length() - 1).split(",");

			for (int j = 0; j < items.length; j++) {
				if (items[j].contains("..")) {
					int step = 1;

					if (items[j].contains("/")) {
						step = Integer.parseInt(items[j].substring(items[j].indexOf("/") + 1));
						items[j] = items[j].substring(0, items[j].indexOf("/"));
					}

					String[] bounds = items[j].split("\\Q..\\E");
					int start = bounds[0].isEmpty() ? 0 : Integer.parseInt(bounds[0]);
					int end = -1;

					if (bounds.length == 1) {
						if (node instanceof MultipleDimensionDataNode)
							end = ((MultipleDimensionDataNode) node).getSizes()[dim];
						else if (node instanceof OneDimensionDataNode)
							end = ((OneDimensionDataNode) node).getSize();
						else if (node instanceof PointerDataNode)
							end = ((PointerDataNode) node).getAllocatedSize();
						else if (node instanceof ListBaseDataNode)
							end = ((ListBaseDataNode) node).getSize();
						else if (node instanceof ValueDataNode)
							throw new Exception("Don't support to expand " + ((ValueDataNode) node).getRawType());
					} else if (bounds.length == 2) {
						end = Integer.parseInt(bounds[1]);
					} else
						throw new Exception("Invalid input");

					if (start < 0 /*|| end > MAX_INDEX*/ || end < 0)
						throw new Exception("Invalid input");

					if (end - start > MAX_INDEX)
						throw new Exception("Expand up to 50 items");

					for (int i = start; i <= end; i += step)
						if (!indexes.contains(String.valueOf(i)))
							indexes.add(String.valueOf(i));

				} else if (!indexes.contains(items[j])) {
//					if (Integer.parseInt(items[j]) <= MAX_INDEX)
					indexes.add(items[j]);
//					else
//						throw new Exception("Invalid input");
				}
			}

			expandIndexes[dim] = String.join(",", indexes);
			dim++;
		}

		return expandIndexes;
	}

	public static int countCharIn(String str, char ch) {
		return (int) str.chars().filter(c -> c == ch).count();
	}

	public static void writeContentToFile(String content, File file) {
		writeContentToFile(content, file.getAbsolutePath());
	}

	public static void writeContentToFile(String content, String filePath) {
		try {
			new File(filePath).getParentFile().mkdirs();
			PrintWriter out = new PrintWriter(filePath);
			out.println(content);
			out.close();
		} catch (FileNotFoundException e) {
			logger.error("Can't write content to " + IdMapping.getInstance().getOrCreate(filePath), e);
			e.printStackTrace();
		}
	}

	public static boolean isSolaris() {
		return System.getProperty("os.name").toLowerCase().contains("sunos");
	}

	public static INode getRoot(INode n) {
		if (n == null)
			return null;
		else if (n.getParent() == null)
			return n;
		else
			return Utils.getRoot(n.getParent());

	}

	public static String readFileContent(File file) {
		return Utils.readFileContent(file.getAbsolutePath());
	}

	/**
	 * Doc noi dung file
	 *
	 * @param filePath duong dan tuyet doi file
	 * @return noi dung file
	 */
	public static String readFileContent(String filePath) {
		StringBuilder fileData = new StringBuilder(3000);
		try {
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return fileData.toString();
		}
	}

	public static File searchFile(File file, String name) {
		if (file.isDirectory()) {
			File[] arr = file.listFiles();
			if (arr != null) {
				List<File> children = Arrays.stream(arr)
						.sorted()
						.collect(Collectors.toList());
				for (File f : children) {
					File found = searchFile(f, name);
					if (found != null)
						return found;
				}
			}
		} else {
			if (file.getName().equals(name)) {
				return file;
			}
		}
		return null;
	}

	public static String insertSpaceToFunctionContent(int expectedStartLine, int expectedNodeOffset, String oldFunction) {
		String addition = "";
		for (int i = 0; i < expectedStartLine - 2; i++) {
			addition += "\n";
		}
		int additionalOffset = expectedNodeOffset - addition.length() - 1;
		for (int i = 0; i < additionalOffset; i++) {
			addition += " ";
		}
		if (expectedStartLine > 1)
			addition += "\n";
		return addition + oldFunction;
	}

	public static String generateVariableDeclaration(String type, String name) {
		String parameterDeclaration;

		List<String> indexes = Utils.getIndexOfArray(type);

		if (indexes.size() > 0) {
			int idx = type.length() - 1;
			while (type.charAt(idx) == SpecialCharacter.CLOSE_SQUARE_BRACE
					|| type.charAt(idx) == SpecialCharacter.OPEN_SQUARE_BRACE
					|| Character.isDigit(type.charAt(idx)))
				idx--;
			parameterDeclaration = type.substring(0, idx + 1) + " " + name;
			for (String index : indexes)
				parameterDeclaration += "[" + index + "]";

		} else {
			parameterDeclaration = type + " " + name;
		}

		return parameterDeclaration;
	}

	public static String getFullFunctionCall(ICommonFunctionNode functionNode) {
		INode realParent = functionNode.getParent();

		if (functionNode instanceof IFunctionNode) {
			INode tmpRealParent = ((IFunctionNode) functionNode).getRealParent();
			if (tmpRealParent != null)
				realParent = tmpRealParent;
		}

		StringBuilder functionCall = new StringBuilder();

//		if (functionNode instanceof LambdaFunctionNode) {
//			FunctionInstrumentForLambda instrument = new FunctionInstrumentForLambda((LambdaFunctionNode) functionNode);
//			functionCall.append(instrument.generateInstrumentedFunction());
//			functionCall.append(generateCallOfArguments(functionNode));
//
//			INode correspondingType = functionNode.getParent();
//			String type = Search.getScopeQualifier(correspondingType);
//
//			if (correspondingType instanceof ClassNode && ((ClassNode) correspondingType).isTemplate()) {
//				String[] templateParams = TemplateUtils.getTemplateParameters(correspondingType);
//				if (templateParams != null) {
//					type += TemplateUtils.OPEN_TEMPLATE_ARG;
//
//					for (String param : templateParams)
//						type += param + ", ";
//
//					type += TemplateUtils.CLOSE_TEMPLATE_ARG;
//					type = type.replace(", >", ">");
//				}
//			}
//
//			String instanceVarName = type.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
//			instanceVarName = INSTANCE_VARIABLE + SpecialCharacter.UNDERSCORE + instanceVarName;
//
//			return functionCall.toString().replaceAll("this\\b", instanceVarName);
//		}

		if (realParent instanceof ISourcecodeFileNode) {
			functionCall.append(functionNode.getSimpleName())
					.append(generateCallOfArguments(functionNode));

		} else if (realParent instanceof NamespaceNode) {
			// find a list of namespace
			INode namespaceRoot = realParent;
			List<String> namespaces = new ArrayList<>();
			while (namespaceRoot.getParent() != null && namespaceRoot.getParent() instanceof NamespaceNode) {
				namespaces.add(namespaceRoot.getName());
				namespaceRoot = namespaceRoot.getParent();
			}
			namespaces.add(namespaceRoot.getName());

			// generate function call
			StringBuilder scope = new StringBuilder();
			for (String namespace : namespaces)
				scope.insert(0, namespace + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);

			functionCall.append(scope)
					.append(functionNode.getSimpleName())
					.append(generateCallOfArguments(functionNode));

		} else if (realParent instanceof StructureNode) {
			if (functionNode instanceof ConstructorNode) {
				functionCall = new StringBuilder("new ");
				String type = Search.getScopeQualifier(realParent);
				functionCall.append(type);
			} else if (functionNode.isStatic()) {
				functionCall.append(Search.getScopeQualifier(realParent))
						.append(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS)
						.append(functionNode.getSingleSimpleName());
			} else {
				String instanceVarName = getInstanceName(realParent);

				functionCall = new StringBuilder(instanceVarName);
				functionCall.append(SpecialCharacter.POINT_TO).append(functionNode.getSingleSimpleName());
			}

			functionCall.append(generateCallOfArguments(functionNode));
		}

		return functionCall.toString();
	}


	public static String getFullFunctionCallForStub(ICommonFunctionNode functionNode){
		String functionCall = getFullFunctionCall(functionNode);
		int index = functionCall.indexOf("(");
		functionCall = functionCall.substring(0,index) + "_stub" + functionCall.substring(index, functionCall.length()-1);
		functionCall =  functionCall.replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, SpecialCharacter.UNDERSCORE);
		functionCall = functionCall.replace(SpecialCharacter.POINT_TO,SpecialCharacter.UNDERSCORE);
		return functionCall;
	}

	public static String getFullFunctionCallNameForStub(ICommonFunctionNode functionNode){
		String name = getFullFunctionCallForStub(functionNode);
		int check = 0;
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) == '(') {
				check = i;
			}
		}
		name = name.substring(0, check);
		return name;
	}

	/**
	 * Ex: "test(a, b)"
	 *
	 * @param functionNode
	 * @return
	 */
	public static StringBuilder generateCallOfArguments(ICommonFunctionNode functionNode) {
		StringBuilder functionCall = new StringBuilder();
		functionCall.append("(");
		for (IVariableNode v : functionNode.getArguments())
			if (VariableTypeUtilsForStd.isUniquePtr(v.getRawType()))
				functionCall.append(String.format("std::move(%s),", v.getName()));

			else if (VariableTypeUtils.isNullPtr(v.getRawType())) {
				functionCall.append(NullPointerDataNode.NULL_PTR).append(",");

			} else if (v.getCorrespondingNode() instanceof FunctionPointerTypeNode && v.getName().isEmpty())
				functionCall.append(((FunctionPointerTypeNode) v.getCorrespondingNode()).getFunctionName()).append(",");
			else
				functionCall.append(v.getName()).append(",");
		functionCall.append(")");
		functionCall = new StringBuilder(functionCall.toString().replace(",)", ")") + SpecialCharacter.END_OF_STATEMENT);
		return functionCall;
	}

	public static File searchFolder(File file, String name) {
		if (file.isDirectory()) {
			if (file.getName().equals(name)) {
				return file;
			}

			File[] arr = file.listFiles();
			if (arr != null) {
				List<File> children = Arrays.stream(arr)
						.sorted()
						.collect(Collectors.toList());
				for (File f : children) {
					File found = searchFolder(f, name);
					if (found != null)
						return found;
				}
			}
		}

		return null;
	}

	public static void copyFileUsingChannel(String sourcePath, String destPath) throws IOException {
		File source = new File(sourcePath);
		File dest = new File(destPath);
		dest.getParentFile().mkdirs();

		FileChannel sourceChannel = new FileInputStream(source).getChannel();
		FileChannel destChannel = new FileOutputStream(dest).getChannel();
		destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		sourceChannel.close();
		destChannel.close();
	}

	public static void deleteFileOrFolder(File path) {
		if (path != null && path.exists())
			try {
				FileDeleteStrategy.FORCE.delete(path);
				// FileUtils.deleteDirectory(new File(path));
				if (!path.exists()) {
				}
			} catch (IOException e) {
				try {
					Thread.sleep(30);
					Utils.deleteFileOrFolder(path);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
	}

	/**
	 * Get the source code file containing a specified node
	 *
	 */
	public static ISourcecodeFileNode getSourcecodeFile(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ISourcecodeFileNode)
			return (ISourcecodeFileNode) n;
		else
			return Utils.getSourcecodeFile(n.getParent());
	}

	/**
	 * Láº¥y danh sÃ¡ch chá»‰ sá»‘ máº£ng
	 *
	 * @param origin VD: a[3][2]
	 * @return VD: 3,2
	 * @problem ChÆ°a xá»­ lÃ½ chá»‰ sá»‘ máº£ng chá»©a
	 * chá»‰ sá»‘ máº£ng khÃ¡c. VD: a[1+b[2]]
	 */
	public static List<String> getIndexOfArray(String origin) {
		List<String> output = new ArrayList<>();

		// "a[]" --- > "a[<something>]"
		final int DEFAULT_INDEX = 23424131;
		String constraint = origin.replaceAll("\\[\\s*\\]", "[" + DEFAULT_INDEX + "]");

		// add prefix to analyze cases such as "int[3]"
		final String PREFIX = "AUT_PREFIX";
		constraint = PREFIX + constraint;

		IASTNode ast = Utils.convertToIAST(constraint + "==" + 0);
		if (ast instanceof ICPPASTBinaryExpression) {
			IASTExpression left = ((ICPPASTBinaryExpression) ast).getOperand1();
			if (left instanceof ICPPASTArraySubscriptExpression) {
				int maxCount = 0;
				while (left instanceof ICPPASTArraySubscriptExpression) {
					if (++maxCount >= 10)
						break; // avoid infinite loop

					ICPPASTArraySubscriptExpression castedLeft = (ICPPASTArraySubscriptExpression) left;
					ICPPASTInitializerClause index = castedLeft.getArgument();

					if (index.getRawSignature().equals(DEFAULT_INDEX + ""))
						output.add(0, "");
					else
						output.add(0, index.getRawSignature());

					left = castedLeft.getArrayExpression();
				}
			}
		} else if (ast instanceof IASTProblemHolder && origin.contains(SpecialCharacter.POINTER)) {
			output = getIndexOfArray(origin.replaceAll(IRegex.POINTER, SpecialCharacter.EMPTY));
		}
		return output;
	}

	public static INode getClassvsStructvsNamesapceNodeParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ClassNode || n instanceof StructNode)
			return n;
		else if (n instanceof NamespaceNode)
			return n;
		else {
			if (n instanceof AbstractFunctionNode)
				if (((AbstractFunctionNode) n).getRealParent() != null)
					return Utils.getClassvsStructvsNamesapceNodeParent(((AbstractFunctionNode) n).getRealParent());
			return Utils.getClassvsStructvsNamesapceNodeParent(n.getParent());
		}
	}

	public static String preprocessorLiteral(String str) throws NumberFormatException {
		IASTNode ast = Utils.convertToIAST(str);

		if (ast instanceof CPPASTLiteralExpression) {
			CPPASTLiteralExpression expr = ((CPPASTLiteralExpression) ast);
			IValue value = expr.getEvaluation().getValue();
			if (value.equals(IntegralValue.ERROR))
				throw new SuffixInputInvalidException(expr.getRawSignature());
			else
				return value.toString();
//			if (str.contains("0x") || str.contains("0b")) {
//				return expr.getEvaluation().getValue().toString();
//			} else {
//				// check available suffix
//				String suffix = new String(expr.getSuffix());
//				suffix = suffix.toLowerCase();
//				if (!SUFFIX_STRINGS.contains(suffix)) {
//					throw new SuffixInputInvalidException(suffix);
//				}
//
//				int suffixLength = expr.getSuffix().length;
//				return str.substring(0, str.length() - suffixLength);
//			}
		}

		return str;
	}


	/**
	 * Get ast corresponding to statement, e.g., x=y+2
	 *
	 * @param content
	 * @return
	 */
	public static IASTNode convertToIAST(String content) {
		IASTNode ast;

		/*
		 * Get type of the statement
		 */
		boolean isCondition = Utils.isCondition(content);
		/*
		 * The statement is assignment
		 */
		if (!isCondition) {
			content += content.endsWith(SpecialCharacter.END_OF_STATEMENT) ? "" : SpecialCharacter.END_OF_STATEMENT;

			ICPPASTFunctionDefinition fn = getFunctionsinAST(("void test(){" + content + "}").toCharArray())
					.get(0);
			ast = fn.getBody().getChildren()[0];
		} else
			/*
			 * The statement is condition
			 */ {
			ICPPASTFunctionDefinition fn = getFunctionsinAST(
					("void test(){if (" + content + "){}}").toCharArray()).get(0);
			ast = fn.getBody().getChildren()[0].getChildren()[0];
		}
		return ASTUtils.shortenAstNode(ast);
	}


	public static boolean isCondition(String content) {
		/*
		 * Get type of the statement
		 */
		boolean isCondition = false;

		// special case: content= "a"
		if (content.matches(IRegex.NAME_REGEX))
			isCondition = true;

		else
			/*
			 * Ex: char c = static_cast<char>(x)
			 *
			 * Ex: char c = static_cast<char>(x);
			 *
			 * Ex: cout << "A";
			 */
			if (content.endsWith(SpecialCharacter.END_OF_STATEMENT) || content.contains(" = ")
					|| Utils.containRegex(content, "\\b=\\b") || content.startsWith("cout ") || content.startsWith("cout<<")
					|| content.startsWith("std::"))
				isCondition = false;
			else {
				final String[] CONDITION_SIGNALS = new String[]{"!=", "<=", ">=", "==", ">", "<", "!"};
				String encodedContent = content.replace("->", ".");
				for (String conditionSignal : CONDITION_SIGNALS)
					if (encodedContent.contains(conditionSignal))
						isCondition = true;
			}
		return isCondition;
	}

	/**
	 * Check whether a string contain regex or not
	 *
	 * @param src
	 * @param regex
	 * @return
	 */
	public static boolean containRegex(String src, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(src);

		return m.find();
	}

	/**
	 * Láº¥y danh sÃ¡ch táº¥t cáº£ má»�i hÃ m á»Ÿ Ä‘á»‹nh
	 * dáº¡ng AST
	 *
	 * @param sourcecode
	 * @return
	 */
	public static List<ICPPASTFunctionDefinition> getFunctionsinAST(char[] sourcecode) {
		List<ICPPASTFunctionDefinition> output = new ArrayList<>();

		try {
			IASTTranslationUnit unit = Utils.getIASTTranslationUnitforCpp(sourcecode);

			if (unit.getChildren()[0] instanceof CPPASTProblemDeclaration)
				unit = Utils.getIASTTranslationUnitforC(sourcecode);

			ASTVisitor visitor = new ASTVisitor() {
				@Override
				public int visit(IASTDeclaration declaration) {
					if (declaration instanceof ICPPASTFunctionDefinition) {
						output.add((ICPPASTFunctionDefinition) declaration);
						return ASTVisitor.PROCESS_SKIP;
					}
					return ASTVisitor.PROCESS_CONTINUE;
				}
			};

			visitor.shouldVisitDeclarations = true;

			unit.accept(visitor);
		} catch (Exception e) {

		}
		return output;
	}

	public static IASTTranslationUnit getIASTTranslationUnitforCpp(char[] code) throws Exception {
		File filePath = new File("");
		FileContent fc = FileContent.create(filePath.getAbsolutePath(), code);
		Map<String, String> macroDefinitions = new HashMap<>();
		String[] includeSearchPaths = new String[0];
		IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		IParserLogService log = new DefaultLogService();
		return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
	}

	public static IASTTranslationUnit getIASTTranslationUnitforC(char[] code) throws Exception {
		File filePath = new File("");
		FileContent fc = FileContent.create(filePath.getAbsolutePath(), code);
		Map<String, String> macroDefinitions = new HashMap<>();
		String[] includeSearchPaths = new String[0];
		IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		IParserLogService log = new DefaultLogService();
		return GCCLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
	}

	/**
	 * Get name of variable
	 * Ex: "a[2]" ----->"a"
	 * Ex: "a.b[2]" ----->"a.b"
	 *
	 * @param variableName
	 * @return
	 */
	public static String getNameVariable(String variableName) {
		String name = variableName;

		// "a[]" --- > "a[<something>]"
		final int DEFAULT_INDEX = 23424131;
		variableName = variableName.replaceAll("\\[\\s*\\]", "[" + DEFAULT_INDEX + "]");

		// add prefix to analyze cases such as "int[3]"
		final String PREFIX = "AUT_PREFIX";
		variableName = PREFIX + variableName;

		IASTNode ast = Utils.convertToIAST(variableName + "==" + 0);
		if (ast instanceof ICPPASTBinaryExpression) {
			IASTExpression left = ((ICPPASTBinaryExpression) ast).getOperand1();
			if (left instanceof ICPPASTArraySubscriptExpression) {
				int maxCount = 0;
				while (left instanceof ICPPASTArraySubscriptExpression) {
					if (++maxCount >= 10)
						break; // avoid infinite loop

					ICPPASTArraySubscriptExpression castedLeft = (ICPPASTArraySubscriptExpression) left;
					ICPPASTInitializerClause index = castedLeft.getArgument();

					left = castedLeft.getArrayExpression();
				}

				name = left.getRawSignature();
			}
		}

		name = name.replaceFirst(PREFIX, ""); // remove prefix
		return name;
	}

	public static int toInt(String str) {
		/*
		  Remove bracket from negative number. Ex: Convert (-2) into -2
		 */
		str = str.replaceAll("\\((" + IRegex.NUMBER_REGEX + ")\\)", "$1");

		/*
		 */
		boolean isNegative = false;
		if (str.startsWith("-")) {
			str = str.substring(1);
			isNegative = true;
		} else if (str.startsWith("+"))
			str = str.substring(1);
		/*

		 */
		int n;
		try {
			n = Integer.parseInt(str);
			if (isNegative)
				n = -n;
		} catch (Exception e) {
			n = Utils.UNDEFINED_TO_INT;
		}
		return n;
	}

	public static INode getTopLevelClassvsStructvsNamesapceNodeParent(INode n) {
		if (n == null)
			return null;
		else if (n instanceof ClassNode || n instanceof StructNode || n instanceof NamespaceNode)
			if (n.getParent() != null && n.getParent() instanceof ISourcecodeFileNode)
				return n;
			else
				return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(n.getParent());
		else if (n instanceof IFunctionNode)
			return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(((IFunctionNode) n).getRealParent());
		else
			return Utils.getTopLevelClassvsStructvsNamesapceNodeParent(n.getParent());
	}

	/**
	 * Shorten ast node. <br/>
	 * Ex:"(a)" -----> "a" <br/>
	 * Ex: "(!a)" --------> "!a"
	 *
	 * @param ast
	 * @return
	 */
	public static IASTNode shortenAstNode(IASTNode ast) {
		IASTNode tmp = ast;
		/*
		 * Ex:"(a)" -----> "a"
		 *
		 * Ex: "(!a)" --------> !a
		 */
		while ((tmp instanceof CPPASTExpressionStatement || tmp instanceof ICPPASTUnaryExpression
				&& tmp.getRawSignature().startsWith("(") && tmp.getRawSignature().endsWith(")"))
				&& tmp.getChildren().length == 1 && !tmp.getRawSignature().startsWith("!"))
			tmp = tmp.getChildren()[0];

		return tmp;
	}

	public static String generateContentForDefaultCase(String controlValue, List<String> caseValues) {
		StringBuilder content = new StringBuilder();

		for (int i = 0; i < caseValues.size(); i++) {
			content.append(controlValue).append(" != ").append(caseValues.get(i));
			if (i < caseValues.size() - 1) {
				content.append(" && ");
			}
		}

		return content.toString();
	}

	public static void copy(File src, File dest, File... ignores) throws IOException {

		if (ignores != null) {
			for (File f : ignores) {
				if (f != null && src.getAbsolutePath().equals(f.getAbsolutePath()))
					return;
			}
		}

		if (src.isDirectory()) {

			// if directory not exists, create it
			if (!dest.exists())
				dest.mkdir();

			// list all the directory contents
			String[] files = src.list();

			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				Utils.copy(srcFile, destFile, ignores);
			}

		} else {
			dest.getParentFile().mkdirs();
			dest.createNewFile();

			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0)
				out.write(buffer, 0, length);

			in.close();
			out.close();
		}
	}

	public static String readResourceContent(String relativePath) {
		InputStream in = Utils.class.getResourceAsStream(relativePath);

		StringBuilder template = new StringBuilder();
		String line;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			while ((line = reader.readLine()) != null)
				template.append(line).append(SpecialCharacter.LINE_BREAK);
		} catch (IOException ex) {
			logger.error("Cant read resource content from " + IdMapping.getInstance().getOrCreate(relativePath));
		}

		return template.toString();
	}

//	public static void copyExeTo(String path) throws IOException {
//		FileInputStream fis = (FileInputStream) Utils.class.getResourceAsStream(Locations.Z3_PATH);
//		BufferedInputStream bis = new BufferedInputStream(fis);
//
//		FileOutputStream fos = new FileOutputStream(path);
//		BufferedOutputStream bos = new BufferedOutputStream(fos);
//
//		int b = 0;
//		while ((b = bis.read()) != -1) {
//			bos.write(b);
//		}
//
//		bos.flush();
//		bos.close();
//
//		String[] args = new String[] {"/bin/bash", "-c", "chmod u+x " + path, "with", "args"};
//		Process proc = new ProcessBuilder(args).start();
//	}
//
//	public static void copyFolderTo(String path,String desPath) throws IOException{
//		InputStream is = Utils.class.getResourceAsStream(path);
//		FileInputStream fis = new FileInputStream(is);
//		String sourcePath = Utils.class.getResourceAsStream(path).getPath();
//		File source = new File(sourcePath);
//		File target = new File(desPath);
//		copyDirectory(source,target);
//	}
//
//	private static void copyDirectory(File source, File target)
//			throws IOException {
//
//		if (source.isDirectory()) {
//
//			//if directory not exists, create it
//			if (!target.exists()) {
//				if (target.mkdir()) {
//					System.out.println("Directory copied from "
//							+ source + "  to " + target);
//				} else {
//					System.err.println("Unable to create directory : " + target);
//				}
//			}
//
//			// list all the directory contents, file walker
//			String[] files = source.list();
//			if (files == null) {
//				return;
//			}
//
//			for (String file : files) {
//				//construct the src and dest file structure
//				File srcFile = new File(source, file);
//				File destFile = new File(target, file);
//				//recursive copy
//				copyDirectory(srcFile, destFile);
//			}
//
//		} else {
//
//			//if file, then copy it
//			//Use bytes stream to support all file types
//			InputStream in = null;
//			OutputStream out = null;
//
//			try {
//
//				in = new FileInputStream(source);
//				out = new FileOutputStream(target);
//
//				byte[] buffer = new byte[1024];
//
//				int length;
//				//copy the file content in bytes
//				while ((length = in.read(buffer)) > 0) {
//					out.write(buffer, 0, length);
//				}
//
//				System.out.println("File copied from " + source + " to " + target);
//
//			} catch (IOException e) {
//
//				System.err.println("IO errors : " + e.getMessage());
//
//			} finally {
//				if (in != null) {
//					in.close();
//				}
//
//				if (out != null) {
//					out.close();
//				}
//			}
//		}
//	}

	public static int getLevel(String rawType) {
		rawType = TemplateUtils.deleteTemplateParameters(rawType);
		return (int) rawType.chars().filter(c -> c == '*').count();
	}

	public static String doubleNormalizePath(String path) {
		String singleBackSlash = "\\";
		String doubleBackSlash = singleBackSlash + singleBackSlash;
		String singleSlash = "/";

		String result = normalizePath(path);

		if (!File.separator.equals(singleSlash)) {
			result = result.replace(File.separator, doubleBackSlash);
		}

		return result;
	}

	public static String normalizePath(String path) {
		Matcher m = Pattern.compile("^(\\w):\\Q\\\\E").matcher(path);
		while (m.find()) {
			path = path.replace(m.group(0), m.group(0).toUpperCase());
		}
		path = path.replace("\r", "").replace("\n", "");
		String singleBackSlash = "\\";
		String doubleBackSlash = singleBackSlash + singleBackSlash;
		String singleSlash = "/";

		return path.replace(singleBackSlash, File.separator)
				.replace(singleSlash, File.separator)
				.replace(doubleBackSlash, File.separator);
	}

	public static double round(double value, int precision) {
//        String format = "%." + precision + "f";
//        String str = String.format(format, value);
//        return Double.parseDouble(str);

		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	public static String computeMd5(String message) {
		String digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(message.getBytes("UTF-8"));

			//converting byte array to Hexadecimal String
			StringBuilder sb = new StringBuilder(2 * hash.length);
			for (byte b : hash) {
				sb.append(String.format("%02x", b & 0xff));
			}

			digest = sb.toString();

		} catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		return digest;
	}

	/**
	 * Láº¥y mÃ£ ASCII cá»§a kÃ­ tá»±
	 *
	 * @param ch
	 * @return
	 */
	public static int getASCII(char ch) {
		return ch;
	}

	/**
	 * Get the reduce index of array item
	 * <p>
	 * Ex: a[1+2][3] --------> [3][3]
	 *
	 * @param arrayItem
	 * @param table
	 * @return
	 * @throws Exception
	 */
	public static String getReducedIndex(String arrayItem, IVariableNodeTable table) throws Exception {
		StringBuilder index = new StringBuilder();
		List<String> indexes = Utils.getIndexOfArray(arrayItem);

		for (String indexItem : indexes) {
			indexItem = ExpressionRewriterUtils.rewrite(table, indexItem);
			index.append(Utils.asIndex(indexItem));
		}
		return index.toString();
	}

	/**
	 * @param expression
	 * @return
	 * @see #{CustomJevalTest.java}
	 */
	public static String transformFloatNegativeE(String expression) {
		Matcher m = Pattern.compile("\\d+E-\\d+").matcher(expression);
		while (m.find()) {
			String beforeE = expression.substring(0, expression.indexOf("E-"));
			String afterE = expression.substring(expression.indexOf("E-") + 2);

			StringBuilder newValue = new StringBuilder();

			if (Utils.toInt(afterE) != Utils.UNDEFINED_TO_INT) {
				int numDemicalPoint = Utils.toInt(afterE);

				if (numDemicalPoint == 0) {
					newValue = new StringBuilder(beforeE);

				} else if (beforeE.length() > numDemicalPoint) {
					for (int i = 0; i < beforeE.length() - numDemicalPoint; i++)
						newValue.append(beforeE.toCharArray()[i]);
					newValue.append(".");

					for (int i = beforeE.length() - numDemicalPoint; i < beforeE.length(); i++) {
						newValue.append(beforeE.toCharArray()[i]);
					}
				} else {
					newValue.append("0.");
					for (int i = 0; i <= numDemicalPoint - 1 - beforeE.length(); i++) {
						newValue.append("0");
					}
					newValue.append(beforeE);
				}
			}

			expression = expression.replace(m.group(0), newValue.toString());
		}
		return expression;
	}

	public static String transformFloatPositiveE(String expression) {
		Matcher m = Pattern.compile("\\d+E\\+\\d+").matcher(expression);
		while (m.find()) {
			String beforeE = expression.substring(0, expression.indexOf("E+"));
			String afterE = expression.substring(expression.indexOf("E+") + 2);

			StringBuilder newValue = new StringBuilder();

			if (Utils.toInt(afterE) != Utils.UNDEFINED_TO_INT) {
				int numDemicalPoint = Utils.toInt(afterE);

				if (numDemicalPoint == 0) {
					newValue = new StringBuilder(beforeE);

				} else {
					newValue = new StringBuilder(beforeE);
					for (int i = 0; i < numDemicalPoint; i++)
						newValue.append("0");
				}
			}

			expression = expression.replace(m.group(0), newValue.toString());
		}
		return expression;
	}

	public static long availableMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.maxMemory() - runtime.freeMemory();
	}

	public static long size(File file) {
		long length = 0;
		if (file.isFile())
			length = file.length();
		else if (file.isDirectory() && file.listFiles() != null) {
			for (File child : Objects.requireNonNull(file.listFiles())) {
				length += size(child);
			}
		}
		return length;
	}

	public static String deleteBracesAndParams(String s){
		s = s.replaceAll("[^\\w]", "_");
		return s;
	}



}

