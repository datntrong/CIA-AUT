package uet.fit.client.ui.controller.cia;

import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CommitResponse;
import uet.fit.cia.communicate.PathsResponse;
import uet.fit.cia.communicate.RepositoryRequest;
import uet.fit.client.ui.view.CommitChoosingTableView;
import uet.fit.client.utils.CiaHttpUtils;
import uet.fit.util.Utils;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static uet.fit.client.ui.controller.cia.LogsViewController.logError;
import static uet.fit.client.ui.controller.cia.LogsViewController.logInfo;
import static uet.fit.client.ui.controller.cia.LogsViewController.logNormal;

public final class CommitsChoosingController {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CommitsChoosingController.class);
	private static final @NotNull SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@FXML private @NotNull VBox content;

	@FXML private @NotNull HBox hbVersions;
	@FXML private @NotNull Button bFetchRepository;
	@FXML private @NotNull Button bCompareCommits;
	@FXML private @NotNull CheckBox cbForceReload;
	@FXML private @NotNull CommitChoosingTableView tvCommits;

	private final @NotNull CommitInfoController oldVersionController = CommitInfoController.create();
	private final @NotNull CommitInfoController newVersionController = CommitInfoController.create();

	private @NotNull ObservableList<CommitResponse> chosenCommits = FXCollections.emptyObservableList();

	public static @NotNull CommitsChoosingController create() {
		try {
			final FXMLLoader loader = new FXMLLoader(CommitsChoosingController.class
					.getResource("/fxml/cia/CommitsChoosing.fxml"));
			loader.load();
			return loader.getController();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void initialize() {
		// selected commits
		oldVersionController.setTitle("Old version");
		newVersionController.setTitle("New version");

		final ConcurrentMap<CommitResponse, PathsResponse> responseCache = new ConcurrentHashMap<>();
		oldVersionController.setResponseCache(responseCache);
		newVersionController.setResponseCache(responseCache);

		final Region tpOldVersion = oldVersionController.getContent();
		final Region tpNewVersion = newVersionController.getContent();
		hbVersions.getChildren().addAll(tpOldVersion, tpNewVersion);

		final DoubleBinding halfWidth = hbVersions.widthProperty().divide(2);
		tpOldVersion.prefWidthProperty().bind(halfWidth);
		tpNewVersion.prefWidthProperty().bind(halfWidth);
		tpNewVersion.prefHeightProperty().bind(tpOldVersion.heightProperty());

		// the refresh button
		bFetchRepository.setOnAction(this::fetchRepository);

		// the compare button
		bCompareCommits.setDisable(true);
		bCompareCommits.setOnAction(this::compareCommits);

		// the table
		final CIAMainViewController instance = CIAMainViewController.getInstance();
		final String username = instance.getUsername();
		final String gitUrl = instance.getGitUrl();
		final String gitUsername = instance.getGitUsername();
		final String gitPassword = instance.getGitPassword();

		final CommitsChoosingTableController controller = tvCommits.getController();
		controller.setChooseQuantity(2);
		controller.setGitInfo(username, gitUrl, gitUsername, gitPassword);
		controller.reload();
		this.chosenCommits = controller.getChosenCommits();
		chosenCommits.addListener((ListChangeListener<CommitResponse>) this::showChosenVersions);
	}

	private <E> void fetchRepository(@NotNull E event) {
		final CIAMainViewController instance = CIAMainViewController.getInstance();
		final String username = instance.getUsername();
		final String gitUrl = instance.getGitUrl();
		final String gitUsername = instance.getGitUsername();
		final String gitPassword = instance.getGitPassword();
		final RepositoryRequest request = RepositoryRequest.of(username, gitUrl, gitUsername, gitPassword);

		logInfo("Fetching newest commits from remote repository of current project...");
		content.setDisable(true);
		CiaHttpUtils.fetchRepository(request).whenCompleteAsync((string, throwable) -> {
			content.setDisable(false);
			if (string != null) {
				logInfo("Success fetching newest commits from remote repository of current project!");
				tvCommits.getController().reload();
			} else {
				logError("Failed fetching newest commits from remote repository of current project!");
				if (throwable != null) {
					logNormal(throwable.getMessage());
					LOGGER.error("CiaHttpUtils.fetchRepository throw!", throwable);
				}
			}
		});
	}

	public @NotNull VBox getContent() {
		return content;
	}

	private <E> void showChosenVersions(@NotNull E event) {
		oldVersionController.setCommit(chosenCommits.size() >= 1 ? chosenCommits.get(0) : null);
		newVersionController.setCommit(chosenCommits.size() >= 2 ? chosenCommits.get(1) : null);
		bCompareCommits.setDisable(chosenCommits.size() < 2);
	}

	private <E> void compareCommits(@NotNull E event) {
		final CommitResponse[] responses = chosenCommits.toArray(CommitResponse[]::new);
		if (responses.length != 2) {
			logError("Cannot compare commits: need two commits to compare, got " + responses.length);
			return;
		}
		final Path oldProPath = oldVersionController.getRelativeProPath();
		if (oldProPath == null) {
			logError("Cannot compare commits: Qt .pro path for old version is not selected!");
			return;
		}
		final Path newProPath = newVersionController.getRelativeProPath();
		if (newProPath == null) {
			logError("Cannot compare commits: Qt .pro path for new version is not selected!");
			return;
		}

		final boolean forceReload = cbForceReload.isSelected();
		final ChangesViewController controller = ChangesViewController.create();
		CIAMainViewController.getInstance().setResultsViewContent(controller.getContent());
		controller.compareChosenCommits(responses[0].getCommitHash(), oldProPath,
				responses[1].getCommitHash(), newProPath, forceReload);
	}

	public static @NotNull String commitGetShortHash(@NotNull CommitResponse commit) {
		return Utils.shortenCommitHash(commit.getCommitHash());
	}

	public static @NotNull String commitGetTags(@NotNull CommitResponse commit) {
		final String[] rawRefs = commit.getRefs();
		final StringJoiner joiner = new StringJoiner(", ");
		for (final String rawName : rawRefs) {
			if (rawName.startsWith("refs/tags/")) {
				joiner.add(rawName.replace("refs/tags/", ""));
			}
		}
		return joiner.toString();
	}

	public static @NotNull String commitGetFormattedTime(@NotNull CommitResponse commit) {
		return DATE_FORMAT.format(commit.getTimeStamp() * 1000L);
	}
}
