package uet.fit.client.ui.controller.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable, IEnvironmentBuilderStep {

	@FXML private VBox pane;
	@FXML private TextField tfUsername;
	@FXML private TextField tfShownPassword;
	@FXML private PasswordField pfHiddenPassword;
	@FXML private Button btnShowPassword;

	private final BooleanProperty disable = new SimpleBooleanProperty();
	private final BooleanProperty showPassword = new SimpleBooleanProperty();

	@FXML
	public void btnShowPassword_clicked() {
		showPassword.setValue(showPassword.not().getValue());
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		showPassword.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old, Boolean show) {
				if (show) {
					btnShowPassword.setText("Hide");
					btnShowPassword.setGraphic(new ImageView("/images/210-eye-blocked.png"));
				} else {
					btnShowPassword.setText("Show");
					btnShowPassword.setGraphic(new ImageView("/images/207-eye.png"));
				}
			}
		});

		// Bind properties. Toggle textField and passwordField
		// visibility and manageability properties mutually when checkbox's state is changed.
		// Because we want to display only one component (textField or passwordField)
		// on the scene at a time.
		tfShownPassword.managedProperty().bind(showPassword);
		tfShownPassword.visibleProperty().bind(showPassword);

		pfHiddenPassword.managedProperty().bind(showPassword.not());
		pfHiddenPassword.visibleProperty().bind(showPassword.not());

		// Bind the textField and passwordField text values bidirectionally.
		tfShownPassword.textProperty().bindBidirectional(pfHiddenPassword.textProperty());

		btnShowPassword.disableProperty().bind(disable);
		tfUsername.disableProperty().bind(disable);
		tfShownPassword.disableProperty().bind(disable);
		pfHiddenPassword.disableProperty().bind(disable);
	}

	public BooleanProperty disableProperty() {
		return disable;
	}

	public String getUsername() {
		String name = tfUsername.getText();
		if (name == null) name = "";
		return name;
	}

	public String getPassword() {
		String password = tfShownPassword.getText();
		if (password == null) password = "";
		return password;
	}

	public TextField getUsernameEditor() {
		return tfUsername;
	}

	public TextField getPasswordEditor() {
		return tfShownPassword;
	}

	public void setBanner(Node node) {
		this.pane.getChildren().add(0, node);
	}

	@Override
	public void clearState() {
		tfShownPassword.clear();
		tfUsername.clear();
		pfHiddenPassword.clear();
	}
}
