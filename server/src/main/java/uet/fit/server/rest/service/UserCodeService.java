package uet.fit.server.rest.service;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.dependency.FunctionCallDependencyGeneration;
import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.dto.UserDTO.DeleteUserCodeIdDTO;
import uet.fit.dto.UserDTO.FunctionDTO;
import uet.fit.dto.UserDTO.ModifyUserCodeDTO;
import uet.fit.dto.UserDTO.UserCodeDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.test.AutoGenDTO;
import uet.fit.dto.logger.LogDTO;
import uet.fit.server.DAO.IEnvironmentDAO;
import uet.fit.server.DAO.IUserCodeDAO;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.entity.UserCodeEntity;
import uet.fit.server.DAO.impl.EnvironmentDAO;
import uet.fit.server.DAO.impl.UserCodeDAO;
import uet.fit.server.logger.ServerLogger;
import uet.fit.server.resource_manager.CacheData;
import uet.fit.server.resource_manager.CacheManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static uet.fit.aut.util.SourceConstant.getInstanceName;

public class UserCodeService {
	private static final Logger logger = LoggerFactory.getLogger(UserCodeService.class);

	private final IEnvironmentDAO envDao;
	private final IUserCodeDAO uscDao;
	public UserCodeService() {
		this.envDao = new EnvironmentDAO();
		this.uscDao = new UserCodeDAO();
	}

	@NotNull
	public List<String> getFuncArgumentList(String env, String sutName, String uutPath) throws Exception {
		EnvironmentEntity environmentEntity = envDao.getByKey(env);
		IFunctionNode sut = findSut(environmentEntity.getProFile(), uutPath, sutName);
		List<String> arguNameList = new ArrayList<>();
		List<IVariableNode> sutList = sut.getArguments();

		INode realParent = sut.getRealParent();
		if(realParent instanceof ClassNode) {
			String instanceVarName = getInstanceName(realParent);
			String className = realParent.getName() + "* " +  instanceVarName;
			arguNameList.add(className);
		}

		for (IVariableNode node : sutList) {
			String argument = node.getRawType() + " " + node.getName();
			arguNameList.add(argument);
		}
		return arguNameList;
	}

	@NotNull
	private IFunctionNode findSut(@NotNull String profile, @NotNull String uut, @NotNull String sutName) {
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

	@NotNull
	public UserCodeDTO userCodeTest(@NotNull FunctionDTO functionDTO) throws Exception {
		logger.debug("put usercode to server");
		String environment = functionDTO.getEnv();
		String uut = functionDTO.getUut();
		String sut = functionDTO.getSutName();
		UserCodeDTO userCodeDTO = new UserCodeDTO();


		return userCodeDTO;
	}

	public HashMap<String, List<String>> getDataUserCode(@NotNull AutoGenDTO dto) {
		try {
			String env = dto.getEnvironment();
			String uut = dto.getUut();
			String funcName = dto.getSut();
			EnvironmentEntity environmentEntity = envDao.getByKey(env);
			IFunctionNode sut = findSut(environmentEntity.getProFile(), uut, funcName);
			String funcPath = sut.getAbsolutePath();

				List<UserCodeEntity> userCodeEntities = uscDao.searchByFunc(funcPath);

			//create map
			HashMap<String, List<String>> userCode = new HashMap<>();
			String key = userCodeEntities.get(0).getVarName();
			List<String> contents = new ArrayList<>();
			for (UserCodeEntity temp : userCodeEntities) {
				if (temp.getVarName().equals(key)) {
					contents.add(temp.getCode());
				} else {
					userCode.put(key, contents);
					contents = new ArrayList<>();
					key = temp.getVarName();
					contents.add(temp.getCode());
				}
			}
			userCode.put(key, contents);

			//set data
			return userCode;
		} catch (Exception e) {
			logger.debug("Can't search usercode from db", e);
			return new HashMap<>();
		}
	}

	@NotNull
       public UserCodeDTO getlistUserCode(@NotNull String env, @NotNull String uut, @NotNull String sut) throws Exception {
// 		get project root from cache
		EnvironmentEntity environmentEntity = envDao.getByKey(env);
		String profile = environmentEntity.getProFile();
		// find uut
		IFunctionNode function = findSut(profile, uut, sut);
		String functionPath = function.getAbsolutePath();
		List<UserCodeEntity> userCode = uscDao.getAllByFuncAndEnv(functionPath, env);

		List<UserTypedefRow> userCodeRows = mapListEntityToDTO(userCode);
	return new UserCodeDTO(env,uut,sut,userCodeRows);
}

	@NotNull
	private List<UserTypedefRow> mapListEntityToDTO(@NotNull List<UserCodeEntity> entities) {
		List<UserTypedefRow> userTypedefRows = new ArrayList<>();
		for (UserCodeEntity entity : entities) {
			String id = entity.getId();
			String type = entity.getType();
			String name = entity.getVarName();
			String owner = entity.getOwner();
			String code = entity.getCode();
			UserTypedefRow row = new UserTypedefRow(id,type,name,code);
			userTypedefRows.add(row);
		}
		return userTypedefRows;
	}

	public void insertNewUserCode(UserCodeDTO dto) throws Exception {
		List<UserTypedefRow> listRows = dto.getListContentUserCode();
		for (UserTypedefRow row: listRows) {
			String type = row.getMyType();
			String varName = row.getName();
			String code = row.getValue();
			String id = row.getId();
			String uut = dto.getUut();
			String funcName = dto.getSut();
			String env = dto.getEnvironment();
			String user = dto.getUser();
			EnvironmentEntity environmentEntity = envDao.getByKey(env);
			IFunctionNode sut = findSut(environmentEntity.getProFile(), uut, funcName);
			String funcPath = sut.getAbsolutePath();
			 {
				UserCodeEntity entity = new UserCodeEntity(id, type, varName, code, funcPath, env, user);
				uscDao.insert(entity);
			}
		}
	}

	public void modifyUserCode(ModifyUserCodeDTO dto) throws Exception {
		List<UserTypedefRow> listRows = dto.getListModifiedUserCode();
		for (UserTypedefRow row : listRows) {
			String id = row.getId();
			UserCodeEntity entity = uscDao.getByKey(id);
			UserCodeEntity updated = entity;
			updated.setCode(row.getValue());
			uscDao.update(updated);
		}
	}

	public void deleteUserCode(DeleteUserCodeIdDTO dto) {
		String username =dto.getUser();
		for (String id : dto.getIds()) {

			try {
				UserCodeEntity userCodeEntity = uscDao.getByKey(id);
				if (userCodeEntity.getOwner().equals(username)) {
					uscDao.delete(id);
					ServerLogger.info(username, LogDTO.Position.TEST_GENERAL, "Successfully deleted user code " + id);
				} else {
					ServerLogger.info(username, LogDTO.Position.TEST_GENERAL, "You don't have permission to delete " + id);
				}
			} catch (Exception e) {
				logger.error("Can't delete test case " + id, e);
				ServerLogger.error(username, LogDTO.Position.TEST_GENERAL, "Can't delete this user code: " + e.getMessage());
			}
		}

	}
}
