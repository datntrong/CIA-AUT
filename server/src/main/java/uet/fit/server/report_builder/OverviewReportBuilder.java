package uet.fit.server.report_builder;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.coverage.CoverageData;
import uet.fit.aut.coverage.CoverageManager;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.parser.dependency.FunctionCallDependencyGeneration;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.testcase.TestCase;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.Source;
import uet.fit.dto.env.Subprogram;
import uet.fit.report.html.component.page.Document;
import uet.fit.report.html.component.table.HorizontalTable;
import uet.fit.report.html.component.table.Table;
import uet.fit.report.html.component.table.VerticalTable;
import uet.fit.report.html.data.TableData;
import uet.fit.report.html.data.TableRecords;
import uet.fit.report.html.element.CoverageElement;
import uet.fit.report.html.element.CoverageLOCElement;
import uet.fit.report.html.element.FunctionElement;
import uet.fit.report.html.element.SourceElement;
import uet.fit.report.html.element.TestCaseElement;
import uet.fit.server.DAO.ITestCaseDAO;
import uet.fit.server.DAO.entity.TestCaseEntity;
import uet.fit.server.DAO.impl.TestCaseDAO;
import uet.fit.server.rest.service.TestCaseService;
import uet.fit.server.util.ServerConstants;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uet.fit.aut.thread.task.ExecuteTestTask.TEST_PATH_NAME;

public class OverviewReportBuilder {

	private static final Logger logger = LoggerFactory.getLogger(OverviewReportBuilder.class);

	private final ITestCaseDAO testDao = new TestCaseDAO();
	private final TestCaseService testService = new TestCaseService();

	private EnvironmentDTO environment;
	private ProjectNode root;

	private final List<Source> uutList = new ArrayList<>();

	public OverviewReportBuilder setEnvironment(EnvironmentDTO dto) {
		this.environment = dto;
		return this;
	}

	public OverviewReportBuilder setRoot(ProjectNode root) {
		this.root = root;
		return this;
	}

	public OverviewReportBuilder appendUut(List<Source> sources) {
		this.uutList.addAll(sources);
		return this;
	}

	public Document build() {
		Document report = new Document();

		LocalDateTime timestamp = LocalDateTime.now();

		try {
			logger.debug("Starting build overview report");
			List<SourceElement> reportElements = genReportElements(environment, root, uutList);
			report.getTableList().add(genTableOfContent());
			report.getTableList().add(genReportInformation(environment, timestamp));
			report.getTableList().add(genCoverageTable(reportElements));
			report.getTableList().add(genStatusTable(reportElements));
		} catch (Exception e) {
			logger.error("Cannot generate report elements for overview report", e);
			e.printStackTrace();
		}

		String timestampStr = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String title = String.format("Overview Report - %s", timestampStr);
		report.setTitle(title);

		return report;
	}

	private List<SourceElement> genReportElements(@NotNull EnvironmentDTO environmentDTO, @NotNull ProjectNode root, @NotNull List<Source> uutList) throws Exception {

		logger.debug("Generating overview report elements");

		final String environmentName = environmentDTO.getName();

		List<SourceElement> sourceElements = new ArrayList<>();

		FolderConfig folderConfig = FolderConfig.load();
		final String workspace = folderConfig.getWorkspace() + File.separator + environmentDTO.getUser();

		String coverageType = environmentDTO.getCoverageType();

		List<Source> sourceList = !uutList.isEmpty() ? uutList : environmentDTO.getSources();

		for (Source source : sourceList) {

			logger.debug("Iterating source: " + IdMapping.getInstance().getOrCreate(source.getTitle()));

			SourceElement sourceElement = new SourceElement();
			sourceElement.setName(source.getTitle());

			//init status
			int sourceTotal = 0;
			int sourcePass = 0;

			//init testcases
			List<TestCase> sourceTestCases = new ArrayList<>();

			for (Subprogram subprogram : source.getFunctions()) {

				logger.debug("Iterating subprogram: " + IdMapping.getInstance().getOrCreate(subprogram.getName()));

				FunctionElement functionElement = new FunctionElement();
				functionElement.setName(subprogram.getName());

				// find uut
				IFunctionNode function = findSut(root, source.getPath(), subprogram.getName());
				String functionPath = function.getAbsolutePath();

				//list test case
				List<TestCaseEntity> testCaseEntities = testDao.getAllByFuncAndEnv(functionPath, environmentName);
				//list test case model
				List<TestCase> testCases = new ArrayList<>();

				//init status
				int funcTotal = 0;
				int funcPass = 0;

				//infer status
				for (TestCaseEntity testCase : testCaseEntities) {
					logger.debug("Iterating test case: " + IdMapping.getInstance().getOrCreate(testCase.getName()));
					if (!testCase.getStatus().equals(ITestCase.STATUS_NA)) {
						TestCaseElement testCaseElement = mappingEntityToElement(testCase);

						//add to function
						funcTotal += 1;
						funcPass += testCaseElement.getStatus().equals("PASSED") ? 1 : 0;
						functionElement.getTestCases().add(testCaseElement);

						//infer coverage
						TestCase testCaseModel = testService.entityToModel(testCase, root);
						String testPathFile = workspace + File.separator + testCase.getId() + File.separator + TEST_PATH_NAME;

						if (!new File(testPathFile).exists()) {
							testService.runTest(testCase.getId(), environmentDTO.getUser());
						}

						testCaseModel.setTestPathFile(testPathFile);
						testCases.add(testCaseModel);
					}
				}

				if (!testCases.isEmpty()) {
					logger.debug("Computing subprogram coverage");
					CoverageData funcCoverage = CoverageManager.getCoverageAtFunctionLevel(testCases, coverageType);
					functionElement.setCoverage(funcCoverage.getProgress());
					functionElement.setLoc(funcCoverage.getTotal());
					functionElement.setVisitedLoc(funcCoverage.getVisited());
				} else {
					functionElement.setCoverage(-1);
					functionElement.setLoc(-1);
					functionElement.setVisitedLoc(-1);
				}

				//fill func element
				functionElement.setTotal(funcTotal);
				functionElement.setPass(funcPass);
				String functionStatus = functionElement.getTotal() > 0 ? functionElement.getPass() > 0 ? "PASSED" : "FAILED" : "";
				functionElement.setStatus(functionStatus);

				//add to source
				sourceTotal += functionElement.getTotal();
				sourcePass += functionElement.getPass();

				sourceElement.getFunctions().add(functionElement);
				sourceTestCases.addAll(testCases);
			}

			//compute coverage
			if (!sourceTestCases.isEmpty()) {
				logger.debug("Computing source coverage");
				CoverageData sourceCovData = CoverageManager.getCoverageAtFileLevel(sourceTestCases, coverageType);
				sourceElement.setCoverage(sourceCovData.getProgress());
				sourceElement.setLoc(sourceCovData.getTotal());
				sourceElement.setVisitedLoc(sourceCovData.getVisited());
			} else {
				sourceElement.setCoverage(-1);
				sourceElement.setLoc(-1);
				sourceElement.setVisitedLoc(-1);
			}

			//fill source element
			sourceElement.setTotal(sourceTotal);
			sourceElement.setPass(sourcePass);
			String sourceStatus = sourceElement.getTotal() > 0 ? sourceElement.getPass() > 0 ? "PASSED" : "FAILED" : "";
			sourceElement.setStatus(sourceStatus);

			sourceElements.add(sourceElement);
		}
		return sourceElements;
	}

	@NotNull
	private IFunctionNode findSut(@NotNull ProjectNode root, @NotNull String uut, @NotNull String sutName) {
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

	private TestCaseElement mappingEntityToElement(TestCaseEntity entity) {
		TestCaseElement element = new TestCaseElement();
		element.setName(entity.getName());
		element.setTotal(entity.getTotal());
		element.setPass(entity.getPass());
		element.setStatus(entity.getPass() > 0 || entity.getTotal() == 0 ? "PASSED" : "FAILED");
		return element;
	}

	private Table genTableOfContent() {

		List<String> fieldNames = new ArrayList<>();
		fieldNames.add("1");
		fieldNames.add("2");
		fieldNames.add("3");

		TableRecords records = new TableRecords();

		records.add(Arrays.asList(renderToCElement("Report Information"), renderToCElement("Coverage"), renderToCElement("Status")));

		TableData tableData = new TableData("Table of Content", fieldNames, records);

		return new VerticalTable(tableData);
	}

	private Table genReportInformation(EnvironmentDTO environmentDTO, LocalDateTime timestamp) {

		List<String> fieldNames = new ArrayList<>();
		fieldNames.add("Version");
		fieldNames.add("Environment");
		fieldNames.add("Profile");
		fieldNames.add("Git URL");
		fieldNames.add("Commit");
		fieldNames.add("Coverage Type");
		fieldNames.add("Author");
		fieldNames.add("Report Creation Time");

		TableRecords records = new TableRecords();
		records.add(Arrays.asList(
				ServerConstants.TOOL_VERSION,
				environmentDTO.getName(),
				new File(environmentDTO.getProFile()).getName(),
				environmentDTO.getGitUrl(),
				environmentDTO.getCommit(),
				environmentDTO.getCoverageType(),
				environmentDTO.getUser(),
				timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

		TableData tableData = new TableData("Report Information", fieldNames, records);

		return new VerticalTable(tableData);
	}

	private Table genCoverageTable(List<SourceElement> sourceElements) {

		List<String> fieldNames = new ArrayList<>();
		fieldNames.add("Source");
		fieldNames.add("Function");
		fieldNames.add("Coverage");
		fieldNames.add("Coverage");

		TableRecords records = new TableRecords();
		for (SourceElement sourceElement : sourceElements) {
			if (sourceElement.getCoverage() >= 0) {
				String sourceCoverage = renderCoveragePercentage(sourceElement.getCoverage());
				records.add(Arrays.asList(sourceElement.getName(), "", renderCoverageBar(sourceCoverage), renderLOCCoverage(sourceElement)));
			} else {
				records.add(Arrays.asList(sourceElement.getName(), "", "", ""));
			}
			for (FunctionElement functionElement : sourceElement.getFunctions()) {
				if (functionElement.getCoverage() >= 0) {
					String funcCoverage = renderCoveragePercentage(functionElement.getCoverage());
					records.add(Arrays.asList("", functionElement.getName(), renderCoverageBar(funcCoverage), renderLOCCoverage(functionElement)));
				} else {
					records.add(Arrays.asList("", functionElement.getName(), "", ""));
				}
			}
		}
		TableData tableData = new TableData("Coverage", fieldNames, records);

		return new HorizontalTable(tableData);
	}

	private Table genStatusTable(List<SourceElement> sourceElements) {
		List<String> fieldNames = new ArrayList<>();

		fieldNames.add("Source");
		fieldNames.add("Function");
		fieldNames.add("Testcase");
		fieldNames.add("Status");

		TableRecords records = new TableRecords();
		for (SourceElement sourceElement : sourceElements) {
			records.add(Arrays.asList(sourceElement.getName(), "", "", renderStatus(sourceElement)));
			for (FunctionElement functionElement : sourceElement.getFunctions()) {
				records.add(Arrays.asList("", functionElement.getName(), "", renderStatus(functionElement)));
				for (TestCaseElement testCaseElement : functionElement.getTestCases()) {
					records.add(Arrays.asList("", "", testCaseElement.getName(), renderStatus(testCaseElement)));
				}
			}
		}
		TableData tableData = new TableData("Status", fieldNames, records);

		return new HorizontalTable(tableData);
	}

	private String renderCoveragePercentage(Float coverage) {
		int percentage = Math.round(coverage * 100);
		return percentage + "%";
	}

	//////////////////////////////
	// TODO SECTION             //
	// move into report package //
	//////////////////////////////
	private String renderStatus(CoverageElement element) {
		if (!element.getStatus().equals("")) {
			String color = element.getPass() == element.getTotal() ? "3C9F0F" : element.getPass() == 0 ? "FF0200" : "E39B4F";
			String content = String.format("%s (%d/%d)", element.getStatus(), element.getPass(), element.getTotal());
			return String.format("<strong style=\"color: #%s;\">%s</strong>", color, content);
		}
		return element.getStatus();
	}

	private String renderToCElement(String field) {
		String href = field.toLowerCase().replaceAll(" ", "");
		String onClickEvent = String.format("document.getElementById(\"%s\").scrollIntoView()", href);
		return String.format("<a href=\"#%s\" onclick=%s>%s<a/>", href, onClickEvent, field);
	}

	private String renderLOCCoverage(CoverageLOCElement element) {
		return String.format("%d/%d", element.getVisitedLoc(), element.getLoc());
	}

	private String renderCoverageBar(String coverage) {
		return String.format("<div style=\"background-color: #FF6602; width: %s;\">%s</div>", coverage, coverage);
	}
	//////////////////////////////
}
