package uet.fit.client.ui.view;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import uet.fit.client.common.User;
import uet.fit.client.ui.controller.IPreviousStateCleanable;

import static uet.fit.client.ui.view.IEnvironmentBuilderStep.*;

public class NewUnitTestView extends AnchorPane implements IPreviousStateCleanable {

	private final IEnvironmentBuilderStep[] envBuilderSteps = new IEnvironmentBuilderStep[STEPS];

	private String repositoryUrl, repositoryVersion;

	public NewUnitTestView() {
		super();

		setEnvBuilderStep(STEP_CLONE_REPOSITORY);
	}

	public <T extends Node> T setEnvBuilderStep(int step) {
		if (envBuilderSteps[step] == null) {
			switch (step) {
				case STEP_BROWSE_REPOSITORY:
					envBuilderSteps[step] = new BrowseRepositoryView();
					break;

				case STEP_CLONE_REPOSITORY:
					CloneRepoView cloneRepoView = new CloneRepoView();
					cloneRepoView.setOnClone(this::cloneAUT);
					cloneRepoView.setOnBrowse(mouseEvent -> setEnvBuilderStep(STEP_BROWSE_REPOSITORY));
					envBuilderSteps[step] = cloneRepoView;
					break;

				case STEP_CHOOSE_VERSION:
					envBuilderSteps[step] = new ChooseVersionView();
					break;

				case STEP_CONFIG_ENVIRONMENT:
					envBuilderSteps[step] = new CreateEnvironmentView();
					break;
			}
		}

		T node = (T) envBuilderSteps[step];
		setContent(node);
		return node;
	}

	private void cloneAUT(CloneRepoView.CloneEvent event) {
		String username = event.getUsername();
		String url = event.getUrl();
		User.Git git = event.getGit();
		ChooseVersionView chooseVersionView = setEnvBuilderStep(STEP_CHOOSE_VERSION);
		chooseVersionView.getController()
				.setGitInfo(username, url, git.getName(), git.getPassword());
		setCurrentRepositoryUrl(url);
	}

	private void setContent(Node node) {
		getChildren().clear();
		getChildren().add(node);
		AnchorPane.setTopAnchor(node, 0.0);
		AnchorPane.setBottomAnchor(node, 0.0);
		AnchorPane.setRightAnchor(node, 0.0);
		AnchorPane.setLeftAnchor(node, 0.0);
	}

	public String getCurrentRepositoryUrl() {
		return repositoryUrl;
	}

	public String getCurrentRepositoryVersion() {
		return repositoryVersion;
	}

	public void setCurrentRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}

	public void setCurrentRepositoryVersion(String repositoryVersion) {
		this.repositoryVersion = repositoryVersion;
	}

	@Override
	public void clearPreviousState() {
		for (IEnvironmentBuilderStep step : envBuilderSteps)
			if (step != null) step.clearState();
		setEnvBuilderStep(STEP_CLONE_REPOSITORY);
		setCurrentRepositoryVersion(null);
		setCurrentRepositoryUrl(null);
	}
}
