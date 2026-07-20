package uet.fit.client.ui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import uet.fit.client.common.User;
import uet.fit.client.logger.WaitingLogs;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.DeleteEnviromentTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.cia.CIAMainViewController;
import uet.fit.client.ui.obj.ObservableProgress;
import uet.fit.client.ui.view.ProgressRow;
import uet.fit.client.ui.view.RingProgressIndicator;
import uet.fit.dto.env.DeletedEnvironmentDTO;
import uet.fit.dto.env.DeletedEnvironmentEntry;
import uet.fit.dto.logger.ProgressDTO;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BaseController implements Initializable {

	private static BaseController instance;

	private final ObservableList<ObservableProgress> observableProgressList = FXCollections.observableArrayList(
			new Callback<ObservableProgress, Observable[]>() {
				@Override
				public Observable[] call(ObservableProgress progress) {
					return new Observable[]{
							progress.titleProperty(),
							progress.currentProperty(),
							progress.totalProperty(),
							progress.timeProperty()
					};
				}
			}
	);

	private Timer timer;

	@FXML
	private RingProgressIndicator progressIndicator;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private Label lProgressTitle;
	@FXML
	private TabPane basePane;
	@FXML
	private Tab homeTab;
	@FXML
	private Tab testTab;
	@FXML
	private Tab ciaTab;
	@FXML
	private Label lUsername;
	@FXML
	private HBox hbProgress;
	@FXML
	private Label lTimer;

	private Scene progressDialog;

	public static BaseController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		instance = this;

		progressIndicator.makeIndeterminate();

		// init cia view
		CIAMainViewController ciaController = CIAMainViewController.getInstance();
		ciaTab.setContent(ciaController.getContent());

		// init list view of jobs
		ListView<ObservableProgress> listView = new ListView<>(observableProgressList);
		listView.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
		listView.setCellFactory(studentListView -> new ProgressRow());
		listView.setFocusTraversable(false);
		listView.setPrefHeight(128);
		progressDialog = new Scene(listView);

		//
		ListProperty<ObservableProgress> bindingProgressList = new SimpleListProperty<>(observableProgressList);
		ObservableValue<Number> progressNumberProperty = bindingProgressList.sizeProperty();
		progressNumberProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number old, Number newVal) {
				int size = newVal.intValue();
				updateProgress(size);
			}
		});

		UIHelper.getPrimaryStage().focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old, Boolean focused) {
				if (focused) {
					int waiting = WaitingLogs.getInstance().size();
					int previous = progressNumberProperty.getValue().intValue();
					int flag = waiting * previous;
					updateProgress(flag);
				}
			}
		});
	}

	public void displayUserName(String username) {
		this.lUsername.setText(username);
		this.lUsername.setVisible(true);
	}

	public void lockPane() {
		testTab.setDisable(true);
		ciaTab.setDisable(true);
	}

	public void unlockPane() {
		testTab.setDisable(false);
		ciaTab.setDisable(false);
	}

	public void moveToHomeTab() {
		basePane.getSelectionModel().select(homeTab);
	}

	public void moveToTestTab() {
		basePane.getSelectionModel().select(testTab);
	}

	public void moveToCiaTab() {
		basePane.getSelectionModel().select(ciaTab);
	}

	private void updateProgress(int jobs) {
		hbProgress.setVisible(jobs > 0);
		progressBar.setVisible(jobs == 1);
		progressIndicator.setVisible(jobs > 1);
		lProgressTitle.setVisible(jobs > 0);

		String title;

		if (jobs == 1)
			title = observableProgressList.get(0).titleProperty().get();
		else
			title = String.format("%d tasks are running...", jobs);

		lProgressTitle.setText(title);
	}

	public void appendNewProgress(ProgressDTO progress) {
		AtomicBoolean isNewer = new AtomicBoolean(false);

		// update list
		observableProgressList.stream()
				.filter(p -> p.getId().equals(progress.getId()))
				.findFirst()
				.ifPresentOrElse(new Consumer<ObservableProgress>() {
					@Override
					public void accept(ObservableProgress exist) {
						if (exist.currentProperty().get() != exist.totalProperty().get()
								&& progress.getTime() > exist.timeProperty().get()) {
							exist.titleProperty().set(progress.getTitle());
							exist.currentProperty().set(progress.getCurrent());
							exist.totalProperty().set(progress.getTotal());
							exist.timeProperty().set(progress.getTime());
							isNewer.set(true);
						}
					}
				}, new Runnable() {
					@Override
					public void run() {
						ObservableProgress observableProgress = new ObservableProgress(progress);
						observableProgressList.add(0, observableProgress);
						isNewer.set(true);
					}
				});

		// remove job
		if (progress.getCurrent() == progress.getTotal()) {
			new Thread(new RemoveJobTask(progress.getId())).start();
		}

		if (isNewer.get()) {
			double percentage = progress.getCurrent() * 1.0f / progress.getTotal();
			progressBar.setProgress(percentage);
		}
	}

	@FXML
	public void progressClicked() {
		// init stage
		Stage stage = new Stage();
		stage.setTitle("Background Tasks");
		stage.setMinWidth(260);
		stage.setScene(progressDialog);
		stage.initStyle(StageStyle.UTILITY);
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);

		// close when dis-focus
		stage.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean was, Boolean isNowFocused) {
				if (!isNowFocused) {
					stage.close();
				}
			}
		});
		// close when there is no job
		progressIndicator.visibleProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean was, Boolean isVisible) {
				if (!isVisible) {
					stage.close();
				}
			}
		});

		// set dialog location
		Bounds bounds = progressBar.localToScene(progressBar.getBoundsInLocal());
		double x = bounds.getCenterX();
		double y = bounds.getMinY() - progressDialog.getHeight();
		stage.setX(x);
		stage.setY(y);

		// show dialog
		stage.show();
	}

	public synchronized void refreshTimer() {
		lTimer.setVisible(true);

		final int size = WaitingLogs.getInstance().size();
		if (size == 0) {
			if (timer != null) timer.cancel();
			forceClearProgresses();
			flashTimerLabel(Color.RED);
		} else if (size == 1) {
			if (timer != null) timer.cancel();
			timer = new Timer();
			timer.schedule(new TimerTask(), 0, 1000);
			flashTimerLabel(Color.BLUE);
		}
	}

	private void flashTimerLabel(Color color) {
		ObjectProperty<Paint> textFill = lTimer.textFillProperty();
		Timeline timeline = new Timeline(
				new KeyFrame(Duration.seconds(0.0), new KeyValue(textFill, color)),
				new KeyFrame(Duration.seconds(0.5), new KeyValue(textFill, Color.BLACK))
		);
		timeline.setAutoReverse(false);
		timeline.setCycleCount(3);
		timeline.play();
	}

	private void forceClearProgresses() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				observableProgressList.clear();
				updateProgress(0);
			}
		});
	}

	public void showDeletedEnvWindow(){
		UIHelper.showTextInputDialog("Delete Environment", "Please input environment name", "Environment Name")
				.showAndWait()
				.ifPresent(new Consumer<String>() {
					@Override
					public void accept(String name) {
						List<String> environmentNames = new ArrayList<>();
//						String environment = environmentRow.getName();
						String username = User.getInstance().getUsername();
						environmentNames.add(name);
						Alert confirmAlert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION, "CONFIRM",
								"Do you want to delete environment?", name);
						Optional<ButtonType> option = confirmAlert.showAndWait();
						if (option.isPresent() && option.get() == ButtonType.OK) {
							DeleteEnviromentTask task = new DeleteEnviromentTask(username, environmentNames);
							task.setOnSucceeded(workerStatEvent -> {
								DeletedEnvironmentDTO deletedEnvi = task.getValue();
								int existCount = 0;
								int deletedCount = 0;
								for (DeletedEnvironmentEntry envi : deletedEnvi.getList()) {
									if (envi.getStatus() != DeletedEnvironmentEntry.Status.SUCCESS) {
										existCount++;
									} else {
										deletedCount++;
										HomeController.getInstance().updateTbRecentEnvs(name);
									}
								}

								if (deletedCount > 0) {
									String logContent = "Successfully deleted " + deletedCount + " test case(s)";
									//				logGeneral(LogDTO.TYPE_INF, logContent);
								}

								if (existCount == 0) {
									UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESS", "Successfully deleted!")
											.showAndWait();
								} else {
//									UIHelper.showStatusDelete(deletedEnvi.getList(), name);
								}
							});
							new AUTThread(task).start();
						}
					}
				});

	}

	/**
	 * Delay 0.3s to delete job
	 */
	private class RemoveJobTask extends Task<String> {

		private final String id;

		public RemoveJobTask(String id) {
			this.id = id;

			EventHandler<WorkerStateEvent> eventHandler = new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					observableProgressList.removeIf(p -> p.getId().equals(id));
				}
			};

			setOnSucceeded(eventHandler);
			setOnFailed(eventHandler);
		}

		@Override
		protected String call() throws Exception {
			Thread.sleep(300);
			return id;
		}
	}

	private class TimerTask extends java.util.TimerTask {

		private final long startTime;

		public TimerTask() {
			this.startTime = System.currentTimeMillis();
		}

		@Override
		public void run() {
			final long millis = System.currentTimeMillis() - startTime;
			final String time = millisToString(millis);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					lTimer.setText(time);
				}
			});
		}

		private String millisToString(long millis) {
			long hours = TimeUnit.MILLISECONDS.toHours(millis);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
					- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
			long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
					- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));

			StringBuilder builder = new StringBuilder();
			if (hours > 0)
				builder.append(hours).append("h");
			if (minutes > 0)
				builder.append(minutes).append("m");
			builder.append(seconds).append("s");
			return builder.toString();
		}
	}

}
