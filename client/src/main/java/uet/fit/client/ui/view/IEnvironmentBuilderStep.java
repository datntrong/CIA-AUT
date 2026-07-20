package uet.fit.client.ui.view;

public interface IEnvironmentBuilderStep {

	int STEPS = 4;
	int STEP_CLONE_REPOSITORY = 0;
	int STEP_CHOOSE_VERSION = 1;
	int STEP_CONFIG_ENVIRONMENT = 2;
	int STEP_BROWSE_REPOSITORY = 3;

	void clearState();
}
