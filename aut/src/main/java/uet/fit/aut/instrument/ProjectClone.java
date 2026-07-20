package uet.fit.aut.instrument;

import org.eclipse.cdt.core.dom.ast.*;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.config.pro.ProHeaderNode;
import uet.fit.aut.config.pro.ProSourceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.IProjectLoader;
import uet.fit.aut.parser.ProjectParser;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.IncludeHeaderNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.SearchCondition;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.IncludeHeaderNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static uet.fit.aut.thread.task.ExecuteTestTask.INSTRUMENT_NAME;

public class ProjectClone {

	public static final String MAIN_REFACTOR_NAME = "deprecated_main" + "(";
	public static final String MAIN_REGEX = "\\b" + "main" + "\\s*" + "\\(";

	private static final Logger logger = LoggerFactory.getLogger(ProjectClone.class);

	private static final ExecutorService es = ProjectParser.es;

	protected final ConcurrentMap<String, String> refactors = new ConcurrentHashMap<>();
	protected final ProjectConfig projectConfig;
	protected final ProjectNode root;
    protected final List<String> globalDeclarations = new ArrayList<>();

	protected final String environmentPath;
	protected final String user;

//    protected static List<String> sLibraries;
//    protected boolean whiteBoxEnable;
//    protected List<String> libraries;
//    protected List<IEnvironmentNode> stubLibraries;
    protected boolean canStub;

	public ProjectClone(String environmentPath, ProjectConfig config, ProjectNode root, String user) {
		this.environmentPath = environmentPath;
		this.projectConfig = config;
		this.root = root;
		this.user = user;
	}

	/**
	 * Clone all source code file with extension in project directories.
	 */
	public void cloneEnvironment() throws IOException, InterruptedException {
//		List<IEnvironmentNode> stubLibraries = EnvironmentSearch
//				.searchNode(Environment.getInstance().getEnvironmentRootNode(), new EnviroLibraryStubNode());

		// clone folder
//		cloneProjectDirectory();

//		List<INode> stubUnits = Environment.getInstance().getStubs();
//		List<INode> sbfUnits = Environment.getInstance().getSBFs();
		List<ISourcecodeFileNode> sources = new ArrayList<>();

		// search sources
		for (ProSourceNode proSource : projectConfig.getSources()) {
			String path = proSource.getAbsolutePath();
			List<ISourcecodeFileNode> searchNodes = Search.searchNodes(root, new SourcecodeFileNodeCondition(), path);
			sources.addAll(searchNodes);
		}

		// search header
		for (ProHeaderNode proHeader : projectConfig.getHeaders()) {
			String path = proHeader.getAbsolutePath();
			List<ISourcecodeFileNode> searchNodes = Search.searchNodes(root, new SourcecodeFileNodeCondition(), path);
			sources.addAll(searchNodes);
		}

		for (ISourcecodeFileNode sourceCode : sources) {
			String cloneFilePath = getClonedFilePath(sourceCode.getAbsolutePath());
			String newContent = generateFileContent(sourceCode);
			logger.debug("Generate instrument file of " + IdMapping.getInstance().getOrCreate(sourceCode.getName()) + " successfully");
			Utils.writeContentToFile(newContent, cloneFilePath);
		}
	}

	private void cloneProjectDirectory() throws IOException {
		// get origin directory
		File originDirectory = new File(projectConfig.getProjectPath());

		// get cloned directory
		File clonedDirectory = getClonedDirectory(environmentPath);

		// ignore dest directory
		File destDir = null;
		String desDirPath = projectConfig.getDestDir();
		if (desDirPath != null)
			destDir = new File(desDirPath);

		// copy folder
		Utils.copy(originDirectory, clonedDirectory, destDir);
	}

	/**
	 * Get cloned source code file path
	 * @param origin file path
	 * @return cloned file path
	 */
	public String getClonedFilePath(String origin) {
		String projectPath = projectConfig.getProjectPath();
		File proFile = new File(projectPath);
		File originFile = new File(origin);
		String relativePath = PathUtils.relative(originFile, proFile);
		File clonedDir = getClonedDirectory(environmentPath);
		return PathUtils.absolute(relativePath, clonedDir);
	}

	public static String getClonedFilePath(String environment, File project, String origin) {
		File originFile = new File(origin);
		String relativePath = PathUtils.relative(originFile, project);
		File clonedDir = getClonedDirectory(environment);
		return PathUtils.absolute(relativePath, clonedDir);
	}

//	/**
//	 * Get origin source code file path
//	 * @param clone file path
//	 * @return origin file path
//	 */
//	public String getOriginFilePath(String clone) {
//		String proPath = projectConfig.getProPath();
//		File proFile = new File(proPath);
//		File clonedDir = getClonedDirectory();
//		File clonedFile = new File(clone);
//		String relativePath = PathUtils.relative(clonedFile, clonedDir);
//		return PathUtils.absolute(relativePath, proFile);
//	}

	/**
	 * Get cloned directory
	 */
	private static File getClonedDirectory(String environmentPath) {
		String newContainer = environmentPath + File.separator + INSTRUMENT_NAME;
		return new File(newContainer);
	}

	/**
	 * Ex: int x;
	 * To #ifdef AUT_GLOBAL_X
	 * #define AUT_GLOBAL_X
	 * int x;
	 * #endif
	 *
	 * @param name    define name
	 * @param content needed to be guard
	 * @return new source code
	 */
	public static String wrapInIncludeGuard(String name, String content) {
		return String.format("/** Guard statement to avoid multiple declaration */\n" +
				"#ifndef %s\n#define %s\n%s\n#endif\n", name, name, content);
	}

	public static String wrapDefineStub(String content){
		return String.format("/** Define directory to stub file */\n" +
				"#define XSTR(x) #x\n#define STR(x) XSTR(x)\n%s", content);
	}

	public static String generateCallingMark(String content) {
        return String.format(DriverConstant.MARK + "(\"Calling: %s\");" + SourceConstant.INCREASE_FCALLS, content);
	}

	/**
	 * Generate clone file content
	 *
	 * @param sourceCode origin source code file content
	 * @return cloned file content
	 */
	protected String generateFileContent(ISourcecodeFileNode sourceCode) throws InterruptedException {
		String filePath = sourceCode.getAbsolutePath();
		String oldContent = Utils.readFileContent(filePath);

		List<SearchCondition> conditions = new ArrayList<>();
		conditions.add(new IncludeHeaderNodeCondition());
//		conditions.add(new GlobalVariableNodeCondition());
		conditions.add(new AbstractFunctionNodeCondition());
		// TODO: macro
//        conditions.add(new MacroFunctionNodeCondition());

		List<INode> redefines = Search.searchNodes(sourceCode, conditions);
		redefines.removeIf(this::isIgnore);

		int size = redefines.size();

		AtomicInteger counter = new AtomicInteger();
		List<Callable<Void>> tasks = new ArrayList<>();
		for (INode child : redefines) {
			Callable<Void> c = () -> {
//                int index = redefines.indexOf(child);

				logger.debug(String.format("[%d/%d] Clone & instrument %s", counter.incrementAndGet(), size, IdMapping.getInstance().getOrCreate(child.getName())));

				if (child instanceof IncludeHeaderNode) {
					refactorInclude((IncludeHeaderNode) child);

//				} else if (child instanceof ExternalVariableNode) {
//					guardGlobalDeclaration((ExternalVariableNode) child);

				} else if (child instanceof IFunctionNode) {
					IFunctionNode function = (IFunctionNode) child;
					refactorFunction(function);
				}

				return null;
			};
			tasks.add(c);
		}

		es.invokeAll(tasks);
//		es.shutdown();

		for (Map.Entry<String, String> entry : refactors.entrySet()) {
			String prev = entry.getKey();
			String newC = entry.getValue();
//			if (!IdMapping.getInstance().isEncrypt())
//				logger.debug("New content: " + newC);
			oldContent = oldContent.replace(prev, newC);
		}

        for (String globalDeclaration : globalDeclarations)
            oldContent = oldContent.replace("#endif\n\n" + globalDeclaration, "#endif");


		oldContent = oldContent
//                .replaceAll(MAIN_REGEX, MAIN_REFACTOR_NAME)
				.replaceAll("\\bconstexpr\\b", SpecialCharacter.EMPTY);

		String additionDeclaration;

		if (filePath.endsWith(IProjectLoader.C_FILE_SYMBOL)) {
			additionDeclaration =
					//"extern int strcmp(const char * str1, const char * str2);\n" +
					"extern int AUT_mark(char* str);\n" +
//					"extern void AUT_assert(char* actualName, int actualVal, char* expectedName, int expectedVal);\n" +
//					"extern int AUT_assert_double(char* actualName, double actualVal, char* expectedName, double expectedVal);\n" +
//					"extern int AUT_assert_ptr(char* actualName, void* actualVal, char* expectedName, void* expectedVal);\n" +
					"extern int AUT_fCall;\n" +
					"extern char* AUT_test_case_name;\n\n\n\n";
		} else  {
			additionDeclaration = "#include <string>\n" +
//					"extern int strcmp(const char * str1, const char * str2);\n" +
					"extern int AUT_mark(std::string append);\n" +
//					"extern void AUT_assert(std::string actualName, int actualVal, std::string expectedName, int expectedVal);\n" +
//					"extern int AUT_assert_double(std::string actualName, double actualVal, std::string expectedName, double expectedVal);\n" +
//					"extern int AUT_assert_ptr(std::string actualName, void* actualVal, std::string expectedName, void* expectedVal);\n" +
					"extern int AUT_fCall;\n" +
					"extern char* AUT_test_case_name;\n\n\n\n";
		}

		oldContent = additionDeclaration + oldContent;

		String defineSourceCodeName = SourceConstant.SRC_PREFIX + sourceCode.getAbsolutePath().toUpperCase()
				.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE);
//		oldContent = wrapDefineStub(oldContent);
		return wrapInIncludeGuard(defineSourceCodeName, oldContent);
	}

	private boolean isIgnore(INode node) {
		if (node instanceof ICommonFunctionNode && node.getParent() instanceof ICommonFunctionNode)
			return true;

		if (node instanceof IFunctionNode) {
			IASTFunctionDefinition ast = ((IFunctionNode) node).getAST();

			IASTNodeLocation[] funcLocations = ast.getNodeLocations();
			if (funcLocations.length == 0)
				return true;

			IASTStatement body = ast.getBody();
			if  (body == null || body.getFileLocation() == null)
				return true;

			return isMacroExpansion(body);
		}

		return false;
	}

	private boolean isMacroExpansion(IASTStatement stm) {
		List<IASTNodeLocation> locations = new ArrayList<>();
		for (IASTNodeLocation location : stm.getNodeLocations()) {
			if (location != null)
				locations.add(location);
		}

		if (locations.size() == 1) {
			return locations.get(0) instanceof IASTMacroExpansionLocation;
		}

		return false;
	}

	/**
	 * Refactor include to clone file and stub libraries source files.
	 * Add new source code file content
	 *
	 * @param includeHeader node
	 */
	private void refactorInclude(IncludeHeaderNode includeHeader) {

		// Guard all include statement in source code
		String guardIncludeStm = guardIncludeHeader(includeHeader);

		// get prev include statement
		String oldIncludeStatement = includeHeader.getAST().getRawSignature();

		refactors.put(oldIncludeStatement, guardIncludeStm);
	}

	/**
	 * Refactor function content and add new source code file content
	 *
	 * @param function node
	 */
	protected void refactorFunction(IFunctionNode function) {
		String oldFunctionCode;

		IASTFunctionDefinition functionAST = ((AbstractFunctionNode) function).getAST();
		oldFunctionCode = functionAST.getRawSignature();

		// generate instrumented function content
		String newFunctionCode = generateInstrumentedFunction(function);

		// include stub code in function scope
//		if (function instanceof FunctionNode && canStub) {
//			newFunctionCode = includeStubFile(newFunctionCode, (FunctionNode) function);
////			StubManager.initializeStubCode((FunctionNode) function, user);
//		}


		// change constexpr function to normal function
		newFunctionCode = newFunctionCode.replaceFirst("constexpr\\s+", " ");

		refactors.put(oldFunctionCode, newFunctionCode);
	}

	/**
	 * Ex: int foo() {
	 * int x, y;
	 * ...
	 * return x + y;
	 * }
	 * To int foo() {
	 * #include "Utils.foo.23.324.stub"
	 * int x, y;
	 * ...
	 * return x + y;
	 * }
	 *
	 * @param oldContent of function source code
	 * @param function   needed to insert stub code
	 * @return new function source code
	 */
	protected String includeStubFile(String oldContent, FunctionNode function) {
		String markFunctionStm;

		markFunctionStm = generateCallingMark(function.getAbsolutePath());

		int bodyBeginPos = oldContent.indexOf(SpecialCharacter.OPEN_BRACE) + markFunctionStm.length() + 1;

//		String stubDirectory = FolderConfig.getStubDirectory(user);
//		String stubFilePath = stubDirectory + (stubDirectory.endsWith(File.separator) ? "" : File.separator)
//				+ StubManager.getStubCodeFileName(function);

//		String originPath = Utils.getSourcecodeFile(function).getAbsolutePath();
//		String clonePath = getClonedFilePath(originPath);
//		Path parentDirPath = Paths.get(clonePath).getParent();
//		Path stubPath = Paths.get(stubFilePath);
//
//		String relativePath = parentDirPath.relativize(stubPath).toString();

//		StubManager.addStubFile(function.getAbsolutePath(), stubFilePath, user);

//		String name = StubManager.rewriteFuncNameToStub(function.toString());
//		return oldContent.substring(0, bodyBeginPos)
//				+ String.format("\n\t/** Include stub source code */\n")
//				+ String.format("\t#ifdef %s\n",name)
//				+ String.format("\t#include STR(%s)\n",name)
//				+ String.format("\t#endif\n")
//				+ oldContent.substring(bodyBeginPos);
		return null;
	}

	/**
	 * Ex: #include "class.h"
	 * To #ifdef AUT_INCLUDE_CLASS_H
	 * #define AUT_INCLUDE_CLASS_H
	 * #include "class.h"
	 * #endif
	 *
	 * @param child corresponding external variable node
	 * @return new guarded include statement
	 */
	private String guardIncludeHeader(IncludeHeaderNode child) {
		String oldIncludeHeader = child.getAST().getRawSignature();
		String header = child.getName().replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE).toUpperCase();
		return wrapInIncludeGuard(SourceConstant.INCLUDE_PREFIX + header, oldIncludeHeader);
	}

	/**
	 * Add new declaration of external variable node
	 *
	 * Ex: int x;
	 * To #ifdef AUT_GLOBAL_X
	 * #define AUT_GLOBAL_X
	 * int x;
	 * #endif
	 *
	 * @param child corresponding external variable node
	 */
	private void guardGlobalDeclaration(ExternalVariableNode child) {
		IASTNodeLocation[] tempAstLocations = child.getASTType().getNodeLocations();
		if (tempAstLocations.length > 0) {
			IASTNodeLocation astNodeLocation = tempAstLocations[0];
			if (astNodeLocation instanceof IASTCopyLocation) {
				IASTNode declaration = ((IASTCopyLocation) astNodeLocation).getOriginalNode().getParent();
				if (declaration instanceof IASTDeclaration) {
					String originDeclaration = declaration.getRawSignature();

					if (!globalDeclarations.contains(originDeclaration))
						globalDeclarations.add(originDeclaration);

					String oldDeclaration = child.getASTType().getRawSignature() + " "
							+ child.getASTDecName().getRawSignature() + ";";

					String header = child.getAbsolutePath();

					if (header.startsWith(File.separator))
						header = header.substring(1);

					header = header.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE).toUpperCase();

					String newDeclaration = wrapInIncludeGuard(SourceConstant.GLOBAL_PREFIX + header, oldDeclaration);

					refactors.put(originDeclaration, newDeclaration + "\n" + originDeclaration);
				}
			}
		}
	}

	/**
	 * Change all private and protected labels in source code to public
	 */
	private static String refactorWhiteBox(String oldContent) {
		oldContent = oldContent.replaceAll("\\bprivate\\b", "public");
		oldContent = oldContent.replaceAll("\\bprotected\\b", "public");
		oldContent = oldContent.replaceAll("\\bstatic \\b", SpecialCharacter.EMPTY);

		return oldContent;
	}

	/**
	 * Perform on instrumentation on the original function
	 */
	private String generateInstrumentedFunction(IFunctionNode functionNode) {
		final String success = String.format("/** Instrumented function %s */\n", functionNode.getName());
		final String fail = String.format("/** Can not instrument function %s */\n", functionNode.getName());

		String instrumentedSourceCode;

		IASTFunctionDefinition astInstrumentedFunction = functionNode.getAST();

		AbstractFunctionInstrumentation fnInstrumentation = new FunctionInstrumentationForAllCoverages(
				astInstrumentedFunction, functionNode);

		fnInstrumentation.setFunctionPath(functionNode.getAbsolutePath());

		String instrument = fnInstrumentation.generateInstrumentedFunction();
		if (instrument == null || instrument.length() == 0) {
			// can not instrument
			instrumentedSourceCode = fail + functionNode.getAST().getRawSignature();
		} else {
			instrumentedSourceCode = success + instrument;
			int bodyIdx = instrumentedSourceCode.indexOf(SpecialCharacter.OPEN_BRACE) + 1;

			instrumentedSourceCode = instrumentedSourceCode.substring(0, bodyIdx)
					+ generateCallingMark(functionNode.getAbsolutePath()) // insert mark start function
					+ instrumentedSourceCode.substring(bodyIdx);
		}

		return instrumentedSourceCode;
	}
}
