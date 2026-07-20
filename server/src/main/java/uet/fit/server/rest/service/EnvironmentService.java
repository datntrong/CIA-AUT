package uet.fit.server.rest.service;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.BuildConfig;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.config.pro.ProSourceNode;
import uet.fit.aut.logger.TimeTracker;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.thread.task.CleanAndMakeProjectTask;
import uet.fit.aut.thread.task.InstrumentTask;
import uet.fit.aut.thread.task.ParseProjectTask;
import uet.fit.aut.thread.task.ParseQTTypeTask;
import uet.fit.aut.thread.task.QmakeProjectTask;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.Utils;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.env.DeletedEnvironmentDTO;
import uet.fit.dto.env.DeletedEnvironmentEntry;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.EnvironmentNamesDTO;
import uet.fit.dto.env.EnvironmentListDTO;
import uet.fit.dto.env.EnvironmentRow;
import uet.fit.dto.env.Source;
import uet.fit.dto.env.Subprogram;
import uet.fit.dto.logger.LogDTO;
import uet.fit.report.html.component.page.Document;
import uet.fit.server.DAO.IEnvironmentDAO;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.impl.EnvironmentDAO;
import uet.fit.server.exception.EntityNotFoundException;
import uet.fit.server.exception.LimitedMemory;
import uet.fit.server.logger.CoreLogger;
import uet.fit.server.logger.ServerLogger;
import uet.fit.server.report_builder.OverviewReportBuilder;
import uet.fit.server.resource_manager.CacheData;
import uet.fit.server.resource_manager.CacheManager;
import uet.fit.server.resource_manager.EnvironmentMutex;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static uet.fit.aut.config.ProFileUtils.getCloneProFile;
import static uet.fit.aut.config.ProFileUtils.getEnvProContainer;
import static uet.fit.aut.parser.ISourceDependencyStorage.FILE_NAME;

public class EnvironmentService {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentService.class);

	private final IEnvironmentDAO dao;

	public EnvironmentService() {
		dao = new EnvironmentDAO();
	}

	@NotNull
	public EnvironmentListDTO getAllEnvironments() throws Exception {
		List<EnvironmentEntity> entities = dao.getAll();
		EnvironmentListDTO dto = new EnvironmentListDTO();
		for (EnvironmentEntity entity : entities) {
			String coverageType = entity.getCoverageType();
			String name = entity.getName();
			String author = entity.getOwner();
			String proFile = new File(entity.getProFile()).getName();
			String commit = uet.fit.util.Utils.shortenCommitHash(entity.getCommit());
			String project = String.format("%s (%s)", commit, proFile);
			EnvironmentRow row = new EnvironmentRow(name, coverageType, project, author);
			dto.add(row);
		}
		return dto;
	}

	@NotNull
	public DeletedEnvironmentDTO deleteEnvironmentDTO(@NotNull EnvironmentNamesDTO environmentNamesDTO){
		String username = environmentNamesDTO.getUser();

		List<DeletedEnvironmentEntry> environmentList = new ArrayList<>();
		for(String id : environmentNamesDTO.getNames()){
			DeletedEnvironmentEntry deletedEnvironmentEntry;

			try{
				EnvironmentEntity environmentEntity = dao.getByKey(id);
				String enviName = environmentEntity.getName();
				if(environmentEntity.getOwner().equals(username)){
					dao.delete(id);
					deletedEnvironmentEntry = new DeletedEnvironmentEntry(id, DeletedEnvironmentEntry.Status.SUCCESS);
					ServerLogger.info(username, LogDTO.Position.ENVIRONMENT, "Deleted environment: " + enviName);
				} else {
					deletedEnvironmentEntry = new DeletedEnvironmentEntry(id, DeletedEnvironmentEntry.Status.PERMISSION_DENY);
					ServerLogger.info(username, LogDTO.Position.ENVIRONMENT, "You don't have permission to delete " + enviName);
				}
			} catch (Exception e) {
				logger.error("Can't deleted environment" + id, e);
				ServerLogger.error(username,LogDTO.Position.ENVIRONMENT, "Can't deleted environment" + e.getMessage());
				deletedEnvironmentEntry = new DeletedEnvironmentEntry(id, DeletedEnvironmentEntry.Status.FAILED);

			}

			environmentList.add(deletedEnvironmentEntry);

		}
		return new DeletedEnvironmentDTO(environmentList);
	}

	@NotNull
	public EnvironmentDTO findEnvironment(@NotNull String name) throws Exception {
		EnvironmentEntity environmentEntity = dao.getByKey(name);

		// map entity to dto
		EnvironmentDTO environmentDTO = new EnvironmentDTO();
		environmentDTO.setProFile(environmentEntity.getProFile());
		environmentDTO.setName(environmentEntity.getName());
		environmentDTO.setCoverageType(environmentEntity.getCoverageType());
		environmentDTO.setGitUrl(environmentEntity.getGitUrl());
		environmentDTO.setCommit(environmentEntity.getCommit());
		environmentDTO.setProject(environmentEntity.getProject());
		environmentDTO.setPath(environmentEntity.getPath());

		return environmentDTO;
	}

	public void saveEnvironment(@NotNull EnvironmentDTO environmentDTO) throws Exception {
		// map dto to entity
		EnvironmentEntity environmentEntity = new EnvironmentEntity(
				environmentDTO.getName(),
				environmentDTO.getProFile(),
				environmentDTO.getProject(),
				environmentDTO.getPath(),
				environmentDTO.getCoverageType(),
				environmentDTO.getGitUrl(),
				environmentDTO.getCommit(),
				environmentDTO.getUser()
		);

		dao.insert(environmentEntity);
	}

	public boolean isExist(@NotNull String environment) throws Exception {
		try {
			dao.getByKey(environment);
			return true;
		} catch (EntityNotFoundException notFoundEx) {
			return false;
		}
	}

	public void buildAndCache(@NotNull EnvironmentDTO environmentDTO, boolean isInstrument) throws Exception {
		final String waitTaskId = UUID.randomUUID().toString();

		// get pro file path
		String proPath = environmentDTO.getProFile();
		File proFile = new File(proPath);
		String username = environmentDTO.getUser();
		FolderConfig folderConfig = FolderConfig.load();
		File envDir = new File(environmentDTO.getPath());
		File project = new File(environmentDTO.getProject());

		try {
			ServerLogger.progress(username, waitTaskId, "Waiting", -1, 1);

			// wait for finishing previous request corresponding to this project
			ServerLogger.debug(username, LogDTO.Position.ENVIRONMENT, "Waiting for finishing previous request corresponding to this project");
			EnvironmentMutex.getInstance().wait(proPath);

			// lock to prevent another request from running
			EnvironmentMutex.getInstance().lock(proPath);

			ServerLogger.progress(username, waitTaskId, "Waiting", 1, 1);

			// build if project is not in cache
			if (!CacheManager.getInstance().containsKey(proPath)) {
				CacheData cacheData = buildEnv(environmentDTO, isInstrument);

				// cache if return success
				if (!environmentDTO.getSources().isEmpty()) {
					CacheManager.getInstance().put(proPath, cacheData);
				}
			} else {
				// get project config &  tree in cache
				CacheData cacheData = CacheManager.getInstance().get(proPath);
				ProjectConfig projectConfig = cacheData.getProjectConfig();
				ProjectNode root = cacheData.getProjectNode();

				// instrument project
				if (isInstrument) {
					// load build config from json
					BuildConfig buildConfig = BuildConfig.load();

					// build project
					qmakeProject(username, buildConfig, proFile, project, environmentDTO.getPath());

					instrument(projectConfig, root, environmentDTO);

					File directory = getEnvProContainer(envDir, project, proFile);
					CleanAndMakeProjectTask makeTask = new CleanAndMakeProjectTask(directory, buildConfig);
					makeTask.setExternalLogger(new EnvironmentCoreLogger(username));
					makeTask.run();
				}

				// update dto with source list
				environmentDTO.setSources(CacheManager.getInstance().get(proPath).getSources());
			}
		} finally {
			// delete and backup the cloned pro file
			backupProFile(username, envDir, project, proFile, folderConfig.getWorkspace());

			// unlock
			EnvironmentMutex.getInstance().unlock(proPath);
		}
	}

	public ReportDTO generateOverviewReport(@NotNull EnvironmentDTO environmentDTO, @NotNull List<Source> uutList) throws Exception {

		logger.debug("Getting information for generate overview report");

		EnvironmentEntity environmentEntity = dao.getByKey(environmentDTO.getName());
		CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		Document report = new OverviewReportBuilder()
				.setEnvironment(environmentDTO)
				.setRoot(root)
				.appendUut(uutList)
				.build();

		return new ReportDTO(report.getTitle(), report.toHTML());
	}

	private void backupProFile(String username, File envDir, File project, File proFile, String workspace) {
		String cloneProPath = getCloneProFile(envDir, username, project, proFile);
		File cloneProFile = new File(cloneProPath);
		try {
			String backupFilePath = workspace + File.separator
					+ username + File.separator + cloneProFile.getName();
			File backupFile = new File(backupFilePath);
			Utils.copy(cloneProFile, backupFile);
		} catch (Exception ex) {
			logger.error("Cannot backup *.pro file in workspace");
		} finally {
			Utils.deleteFileOrFolder(cloneProFile);
		}
	}

	private long predictRequiredMemory(long projectSize) {
		final int SCALE_FACTOR = 5;
		final long LOWER_BOUNDARY = 128 * 1024;
		long requiredMemory = projectSize * SCALE_FACTOR;
		if (requiredMemory < LOWER_BOUNDARY)
			requiredMemory = LOWER_BOUNDARY;
		return requiredMemory;
	}

	@NotNull
	public CacheData buildEnv(@NotNull EnvironmentDTO environmentDTO, boolean isInstrument) throws Exception {
		String username = environmentDTO.getUser();

		// get pro file
		String proPath = environmentDTO.getProFile();
		File proFile = new File(proPath);
		File project = new File(environmentDTO.getProject());

		long projectSize = Utils.size(proFile.getParentFile());
		long requiredMemory = predictRequiredMemory(projectSize);
		long availableMemory = Utils.availableMemory();
		if (availableMemory < requiredMemory)
			throw new LimitedMemory(projectSize);

		try {
			// load build config from json
			BuildConfig buildConfig = BuildConfig.load();

			// build project and retrieve project config
			ProjectConfig projectConfig = qmakeProject(username, buildConfig, proFile,
					project, environmentDTO.getPath());

			// update source list
			for (ProSourceNode source : projectConfig.getSources()) {
				Source newSource = new Source(source.getPath());
				environmentDTO.getSources().add(newSource);
			}

			// parse project
			ProjectNode root = parse(projectConfig, environmentDTO, isInstrument);
			// create cache data
			CacheData cacheData = new CacheData();
			cacheData.setProjectConfig(projectConfig);
			cacheData.setProjectNode(root);
			cacheData.setSources(environmentDTO.getSources());
			return cacheData;

		} catch (Exception e) {
			logger.error("Can't build project " + proPath, e);
			ServerLogger.error(username, LogDTO.Position.ENVIRONMENT, "Can't build project " + proPath);
			throw e;

		}
	}

	private ProjectConfig qmakeProject(String username, BuildConfig buildConfig,
			File proFile, File project, String envPath) throws Exception {
		File envDir = new File(envPath);

		// build project and retrieve project config
		long startTime = System.currentTimeMillis();

		QmakeProjectTask task = new QmakeProjectTask(proFile, project, envDir, buildConfig, username);
		task.setExternalLogger(new EnvironmentCoreLogger(username));
		ProjectConfig projectConfig = task.run();

		TimeTracker.add("Build", System.currentTimeMillis() - startTime);

		return projectConfig;
	}

	@NotNull
	private ProjectNode parse(@NotNull ProjectConfig projectConfig, @NotNull EnvironmentDTO envDto, boolean isInstrument) throws Exception {
		final String username = envDto.getUser();

		// parse project
		ParseProjectTask task = new ParseProjectTask(projectConfig);
		task.setExternalLogger(new EnvironmentCoreLogger(username));
		task.setLoadDependenciesFromFile(!isInstrument);

		// get environment folder
		String environmentPath = envDto.getPath();
		String depdFilePath = PathUtils.absolute(FILE_NAME, new File(environmentPath));
		task.setDependenciesFile(new File(depdFilePath));

		ProjectNode root = task.run();

		// parse qt
		FolderConfig folderConfig = FolderConfig.load();
		String qtConstructorsFolder = folderConfig.getEnvironment() + File.separator + envDto.getName() + File.separator + "qtConstructors";
		if (!new File(qtConstructorsFolder).exists() && isInstrument) {
			ParseQTTypeTask parseQTTypeTask = new ParseQTTypeTask(task.getProjectParser(), envDto.getName());
			parseQTTypeTask.run();
		}

		// instrument project
		if (isInstrument) {
			instrument(projectConfig, root, envDto);

			File envDir = new File(environmentPath);
			File proFile = new File(envDto.getProFile());
			File project = new File(envDto.getProject());
			File directory = getEnvProContainer(envDir, project, proFile);

			CleanAndMakeProjectTask cleanAndMakeProjectTask = new CleanAndMakeProjectTask(directory, BuildConfig.load());
			cleanAndMakeProjectTask.setExternalLogger(new EnvironmentCoreLogger(username));
			cleanAndMakeProjectTask.run();
		}

		// update function list for each source
		getFunctions(root, envDto);

		return root;
	}

	private void instrument(@NotNull ProjectConfig config, @NotNull ProjectNode root, @NotNull EnvironmentDTO envDto) throws Exception {
		// get environment folder
		String environmentPath = envDto.getPath();

		// run instrument task
		ServerLogger.debug(envDto.getUser(), LogDTO.Position.ENVIRONMENT, "Instrumenting project " + root.getName());
		InstrumentTask task = new InstrumentTask(environmentPath, config, root, envDto.getUser());
		task.run();
	}

	private void getFunctions(@NotNull ProjectNode root, @NotNull EnvironmentDTO environmentDTO) {
		environmentDTO.getSources().forEach(s -> {
			ISourcecodeFileNode fileNode = searchSource(s.getPath(), root);
			if (fileNode != null)
				updateListFunctions(fileNode, s);
		});
	}

	@Nullable
	private ISourcecodeFileNode searchSource(@NotNull String path, @NotNull ProjectNode root) {
		List<ISourcecodeFileNode> searchNodes = Search.searchNodes(root, new SourcecodeFileNodeCondition(), path);
		if (!searchNodes.isEmpty())
			return searchNodes.get(0);
		return null;
	}

	private void updateListFunctions(@NotNull ISourcecodeFileNode sourceNode, @NotNull Source source) {
		List<IFunctionNode> functionNodes = Search.searchNodes(sourceNode, new FunctionNodeCondition());
		functionNodes.removeIf(f -> f.getVisibility() != ICPPASTVisibilityLabel.v_public);
		List<Subprogram> convertedFunctions = functionNodes.stream()
				.map(f -> new Subprogram(f.getName()))
				.collect(Collectors.toList());
		source.setFunctions(convertedFunctions);
	}

	private static class EnvironmentCoreLogger extends CoreLogger {
		public EnvironmentCoreLogger(String username) {
			super(username, LogDTO.Position.ENVIRONMENT);
		}
	}

}
