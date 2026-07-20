package uet.fit.server.rest.service;

import org.jetbrains.annotations.NotNull;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.util.Utils;
import uet.fit.server.DAO.entity.UserEntity;
import uet.fit.server.DAO.impl.UserDAO;
import uet.fit.server.exception.EntityNotFoundException;
import uet.fit.server.exception.MultipleOnlineUserException;
import uet.fit.server.exception.WrongAuthenticationException;
import uet.fit.server.resource_manager.CacheManager;
import uet.fit.server.resource_manager.LogCache;
import uet.fit.server.resource_manager.ProjectUsers;

import java.io.File;
import java.util.List;

public class UserService {

	private final UserDAO dao = new UserDAO();

	public List<UserEntity> list() throws Exception {
		return dao.getAll();
	}

	public boolean isExist(String username) throws Exception {
		try {
			dao.getByKey(username);
			return true;
		} catch (EntityNotFoundException e) {
			return false;
		}
	}

	public void add(String username, String password) throws Exception {
		UserEntity entity = new UserEntity(username, password, null, false);
		dao.insert(entity);
	}

	public void changePassword(String username, String oldPw, String newPw, String cfPw) throws Exception {
		if (!newPw.equals(cfPw))
			throw new Exception("Confirmed password is not exact to new password");

		UserEntity entity = dao.getByKey(username);

		if (!entity.getPassword().equals(oldPw))
			throw new Exception("Please entering the correct old password");

		UserEntity updateEntity = new UserEntity(username, newPw, null, null);
		dao.update(updateEntity);
	}

	public void delete(String username) throws Exception {
		dao.delete(username);
	}

	public void logon(@NotNull String username, String password) throws Exception {
		UserEntity userEntity = dao.getByKey(username);

		if (!userEntity.getPassword().equals(password))
			throw new WrongAuthenticationException();

		if (userEntity.isOnline())
			throw new MultipleOnlineUserException(username);

		UserEntity updateEntity = new UserEntity(username, null, null, true);
		dao.update(updateEntity);

		LogCache.getInstance().register(username);
		deleteClonedRepositories(username);
	}

	private void deleteClonedRepositories(@NotNull String username) {
		String clonedRepoDir = FolderConfig.load().getWorkspace() + File.separator + username
				+ File.separator + FolderConfig.REPOSITORY;
		Utils.deleteFileOrFolder(new File(clonedRepoDir));
	}

	public void logoff(@NotNull String username) throws Exception {
		UserEntity updateEntity = new UserEntity(username, null, null, false);
		dao.update(updateEntity);

//		CacheRequest.getInstance().remove(username);
//		CacheRequest.getInstance().getPrevious().remove(username);
		LogCache.getInstance().remove(username);
		deleteClonedRepositories(username);

		String project = ProjectUsers.getInstance().remove(username);
		if (project != null && !ProjectUsers.getInstance().containsValue(project)) {
			CacheManager.getInstance().remove(project);
			System.gc();
		}
	}

	public void logoffAllUsers() throws Exception {
		dao.logoffAll();

//		CacheRequest.getInstance().clear();
//		CacheRequest.getInstance().getPrevious().clear();
		LogCache.getInstance().clear();
		ProjectUsers.getInstance().clear();
	}

	public boolean authorize(String username, String password) {
		try {
			UserEntity userEntity = dao.getByKey(username);
			return userEntity.getPassword().equals(password);
		} catch (Exception e) {
			return false;
		}
	}
}
