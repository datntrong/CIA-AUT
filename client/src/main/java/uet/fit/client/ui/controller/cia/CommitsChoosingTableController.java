package uet.fit.client.ui.controller.cia;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CommitResponse;
import uet.fit.cia.communicate.CommitsResponse;
import uet.fit.cia.communicate.FilterRequest;
import uet.fit.cia.communicate.FilterRequest.FilterType;
import uet.fit.cia.communicate.RepositoryRequest;
import uet.fit.cia.communicate.StringsResponse;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;
import uet.fit.client.utils.CiaHttpUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static uet.fit.client.ui.controller.cia.LogsViewController.logError;
import static uet.fit.client.ui.controller.cia.LogsViewController.logInfo;
import static uet.fit.client.ui.controller.cia.LogsViewController.logNormal;

public final class CommitsChoosingTableController implements IEnvironmentBuilderStep {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ChangesViewController.class);
	private static final int NUM_OF_LOADING_COMMITS = 128;

	@FXML private @NotNull Region content;

	@FXML private @NotNull TableView<CommitResponse> tvAllCommits;
	@FXML private @NotNull TableColumn<CommitResponse, Void> clChoose;
	@FXML private @NotNull TableColumn<CommitResponse, String> clCommit;
	@FXML private @NotNull TableColumn<CommitResponse, Void> clMessage;
	@FXML private @NotNull TableColumn<CommitResponse, String> clUser;
	@FXML private @NotNull TableColumn<CommitResponse, String> clDate;
	@FXML private @NotNull TableColumn<CommitResponse, String> clTags;

	@FXML private @NotNull ComboBox<String> cbTagFilterContent;
	@FXML private @NotNull ComboBox<String> cbBranchFilterContent;
	@FXML private @NotNull ComboBox<FilterType> cbFilterType;
	@FXML private @NotNull TextField tfMessageFilterContent;
	@FXML private @NotNull TextField tfCommitIDFilterContent;
	@FXML private @NotNull TextField tfAllFilterContent;
	@FXML private @NotNull Button bFilter;
	@FXML private @NotNull Button bClear;

	private final @NotNull SimpleBooleanProperty isLoading = new SimpleBooleanProperty(false);
	private final @NotNull SimpleBooleanProperty isExhausted = new SimpleBooleanProperty(false);

	private final @NotNull SimpleIntegerProperty chooseQuantity = new SimpleIntegerProperty(1);
	private final @NotNull SimpleBooleanProperty fullyChosen = new SimpleBooleanProperty(false);
	private final @NotNull ObservableList<CommitResponse> chosenCommits = FXCollections.observableArrayList();

	private @NotNull String username = "";
	private @NotNull String gitUrl = "";
	private @NotNull String gitUsername = "";
	private @NotNull String gitPassword = "";

	// region create and configure

	public void setChooseQuantity(int chooseQuantity) {
		this.chooseQuantity.set(chooseQuantity);
	}

	public void setGitInfo(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) {
		this.username = username;
		this.gitUrl = gitUrl;
		this.gitUsername = gitUsername;
		this.gitPassword = gitPassword;
	}

	public void reload() {
		Platform.runLater(() -> {
			clearState();
			loadRefs();
			loadCommits();
		});
	}

	// endregion create and configure

	public @NotNull Region getContent() {
		return content;
	}

	public @NotNull ObservableList<CommitResponse> getChosenCommits() {
		return FXCollections.unmodifiableObservableList(chosenCommits);
	}

	public @NotNull ReadOnlyBooleanProperty isFullyChosen() {
		return fullyChosen;
	}

	@FXML
	public void initialize() {
		// the filter
		cbFilterType.getItems().addAll(FilterType.values);
		cbFilterType.setValue(FilterType.ALL);
		cbFilterType.setOnScroll(event -> {
			final double deltaY = event.getDeltaY();
			if (deltaY == 0.0) return;
			final FilterType value = cbFilterType.getValue();
			final int newIndex = value.ordinal() + Double.compare(deltaY, 0);
			if (newIndex < 0 || newIndex >= FilterType.values.size()) return;
			cbFilterType.setValue(FilterType.values.get(newIndex));
		});
//		cbFilterType.getSelectionModel().selectedItemProperty()
//				.addListener((observableValue, old, newVal) -> tvAllCommits.getSelectionModel().clearSelection());

		final ObjectProperty<FilterType> filterType = cbFilterType.valueProperty();
		cbTagFilterContent.visibleProperty().bind(filterType.isEqualTo(FilterType.TAG));
		cbTagFilterContent.addEventFilter(KeyEvent.KEY_PRESSED, this::doFilterOnEnter);

		cbBranchFilterContent.visibleProperty().bind(filterType.isEqualTo(FilterType.BRANCH));
		cbBranchFilterContent.addEventFilter(KeyEvent.KEY_PRESSED, this::doFilterOnEnter);

		tfMessageFilterContent.visibleProperty().bind(filterType.isEqualTo(FilterType.MESSAGE));
		tfMessageFilterContent.setOnAction(this::doFilter);

		tfCommitIDFilterContent.visibleProperty().bind(filterType.isEqualTo(FilterType.COMMIT));
		tfCommitIDFilterContent.setOnAction(this::doFilter);

		tfAllFilterContent.visibleProperty().bind(filterType.isEqualTo(FilterType.ALL));
		bFilter.setOnAction(this::doFilter);
		bFilter.disableProperty().bind(isLoading);
		bClear.setOnAction(event -> chosenCommits.clear());

		// the table
		tvAllCommits.setSkin(new TableViewSkin<>(tvAllCommits) {{
			// this bit is the constructor of the anonymous TableViewSkin class. Java is weird sometimes...
			getVirtualFlow().positionProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue.doubleValue() >= 0.9 && !isExhausted.get()) {
					loadCommits();
				}
			});
			getVirtualFlow().cellCountProperty().addListener((observable, oldValue, newValue) -> {
				final TableRow<CommitResponse> cell = getVirtualFlow().getLastVisibleCell();
				if ((cell == null || cell.getIndex() == newValue.intValue() - 1) && !isExhausted.get()) {
					loadCommits();
				}
			});
		}});
		tvAllCommits.setOnKeyReleased(event -> {
			if (event.getCode() == KeyCode.SPACE) {
				final CommitResponse commitResponse = tvAllCommits.getSelectionModel().getSelectedItem();
				if (commitResponse != null) {
					final int quantity = chooseQuantity.get();
					if (!chosenCommits.remove(commitResponse)) {
						if (quantity == 1) {
							chosenCommits.setAll(commitResponse);
						} else if (chosenCommits.size() < quantity) {
							chosenCommits.add(commitResponse);
						}
					}
				}
			}
		});

		clChoose.setCellFactory(CheckBoxCell::new);
		clMessage.setCellFactory(MessageCell::new);

		clCommit.setCellValueFactory(item -> new SimpleStringProperty(
				CommitsChoosingController.commitGetShortHash(item.getValue())));
		clUser.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getAuthor()));
		clDate.setCellValueFactory(item -> new SimpleStringProperty(
				CommitsChoosingController.commitGetFormattedTime(item.getValue())));
		clTags.setCellValueFactory(item -> new SimpleStringProperty(
				CommitsChoosingController.commitGetTags(item.getValue())));

		// the list
		final IntegerBinding chosenSize = Bindings.size(chosenCommits);
		fullyChosen.bind(chosenSize.greaterThanOrEqualTo(chooseQuantity));
		chosenSize.addListener((observable, oldValue, newValue) ->
				chosenCommits.sort(Comparator.comparing(CommitResponse::getTimeStamp)));
	}

	@Override
	public void clearState() {
		chosenCommits.clear();
		tvAllCommits.getItems().clear();
		cbTagFilterContent.getItems().clear();
		cbTagFilterContent.setValue(null);
		cbBranchFilterContent.getItems().clear();
		cbBranchFilterContent.setValue(null);
		cbFilterType.setValue(FilterType.ALL);
		tfMessageFilterContent.clear();
		tfAllFilterContent.clear();
	}

	private void doFilterOnEnter(@NotNull KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) doFilter(event);
	}

	private void doFilter(@NotNull Event event) {
		// clear
		tvAllCommits.getItems().clear();
		tvAllCommits.getSelectionModel().clearSelection();
		isExhausted.set(false);
		loadCommits();
	}

	private void loadCommits() {
		synchronized (isLoading) {
			if (isLoading.get()) return;
			isLoading.set(true);
		}
		logInfo("Getting commits...");
		CiaHttpUtils.getCommits(FilterRequest.of(username, gitUrl, gitUsername, gitPassword,
						cbFilterType.getValue(), getFilterContent(),
						tvAllCommits.getItems().size(), NUM_OF_LOADING_COMMITS))
				.whenCompleteAsync(this::onFinishLoadingCommits);
	}

	private void onFinishLoadingCommits(@Nullable CommitsResponse response, @Nullable Throwable throwable) {
		if (response != null) {
			logInfo("Success getting commits...");
			final CommitResponse[] commits = response.getCommits();
			if (commits.length > 0) {
				Platform.runLater(() -> {
					tvAllCommits.getItems().addAll(commits);
					isLoading.set(false);
				});
			} else {
				isExhausted.set(true);
				isLoading.set(false);
			}
		} else {
			logError("Failed getting commits...");
			final String message = throwable != null ? throwable.getMessage() : "";
			if (throwable != null) {
				logNormal(message);
				LOGGER.error("CiaHttpUtils.getCommits throw!", throwable);
			}

			final Alert alert = UIHelper.showErrorAlert("Getting commits failed.", message);
			Platform.runLater(alert::showAndWait);
			isLoading.set(false);
		}
	}

	private void loadRefs() {
		logInfo("Getting refs...");
		CiaHttpUtils.getRefs(RepositoryRequest.of(username, gitUrl, gitUsername, gitPassword))
				.whenCompleteAsync(this::onFinishLoadingRefs);
	}

	private void onFinishLoadingRefs(@Nullable StringsResponse response, @Nullable Throwable throwable) {
		if (response != null) {
			logInfo("Success getting refs...");
			final String[] refs = response.getStrings();
			final List<String> branches = new ArrayList<>();
			final List<String> tags = new ArrayList<>();
			for (final String ref : refs) {
				if (ref.startsWith("refs/tags/")) {
					tags.add(ref.replace("refs/tags/", ""));
				} else if (ref.startsWith("refs/heads/")) {
					branches.add(ref.replace("refs/heads/", ""));
				}
			}
			Utils.enableAutoComplete(cbBranchFilterContent, branches);
			Utils.enableAutoComplete(cbTagFilterContent, tags);
		} else {
			logError("Failed getting refs...");
			final String message = throwable != null ? throwable.getMessage() : "";
			if (throwable != null) {
				logNormal(message);
				LOGGER.error("CiaHttpUtils.getRefs throw!", throwable);
			}

			final Alert alert = UIHelper.showErrorAlert("Getting refs failed.", message);
			Platform.runLater(alert::showAndWait);
		}
	}

	private @NotNull String getFilterContent() {
		switch (cbFilterType.getValue()) {
			case TAG:
				return cbTagFilterContent.getEditor().getText();
			case BRANCH:
				return cbBranchFilterContent.getEditor().getText();
			case MESSAGE:
				return tfMessageFilterContent.getText();
			case COMMIT:
				return tfCommitIDFilterContent.getText();
		}
		return "";
	}

	private static final class MessageCell extends TableCell<CommitResponse, Void> {
		public MessageCell(@NotNull TableColumn<CommitResponse, Void> column) {
		}

		private @Nullable CommitResponse getRowItem() {
			final TableRow<CommitResponse> tableRow = getTableRow();
			return tableRow != null ? tableRow.getItem() : null;
		}

		@Override
		protected void updateItem(@Nullable Void unused, boolean empty) {
			super.updateItem(unused, empty);
			if (empty) {
				setTooltip(null);
				setText(null);
			} else {
				final CommitResponse item = getRowItem();
				if (item != null) {
					final String content = item.getMessage().trim();
					setTooltip(content.isEmpty() ? null : new Tooltip(content));
					final String line = content.lines().findFirst().orElse(content);
					setText(line.equals(content) ? line : line + "...");
				}
			}
		}
	}

	private final class CheckBoxCell extends TableCell<CommitResponse, Void> {
		private final @NotNull CheckBox checkBox = new CheckBox();

		public CheckBoxCell(@NotNull TableColumn<CommitResponse, Void> column) {
			checkBox.disableProperty().bind(chooseQuantity.greaterThan(1)
					.and(fullyChosen.and(checkBox.selectedProperty().not())));
			checkBox.setOnMouseClicked(this::handleMouseClicked);
			chosenCommits.addListener((ListChangeListener<CommitResponse>) this::onChosenCommits);
		}

		private <E> void onChosenCommits(@Nullable E unused) {
			final CommitResponse item = getRowItem();
			checkBox.setSelected(item != null && chosenCommits.contains(item));
		}

		@Override
		protected void updateItem(@Nullable Void unused, boolean empty) {
			super.updateItem(unused, empty);
			setGraphic(empty ? null : checkBox);
			onChosenCommits(null);
		}

		private @Nullable CommitResponse getRowItem() {
			final TableRow<CommitResponse> tableRow = getTableRow();
			return tableRow != null ? tableRow.getItem() : null;
		}

		private void handleMouseClicked(@NotNull MouseEvent event) {
			final CommitResponse commitResponse = getRowItem();
			if (commitResponse == null) return;
			if (!checkBox.isSelected()) {
				chosenCommits.remove(commitResponse);
			} else if (chooseQuantity.get() == 1) {
				chosenCommits.setAll(commitResponse);
			} else {
				chosenCommits.add(commitResponse);
			}
		}
	}
}
