package uet.fit.server.rest.service;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.util.Utils;
import uet.fit.dto.func.FileLocation;
import uet.fit.dto.func.ViewSourceDTO;
import uet.fit.server.DAO.IEnvironmentDAO;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.impl.EnvironmentDAO;
import uet.fit.server.resource_manager.CacheData;
import uet.fit.server.resource_manager.CacheManager;

public class FunctionService {

	private final IEnvironmentDAO envDao;

	public FunctionService() {
		this.envDao = new EnvironmentDAO();
	}

	@NotNull
	public ViewSourceDTO viewSource(@NotNull String filePath, @Nullable String funcName, @NotNull String envName) throws Exception {
		ViewSourceDTO viewSourceDTO = new ViewSourceDTO();

		viewSourceDTO.setName(filePath);

		EnvironmentEntity environmentEntity = envDao.getByKey(envName);
		CacheData data = CacheManager.getInstance().get(environmentEntity.getProFile());
		ProjectNode root = data.getProjectNode();

		ISourcecodeFileNode sourceNode = (ISourcecodeFileNode) Search
				.searchNodes(root, new SourcecodeFileNodeCondition(), filePath).get(0);
		String content = Utils.readFileContent(sourceNode.getAbsolutePath());
		viewSourceDTO.setContent(content);

		if (funcName != null) {
			IFunctionNode functionNode = (IFunctionNode) Search
					.searchNodes(sourceNode, new FunctionNodeCondition(), funcName).get(0);

			FileLocation fileLocation = new FileLocation();
			IASTFileLocation ASTFileLocation = functionNode.getAST().getFileLocation();
			fileLocation.setOffset(ASTFileLocation.getNodeOffset());
			fileLocation.setLength(ASTFileLocation.getNodeLength());
			fileLocation.setStartLine(ASTFileLocation.getStartingLineNumber());
			fileLocation.setEndLine(ASTFileLocation.getEndingLineNumber());
			viewSourceDTO.setFileLocation(fileLocation);
		}

		return viewSourceDTO;
	}
}
