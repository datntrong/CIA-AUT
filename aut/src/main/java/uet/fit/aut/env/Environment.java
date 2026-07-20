package uet.fit.aut.env;

import uet.fit.aut.boundary.BoundOfDataTypes;
import uet.fit.aut.boundary.BoundaryManager;
import uet.fit.aut.config.FunctionConfig;

public class Environment {

	private static Environment instance;

	@Deprecated
	public static Environment getInstance() {
		if (instance == null)
			instance = new Environment();
		return instance;
	}

	// default function config for auto gen test
	private FunctionConfig defaultFunctionConfig;

	public FunctionConfig getDefaultFunctionConfig() {
		if (defaultFunctionConfig == null) {
			defaultFunctionConfig = new FunctionConfig();
		}
		return defaultFunctionConfig;
	}

	public static BoundOfDataTypes getBoundOfDataTypes() {
		return BoundaryManager.getInstance().getUsingBoundOfDataTypes();
	}

}
