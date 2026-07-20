package uet.fit.server.DAO;

import uet.fit.server.DAO.entity.EnvironmentEntity;

import java.util.List;
import java.util.Set;

public interface IEnvironmentDAO extends IDAO<EnvironmentEntity> {

	List<EnvironmentEntity> getAll(String url, String commit) throws Exception;

	Set<String> getCommits(String url) throws Exception;
}
