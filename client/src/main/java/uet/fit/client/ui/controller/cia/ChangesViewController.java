package uet.fit.client.ui.controller.cia;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CiaRequest;
import uet.fit.cia.communicate.CompareRequest;
import uet.fit.cia.communicate.DifferenceResponse;
import uet.fit.cia.communicate.DifferencesResponse;
import uet.fit.cia.communicate.FileRequest;
import uet.fit.cia.communicate.Utils;
import uet.fit.cia.cpp.display.ProjectResponse;
import uet.fit.cia.cpp.display.ProjectResponse.Element;
import uet.fit.cia.cpp.display.ProjectResponse.Location;
import uet.fit.cia.cpp.display.ProjectResponse.Position;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.cia.obj.ImpactedElement;
import uet.fit.client.ui.controller.cia.obj.ModifiedElement;
import uet.fit.client.utils.CiaHttpUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import static uet.fit.client.ui.controller.cia.LogsViewController.logError;
import static uet.fit.client.ui.controller.cia.LogsViewController.logInfo;
import static uet.fit.client.ui.controller.cia.LogsViewController.logNormal;

public final class ChangesViewController {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ChangesViewController.class);

	private static final @NotNull Path EMPTY_PATH = Path.of("");

	public static final @NotNull Map<ProjectResponse.Change, String> CHANGE_CLASS_MAP = Map.of(
			ProjectResponse.Change.ADDED, "cia-added",
			ProjectResponse.Change.CHANGED, "cia-changed",
			ProjectResponse.Change.UNCHANGED, "cia-unchanged",
			ProjectResponse.Change.REMOVED, "cia-removed"
	);
	public static final @NotNull Map<ProjectResponse.Type, String> TYPE_CLASS_MAP = Map.ofEntries(
			Map.entry(ProjectResponse.Type.ROOT, "cia-root"),
			Map.entry(ProjectResponse.Type.DIRECTORY, "cia-directory"),
			Map.entry(ProjectResponse.Type.FILE, "cia-file"),
			Map.entry(ProjectResponse.Type.CLASS, "cia-class"),
			Map.entry(ProjectResponse.Type.ENUM, "cia-enum"),
			Map.entry(ProjectResponse.Type.FUNCTION, "cia-function"),
			Map.entry(ProjectResponse.Type.NAMESPACE, "cia-namespace"),
			Map.entry(ProjectResponse.Type.STRUCT, "cia-struct"),
			Map.entry(ProjectResponse.Type.TYPEDEF, "cia-typedef"),
			Map.entry(ProjectResponse.Type.UNION, "cia-union"),
			Map.entry(ProjectResponse.Type.VARIABLE, "cia-variable")
	);
	public static final @NotNull Map<ProjectResponse.Type, String> TYPE_ICON_MAP = Map.ofEntries(
			Map.entry(ProjectResponse.Type.ROOT, "/images/cia/folder.fxml"),
			Map.entry(ProjectResponse.Type.DIRECTORY, "/images/cia/folder.fxml"),
			Map.entry(ProjectResponse.Type.FILE, "/images/cia/file.fxml"),
			Map.entry(ProjectResponse.Type.CLASS, "/images/cia/class.fxml"),
			Map.entry(ProjectResponse.Type.ENUM, "/images/cia/enum.fxml"),
			Map.entry(ProjectResponse.Type.FUNCTION, "/images/cia/function.fxml"),
			Map.entry(ProjectResponse.Type.NAMESPACE, "/images/cia/namespace.fxml"),
			Map.entry(ProjectResponse.Type.STRUCT, "/images/cia/struct.fxml"),
			Map.entry(ProjectResponse.Type.TYPEDEF, "/images/cia/typedef.fxml"),
			Map.entry(ProjectResponse.Type.UNION, "/images/cia/union.fxml"),
			Map.entry(ProjectResponse.Type.VARIABLE, "/images/cia/variable.fxml")
	);

	@FXML private @NotNull Region content;

	@FXML private @NotNull TabPane tpMain;
	@FXML private @NotNull TabPane tpSource;
	@FXML private @NotNull Tab tabSource;

	@FXML private @NotNull TreeTableView<MyTreeItem> ttvFileStructure;
	@FXML private @NotNull TreeTableColumn<MyTreeItem, String> clComponentFS;
	@FXML private @NotNull TreeTableColumn<MyTreeItem, String> clImpactFS;
	@FXML private @NotNull TreeTableView<MyTreeItem> ttvLanguageStructure;

	@FXML private @NotNull TreeTableColumn<MyTreeItem, String> clComponentLS;
	@FXML private @NotNull TreeTableColumn<MyTreeItem, String> clImpactLS;
	@FXML private @NotNull TableView<ModifiedElement> tvModificationDetails;
	@FXML private @NotNull TableColumn<ModifiedElement, String> clFunction;
	@FXML private @NotNull TableColumn<ModifiedElement, String> clOldPath;
	@FXML private @NotNull TableColumn<ModifiedElement, String> clNewPath;
	@FXML private @NotNull TableColumn<ModifiedElement, String> clType;

	@FXML private @NotNull TableView<ImpactedElement> tvImpactedComponents;
	@FXML private @NotNull TableColumn<ImpactedElement, String> clICComponent;
	@FXML private @NotNull TableColumn<ImpactedElement, String> clICPath;
	@FXML private @NotNull TableColumn<ImpactedElement, String> clICType;
	@FXML private @NotNull TableColumn<ImpactedElement, String> clICImpact;

	public static @NotNull ChangesViewController create() {
		try {
			final FXMLLoader loader = new FXMLLoader(ChangesViewController.class
					.getResource("/fxml/cia/ChangesView.fxml"));
			loader.load();
			return loader.getController();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final @NotNull Map<Element, Path> fileElementPathMap = new HashMap<>();
	private final @NotNull Map<FileRequest, String> sourceCodeCache = new HashMap<>();
	private final @NotNull Map<FileRequest, Set<Pair<SourceCompareController, Boolean>>> singleSourceViews = new HashMap<>();
	private final @NotNull Map<Pair<FileRequest, FileRequest>, SourceCompareController> pairSourceViews = new HashMap<>();

	private final @NotNull SortedSet<MyTreeItem> directoryTreeItems = new TreeSet<>(ChangesViewController::compareByPath);


	private @NotNull Element[] displayElements = new Element[0];
	private @NotNull String commitA = "";
	private @NotNull String commitB = "";

	private ObservableValue<String> getCallBackRepresentName(TreeTableColumn.CellDataFeatures<MyTreeItem,
			String> param) {
		String name = null;
		MyTreeItem data = param.getValue().getValue();
		if (data != null) {
			name = data.getElement().getName();
		}
		return new ReadOnlyStringWrapper(name);
	}

	private ObservableValue<String> getCallBackRepresentImpact(TreeTableColumn.CellDataFeatures<MyTreeItem,
			String> param) {
		MyTreeItem data = param.getValue().getValue();
		double impact;
		if (data != null) {
			final List<ProjectResponse.Type> types = new ArrayList<>(Set.of(
					ProjectResponse.Type.NAMESPACE,
					ProjectResponse.Type.VARIABLE,
					ProjectResponse.Type.FUNCTION,
					ProjectResponse.Type.CLASS,
					ProjectResponse.Type.ENUM,
					ProjectResponse.Type.STRUCT,
					ProjectResponse.Type.TYPEDEF,
					ProjectResponse.Type.UNION));
			impact = data.getElement().getImpact();
			impact = Math.round(impact * 100.0) / 100.0;
			if (types.contains(data.getElement().getType()))
				return new ReadOnlyStringWrapper(String.valueOf(impact));
			else return null;
		}
		return null;
	}

	@FXML
	private void initialize() {
		ttvFileStructure.setTableMenuButtonVisible(true);
		ttvLanguageStructure.setTableMenuButtonVisible(true);

		clComponentFS.setCellFactory(param -> new MyTreeTableCell());
		clComponentFS.setCellValueFactory(this::getCallBackRepresentName);
		clImpactFS.setCellValueFactory(this::getCallBackRepresentImpact);

		clComponentLS.setCellFactory(param -> new MyTreeTableCell());
		clComponentLS.setCellValueFactory(this::getCallBackRepresentName);
		clImpactLS.setCellValueFactory(this::getCallBackRepresentImpact);

		tvModificationDetails.setRowFactory(view -> new MyTableRow<>());

		clFunction.setCellValueFactory(item ->
				new SimpleStringProperty(item.getValue().getName()));
		clOldPath.setCellValueFactory(item ->
				new SimpleStringProperty(Objects.requireNonNullElse(item.getValue().getOldPath(), "").toString()));
		clNewPath.setCellValueFactory(item ->
				new SimpleStringProperty(Objects.requireNonNullElse(item.getValue().getNewPath(), "").toString()));
		clType.setCellValueFactory(item ->
				new SimpleStringProperty(item.getValue().getChange().toString()));

		tvImpactedComponents.setRowFactory(view -> new MyTableRow<>());
		clICComponent.setCellValueFactory(item ->
				new SimpleStringProperty(item.getValue().getName()));
		clICPath.setCellValueFactory(item ->
				new SimpleStringProperty(Objects.requireNonNull(item.getValue().getNewPath()).toString()));
		clICType.setCellValueFactory(item ->
				new SimpleStringProperty(Objects.requireNonNull(item.getValue().getChange()).toString()));
		clICImpact.setCellValueFactory(item ->
				new SimpleStringProperty(String.valueOf(item.getValue().getImpact())));
	}

	public @NotNull Region getContent() {
		return content;
	}

	public void compareChosenCommits(@NotNull String commitA, @NotNull Path proPathA,
			@NotNull String commitB, @NotNull Path proPathB, boolean forceReload) {
		content.setDisable(true);
		this.commitA = commitA;
		this.commitB = commitB;

		logInfo("Comparing commit " + commitA + " to commit " + commitB);

		final CIAMainViewController instance = CIAMainViewController.getInstance();
		final String username = instance.getUsername();
		final String gitUrl = instance.getGitUrl();
		final String gitUsername = instance.getGitUsername();
		final String gitPassword = instance.getGitPassword();

		final CompletableFuture<ProjectResponse> compareFuture = CiaHttpUtils.compareCommits(
						CiaRequest.of(username, gitUrl, gitUsername, gitPassword,
								commitA, commitB, proPathA, proPathB, forceReload))
				.exceptionally(throwable -> {
					logError("Failed to compare commit " + commitA + " to commit " + commitB + " using CIA");
					if (throwable != null) {
						logNormal(throwable.getMessage());
						LOGGER.error("CiaHttpUtils.compareCommits throw!", throwable);
					}
					return null;
				});
		final CompletableFuture<DifferencesResponse> compareTreesFuture = CiaHttpUtils.compareTrees(
						new CompareRequest(username, gitUrl, gitUsername, gitPassword, commitA, commitB))
				.exceptionally(throwable -> {
					logError("Failed to compare commit " + commitA + " to commit " + commitB + " using git");
					if (throwable != null) {
						logNormal(throwable.getMessage());
						LOGGER.error("CiaHttpUtils.compareTrees throw!", throwable);
					}
					return null;
				});
		compareFuture.thenAcceptBothAsync(compareTreesFuture, (projectResponse, compareTreesResponses) -> {
			if (projectResponse != null && compareTreesResponses != null) {
				if (!processData(projectResponse, compareTreesResponses)) {
					final Alert alert = UIHelper.showErrorAlert("Failed to display compare result.",
							"Success comparing commit " + commitA + " to commit " + commitB
									+ " but failed to display the result.");
					Platform.runLater(alert::showAndWait);
				}
				content.setDisable(false);
			} else {
				final Alert alert = UIHelper.showErrorAlert("Failed to compare commit.",
						"Failed to compare commit " + commitA + " to commit " + commitB);
				Platform.runLater(alert::showAndWait);
			}
		});
	}

	private void addData(@NotNull DifferencesResponse differencesResponse) {
		final Set<Path> paths = new HashSet<>(fileElementPathMap.values());
		final Map<Path, MyTreeItem> map = new HashMap<>();
		final Path root = EMPTY_PATH;
		for (final MyTreeItem item : directoryTreeItems) {
			final Path path = Objects.requireNonNullElse(item.getPath(), root);
			map.put(path, item);
		}
		directoryTreeItems.clear();

		for (DifferenceResponse differenceResponse : differencesResponse.getDifferences()) {
			Path path = differenceResponse.getPath(EMPTY_PATH);
			if (paths.contains(path)) continue;
			if (map.containsKey(path)) continue;

			Path pathFromClosest = path;
			while (!map.containsKey(pathFromClosest)) {
				pathFromClosest = pathFromClosest.getParent();
				if (pathFromClosest == null) {
					pathFromClosest = root;
					break;
				}
			}

			Element file = new Element();
			file.setType(ProjectResponse.Type.FILE);
			file.setName(path.getFileName().toString());
			file.setChange(ProjectResponse.Change.valueOf(differenceResponse.getChange().name()));

			MyTreeItem currentItem = new MyTreeItem(file, List.of(), path);
			currentItem.setAdditional(true);

			Path tmpPath = path;
			while (true) {
				tmpPath = tmpPath.getParent(); // a directory
				if (tmpPath == null) tmpPath = root;
				if (map.containsKey(tmpPath)) break;

				Element dir = new Element();
				dir.setType(ProjectResponse.Type.DIRECTORY);
				dir.setName(tmpPath.getFileName().toString());
				dir.setChange(ProjectResponse.Change.UNCHANGED);

				MyTreeItem treeItem = new MyTreeItem(dir, List.of(), tmpPath);
				treeItem.setAdditional(true);
				treeItem.getChildren().add(currentItem);
				currentItem = treeItem;

				map.put(tmpPath, currentItem);
			}


			final MyTreeItem closestParent = map.get(pathFromClosest);
			closestParent.getChildren().add(currentItem);
		}
		for (final MyTreeItem item : map.values()) {
			item.getChildren().sort(ChangesViewController::compareTreeItem);
		}
	}

	private static int compareByPath(@NotNull MyTreeItem itemA, @NotNull MyTreeItem itemB) {
		final Path pathA = itemA.getPath();
		final Path pathB = itemB.getPath();
		if (pathA == null) return pathB == null ? 0 : 1;
		if (pathB == null) return -1;
		int compare = Integer.compare(pathB.getNameCount(), pathA.getNameCount());
		return compare != 0 ? compare : pathB.compareTo(pathA);
	}


	private @NotNull CompletableFuture<String> getSourceCode(@NotNull FileRequest fileRequest) {
		final String cachedSource = sourceCodeCache.get(fileRequest);
		if (cachedSource != null) return CompletableFuture.completedFuture(cachedSource);
		final Path filePath = fileRequest.getPath(EMPTY_PATH);
		final String obfuscatedPath = Utils.obfuscatePath(filePath);
		final String commitHash = fileRequest.getCommit();
		logInfo("Getting the source code of the file " + obfuscatedPath + " from commit " + commitHash);
		return CiaHttpUtils.getFileContent(fileRequest).handleAsync((fileContent, throwable) -> {
			if (fileContent != null) {
				logInfo("Success getting the source code of the file " + obfuscatedPath + " from commit " + commitHash);
				sourceCodeCache.put(fileRequest, fileContent.getContent());
				return fileContent.getContent();
			} else {
				logError("Failed getting the source code of the file " + obfuscatedPath + " from commit " + commitHash);
				if (throwable != null) {
					logNormal(throwable.getMessage());
					LOGGER.error("CiaHttpUtils.getFileContent throw!", throwable);
				}
				return "";
			}
		});
	}

	private void activeTab(@NotNull Tab tab) {
		// select Source Tab
		tpMain.getSelectionModel().select(tabSource);

		// select the file tab
		final ObservableList<Tab> sourceTabs = tpSource.getTabs();
		if (!sourceTabs.contains(tab)) sourceTabs.add(tab);
		tpSource.getSelectionModel().select(tab);
	}

	private void viewSingleSourceCode(boolean old, @NotNull FileRequest fileRequest, int startLine, int endLine) {
		final Set<Pair<SourceCompareController, Boolean>> pairSet = singleSourceViews.get(fileRequest);
		if (pairSet != null) {
			final Pair<SourceCompareController, Boolean> pair = pairSet.stream().findFirst().orElse(null);
			if (pair != null) {
				final SourceCompareController controller = pair.getKey();
				activeTab(controller.getTab());
				controller.highlight(pair.getValue(), startLine, endLine, true);
				return;
			}
		}
		getSourceCode(fileRequest).thenAcceptAsync(source -> {
			final SourceCompareController compareController = SourceCompareController.create();
			compareController.configSingleMode(old);
			compareController.setTabTitle(fileRequest.getPath(EMPTY_PATH).toString());
			final Pair<SourceCompareController, Boolean> key = new Pair<>(compareController, old);
			singleSourceViews.computeIfAbsent(fileRequest, any -> new HashSet<>()).add(key);
			compareController.getTab().setOnCloseRequest(event -> singleSourceViews.get(fileRequest).remove(key));
			Platform.runLater(() -> {
				activeTab(compareController.getTab());
				compareController.setSourceCode(old, source);
				compareController.highlight(old, startLine, endLine, true);
			});
		});
	}

	private void viewSourceCodePair(@NotNull FileRequest fileRequest0, int startLine0, int endLine0,
			@NotNull FileRequest fileRequest1, int startLine1, int endLine1) {
		final Pair<FileRequest, FileRequest> cacheKey = new Pair<>(fileRequest0, fileRequest1);
		{
			final SourceCompareController compareController = pairSourceViews.get(cacheKey);
			if (compareController != null) { // created, so no need to request to server
				activeTab(compareController.getTab());
				compareController.highlight(true, startLine0, endLine0, false);
				compareController.highlight(false, startLine1, endLine1, false);
				return;
			}
		}

		getSourceCode(fileRequest0).thenAcceptBothAsync(getSourceCode(fileRequest1), (source0, source1) -> {
			final SourceCompareController compareController = SourceCompareController.create();
			compareController.setTabTitle(fileRequest0.getPath(EMPTY_PATH)
					+ " -> " + fileRequest1.getPath(EMPTY_PATH));
			pairSourceViews.put(cacheKey, compareController);
			final Pair<SourceCompareController, Boolean> oldKey = new Pair<>(compareController, true);
			final Pair<SourceCompareController, Boolean> newKey = new Pair<>(compareController, false);
			singleSourceViews.computeIfAbsent(fileRequest0, any -> new HashSet<>()).add(oldKey);
			singleSourceViews.computeIfAbsent(fileRequest1, any -> new HashSet<>()).add(newKey);
			compareController.getTab().setOnCloseRequest(event -> {
				pairSourceViews.remove(cacheKey);
				singleSourceViews.get(fileRequest0).remove(oldKey);
				singleSourceViews.get(fileRequest1).remove(newKey);
			});
			Platform.runLater(() -> {
				activeTab(compareController.getTab());
				compareController.setSourceCode(true, source0);
				compareController.setSourceCode(false, source1);
				compareController.highlight(true, startLine0, endLine0, false);
				compareController.highlight(false, startLine1, endLine1, false);
			});
		});
	}

	private @Nullable FileRequest createFileRequest(boolean oldCommit, @NotNull Element element) {
		if (element.getType() == null) return null;
		if (!element.getType().equals(ProjectResponse.Type.FILE)) return null;
		final CIAMainViewController instance = CIAMainViewController.getInstance();
		final String username = instance.getUsername();
		final String gitUrl = instance.getGitUrl();
		final String gitUsername = instance.getGitUsername();
		final String gitPassword = instance.getGitPassword();
		final Path filePath = fileElementPathMap.get(element);
		return FileRequest.of(username, gitUrl, gitUsername, gitPassword, oldCommit ? commitA : commitB, filePath);
	}

	private @Nullable FileRequest createFileRequest(boolean oldCommit, @NotNull Location location) {
		final Position position = oldCommit ? location.getOldPosition() : location.getNewPosition();
		return position != null ? createFileRequest(oldCommit, getDisplayElement(position.getFile())) : null;
	}

	private void viewSingleSourceCode(boolean oldCommit, @NotNull Element element) {
		final FileRequest fileRequest = createFileRequest(oldCommit, element);
		if (fileRequest != null) viewSingleSourceCode(oldCommit, fileRequest, -1, -1);
	}

	private void viewSingleSourceCode(boolean oldCommit, @NotNull Location location) {
		final FileRequest fileRequest = createFileRequest(oldCommit, location);
		if (fileRequest == null) return;
		final Position position = oldCommit ? location.getOldPosition() : location.getNewPosition();
		final int startLine = position != null ? position.getStartLine() : -1;
		final int endLine = position != null ? position.getEndLine() : -1;
		viewSingleSourceCode(oldCommit, fileRequest, startLine, endLine);
	}

	private void viewSourceCodePair(@NotNull Element element) {
		final FileRequest fileRequest0 = createFileRequest(true, element);
		if (fileRequest0 == null) return;
		final FileRequest fileRequest1 = createFileRequest(false, element);
		if (fileRequest1 == null) return;
		viewSourceCodePair(fileRequest0, -1, -1, fileRequest1, -1, -1);
	}

	private void viewSourceCodePair(@NotNull Location location) {
		final FileRequest fileRequest0 = createFileRequest(true, location);
		if (fileRequest0 == null) return;
		final FileRequest fileRequest1 = createFileRequest(false, location);
		if (fileRequest1 == null) return;
		final Position oldPosition = location.getOldPosition();
		final int startLine0 = oldPosition != null ? oldPosition.getStartLine() : -1;
		final int endLine0 = oldPosition != null ? oldPosition.getEndLine() : -1;
		final Position newPosition = location.getNewPosition();
		final int startLine1 = newPosition != null ? newPosition.getStartLine() : -1;
		final int endLine1 = newPosition != null ? newPosition.getEndLine() : -1;
		viewSourceCodePair(fileRequest0, startLine0, endLine0, fileRequest1, startLine1, endLine1);
	}

	private boolean processData(@NotNull ProjectResponse projectResponse,
			@NotNull DifferencesResponse differencesResponse) {
		try {
			final Element[] displayElements = projectResponse.getElements();
			if (displayElements == null) {
				logNormal("Cannot show project tree: Display elements array is null!");
				return false;
			}
			this.displayElements = displayElements;

			final Element fileRoot = getDisplayElement(projectResponse.getFileRoot());
			final MyTreeItem fileStructureRootItem
					= createTreeItemForFileStructure(fileRoot, null, EMPTY_PATH);

			final Element languageRoot = getDisplayElement(projectResponse.getLanguageRoot());
			final MyTreeItem componentStructureRootItem = createTreeItemForLanguageStructure(languageRoot);

			addData(differencesResponse);

			Platform.runLater(() -> {
				ttvFileStructure.setRoot(fileStructureRootItem);
				ttvLanguageStructure.setRoot(componentStructureRootItem);
			});

			return true;
		} catch (final Exception exception) {
			LOGGER.error("Processing data to show failed!", exception);
			return false;
		}
	}

	private @NotNull List<Location> filterLocationByFile(@NotNull Element element, @NotNull Element fileElement) {
		final Location[] locations = element.getLocations();
		if (locations == null) return List.of();
		final List<Location> filteredLocations = new ArrayList<>();
		for (final Location location : locations) {
			final Position oldPosition = location.getOldPosition();
			if (oldPosition != null && fileElement == getDisplayElement(oldPosition.getFile())) {
				filteredLocations.add(location);
			} else {
				final Position newPosition = location.getNewPosition();
				if (newPosition != null && fileElement == getDisplayElement(newPosition.getFile())) {
					filteredLocations.add(location);
				}
			}
		}
		return filteredLocations;
	}

	private @Nullable MyTreeItem createTreeItemForFileStructure(@NotNull Element parent, @Nullable Element parentFile,
			@NotNull Path path) {
		final List<Location> filteredLocations = parentFile != null
				? filterLocationByFile(parent, parentFile)
				: List.of();

		final MyTreeItem parentItem = new MyTreeItem(parent, filteredLocations, path);
		final ObservableList<TreeItem<MyTreeItem>> childItems = parentItem.getChildren();

		final ProjectResponse.Type parentType = Objects.requireNonNull(parent.getType());
		if (parentType == ProjectResponse.Type.DIRECTORY || parentType == ProjectResponse.Type.ROOT) {
			directoryTreeItems.add(parentItem);
		}

		final int[] childrenId = parent.getChildren();
		if (childrenId != null) {
			for (int childId : childrenId) {
				final Element child = getDisplayElement(childId);
				final String name = Objects.requireNonNullElse(child.getName(), "");
				final ProjectResponse.Type type = Objects.requireNonNull(child.getType());
				final Path childPath = type == ProjectResponse.Type.FILE || type == ProjectResponse.Type.DIRECTORY
						? path.resolve(name)
						: path;
				if (type == ProjectResponse.Type.FILE) {
					fileElementPathMap.put(child, childPath);
				}
				final MyTreeItem childItem = createTreeItemForFileStructure(child,
						type == ProjectResponse.Type.FILE ? child : parentFile, childPath);
				if (childItem != null) childItems.add(childItem);
			}
		}

		if (childItems.isEmpty()) {
			// skip this element if this
			if (filteredLocations.isEmpty()
					&& parentType != ProjectResponse.Type.ROOT
					&& parentType != ProjectResponse.Type.DIRECTORY
					&& parentType != ProjectResponse.Type.FILE) {
				return null;
			}
		} else {
			// sort a to z, folder first, file later
			if (parentType == ProjectResponse.Type.ROOT || parentType == ProjectResponse.Type.DIRECTORY) {
				childItems.sort(ChangesViewController::compareTreeItem);
			}
		}
		return parentItem;
	}

	private @NotNull MyTreeItem createTreeItemForLanguageStructure(@NotNull Element parent) {
		final Location[] locations = parent.getLocations();
		final MyTreeItem parentItem = new MyTreeItem(parent, locations, null);

		final String parentName = Objects.requireNonNullElse(parent.getName(), "");
		final ProjectResponse.Type parentType = Objects.requireNonNull(parent.getType());
		final ProjectResponse.Change parentChange = Objects.requireNonNull(parent.getChange());

		if (locations != null ) {
			for (final Location location : locations) {
				final ProjectResponse.Change change = Objects.requireNonNull(location.getChange());
				final Position oldPosition = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNull(location.getOldPosition()) : null;
				final Position newPosition = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNull(location.getNewPosition()) : null;
				final Element oldFile = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNull(getDisplayElement(oldPosition.getFile())) : null;
				final Element newFile = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNull(getDisplayElement(newPosition.getFile())) : null;
				final Path oldPath = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNull(fileElementPathMap.get(oldFile)) : null;
				final Path newPath = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNull(fileElementPathMap.get(newFile)) : null;

				// Function filter for adding item to tvModificationDetails
				if (parentType == ProjectResponse.Type.FUNCTION
						&& parentChange != ProjectResponse.Change.UNCHANGED
						&& change != ProjectResponse.Change.UNCHANGED) {
					final ObservableList<ModifiedElement> items = tvModificationDetails.getItems();
					if (change == ProjectResponse.Change.ADDED) {
						items.add(new ModifiedElement(null, newPath, parentName, change, location));
					} else if (change == ProjectResponse.Change.CHANGED) {
						items.add(new ModifiedElement(oldPath, newPath, parentName, change, location));
					} else if (change == ProjectResponse.Change.REMOVED) {
						items.add(new ModifiedElement(oldPath, null, parentName, change, location));
					}
				}

				// element filter for adding item to tvImpactedComponents
				// just for didn't remove elements
				final ObservableList<ImpactedElement> items = tvImpactedComponents.getItems();
				if (parent.getImpact() > 0
						&& !parent.getType().equals(ProjectResponse.Type.ROOT)) { // not root elements
					items.add(new ImpactedElement(oldPath, newPath, parentName, change, location, parent.getImpact()));
				}
				items.sort(Comparator.comparing(ModifiedElement::getChange));
			}
		}

		final int[] childrenId = parent.getChildren();
		if (childrenId != null) {
			for (int childId : childrenId) {
				final Element child = getDisplayElement(childId);
				parentItem.getChildren().add(createTreeItemForLanguageStructure(child));
			}
		}
		return parentItem;
	}

	private @NotNull Element getDisplayElement(int id) {
		if (id < 0 || id > displayElements.length) {
			logNormal("Element id is out of bound. id = " + id);
			throw new IndexOutOfBoundsException("Element id is out of bound. id = " + id);
		}
		return displayElements[id];
	}

	private static int compareTreeItem(@NotNull TreeItem<MyTreeItem> itemA, @NotNull TreeItem<MyTreeItem> itemB) {
		final Element elementA = itemA.getValue().getElement();
		final Element elementB = itemB.getValue().getElement();
		final ProjectResponse.Type typeA = Objects.requireNonNullElse(elementA.getType(), ProjectResponse.Type.ROOT);
		final ProjectResponse.Type typeB = Objects.requireNonNullElse(elementB.getType(), ProjectResponse.Type.ROOT);
		if (typeA != typeB) return typeA.compareTo(typeB);
		final String nameA = Objects.requireNonNullElse(elementA.getName(), "");
		final String nameB = Objects.requireNonNullElse(elementB.getName(), "");
		return nameA.compareTo(nameB);
	}

	private static @Nullable Pane loadIcon(@NotNull ProjectResponse.Type type, boolean isAdditional) {
		try {
			final String path = isAdditional
					? type == ProjectResponse.Type.FILE ? "/images/cia/fileAlt.fxml"
					: type == ProjectResponse.Type.DIRECTORY ? "/images/cia/folderAlt.fxml" : null
					: TYPE_ICON_MAP.get(type);
			return path != null ? new FXMLLoader(ChangesViewController.class.getResource(path)).load() : null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final class MyTreeItem extends TreeItem<MyTreeItem> {
		private final @NotNull Element element;
		private final @Nullable Path path;
		private final @NotNull List<Location> locations;
		private boolean isAdditional = false;
		private @Nullable ContextMenu contextMenu = null;

		public MyTreeItem(@NotNull Element element, @NotNull Location @Nullable [] locations, @Nullable Path path) {
			this.element = element;
			this.locations = locations != null ? Arrays.asList(locations) : List.of();
			this.path = path;

			setValue(this);
		}

		public MyTreeItem(@NotNull Element element, @NotNull List<Location> locations, @Nullable Path path) {
			this.element = element;
			this.locations = locations;
			this.path = path;

			setValue(this);
		}

		public void setAdditional(boolean additional) {
			isAdditional = additional;
		}

		public @NotNull Element getElement() {
			return element;
		}

		public @Nullable Path getPath() {
			return path;
		}

		private void mouseClicked(@NotNull MouseEvent event) {
			if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() <= 1) return;
			final ProjectResponse.Type type = Objects.requireNonNull(element.getType());
			if (type == ProjectResponse.Type.DIRECTORY || type == ProjectResponse.Type.ROOT || isAdditional) return;
			if (type == ProjectResponse.Type.FILE) {
				final ProjectResponse.Change change = Objects.requireNonNull(element.getChange());
				if (change == ProjectResponse.Change.ADDED) {
					viewSingleSourceCode(false, element);
				} else if (change == ProjectResponse.Change.CHANGED || change == ProjectResponse.Change.UNCHANGED) {
					viewSourceCodePair(element);
				} else if (change == ProjectResponse.Change.REMOVED) {
					viewSingleSourceCode(true, element);
				}
			} else if (locations.size() > 2) {
				showContextMenu(event);
			} else if (!locations.isEmpty()) {
				final Location location = locations.get(0);
				final ProjectResponse.Change change = Objects.requireNonNull(location.getChange());
				if (change == ProjectResponse.Change.ADDED) {
					viewSingleSourceCode(false, location);
				} else if (change == ProjectResponse.Change.CHANGED || change == ProjectResponse.Change.UNCHANGED) {
					viewSourceCodePair(location);
				} else if (change == ProjectResponse.Change.REMOVED) {
					viewSingleSourceCode(true, location);
				}
			}
		}

		private void showContextMenu(@NotNull MouseEvent event) {
			final ContextMenu contextMenu = this.contextMenu != null
					? this.contextMenu
					: (this.contextMenu = new ContextMenu(createMenuItems()));
			contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
		}

		private @NotNull MenuItem @NotNull [] createMenuItems() {
			final List<MenuItem> items = new ArrayList<>(locations.size());
			for (final Location location : locations) {
				final MenuItem menuItem = new MenuItem();
				final ProjectResponse.Change change = Objects.requireNonNull(location.getChange());
				final Position oldPosition = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNull(location.getOldPosition()) : null;
				final Position newPosition = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNull(location.getNewPosition()) : null;
				final Element oldFile = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNull(getDisplayElement(oldPosition.getFile())) : null;
				final Element newFile = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNull(getDisplayElement(newPosition.getFile())) : null;
				final String oldFileName = change != ProjectResponse.Change.ADDED
						? Objects.requireNonNullElse(oldFile.getName(), "")
						+ '@' + oldPosition.getStartLine() + ':' + oldPosition.getEndLine()
						: null;
				final String newFileName = change != ProjectResponse.Change.REMOVED
						? Objects.requireNonNullElse(newFile.getName(), "")
						+ '@' + newPosition.getStartLine() + ':' + newPosition.getEndLine()
						: null;
//				menuItem.setStyle(CHANGE_CLASS_MAP.get(change));
				if (change == ProjectResponse.Change.ADDED) {
					menuItem.setText(newFileName);
					menuItem.setOnAction(event -> viewSingleSourceCode(false, location));
				} else if (change == ProjectResponse.Change.CHANGED || change == ProjectResponse.Change.UNCHANGED) {
					menuItem.setText(oldFileName + " -> " + newFileName);
					menuItem.setOnAction(event -> viewSourceCodePair(location));
				} else if (change == ProjectResponse.Change.REMOVED) {
					menuItem.setText(oldFileName);
					menuItem.setOnAction(event -> viewSingleSourceCode(true, location));
				}
				items.add(menuItem);
			}
			return items.toArray(MenuItem[]::new);
		}

		public @NotNull Menu createMenu() {
			final Menu menu = new Menu("Goto source code");
			final ProjectResponse.Type type = Objects.requireNonNull(element.getType());
			if (type == ProjectResponse.Type.FILE && !isAdditional) {
				final String fileName = Objects.requireNonNullElse(element.getName(), "");
				menu.setText("Goto " + fileName);
				final ProjectResponse.Change change = Objects.requireNonNull(element.getChange());
				menu.setStyle(CHANGE_CLASS_MAP.get(change));
				if (change == ProjectResponse.Change.ADDED) {
					menu.setOnAction(event -> viewSingleSourceCode(false, element));
				} else if (change == ProjectResponse.Change.CHANGED || change == ProjectResponse.Change.UNCHANGED) {
					menu.setOnAction(event -> viewSourceCodePair(element));
				} else if (change == ProjectResponse.Change.REMOVED) {
					menu.setOnAction(event -> viewSingleSourceCode(true, element));
				}
			} else if (type == ProjectResponse.Type.DIRECTORY || type == ProjectResponse.Type.ROOT
					|| locations.isEmpty() || isAdditional) {
				menu.setDisable(true);
			} else {
				menu.getItems().addAll(createMenuItems());
			}
			return menu;
		}

		@Override
		public @NotNull String toString() {
			final String name = element.getName();
			return name != null ? name : "null";
		}
	}

	// for project constructor tree
	private static final class MyTreeTableCell extends TreeTableCell<MyTreeItem, String> {
		@Override
		protected void updateItem(String s, boolean empty) {
			super.updateItem(s, empty);
			MyTreeItem item = getTreeTableRow().getItem();

			final ObservableList<String> styleClass = getTreeTableRow().getStyleClass();
			styleClass.removeAll(CHANGE_CLASS_MAP.values());
			styleClass.removeAll(TYPE_CLASS_MAP.values());
			if (empty || item == null) {
				setText(null);
				setContextMenu(null);
				setOnMouseClicked(null);
				setGraphic(null);
			} else {
				final Element element = item.getElement();
				final String name = Objects.requireNonNullElse(element.getName(), "");
				final ProjectResponse.Type type = Objects.requireNonNull(element.getType());
				final ProjectResponse.Change change = Objects.requireNonNull(element.getChange());
				setText(name);
				styleClass.add(CHANGE_CLASS_MAP.get(change));
				styleClass.add(TYPE_CLASS_MAP.get(type));
				setContextMenu(createContextMenu(item));
				setOnMouseClicked(item::mouseClicked);
				setGraphic(loadIcon(type, item.isAdditional));
			}
		}

		private @NotNull ContextMenu createContextMenu(@NotNull MyTreeItem item) {
			final ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(item.createMenu());
			return contextMenu;
		}
	}

	private final class MyTableRow<T extends ModifiedElement> extends TableRow<T> {
		@Override
		protected void updateItem(@Nullable T component, boolean empty) {
			super.updateItem(component, empty);

			final ObservableList<String> styleClass = getStyleClass();
			styleClass.removeAll(CHANGE_CLASS_MAP.values());
			if (empty || component == null) {
				setOnMouseClicked(null);
			} else {
				styleClass.add(CHANGE_CLASS_MAP.get(component.getChange()));
				setOnMouseClicked(this::mouseClicked);
			}
		}

		private void mouseClicked(@NotNull MouseEvent event) {
			if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() <= 1) return;
			final ModifiedElement modifiedElement = getItem();
			if (modifiedElement == null) return;
			switch (modifiedElement.getChange()) {
				case ADDED:
					viewSingleSourceCode(false, modifiedElement.getLocation());
					break;
				case CHANGED:
				case UNCHANGED:
					viewSourceCodePair(modifiedElement.getLocation());
					break;
				case REMOVED:
					viewSingleSourceCode(true, modifiedElement.getLocation());
					break;
			}
		}
	}
}