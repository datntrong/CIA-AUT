package uet.fit.aut.logger;

import uet.fit.config.Config;

public interface Locations {

	String Z3_PATH = "/z3-4.8.9-x64-ubuntu/bin/z3";
	String STUB_LIB_PATH = "/cpp-stub.zip";
	static String getHome() {
		return Config.getHomePath();
	}
}
