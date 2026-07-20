package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.logger.TimeTracker;
import uet.fit.aut.exception.LoadProjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.dependency.CompoundDependencyGeneration;
import uet.fit.aut.parser.dependency.DefinitionDependencyGeneration;
import uet.fit.aut.parser.dependency.ExtendedDependencyGeneration;
import uet.fit.aut.parser.dependency.FunctionCallDependencyGeneration;
import uet.fit.aut.parser.dependency.GlobalVariableDependencyGeneration;
import uet.fit.aut.parser.dependency.IncludeHeaderDependencyGeneration;
import uet.fit.aut.parser.dependency.IncludeQtHeaderPreprocessor;
import uet.fit.aut.parser.dependency.RealParentDependencyGeneration;
import uet.fit.aut.parser.dependency.SetterGetterDependencyGeneration;
import uet.fit.aut.parser.dependency.TypedefDependencyGeneration;
import uet.fit.aut.parser.obj.AttributeOfStructureVariableNode;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.IncludeHeaderNode;
import uet.fit.aut.parser.obj.NamespaceNode;
import uet.fit.aut.parser.obj.Node;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.parser.obj.QtHeaderNode;
import uet.fit.aut.parser.obj.SourcecodeFileNode;
import uet.fit.aut.parser.obj.StructOrClassNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.SearchCondition;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.AttributeVariableNodeCondition;
import uet.fit.aut.search.condition.ClassNodeCondition;
import uet.fit.aut.search.condition.DefinitionFunctionNodeCondition;
import uet.fit.aut.search.condition.EmptyStructureNodeCondition;
import uet.fit.aut.search.condition.EnumNodeCondition;
import uet.fit.aut.search.condition.GlobalVariableNodeCondition;
import uet.fit.aut.search.condition.NamespaceNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.search.condition.StructNodeCondition;
import uet.fit.aut.search.condition.TypedefNodeCondition;
import uet.fit.aut.search.condition.UnionNodeCondition;
import uet.fit.aut.thread.AutExecutors;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProjectParser {

	private final static Logger logger = LoggerFactory.getLogger(ProjectParser.class);

	public final static ExecutorService es = AutExecutors.newWorkStealingPool();

	public static final FileContent EMPTY_FILE = FileContent.create("", new char[]{});
	public static final IncludeFileContentProvider CONTENT_PROVIDER = new ContentProvider();
	public static final IParserLogService LOG_SERVICE = new DefaultLogService();

	private final File project;

	private List<File> ignoreFolders = new ArrayList<>();
	private List<File> considerFiles = new ArrayList<>();
	private List<File> considerFolders = new ArrayList<>();
	private File qtDir;
	private Map<String, String> defines = new HashMap<>();

	private CppConfig cppConfig;
	private List<QtHeaderNode> qtHeaderNodes;

	// for logger on server
	private ExternalLogger externalLogger;
	private final String _preprocessQtTaskId, _expandTaskId, _resolveDependTaskId;

	// default setting
	private final boolean optExpandUptoMethod = true;
	private final boolean optMergeNamespaces = true;
	private final boolean optPreprocessQtHeader = true;

	// not used setting
	private final boolean optAnalyzeSizeof = false;
	private final boolean optAnalyzeTypeDependencies = false;

	// changeable setting
	private boolean optAnalyzeIncludeDependencies = true;
	private boolean optAnalyzeRealParent = true;
	private boolean optAnalyzeExtendDependencies = true;
	private boolean optAnalyzeGetSetDependencies = true;
	private boolean optAnalyzeCallDependencies = false;
	private boolean optAnalyzeGlobalVarUsed = true;
	private boolean optAnalyzeTypedefDependencies = true;
	private boolean optAnalyzeDefinitionDependencies = true;

	private final Map<String, ISourcecodeFileNode> sourceCache = new HashMap<>();

	public ProjectParser(File project) {
		this.project = project;
		_preprocessQtTaskId = UUID.randomUUID().toString();
		_expandTaskId = UUID.randomUUID().toString();
		_resolveDependTaskId = UUID.randomUUID().toString();
	}

	public ProjectNode getRootTree() throws InterruptedException, LoadProjectException, CoreException, IOException {
		ProjectLoader loader = new ProjectLoader();
		loader.setIgnoreFolders(ignoreFolders);
		logger.debug("Ignore folders: " + IdMapping.getInstance().getOrCreate(ignoreFolders.toString()));

		loader.setConsiderFiles(considerFiles);
		loader.setConsiderFolders(considerFolders);

		ExternalLogger.log(externalLogger, "Loading " + project.getName() + " project structure");
		logger.debug("Loading the project " + IdMapping.getInstance().getOrCreate(project.getAbsolutePath()));
		ProjectNode root = loader.load(project);

		if (root == null) {
			logger.error("Failed to Load the project " + IdMapping.getInstance().getOrCreate(project.getAbsolutePath()));
			ExternalLogger.error(externalLogger, "Failed to load " + project.getName() + " project structure");
			throw new LoadProjectException(project);
		}

		root.setQtDirectory(qtDir);

		logger.debug("Loaded the project " + IdMapping.getInstance().getOrCreate(project.getAbsolutePath()) + " successfully");

		List<ISourcecodeFileNode> sourcecodeFileNodes = Search.searchNodes(root, new SourcecodeFileNodeCondition());
		final int sourceSize = sourcecodeFileNodes.size();

		ExternalLogger.log(externalLogger, "Preprocessing Qt in " + project.getName() + " project");

		ExternalLogger.progress(externalLogger, _preprocessQtTaskId, "Preprocessing Qt", -1, 1);

		cppConfig = CppConfig.retrieve();

		ExternalLogger.registerTask(externalLogger, _preprocessQtTaskId, "Preprocessing Qt", sourceSize);

		// STEP 1: ANALYZING INCLUDE DEPENDENCIES
		if (optPreprocessQtHeader) {
			long start = System.currentTimeMillis();
			qtHeaderNodes = preprocessQtHeaders(cppConfig, sourcecodeFileNodes);
			TimeTracker.add("Preprocess Qt", System.currentTimeMillis() - start);
		}

		final IASTTranslationUnit translationUnit;

		// STEP 2: EXPAND TREE UP TO METHOD LEVEL
		if (optExpandUptoMethod) {
			long startTime = System.currentTimeMillis();
			translationUnit = expandWholeProject(cppConfig, qtHeaderNodes, sourcecodeFileNodes);
			TimeTracker.add("Parse file", System.currentTimeMillis() - startTime);
		}

		ExternalLogger.registerTask(externalLogger, _resolveDependTaskId, "Resolving dependencies", sourceSize);

		IASTPreprocessorIncludeStatement[] includeStms = translationUnit.getIncludeDirectives();

		List<NamespaceNode> namespaces = Search.searchNodes(root, new NamespaceNodeCondition());

		List<IFunctionNode> functionNodes = Search.searchNodes(root, new AbstractFunctionNodeCondition());

		List<ExternalVariableNode> externalVariableNodes = Search.searchNodes(root, new GlobalVariableNodeCondition());

		List<SearchCondition> uncompletedNodeConds = new ArrayList<>();
		uncompletedNodeConds.add(new EmptyStructureNodeCondition());
		uncompletedNodeConds.add(new DefinitionFunctionNodeCondition());
		List<INode> uncompletedNodes = Search.searchNodes(root, uncompletedNodeConds);

		List<SearchCondition> structConds = new ArrayList<>();
		structConds.add(new ClassNodeCondition());
		structConds.add(new StructNodeCondition());
		List<StructOrClassNode> structOrClassNodes = Search.searchNodes(root, structConds);
//				structNodes = structNodes.stream().filter(node -> !(node.getParent() instanceof ClassNode))
//						.collect(Collectors.toList()); // why?

		List<IFunctionNode> considerFunctions = functionNodes.stream()
				.filter(f -> {
					ISourcecodeFileNode src = Utils.getSourcecodeFile(f);
					return considerFiles.contains(src.getFile());
				})
				.collect(Collectors.toList());

		List<SearchCondition> structureConds = new ArrayList<>();
		structureConds.add(new EnumNodeCondition());
		structureConds.add(new StructNodeCondition());
		structureConds.add(new UnionNodeCondition());
		structureConds.add(new ClassNodeCondition());
		List<INode> structures = Search.searchNodes(root, structureConds);

		List<AttributeOfStructureVariableNode> attributes = Search.searchNodes(root, new AttributeVariableNodeCondition());

		int computedTaskNum = namespaces.size();

		if (optAnalyzeIncludeDependencies)
			computedTaskNum += includeStms.length;

		if (optAnalyzeGlobalVarUsed)
			computedTaskNum += functionNodes.size();

		if (optAnalyzeRealParent)
			computedTaskNum += functionNodes.size() + externalVariableNodes.size();

		if (optAnalyzeDefinitionDependencies)
			computedTaskNum += functionNodes.size() + uncompletedNodes.size();

		if (optAnalyzeExtendDependencies)
			computedTaskNum	+= structOrClassNodes.size();

		if (optAnalyzeCallDependencies)
			computedTaskNum += considerFunctions.size();

		if (optAnalyzeTypedefDependencies)
			computedTaskNum	+= structures.size();

		if (optAnalyzeGetSetDependencies)
			computedTaskNum += 1;

		final int taskNum = computedTaskNum;

		AtomicInteger analyzeProgress = new AtomicInteger();

		if (optAnalyzeIncludeDependencies) {
			long startTime = System.currentTimeMillis();

			List<Callable<Void>> analyzeIncludeTasks = new ArrayList<>();

			for (IASTPreprocessorIncludeStatement includeStm : translationUnit.getIncludeDirectives()) {
				ISourcecodeFileNode owner = null;

				if (includeStm.isActive()) {
					String fileName = includeStm.getFileLocation().getFileName();
					owner = sourceCache.get(fileName);
				}

				final ISourcecodeFileNode finalOwner = owner;

				if (finalOwner != null) {
					IncludeHeaderNode includeHeaderNode = new IncludeHeaderNode();
					includeHeaderNode.setAST(includeStm);
					includeHeaderNode.setAbsolutePath(finalOwner.getAbsolutePath()
							+ File.separator + "\"" + includeHeaderNode.getNewType()
							+ "\"");
					finalOwner.getChildren().add(includeHeaderNode);

					analyzeIncludeTasks.add(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							IncludeHeaderDependencyGeneration gen = new IncludeHeaderDependencyGeneration(root, considerFolders, includeStm);
							gen.dependencyGeneration(finalOwner);
							ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
							return null;
						}
					});
				} else {
					ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
				}
			}

			es.invokeAll(analyzeIncludeTasks);

			TimeTracker.add("Analyze Include", System.currentTimeMillis() - startTime);
		}

		// STEP 3: MERGE NAMESPACES
		if (optMergeNamespaces) {
			long startTime = System.currentTimeMillis();

			logger.debug("Merging Namespaces");
			ExternalLogger.log(externalLogger, "Merging namespaces in project " + project.getName());

			CompoundDependencyGeneration gen = new CompoundDependencyGeneration(root, namespaces);
			for (NamespaceNode node : namespaces) {
				gen.dependencyGeneration(node);
				ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
			}

			TimeTracker.add("Merge namespace", System.currentTimeMillis() - startTime);
		}

		// STEP 4: ANALYZE REAL PARENT DEPENDENCIES
		if (optAnalyzeRealParent) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Real Parent Dependencies");

			List<Callable<Void>> analyzeRealParentTasks = new ArrayList<>();
			AtomicInteger counter = new AtomicInteger();

			final int size = functionNodes.size() + externalVariableNodes.size();
			List<INode> nodes = new ArrayList<>(functionNodes);
			nodes.addAll(externalVariableNodes);

			for (INode node : nodes) {
				Callable<Void> analyzeRealParentTask = new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						int index = counter.incrementAndGet();
						logger.debug(String.format("[%d/%d] ", index, size) + "Finding Real parent of " + IdMapping.getInstance().getOrCreate(node.getName()));
						new RealParentDependencyGeneration().dependencyGeneration(node);
						ExternalLogger.log(externalLogger, "Found real parent of function " + node.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				};
				analyzeRealParentTasks.add(analyzeRealParentTask);
			}

			es.invokeAll(analyzeRealParentTasks);

			TimeTracker.add("Real parent", System.currentTimeMillis() - startTime);
		}

		// STEP 5: ANALYZE GLOBAL VARIABLES USED
		if (optAnalyzeGlobalVarUsed) {
			long startTime = System.currentTimeMillis();

			logger.debug("Finding Global Variables Usages");

			List<Callable<Void>> analyzeGlobalUsedTasks = new ArrayList<>();
			AtomicInteger counter = new AtomicInteger();

			final int size = functionNodes.size();
			for (IFunctionNode node : functionNodes) {
				Callable<Void> analyzeGlobalUsedTask = new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						int index = counter.incrementAndGet();
						logger.debug(String.format("[%d/%d] ", index, size) + "Analyzing Global used of " + IdMapping.getInstance().getOrCreate(node.getName()));
						new GlobalVariableDependencyGeneration().dependencyGeneration(node);
						ExternalLogger.log(externalLogger, "Found global variable usages in function " + node.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				};
				analyzeGlobalUsedTasks.add(analyzeGlobalUsedTask);
			}

			es.invokeAll(analyzeGlobalUsedTasks);

			TimeTracker.add("Global usage", System.currentTimeMillis() - startTime);
		}

		// STEP 6: ANALYZE DEFINITION DEPENDENCIES
		if (optAnalyzeDefinitionDependencies) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Definition Dependencies");
			AtomicInteger counter = new AtomicInteger();

			List<Callable<Void>> analyzeDeclarationTasks = new ArrayList<>();
			for (IFunctionNode functionNode : functionNodes) {
				analyzeDeclarationTasks.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						DefinitionDependencyGeneration gen = new DefinitionDependencyGeneration();
						gen.setSourceCache(sourceCache);
						gen.dependencyGeneration(functionNode);
						ExternalLogger.log(externalLogger, "Found completed definition of " + functionNode.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				});
			}
			es.invokeAll(analyzeDeclarationTasks);

			List<Callable<Void>> analyzeDefinitionTasks = new ArrayList<>();
			final int size = uncompletedNodes.size();
			for (INode node : uncompletedNodes) {
				Callable<Void> analyzeDefinitionTask = new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						int index = counter.incrementAndGet();
						logger.debug(String.format("[%d/%d] ", index, size) + "Finding Definition of " + IdMapping.getInstance().getOrCreate(node.getName()));
						DefinitionDependencyGeneration gen = new DefinitionDependencyGeneration();
						gen.dependencyGeneration(node);
						ExternalLogger.log(externalLogger, "Found completed definition of " + node.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				};
				analyzeDefinitionTasks.add(analyzeDefinitionTask);
			}
			es.invokeAll(analyzeDefinitionTasks);

			TimeTracker.add("Definition", System.currentTimeMillis() - startTime);
		}

//			// STEP 7: FIND DEPENDENCY BETWEEN PARAMETERS
//			// POINTERS/ARRAYS & THEIR SIZE
//			if (optAnalyzeSizeof) {
//				logger.debug("Guessing size of array parameter");
//				for (IFunctionNode node : functionNodes) {
//					if (node instanceof FunctionNode)
//						new SizeOfArrayDepencencyGeneration().dependencyGeneration(node);
//				}
//			}

		// STEP 8: ANALYZE EXTENDED DEPENDENCIES BETWEEN STRUCTURES
		if (optAnalyzeExtendDependencies) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Extended Dependencies");

			List<Callable<Void>> analyzeExtendTasks = new ArrayList<>();
			AtomicInteger counter = new AtomicInteger();

			final int size = structOrClassNodes.size();
			for (StructOrClassNode node : structOrClassNodes) {
				Callable<Void> task = new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						int index = counter.incrementAndGet();
						logger.debug(String.format("[%d/%d] ", index, size) + "Analyzing Extend dependency of " + IdMapping.getInstance().getOrCreate(node.getName()));
						new ExtendedDependencyGeneration().dependencyGeneration(node);
						ExternalLogger.log(externalLogger, "Finish analyzing extend dependencies of " + node.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				};
				analyzeExtendTasks.add(task);
			}

			es.invokeAll(analyzeExtendTasks);

			TimeTracker.add("Extends", System.currentTimeMillis() - startTime);
		}

		// STEP 9: ANALYZE FUNCTION CALL DEPENDENCIES
		if (optAnalyzeCallDependencies) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Function Call Dependencies");
			AtomicInteger counter = new AtomicInteger();
			List<Callable<Void>> analyzeFuncCallTasks = new ArrayList<>();

			final int size = considerFunctions.size();
			for (IFunctionNode node : considerFunctions) {
				Callable<Void> analyzeFuncCallTask = new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						int index = counter.incrementAndGet();
						logger.debug(String.format("[%d/%d] ", index, size) + "Analyzing function " + IdMapping.getInstance().getOrCreate(node.getName()));
						new FunctionCallDependencyGeneration().dependencyGeneration(node);
						ExternalLogger.log(externalLogger, "Finish analyzing function call dependencies of function " + node.getName());
						ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
						return null;
					}
				};
				analyzeFuncCallTasks.add(analyzeFuncCallTask);
			}

			es.invokeAll(analyzeFuncCallTasks);

			TimeTracker.add("Func call", System.currentTimeMillis() - startTime);
		}

//			// STEP 10: ANALYZE TYPE DEPENDENCIES OF VARIABLES
//			if (optAnalyzeTypeDependencies) {
//				logger.debug("Analyzing Type Dependencies");
//				for (IFunctionNode node : functionNodes) {
//					for (IVariableNode var : node.getArguments()) {
//						CTypeDependencyGeneration gen = new CTypeDependencyGeneration();
//						gen.setAddToTreeAutomatically(true);
//						gen.dependencyGeneration(var);
//					}
//				}
//			}

		// STEP 11: ANALYZE TYPEDEF DEPENDENCIES
		if (optAnalyzeTypedefDependencies) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Typedef Dependencies");

			List<INode> allTypedefs = Search.searchNodes(root, new TypedefNodeCondition());
			TypedefDependencyGeneration gen = new TypedefDependencyGeneration(allTypedefs);

			for (INode node : structures) {
				ExternalLogger.log(externalLogger, "Analyzing typedef dependency of " + node.getName());
				gen.dependencyGeneration(node);
				ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", analyzeProgress.incrementAndGet(), taskNum);
			}

			TimeTracker.add("Typedef", System.currentTimeMillis() - startTime);
		}

		// STEP 12: ANALYZE GETTER/SETTER DEPENDENCIES
		if (optAnalyzeGetSetDependencies) {
			long startTime = System.currentTimeMillis();

			logger.debug("Analyzing Getter/Setter Dependencies");

			for (AttributeOfStructureVariableNode var : attributes) {
				ExternalLogger.log(externalLogger, "Finding getter/setter assessor of attribute " + var.getName());
				new SetterGetterDependencyGeneration().dependencyGeneration(var);
			}

			TimeTracker.add("Get/set", System.currentTimeMillis() - startTime);
		}

//		es.shutdown();
		ExternalLogger.progress(externalLogger, _resolveDependTaskId, "Resolving dependencies", taskNum, taskNum);

		return root;
	}

	private IASTTranslationUnit expandWholeProject(CppConfig cppConfig, List<QtHeaderNode> qtHeaderNodes, List<ISourcecodeFileNode> sourcecodeFileNodes) throws CoreException, InterruptedException {
		logger.debug("Expanding Tree Up to Method Level");

		ExternalLogger.progress(externalLogger, _expandTaskId, "Parsing project", -1, 1);

		Map<String, String> realPredefinedMacros = new HashMap<>(defines);
		realPredefinedMacros.putAll(cppConfig.getMacros());

		List<String> includePaths = considerFolders.stream().map(File::getAbsolutePath).collect(Collectors.toList());
		includePaths.addAll(Arrays.asList(cppConfig.getIncludePaths()));
		String[] includePathList = includePaths.toArray(String[]::new);
		String[] projectFileList = considerFiles.stream().map(File::getAbsolutePath).toArray(String[]::new);

		String[] macroCodes = null;
		if (qtHeaderNodes != null) {
			List<String> macroList = qtHeaderNodes.stream().map(Node::getAbsolutePath).collect(Collectors.toList());
			macroCodes = macroList.toArray(String[]::new);
		}

		final IScannerInfo scannerInfo = new ExtendedScannerInfo(realPredefinedMacros, includePathList, macroCodes, projectFileList);

		IASTTranslationUnit translationUnit = GPPLanguage.getDefault()
				.getASTTranslationUnit(EMPTY_FILE, scannerInfo, CONTENT_PROVIDER, null,
						ILanguage.OPTION_IS_SOURCE_UNIT, LOG_SERVICE);

		final int sourceSize = sourcecodeFileNodes.size();

		ExternalLogger.registerTask(externalLogger, _expandTaskId, "Parsing project", sourceSize);

		List<Callable<Void>> expandTasks = new ArrayList<>();
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger parseProgress = new AtomicInteger();

		IASTDeclaration[] declarations = translationUnit.getDeclarations();
		final int declarationLength = declarations.length;
		for (IASTDeclaration declaration : declarations) {
			String filename = declaration.getFileLocation().getFileName();
			ISourcecodeFileNode searchedRoot = sourceCache.get(filename);
			if (searchedRoot == null) {
				searchedRoot = sourcecodeFileNodes.stream()
						.filter(f -> PathUtils.equals(f.getAbsolutePath(), filename))
						.findFirst()
						.orElse(null);

				if (searchedRoot != null)
					sourceCache.put(filename, searchedRoot);
				else {
					continue;
				}
			}

			final INode expandRoot = searchedRoot;

			// if the source code file is not expanded to method level
			Callable<Void> expandTask = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					int index = counter.incrementAndGet();
					logger.debug(String.format("[%d/%d] ", index, declarationLength) + "Expanding " + IdMapping.getInstance().getOrCreate(expandRoot.getAbsolutePath()));

					SourcecodeFileExpander srcParser = new SourcecodeFileExpander();
					srcParser.setRoot(expandRoot);

					try {
						srcParser.expand(declaration);
						ExternalLogger.log(externalLogger, "Expanding source code file " + expandRoot.getName() + " successfully");
					} catch (Exception ex) {
						logger.error("Cant parse file " + IdMapping.getInstance().getOrCreate(expandRoot.getName()), ex);
					}

					ExternalLogger.progress(externalLogger, _expandTaskId, "Parsing project", parseProgress.incrementAndGet(), sourceSize);
					return null;
				}
			};

			expandTasks.add(expandTask);
		}

		es.invokeAll(expandTasks);

		ExternalLogger.progress(externalLogger, _expandTaskId, "Parsing project", sourceSize, sourceSize);

		return translationUnit;
	}

	private List<QtHeaderNode> preprocessQtHeaders(CppConfig cppConfig, List<ISourcecodeFileNode> sourcecodeFileNodes) throws InterruptedException {
		logger.debug("Preprocessing Qt Headers");

		Map<String, QtHeaderNode> qtCache = new ConcurrentHashMap<>();

		List<Callable<Void>> preprocessQtTasks = new ArrayList<>();

		AtomicInteger counter = new AtomicInteger();
		AtomicInteger qtProgress = new AtomicInteger();

		final int sourceSize = sourcecodeFileNodes.size();

		for (ISourcecodeFileNode sourceNode : sourcecodeFileNodes) {
			Callable<Void> preprocessQtTask = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					int index = counter.incrementAndGet();
					logger.debug(String.format("[%d/%d] ", index, sourceSize) + "Preprocessing Qt of " + IdMapping.getInstance().getOrCreate(sourceNode.getAbsolutePath()));
					IncludeQtHeaderPreprocessor gen = new IncludeQtHeaderPreprocessor(qtDir, qtCache);
					gen.setDefines(cppConfig.getMacros());
					gen.dependencyGeneration(sourceNode);
					ExternalLogger.log(externalLogger, "Finish preprocessing Qt of " + sourceNode.getName());
					ExternalLogger.progress(externalLogger, _preprocessQtTaskId, "Preprocessing Qt", qtProgress.incrementAndGet(), sourceSize);
					return null;
				}
			};
			preprocessQtTasks.add(preprocessQtTask);
		}

		es.invokeAll(preprocessQtTasks);

		ExternalLogger.progress(externalLogger, _preprocessQtTaskId, "Preprocessing Qt", sourceSize, sourceSize);

		return qtCache.values().stream().distinct().collect(Collectors.toList());
	}

	public void disableAnalyzeDependencies() {
		this.optAnalyzeRealParent = false;
		this.optAnalyzeExtendDependencies = false;
		this.optAnalyzeGetSetDependencies = false;
		this.optAnalyzeCallDependencies = false;
		this.optAnalyzeGlobalVarUsed = false;
		this.optAnalyzeTypedefDependencies = false;
		this.optAnalyzeDefinitionDependencies = false;
	}

	public void setIgnoreFolders(List<File> ignoreFolders) {
		this.ignoreFolders = ignoreFolders;
	}

	public void setConsiderFiles(List<File> considerFiles) {
		this.considerFiles = considerFiles;
	}

	public void setConsiderFolders(List<File> considerFolders) {
		this.considerFolders = considerFolders;
	}

	public void setQtDir(File qtDir) {
		this.qtDir = qtDir;
	}

	public void setDefines(Map<String, String> defines) {
		this.defines = defines;
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}

	public CppConfig getCppConfig() {
		return cppConfig;
	}

	public List<QtHeaderNode> getQtHeaderNodes() {
		return qtHeaderNodes;
	}

	public File getQtDir() {
		return qtDir;
	}

	public List<File> getConsiderFiles() {
		return considerFiles;
	}

	public List<File> getConsiderFolders() {
		return considerFolders;
	}

	public Map<String, String> getDefines() {
		return defines;
	}

	static final class ContentProvider extends InternalFileContentProvider {
		private @Nullable InternalFileContent fileContent(@NotNull String path) {
			if (!getInclusionExists(path)) return null;
			return (InternalFileContent) FileContent.createForExternalFileLocation(path);
		}

		@Override
		public @Nullable InternalFileContent getContentForInclusion(@NotNull String path,
				@Nullable IMacroDictionary dictionary) {
			return fileContent(path);
		}

		@Override
		public @Nullable InternalFileContent getContentForInclusion(@NotNull IIndexFileLocation location,
				@Nullable String astPath) {
			final String path = location.getFullPath();
			return path == null ? null : fileContent(path);
		}
	}
}