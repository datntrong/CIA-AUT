package uet.fit.aut.config;

import com.google.gson.annotations.Expose;
import uet.fit.config.Config;

import java.io.File;

public class FolderConfig {

	public static final String REPOSITORY = "repo";
	private static final String ENVIRONMENT = "env";
	private static final String WORKSPACE = "ws";

	@Expose
	private String repository = "";

	@Expose
	private String environment = "";

	@Expose
	private String workspace = "";

	private FolderConfig() {

	}

	public static FolderConfig load() {
		FolderConfig folderConfig = new FolderConfig();
		String home = Config.getHomePath();
		folderConfig.repository = home + File.separator + REPOSITORY;
		folderConfig.environment = home + File.separator + ENVIRONMENT;
		folderConfig.workspace = home + File.separator + WORKSPACE;
		return folderConfig;
	}

	public static String getStubDirectory(String user){
		String workspace = FolderConfig.load().getWorkspace();
		return workspace + File.separator + user + "/stubs";
	}

	public String getRepository() {
		return repository;
	}

	public String getEnvironment() {
		return environment;
	}

	public String getWorkspace() {
		return workspace;
	}

	public String getStubCodeDirectory(){
		//TODO
		return workspace;
	}
}
