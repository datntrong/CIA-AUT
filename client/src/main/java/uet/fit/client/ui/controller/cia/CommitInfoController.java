package uet.fit.client.ui.controller.cia;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CommitRequest;
import uet.fit.cia.communicate.CommitResponse;
import uet.fit.cia.communicate.PathResponse;
import uet.fit.cia.communicate.PathsResponse;
import uet.fit.client.utils.CiaHttpUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static uet.fit.client.ui.controller.cia.LogsViewController.logError;
import static uet.fit.client.ui.controller.cia.LogsViewController.logInfo;
import static uet.fit.client.ui.controller.cia.LogsViewController.logNormal;

public final class CommitInfoController {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CommitInfoController.class);
	private static final @NotNull Path EMPTY_PATH = Path.of("");

	@FXML private @NotNull TitledPane content;

	@FXML private @NotNull Label lShortHash;
	@FXML private @NotNull Label lAuthor;
	@FXML private @NotNull Label lDate;
	@FXML private @NotNull TextArea taMessage;
	@FXML private @NotNull ComboBox<Path> cbQtPro;

	private final @NotNull ObjectProperty<CommitResponse> observableCommit = new SimpleObjectProperty<>();
	private @NotNull ConcurrentMap<CommitResponse, PathsResponse> responseCache = new ConcurrentHashMap<>();


	public static @NotNull CommitInfoController create() {
		try {
			final FXMLLoader loader = new FXMLLoader(CommitInfoController.class
					.getResource("/fxml/cia/CommitInfo.fxml"));
			loader.load();
			return loader.getController();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@FXML
	private void initialize() {
		final PauseTransition transition = new PauseTransition(Duration.millis(100));
		observableCommit.addListener((observable, oldCommit, newCommit) -> {
			if (newCommit != null) {
				lShortHash.setText(CommitsChoosingController.commitGetShortHash(newCommit));
				lAuthor.setText(newCommit.getAuthor());
				lDate.setText(CommitsChoosingController.commitGetFormattedTime(newCommit));
				taMessage.setText(newCommit.getMessage());
				final PathsResponse cachedResponse = responseCache.get(newCommit);
				if (cachedResponse != null) {
					processResponse(cachedResponse);
				} else {
					cbQtPro.getItems().clear();
					cbQtPro.setValue(null);
					transition.playFromStart();
				}
			} else {
				lShortHash.setText("");
				lAuthor.setText("");
				lDate.setText("");
				taMessage.setText("");
				cbQtPro.getItems().clear();
				cbQtPro.setValue(null);
			}
		});

		transition.setOnFinished(event -> {
			final CommitResponse currentCommit = observableCommit.get();
			if (currentCommit == null) return;

			final PathsResponse cachedResponse = responseCache.get(currentCommit);
			if (cachedResponse != null) {
				processResponse(cachedResponse);
				return;
			}

			final CIAMainViewController instance = CIAMainViewController.getInstance();
			final String username = instance.getUsername();
			final String gitUrl = instance.getGitUrl();
			final String gitUsername = instance.getGitUsername();
			final String gitPassword = instance.getGitPassword();
			final String commitHash = currentCommit.getCommitHash();

			logInfo("Finding Qt .pro files from commit " + commitHash);


			CiaHttpUtils.findQtPros(CommitRequest.of(username, gitUrl, gitUsername, gitPassword, commitHash))
					.whenCompleteAsync((responses, throwable) -> {
						if (responses != null) {
							logInfo("Success finding Qt .pro files from commit " + commitHash);
							responseCache.put(currentCommit, responses);

							final CommitResponse nextCommit = observableCommit.get();
							if (nextCommit == currentCommit) processResponse(responses);
						} else {
							logError("Failed finding Qt .pro files from commit " + commitHash);
							if (throwable != null) {
								logNormal(throwable.getMessage());
								LOGGER.error("CiaHttpUtils.findQtPros throw!", throwable);
							}
						}
					});
		});
	}

	public void setTitle(@NotNull String title) {
		content.setText(title);
	}

	public void setResponseCache(@NotNull ConcurrentMap<CommitResponse, PathsResponse> responseCache) {
		this.responseCache = responseCache;
	}


	public @NotNull TitledPane getContent() {
		return content;
	}

	public @Nullable Path getRelativeProPath() {
		return cbQtPro.getSelectionModel().getSelectedItem();
	}


	private void processResponse(@NotNull PathsResponse pathsResponse) {
		Platform.runLater(() -> {
			final List<Path> proPaths = new ArrayList<>();
			for (final PathResponse pathResponse : pathsResponse.getPaths()) {
				proPaths.add(pathResponse.getPath(EMPTY_PATH));
			}
			cbQtPro.getItems().setAll(proPaths);
			cbQtPro.setValue(proPaths.isEmpty() ? null : proPaths.get(0));
		});
	}

	public void setCommit(@Nullable CommitResponse commit) {
		observableCommit.set(commit);
	}
}
