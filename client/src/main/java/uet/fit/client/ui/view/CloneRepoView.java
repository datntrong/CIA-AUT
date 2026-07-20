package uet.fit.client.ui.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import uet.fit.client.common.User;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.env.CloneRepoController;

public class CloneRepoView extends VBox implements IEnvironmentBuilderStep {

	private final CloneRepoController controller;

	public CloneRepoView() {
		super();

		controller = UIHelper.loadFXML(this, "/fxml/env/CloneRepo.fxml");

		controller.setOnCloneSuccess((username, git, url, projectPath) -> 
				getOnClone().handle(new CloneEvent(username, git, url, projectPath)));

		controller.setOnBrowse(event -> getOnBrowse().handle(event));
	}

	@Override
	public void clearState() {
		controller.clearState();
	}

	// Event handler section
	private final ObjectProperty<EventHandler<MouseEvent>> onBrowseProperty = new SimpleObjectProperty<>();

	public final ObjectProperty<EventHandler<MouseEvent>> onBrowseProperty() {
		return onBrowseProperty;
	}

	public final void setOnBrowse(EventHandler<MouseEvent> handler) {
		onBrowseProperty().set(handler);
	}

	public final EventHandler<MouseEvent> getOnBrowse() {
		return onBrowseProperty().get();
	}

	private final ObjectProperty<EventHandler<CloneEvent>> onCloneProperty = new SimpleObjectProperty<>();

	public final ObjectProperty<EventHandler<CloneEvent>> onCloneProperty() {
		return onCloneProperty;
	}

	public final void setOnClone(EventHandler<CloneEvent> handler) {
		onCloneProperty().set(handler);
	}

	public final EventHandler<CloneEvent> getOnClone() {
		return onCloneProperty().get();
	}

	public static class CloneEvent extends Event {

		private static final EventType<CloneEvent> CUSTOM_EVENT_TYPE = new EventType<>(ANY);

		private String username;
		private User.Git git;
		private String url;
		private String projectPath;

		public CloneEvent( String username, User. Git git,  String url,  String projectPath) {
			this(CUSTOM_EVENT_TYPE);
			this.username = username;
			this.git = git;
			this.url = url;
			this.projectPath = projectPath;
		}

		public CloneEvent(EventType<? extends Event> eventType) {
			super(eventType);
		}

		public String getUsername() {
			return username;
		}

		public User.Git getGit() {
			return git;
		}

		public String getUrl() {
			return url;
		}

		public String getProjectPath() {
			return projectPath;
		}

		@Override
		public String toString() {
			return "CloneEvent{" +
					"username='" + username + '\'' +
					", git=" + git +
					", url='" + url + '\'' +
					", projectPath='" + projectPath + '\'' +
					'}';
		}
	}
}
