package uet.fit.server.rest.service;

import com.google.gson.GsonBuilder;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.env.build_result.Execution;
import uet.fit.aut.logger.TimeTracker;
import uet.fit.aut.config.BuildConfig;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.coverage.CoverageData;
import uet.fit.aut.coverage.CoverageManager;
import uet.fit.aut.parser.dependency.FunctionCallDependencyGeneration;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.Search2;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.execution.result_trace.AssertResultImporter;
import uet.fit.aut.execution.result_trace.AssertionResult;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestCaseDataExporter;
import uet.fit.aut.testcase.TestCaseDataImporter;
import uet.fit.aut.testcase.TestCaseManager;
import uet.fit.aut.testdata.InputCellHandler;
import uet.fit.aut.testdata.comparable.AssertMethod;
import uet.fit.aut.testdata.object.EnumDataNode;
import uet.fit.aut.testdata.object.GlobalRootDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.NormalDataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.thread.task.ExecuteTestTask;
import uet.fit.aut.thread.task.GenerateTestdataTask;
import uet.fit.aut.util.DateTimeUtils;
import uet.fit.aut.util.Utils;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.AutoGenDTO;
import uet.fit.dto.test.CreateTestDTO;
import uet.fit.dto.test.DeletedTestEntry;
import uet.fit.dto.test.DeletedTestsDTO;
import uet.fit.dto.test.DuplicateTestDTO;
import uet.fit.dto.test.ExportListDTO;
import uet.fit.dto.test.ExportTestDTO;
import uet.fit.dto.test.GenResultDTO;
import uet.fit.dto.test.ImportTestDTO;
import uet.fit.dto.test.ModifyTestDTO;
import uet.fit.dto.test.MultiTestResultDTO;
import uet.fit.dto.test.SingleTestResultDTO;
import uet.fit.dto.test.TestCaseFileDTO;
import uet.fit.dto.test.TestDataImportDTO;
import uet.fit.dto.test.TestIdsDTO;
import uet.fit.dto.test.TestListDTO;
import uet.fit.dto.test.TestResultDTO;
import uet.fit.dto.test.TestRow;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.server.DAO.IEnvironmentDAO;
import uet.fit.server.DAO.ITestCaseDAO;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.entity.TestCaseEntity;
import uet.fit.server.DAO.impl.EnvironmentDAO;
import uet.fit.server.DAO.impl.TestCaseDAO;
import uet.fit.server.exception.DataNodeNotFoundException;
import uet.fit.server.exception.GenerateTestFailure;
import uet.fit.server.exception.InvalidTestChangeException;
import uet.fit.server.exception.PermissionDenyException;
import uet.fit.server.logger.CoreLogger;
import uet.fit.server.logger.ServerLogger;
import uet.fit.server.resource_manager.CacheData;
import uet.fit.server.resource_manager.CacheManager;
import uet.fit.server.util.TestDataMapping;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static uet.fit.aut.thread.task.ExecuteTestTask.LAST_VERSION_NAME;
import static uet.fit.aut.thread.task.ExecuteTestTask.TEST_PATH_NAME;

public class TestCaseService {

	private static final Logger logger = LoggerFactory.getLogger(TestCaseService.class);

	private static final String TEST_CASE_DIR = "testcases";

	private final ITestCaseDAO testDao;
	private final IEnvironmentDAO envDao;

	public TestCaseService() {
		this.testDao = new TestCaseDAO();
		this.envDao = new EnvironmentDAO();
	}

	public boolean isExistTest(@NotNull String environment, @NotNull String uutPath,
			@NotNull String sutName, @NotNull String testName) throws Exception {
		EnvironmentEntity environmentEntity = envDao.getByKey(environment);

		IFunctionNode sut = findSut(environmentEntity.getProFile(), uutPath, sutName);
		String sutPath = sut.getAbsolutePath();

		return !testDao.searchByName(environment, sutPath, testName).isEmpty();
	}



	@NotNull
	public TestDataDTO modifyTest(@NotNull ModifyTestDTO modifyDTO) throws Exception {
		String testId = modifyDTO.getTestCaseId();
		TestCaseEntity tcEntity = testDao.getByKey(testId);

		// check permission
		String owner = tcEntity.getOwner();
		String requestedUser = modifyDTO.getUser();
		if (!owner.equals(requestedUser)) {
			ServerLogger.error(modifyDTO.getUser(), LogDTO.Position.TEST_GENERAL, "You try to modify a resource belong to " + owner);
			throw new PermissionDenyException(requestedUser, owner);
		}

		// get project root from cache
		EnvironmentEntity environmentEntity = envDao.getByKey(tcEntity.getEnv());
		CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		// map entity to model
		TestCase testCase = entityToModel(tcEntity, root);

		// import test data from json
		TestCaseDataImporter importer = new TestCaseDataImporter(root);
		importer.setTestCase(testCase);
		String json = Utils.readFileContent(tcEntity.getTestData());
		RootDataNode rootDataNode = importer.importRootDataNode(json);

		// find node to modify
		String nodePath = modifyDTO.getPath();
		String[] pathItems = nodePath.split(ITestNode.PATH_DELIMITER);
		pathItems = removeFirstItem(pathItems);
		IDataNode node = findDataNode(rootDataNode, pathItems);

		// commit new value
		if (node instanceof ValueDataNode) {
			ValueDataNode valueNode = (ValueDataNode) node;
			ModifyTestDTO.ChangeType changeType = modifyDTO.getChangeType();
			String newValue = modifyDTO.getValue();

			handleNewValue(testCase, valueNode, changeType, newValue);

			ServerLogger.info(modifyDTO.getUser(), LogDTO.Position.TEST_GENERAL, "Modify " + testCase.getName() + " successfully");

			// save new test data to file
			testCase.setRootDataNode(rootDataNode);
			saveTestCaseToFile(testCase);
		}

		// delete old result
		deleteOldTestFiles(requestedUser, testId);

		// update last modified time
		TestCaseEntity updatedEntity = new TestCaseEntity();
		updatedEntity.setId(testId);
		updatedEntity.setLastModified(LocalDateTime.now().toString());
		testDao.update(updatedEntity);

		// return new test data DTO
		TestDataDTO testDataDTO = TestDataMapping.toDTO(testCase.getRootDataNode());
		testDataDTO.setId(tcEntity.getId());
		testDataDTO.setTestCaseName(tcEntity.getName());
		testDataDTO.setEditable(true);
		testDataDTO.setUser(tcEntity.getOwner());

		return testDataDTO;
	}

	private void handleNewValue(TestCase testCase, ValueDataNode valueNode,
			ModifyTestDTO.ChangeType changeType, String newValue) throws Exception {

		switch (changeType) {
			case ENTER:
				InputCellHandler handler = new InputCellHandler();
				handler.setTestCase(testCase);
				handler.setInAutoGenMode(false);
				handler.commitEdit(valueNode, newValue);

				if (valueNode.isExpected()) {
					if (valueNode instanceof NormalDataNode || valueNode instanceof EnumDataNode) {
						valueNode.getIterators()
								.forEach(i -> i.getDataNode().setAssertMethod(AssertMethod.ASSERT_EQUAL));
						ValueDataNode actualNode = Search2.getActualValue(valueNode);
						if (actualNode != null)
							actualNode.getIterators()
									.forEach(i -> i.getDataNode().setAssertMethod(AssertMethod.ASSERT_EQUAL));
					}
				}
				break;

			case ASSERT:
				ValueDataNode expectedNode = Search2.getExpectedValue(valueNode);
				if (expectedNode != null) {
					expectedNode.setAssertMethod(newValue);
				}
				valueNode.setAssertMethod(newValue);
				break;

			default:
				throw new InvalidTestChangeException(changeType);
		}
	}

	private void deleteOldTestFiles(String user, String id) {
		String testCaseFolderPath = FolderConfig.load().getWorkspace() + File.separator + user + File.separator + id;
		File testCaseFolder = new File(testCaseFolderPath);
		File[] children = testCaseFolder.listFiles();
		if (testCaseFolder.isDirectory() && children != null) {
			for (File child : children) {
				if (!child.getName().equals(LAST_VERSION_NAME))
					Utils.deleteFileOrFolder(child);
			}
		}
	}

	@NotNull
	public TestDataDTO insertTestCase(@NotNull CreateTestDTO createTestDTO) throws Exception {
		String environment = createTestDTO.getEnvironment();

		EnvironmentEntity environmentEntity = envDao.getByKey(environment);

		// Create new testcase
		String uutPath = createTestDTO.getUut();
		String sutName = createTestDTO.getSut();
		String testName = createTestDTO.getName();
		final String user = createTestDTO.getUser();
		TestCase testCase = createNewTestCase(uutPath, sutName, testName, environmentEntity);

		String testId = UUID.randomUUID().toString();

		// Save testcase to file
		// location {environments.dir}/{environment.name}/testcases/{test.id}.json
		String environmentPath = environmentEntity.getPath();
		String path = environmentPath + File.separator + TEST_CASE_DIR + File.separator + testId + ".json";
		testCase.setPath(path);
		saveTestCaseToFile(testCase);
		logger.debug("Test case: " + testCase);
		ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Created new test case: " + testCase + " for " + sutName);
		// Save testcase to database
		ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Saving test case " + testCase + " to database");
		TestCaseEntity testCaseEntity = mapModelToEntity(environment, testCase, testId, user, -1);
		testDao.insert(testCaseEntity);

		// Map testcase to TestDataDTO
		TestDataDTO testDataDTO = TestDataMapping.toDTO(testCase.getRootDataNode());
		testDataDTO.setId(testId);
		testDataDTO.setTestCaseName(testCase.getName());
		testDataDTO.setEditable(true);
		testDataDTO.setUser(testCaseEntity.getOwner());

		return testDataDTO;
	}

	private TestCaseEntity mapModelToEntity(String environment, TestCase testCase, String testId, String user, float coverage) {
		int pass = 0, total = 0;
		return new TestCaseEntity(testId, testCase.getName(), testCase.getStatus(), coverage,
				testCase.getPath(), environment, user, testCase.getFunctionNode().getAbsolutePath(),
				testCase.getCreationDateTime().toString(), testCase.getCreationDateTime().toString(), pass, total);
	}

	@NotNull
	private TestCase createNewTestCase(@NotNull String uutPath, @NotNull String sutName, @NotNull String testName,
			@NotNull EnvironmentEntity environmentEntity) {
		IFunctionNode sut = findSut(environmentEntity.getProFile(), uutPath, sutName);
//		System.out.println("uut + " + uutPath);
//		System.out.println("sut+ " +sutName);
//		System.out.println("env+" + environmentEntity);


		TestCase tc;
		if (!testName.isEmpty()) {
			tc = TestCaseManager.getInstance().createTestCase(sut, testName);
		} else {
			tc = TestCaseManager.getInstance().createTestCase(sut);
		}
		return tc;
	}

	private synchronized void saveTestCaseToFile(@NotNull TestCase tc) {
		Utils.writeContentToFile(new TestCaseDataExporter().exportDataNode(tc), tc.getPath());
	}

	private void copyTestCaseToFile(@NotNull TestCaseEntity tcEntity,@NotNull TestCase testCase) {
		String content = Utils.readFileContent(new File(tcEntity.getTestData()));
		Utils.writeContentToFile(content, testCase.getPath());
	}

	@NotNull
	protected IFunctionNode findSut(@NotNull String profile, @NotNull String uut, @NotNull String sutName) {
		CacheData data = CacheManager.getInstance().get(profile);
		ProjectNode root = data.getProjectNode();
		INode sourceNode = Search.searchNodes(root, new SourcecodeFileNodeCondition(), uut).get(0);
		IFunctionNode sut = Search.searchNodes(sourceNode, new FunctionNodeCondition())
				.stream()
				.filter(f -> f.getName().equals(sutName))
				.map(f -> (IFunctionNode) f)
				.collect(Collectors.toList())
				.get(0);

		new FunctionCallDependencyGeneration().dependencyGeneration(sut);

		return sut;
	}

	private List<IFunctionNode> findSuts(@NotNull String profile, @NotNull String uut) {
		CacheData data = CacheManager.getInstance().get(profile);
		ProjectNode root = data.getProjectNode();
		INode sourceNode = Search.searchNodes(root, new SourcecodeFileNodeCondition(), uut).get(0);
		return Search.searchNodes(sourceNode, new FunctionNodeCondition())
				.stream()
				.map(f -> (IFunctionNode) f)
				.collect(Collectors.toList());
	}

	@NotNull
	private IDataNode findDataNode(@NotNull IDataNode root, @NotNull String[] path) throws DataNodeNotFoundException {
		if (path.length == 0)
			return root;

		List<IDataNode> children = new ArrayList<>();
		if (path[0].equals(ITestNode.EXPECT)) {
			if (root instanceof GlobalRootDataNode)
				children.addAll(((GlobalRootDataNode) root).getGlobalInputExpOutputMap().values());
			else if (root instanceof SubprogramNode)
				children.addAll(((SubprogramNode) root).getInputToExpectedOutputMap().values());
			else
				children.addAll(root.getChildren());
			path = removeFirstItem(path);
		} else {
			children.addAll(root.getChildren());
		}

		for (IDataNode child : children) {
			if (path[0].equals(child.getDisplayName())) {
				String[] newPath = removeFirstItem(path);
				return findDataNode(child, newPath);
			}
		}

		String relativePath = String.join(ITestNode.PATH_DELIMITER, path);
		throw new DataNodeNotFoundException(root, relativePath);
	}

	private String[] removeFirstItem(String[] items) {
		String[] newItems = new String[items.length - 1];
		System.arraycopy(items, 1, newItems, 0, items.length - 1);
		return newItems;
	}

	public TestCase entityToModel(TestCaseEntity testCaseEntity, ProjectNode root) {
		String sutPath = testCaseEntity.getFunction();
		IFunctionNode func = (IFunctionNode) Search.searchNodes(root, new FunctionNodeCondition(), sutPath).get(0);

		TestCase testCase = new TestCase(func, testCaseEntity.getName());

		testCase.setId(testCaseEntity.getId());
		testCase.setStatus(testCaseEntity.getStatus());
		testCase.setPath(testCaseEntity.getTestData());

		LocalDateTime createdDateTime = DateTimeUtils.parse(testCaseEntity.getCreatedTime());
		testCase.setCreationDateTime(createdDateTime);

		LocalDateTime lastModifiedTime = DateTimeUtils.parse(testCaseEntity.getLastModified());
		testCase.setLastModifiedTime(lastModifiedTime);

		return testCase;
	}

	@NotNull
	public TestListDTO listTest(@NotNull String env, @NotNull String uut, @NotNull String sut) throws Exception {
		// get project root from cache
		EnvironmentEntity environmentEntity = envDao.getByKey(env);
		String profile = environmentEntity.getProFile();

		// find uut
		IFunctionNode function = findSut(profile, uut, sut);
		String functionPath = function.getAbsolutePath();

		List<TestCaseEntity> testcases = testDao.getAllByFuncAndEnv(functionPath, env);
		List<TestRow> testRows = mapListEntityToDTO(testcases);
		return new TestListDTO(uut, sut, testRows);
	}

	@NotNull
	public TestDataDTO viewTest(@NotNull String id, @NotNull String user) throws Exception {
		TestCaseEntity testCaseEntity = testDao.getByKey(id);

		// get project root from cache
		EnvironmentEntity environmentEntity = envDao.getByKey(testCaseEntity.getEnv());
		CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		// import test data from json
		TestCaseDataImporter importer = new TestCaseDataImporter(root);
		String json = Utils.readFileContent(testCaseEntity.getTestData());
		RootDataNode rootDataNode = importer.importRootDataNode(json);

		// Map testcase to TestDataDTO
		TestDataDTO testDataDTO = TestDataMapping.toDTO(rootDataNode);
		testDataDTO.setId(testCaseEntity.getId());
		testDataDTO.setTestCaseName(testCaseEntity.getName());
		// check editable
		testDataDTO.setEditable(user.equals(testCaseEntity.getOwner()));

		return testDataDTO;
	}

	@NotNull
	private List<TestRow> mapListEntityToDTO(@NotNull List<TestCaseEntity> entities) {
		List<TestRow> testRows = new ArrayList<>();
		for (TestCaseEntity entity : entities) {
			String id = entity.getId();
			String name = entity.getName();
			String status = entity.getStatus();
			int pass = entity.getPass();
			int total = entity.getTotal();
			if (total != 0)
				status = String.format("%d/%d", pass, total);
			float coverage = entity.getCoverage();
			String owner = entity.getOwner();
			TestRow testRow = new TestRow(id, name, status, coverage, owner, entity.getCreatedTime());
			testRows.add(testRow);
		}
		return testRows;
	}

	@NotNull
	public DeletedTestsDTO deleteTestCases(@NotNull TestIdsDTO testListDTO) {
		String username = testListDTO.getUser();

		List<DeletedTestEntry> testList = new ArrayList<>();
		for (String id : testListDTO.getIds()) {
			DeletedTestEntry deletedTestEntry;

			try {
				TestCaseEntity testCaseEntity = testDao.getByKey(id);
				String testName = testCaseEntity.getName();
				if (testCaseEntity.getOwner().equals(username)) {
					testDao.delete(id);
					deletedTestEntry = new DeletedTestEntry(id, DeletedTestEntry.Status.SUCCESS);
					ServerLogger.info(username, LogDTO.Position.TEST_GENERAL, "Deleted test case " + testName);
				} else {
					deletedTestEntry = new DeletedTestEntry(id, DeletedTestEntry.Status.PERMISSION_DENY);
					ServerLogger.info(username, LogDTO.Position.TEST_GENERAL, "You don't have permission to delete " + testName);
				}
			} catch (Exception e) {
				logger.error("Can't delete test case " + id, e);
				ServerLogger.error(username, LogDTO.Position.TEST_GENERAL, "Can't delete test case: " + e.getMessage());
				deletedTestEntry = new DeletedTestEntry(id, DeletedTestEntry.Status.FAILED);
			}

			testList.add(deletedTestEntry);
		}

		return new DeletedTestsDTO(testList);
	}

	public TestDataDTO duplicateTest(DuplicateTestDTO dto) throws Exception {
		TestCaseEntity tcEntity = testDao.getByKey(dto.getTestCaseId());

		String environment = tcEntity.getEnv();
		EnvironmentEntity environmentEntity = envDao.getByKey(environment);

		// Create new testcase
		String newId = UUID.randomUUID().toString();
		ServerLogger.debug(dto.getUser(), LogDTO.Position.TEST_GENERAL, "Duplicate new test case from selected");
		TestCase newTestCase = createNewTestCase(dto.getUut(), dto.getSut(), dto.getName(), environmentEntity);

		// Save testcase to file
		// location {environments.dir}/{environment.name}/testcases/{test.id}.json
		String environmentPath = environmentEntity.getPath();
		String path = environmentPath + File.separator + TEST_CASE_DIR + File.separator + newId + ".json";
		newTestCase.setPath(path);

		copyTestCaseToFile(tcEntity, newTestCase);
		logger.debug("Duplicated test case: " + newTestCase);
		// Save testcase to database
		ServerLogger.debug(dto.getUser(), LogDTO.Position.TEST_GENERAL, "Save duplicated test case: " + newTestCase + " to database");
		TestCaseEntity testCaseEntity = mapModelToEntity(environment, newTestCase, newId, dto.getUser(), -1);
		testDao.insert(testCaseEntity);

		// get project root from cache
		CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		// import test data from json
		TestCaseDataImporter importer = new TestCaseDataImporter(root);
		String json = Utils.readFileContent(testCaseEntity.getTestData());
		RootDataNode rootDataNode = importer.importRootDataNode(json);

		// return dto
		// Map testcase to TestDataDTO
		TestDataDTO testDataDTO = TestDataMapping.toDTO(rootDataNode);
		testDataDTO.setId(testCaseEntity.getId());
		testDataDTO.setTestCaseName(testCaseEntity.getName());
		testDataDTO.setEditable(true);
		testDataDTO.setUser(testCaseEntity.getOwner());
		ServerLogger.info(dto.getUser(), LogDTO.Position.TEST_GENERAL, "Duplicate test case successfully");
		return testDataDTO;
	}

	public void exportTestCase(ExportListDTO exportListDTO) throws Exception {
		for(ExportTestDTO exportTestDTO : exportListDTO.getExportTestList()) {
			TestCaseEntity tcEntity = testDao.getByKey(exportTestDTO.getTestCaseId());

			String environment = tcEntity.getEnv();
			String testData = Utils.readFileContent(tcEntity.getTestData());

			TestCaseFileDTO testCase = new TestCaseFileDTO();
			testCase.setName(exportTestDTO.getTestCaseName());
			testCase.setSut(exportTestDTO.getSut());
			testCase.setUut(exportTestDTO.getUut());
			testCase.setOldEnvironment(environment);
			testCase.setTestData(testData);

			String json = new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(testCase);

			String filePath = exportTestDTO.getFilePath() + "/" + exportTestDTO.getTestCaseName() + ".json";
			File testCaseFile = new File(filePath);
			testCaseFile.createNewFile();
			Utils.writeContentToFile(json, filePath);
		}
	}

	public TestDataImportDTO importTestCase(ImportTestDTO importTestDTO, TestCaseFileDTO testCaseFileDTO) throws Exception {
		String environment = importTestDTO.getEnv();
		EnvironmentEntity envEntity = envDao.getByKey(environment);
		EnvironmentEntity oldEnvEntity = envDao.getByKey(testCaseFileDTO.getOldEnvironment());
		Random random = new Random();

		// Create new testcase
		String newId = UUID.randomUUID().toString();
		String randomName = testCaseFileDTO.getName() + random.nextInt(99999);
		TestCase newTestCase = createNewTestCase(testCaseFileDTO.getUut(), testCaseFileDTO.getSut(), randomName, envEntity);

		// Save testcase to file
		// location {environments.dir}/{environment.name}/testcases/{test.id}.json
		String environmentPath = envEntity.getPath();
		String path = environmentPath + File.separator + TEST_CASE_DIR + File.separator + newId + ".json";
		newTestCase.setPath(path);

		String content = testCaseFileDTO.getTestData();
		String newContent = content.replace(oldEnvEntity.getCommit(), envEntity.getCommit());
		Utils.writeContentToFile(newContent, newTestCase.getPath());

		// Save testcase to database
		TestCaseEntity testCaseEntity = mapModelToEntity(environment, newTestCase, newId, importTestDTO.getUser(), -1);
		testDao.insert(testCaseEntity);

		// get project root from cache
		CacheData data = CacheManager.getInstance().get(envEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		// import test data from json
		TestCaseDataImporter importer = new TestCaseDataImporter(root);
		String json = Utils.readFileContent(testCaseEntity.getTestData());
		RootDataNode rootDataNode = importer.importRootDataNode(json);

		// return dto
		// Map testcase to TestDataDTO
		TestDataDTO testDataDTO = TestDataMapping.toDTO(rootDataNode);
		testDataDTO.setId(testCaseEntity.getId());
		testDataDTO.setTestCaseName(testCaseEntity.getName());
		testDataDTO.setEditable(true);
		testDataDTO.setUser(testCaseEntity.getOwner());
		ServerLogger.info(importTestDTO.getUser(), LogDTO.Position.TEST_GENERAL, "Import test case " + testCaseEntity.getName() + " successfully");

		return new TestDataImportDTO(testCaseFileDTO.getSut(), testCaseFileDTO.getUut(), newId, testCaseEntity.getName());
	}

	public TestListDTO getAllTests(@NotNull String proFile, @NotNull String uutPath, @NotNull String sutName) throws Exception {
		IFunctionNode sut = findSut(proFile, uutPath, sutName);
		String sutPath = sut.getAbsolutePath();

		// get test case from db
		List<TestCaseEntity> testCaseEntities = testDao.searchByFunc(sutPath);

		TestListDTO testList = new TestListDTO();
		testList.setUut(uutPath);
		testList.setSut(sutName);
		List<TestRow> testRows = mapListEntityToDTO(testCaseEntities);
		testList.setList(testRows);
		return testList;
	}

	public TestListDTO getAllTests(@NotNull String env) throws Exception {
		List<TestCaseEntity> testCaseEntities = testDao.getAllByEnv(env);

		TestListDTO testList = new TestListDTO();
		List<TestRow> testRows = mapListEntityToDTO(testCaseEntities);
		testList.setList(testRows);
		return testList;
	}

	/**
	 * Retrieve test data, run test case and update into database
	 * @param user - requested username
	 * @param id - test case id
	 * @param buildConfig - build configuration
	 * @param folderConfig - folder locations
	 * @return execution wrapper {test case, coverage, coverage type}
	 */
	private Execution runSingleTest(@NotNull String user, @NotNull String id,
			@NotNull BuildConfig buildConfig, @NotNull FolderConfig folderConfig) throws Exception {

		final String taskId = UUID.randomUUID().toString();
		final int taskNum = 4;

		try {
			ServerLogger.progress(user, taskId, "Preparing", 0, taskNum);
			ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Loading test case data");
			TestCaseEntity testCaseEntity = testDao.getByKey(id);

			ServerLogger.progress(user, taskId, "Loading " + testCaseEntity.getName(), 1, taskNum);

			// get project root from cache
			EnvironmentEntity environmentEntity = envDao.getByKey(testCaseEntity.getEnv());
			CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
			ProjectNode root = data.getProjectNode();

			// map entity to model
			TestCase testCase = entityToModel(testCaseEntity, root);
			// import test data from json
			TestCaseDataImporter importer = new TestCaseDataImporter(root);
			importer.setTestCase(testCase);
			String json = Utils.readFileContent(testCaseEntity.getTestData());
			RootDataNode rootDataNode = importer.importRootDataNode(json);
			testCase.setRootDataNode(rootDataNode);

			ServerLogger.progress(user, taskId, "Running " + testCase.getName(), 2, taskNum);

			// execute test
			String workspace = folderConfig.getWorkspace() + File.separator + user;
			String environment = environmentEntity.getPath();

			ProjectConfig projectConfig = data.getProjectConfig();
			ExecuteTestTask executeTestTask = new ExecuteTestTask(buildConfig, environment, workspace, projectConfig, testCase);
			executeTestTask.setGeneralLogger(new TestGeneralCoreLogger(user));
			executeTestTask.setExternalLogger(new TestBuildCoreLogger(user));
			Execution execution = executeTestTask.run();

			ServerLogger.progress(user, taskId, "Processing results", 3, taskNum);

			// compute coverage
			ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Compute test result of " + testCase.getName());

			long startTime = System.currentTimeMillis();

			String coverageType = environmentEntity.getCoverageType();
			float coverageProgress = CoverageManager.getCoverageAtFunctionLevel(coverageType, testCase);

			// compute status
			AssertResultImporter assertResultImporter = new AssertResultImporter(testCase);
			AssertionResult assertionResult = assertResultImporter.generate();
//			testCase.setExecutionResult(assertionResult);

			// update db status, coverage...
			TestCaseEntity updated = new TestCaseEntity();
			updated.setId(id);
			updated.setCoverage(coverageProgress);
			updated.setStatus(testCase.getStatus());
			updated.setTotal(assertionResult.getTotal());
			updated.setPass(assertionResult.getPass());
			testDao.update(updated);

			execution.setCoverage(coverageType);
			execution.setAssertionResult(assertionResult);

			TimeTracker.add("Gen report", System.currentTimeMillis() - startTime);

			return execution;

		} finally {
			ServerLogger.progress(user, taskId, "Running completed", taskNum, taskNum);
		}
	}

	public SingleTestResultDTO runTest(@NotNull String id, @NotNull String user) throws Exception {
		BuildConfig buildConfig = BuildConfig.load();
		FolderConfig folderConfig = FolderConfig.load();

		Execution singleExecution = runSingleTest(user, id, buildConfig, folderConfig);

		TestCase testCase = singleExecution.getTestCase();
		AssertionResult assertionResult = singleExecution.getAssertionResult();

		// return dto
		long startTime = System.currentTimeMillis();

		String title = "Execution Result - " + testCase.getName();
		String taskTitle = "Generating " + title;
		String taskId = UUID.randomUUID().toString();
		ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, taskTitle);
		ServerLogger.progress(user, taskId, taskTitle, -1, 1);

		String coverageType = singleExecution.getCoverage();
		float coverageProgress = CoverageManager.getCoverageAtFunctionLevel(coverageType, testCase);

		String status = assertionResult.getTotal() != 0
				? String.format("%d/%d", assertionResult.getPass(), assertionResult.getTotal())
				: testCase.getStatus();
		TestResultDTO.Entry entry = new TestResultDTO.Entry(id, status, coverageProgress);

		// generate highlight report
		String coverageHighlight = CoverageManager.highlightCoverage(coverageType, testCase);
		ServerLogger.progress(user, taskId, taskTitle, 1, 1);
		ReportDTO reportDTO = new ReportDTO(title, coverageHighlight);

		TimeTracker.add("Gen report", System.currentTimeMillis() - startTime);

		return new SingleTestResultDTO(entry, coverageProgress, reportDTO);
	}

	public SingleTestResultDTO runRegressionTest(@NotNull String project, @NotNull String regressionUser, @NotNull TestCaseEntity testCaseEntity,
			@NotNull EnvironmentEntity env, @NotNull BuildConfig buildConfig, @NotNull FolderConfig folderConfig, @NotNull CacheData data) throws Exception{
		final String taskId = UUID.randomUUID().toString();
		final int taskNum = 4;

		try {
			// get project root from cache
			ProjectNode root = data.getProjectNode();

			// map entity to model
			TestCase testCase = entityToModel(testCaseEntity, root);

			// import test data from json
			TestCaseDataImporter importer = new TestCaseDataImporter(root);
			importer.setTestCase(testCase);
			String json = Utils.readFileContent(testCaseEntity.getTestData());
			RootDataNode rootDataNode = importer.importRootDataNode(json);
			testCase.setRootDataNode(rootDataNode);

			// execute test
			String workspace = folderConfig.getWorkspace() + File.separator + regressionUser;
			String environment = folderConfig.getEnvironment() + File.separator + regressionUser + File.separator + project;

			ProjectConfig projectConfig = data.getProjectConfig();
			ExecuteTestTask executeTestTask = new ExecuteTestTask(buildConfig, environment, workspace, projectConfig, testCase);
			executeTestTask.setGeneralLogger(new TestGeneralCoreLogger(regressionUser));
			executeTestTask.setExternalLogger(new TestBuildCoreLogger(regressionUser));
			Execution execution = executeTestTask.run();

			String coverageType = env.getCoverageType();
			float coverageProgress = CoverageManager.getCoverageAtFunctionLevel(coverageType, testCase);

			// compute status
			AssertResultImporter assertResultImporter = new AssertResultImporter(testCase);
			AssertionResult assertionResult = assertResultImporter.generate();

			// return dto
			String title = "Execution Result - " + testCase.getName();

			String status = String.format("%d/%d", assertionResult.getPass(), assertionResult.getTotal());
			TestResultDTO.Entry entry = new TestResultDTO.Entry(testCaseEntity.getId(), status, coverageProgress);

			// generate highlight report
			String coverageHighlight = CoverageManager.highlightCoverage(coverageType, testCase);
			ReportDTO reportDTO = new ReportDTO(title, coverageHighlight);

			return new SingleTestResultDTO(entry, coverageProgress, reportDTO);
		} finally {
			ServerLogger.progress(regressionUser, taskId, "Running completed", taskNum, taskNum);
		}
	}

	public GenResultDTO autoGenForSubprogram(AutoGenDTO dto, HashMap<String, List<String>> userCode) throws Exception {
		// get project config, sut and build config
		String environment = dto.getEnvironment();

		EnvironmentEntity environmentEntity = envDao.getByKey(environment);
		CacheData cacheData = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectConfig projectConfig = cacheData.getProjectConfig();

		IFunctionNode sut = findSut(environmentEntity.getProFile(), dto.getUut(), dto.getSut());

		BuildConfig buildConfig = BuildConfig.load();
		FolderConfig folderConfig = FolderConfig.load();
		String workspacePath = folderConfig.getWorkspace() + File.separator + dto.getUser();

		String coverageType = environmentEntity.getCoverageType();

		AutoTestGenerationWrapper genResultWrapper = generateTests(dto.getUser(), environment, coverageType,
				dto.getUut(), dto.getSut(), sut,
				buildConfig, projectConfig, workspacePath, environmentEntity.getPath(), userCode, dto.getStrategy()
		);

		// return dto
		long startTime = System.currentTimeMillis();

		float coverage = -1;
		List<TestRow> testRows = mapListEntityToDTO(genResultWrapper.entities);
		String title = "Automatic generate test " + dto.getSut();
		String content = "";
		String taskTitle = "Generating report " + title;
		String taskId = UUID.randomUUID().toString();
		ServerLogger.debug(dto.getUser(), LogDTO.Position.TEST_GENERAL, taskTitle);
		ServerLogger.progress(dto.getUser(), taskId, taskTitle, -1, 1);

		if (!dto.getStrategy().equals(AutoGenDTO.BASIS_PATH)) {
			List<TestCase> testCases = genResultWrapper.testCases;

			CoverageData funcCoverageData = CoverageManager.getCoverageAtFunctionLevel(testCases, coverageType);
			coverage = funcCoverageData.getProgress();

			CoverageData srcCoverageData = CoverageManager.getCoverageAtFileLevel(testCases, coverageType);
			content = srcCoverageData.getContent();
		}

		ReportDTO report = new ReportDTO(title, content);
		ServerLogger.progress(dto.getUser(), taskId, taskTitle, 1, 1);

		TimeTracker.add("Generate report", System.currentTimeMillis() - startTime);

		return new GenResultDTO(testRows, coverage, report);
	}

	public GenResultDTO autoGenForUnit(AutoGenDTO dto, HashMap<String, List<String>> userCode) throws Exception {
		// get project config, sut and build config
		String environment = dto.getEnvironment();
		String username = dto.getUser();
		String uutName = dto.getUut();
		String strategy = dto.getStrategy();

		EnvironmentEntity environmentEntity = envDao.getByKey(environment);
		String profile = environmentEntity.getProFile();
		CacheData cacheData = CacheManager.getInstance().get(profile);
		ProjectConfig projectConfig = cacheData.getProjectConfig();
		String coverageType = environmentEntity.getCoverageType();

		ProjectNode root = cacheData.getProjectNode();
		INode sourceNode = Search.searchNodes(root, new SourcecodeFileNodeCondition(), uutName).get(0);
		List<IFunctionNode> functionNodes = Search.searchNodes(sourceNode, new FunctionNodeCondition());
		// filter public functions
		functionNodes.removeIf(f -> f.getVisibility() != ICPPASTVisibilityLabel.v_public);

		BuildConfig buildConfig = BuildConfig.load();
		FolderConfig folderConfig = FolderConfig.load();
		String workspacePath = folderConfig.getWorkspace() + File.separator + username;

		List<TestCaseEntity> entities = new ArrayList<>();
		List<TestCase> testCases = new ArrayList<>();
		for (IFunctionNode functionNode : functionNodes){
			try {
				new FunctionCallDependencyGeneration().dependencyGeneration(functionNode);
				AutoTestGenerationWrapper wrapper = generateTests(username, environment, coverageType,
						uutName, functionNode.getName(), functionNode,
						buildConfig, projectConfig,
						workspacePath, environmentEntity.getPath(), userCode, strategy
				);
				entities.addAll(wrapper.entities);
				testCases.addAll(wrapper.testCases);
			} catch (Exception ex) {
				ServerLogger.error(dto.getUser(), LogDTO.Position.TEST_GENERAL, ex.getMessage());
			}
		}

		// return dto
		String title = "Automatic generate test " + dto.getUut();
		String taskTitle = "Generating report " + title;
		String taskId = UUID.randomUUID().toString();
		String content = "";

		ServerLogger.debug(dto.getUser(), LogDTO.Position.TEST_GENERAL, taskTitle);
		ServerLogger.progress(dto.getUser(), taskId, taskTitle, -1, 1);

		List<TestRow> testRows = mapListEntityToDTO(entities);
		float coverage = -1;

		if (!dto.getStrategy().equals(AutoGenDTO.BASIS_PATH)) {
			CoverageData srcCoverageData = CoverageManager.getCoverageAtFileLevel(testCases, coverageType);
			coverage = srcCoverageData.getProgress();
			content = srcCoverageData.getContent();
		}

		ReportDTO report = new ReportDTO(title, content);

		ServerLogger.progress(dto.getUser(), taskId, taskTitle, 1, 1);

		return new GenResultDTO(testRows, coverage, report);
	}

	private AutoTestGenerationWrapper generateTests(String user, String environment, String coverageType,
			String uutName, String sutName, IFunctionNode sut,
			BuildConfig buildConfig, ProjectConfig projectConfig,
			String workspacePath, String environmentPath, HashMap<String, List<String>> userCode,
			String strategy) throws Exception {

		// create auto-gen task
		ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Creating auto-gen task for " + sut);
//		String workspacePath = folderConfig.getWorkspace() + File.separator + user;
//		String environmentPath = folderConfig.getEnvironment() + File.separator + environment;

		// genTdataTask->
		GenerateTestdataTask genTask = new GenerateTestdataTask(buildConfig, projectConfig,
				workspacePath, environmentPath, coverageType, userCode, strategy);
		genTask.setFunction(sut);
		genTask.setGeneralLogger(new TestGeneralCoreLogger(user));
		genTask.setBuildLogger(new TestBuildCoreLogger(user));

		// run
		ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Running auto-gen task");
		List<TestCase> originTestCases = genTask.run();
		List<TestCase> testCases = originTestCases.stream()
				.filter(tc -> !tc.getStatus().equals(TestCase.STATUS_FAILED))
				.collect(Collectors.toList());

		if (testCases.isEmpty()) {
			ServerLogger.error(user, LogDTO.Position.TEST_GENERAL, "Failed to generate test data for " + sutName);
			throw new GenerateTestFailure(uutName, sutName);
		} else if (testCases.size() > 1) {
			ServerLogger.info(user, LogDTO.Position.TEST_GENERAL, "Created " + testCases.size() + " test cases");
		} else {
			ServerLogger.info(user, LogDTO.Position.TEST_GENERAL, "Created test case " + testCases.get(0).getName());
		}

		// compute coverage
		List<TestCaseEntity> entities = new ArrayList<>();
		for (TestCase testCase: testCases) {
			String testId = testCase.getId();

			// Save testcase to file
			// location {environments.dir}/{environment.name}/testcases/{test.id}.json
			String path = environmentPath + File.separator + TEST_CASE_DIR + File.separator + testId + ".json";
			testCase.setPath(path);
			saveTestCaseToFile(testCase);

			// save testcase to db
			float funcCoverage = -1;
			if (!strategy.equals(AutoGenDTO.BASIS_PATH))
				funcCoverage = CoverageManager.getCoverageAtFunctionLevel(coverageType, testCase);
			TestCaseEntity entity = mapModelToEntity(environment, testCase, testId, user, funcCoverage);

			if (!Float.isNaN(funcCoverage))
				entity.setCoverage(funcCoverage);
			else
				entity.setCoverage((float) -1);

			ServerLogger.debug(user, LogDTO.Position.TEST_GENERAL, "Saving test case " + testCase + " to database");
			testDao.insert(entity);

			entities.add(entity);
		}

		return new AutoTestGenerationWrapper(entities, testCases);
	}

	public MultiTestResultDTO viewCoverage(TestIdsDTO dto) throws Exception {
		String username = dto.getUser();

		List<TestCase> testCases = new ArrayList<>();
		List<TestResultDTO.Entry> updatedEntries = new ArrayList<>();

		ProjectNode root = null;
		String coverageType = null;

		BuildConfig buildConfig = BuildConfig.load();
		FolderConfig folderConfig = FolderConfig.load();

		for (String id : dto.getIds()) {
			TestCaseEntity testCaseEntity = testDao.getByKey(id);

			String testWsDir = folderConfig.getWorkspace() + File.separator + username
					+ File.separator + testCaseEntity.getId();

			// get root and coverage once
			if (root == null || coverageType == null) {
				EnvironmentEntity environmentEntity = envDao.getByKey(testCaseEntity.getEnv());
				CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
				root = data.getProjectNode();
				coverageType = environmentEntity.getCoverageType();
			}

			// run test case if test case has not run yet
			if (!isCurrentTest(testCaseEntity, testWsDir)) {
				Execution singleExecution = runSingleTest(username, id, buildConfig, folderConfig);

				TestCase testCase = singleExecution.getTestCase();
				testCases.add(testCase);

				// add updated entry
				AssertionResult assertionResult = singleExecution.getAssertionResult();
				String status = String.format("%d/%d", assertionResult.getPass(), assertionResult.getTotal());
				Float coverage = CoverageManager.getCoverageAtFunctionLevel(coverageType, testCase);
				TestResultDTO.Entry entry = new TestResultDTO.Entry(id, status, coverage);
				updatedEntries.add(entry);

			} else {
				TestCase testCase = entityToModel(testCaseEntity, root);
				String testPathFilePath = testWsDir + File.separator + TEST_PATH_NAME;
				testCase.setTestPathFile(testPathFilePath);
				testCases.add(testCase);
			}
		}

		// compute coverage
		ServerLogger.debug(username, LogDTO.Position.TEST_GENERAL, "Compute test result of " + dto.getIds().size() + " test cases");
		CoverageData coverageData = CoverageManager.getCoverageAtFunctionLevel(testCases, coverageType);
		float coverage = coverageData.getProgress();

		// gen report
		String title = "Coverage Report " + DateTimeUtils.toString(LocalDateTime.now());
		CoverageData srcCoverageData = CoverageManager.getCoverageAtFileLevel(testCases, coverageType);
		String content = srcCoverageData.getContent();
		ReportDTO report = new ReportDTO(title, content);

		return new MultiTestResultDTO(updatedEntries, coverage, report);
	}

	private boolean isCurrentTest(TestCaseEntity testCaseEntity, String testWsDir) {
		if (!new File(testWsDir).exists())
			return false;

		String testCaseVersionPath = testWsDir + File.separator + LAST_VERSION_NAME;
		File lastModifiedFile = new File(testCaseVersionPath);
		if (lastModifiedFile.exists()) {
			String executedVersion = Utils.readFileContent(lastModifiedFile);
			executedVersion = executedVersion.trim();
			String lastModified = testCaseEntity.getLastModified().trim();
			return executedVersion.equals(lastModified);
		}

		return false;
	}

	@NotNull
	public List<TestCaseEntity> searchByFunc(String sut) throws Exception {
		return testDao.searchByFunc(sut);
	}

	private static class AutoTestGenerationWrapper {

		private final @NotNull List<TestCaseEntity> entities;

		private final @NotNull List<TestCase> testCases;


		private AutoTestGenerationWrapper(@NotNull List<TestCaseEntity> entities, @NotNull List<TestCase> testCases) {
			this.entities = entities;
			this.testCases = testCases;
		}
	}

	private static class TestBuildCoreLogger extends CoreLogger {

		public TestBuildCoreLogger(String username) {
			super(username, LogDTO.Position.TEST_BUILD);
		}
	}

	private static class TestGeneralCoreLogger extends CoreLogger {

		public TestGeneralCoreLogger(String username) {
			super(username, LogDTO.Position.TEST_GENERAL);
		}
	}
}
