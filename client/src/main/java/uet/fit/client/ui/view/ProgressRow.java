package uet.fit.client.ui.view;

import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import uet.fit.client.ui.obj.ObservableProgress;
import uet.fit.dto.logger.ProgressDTO;

import java.io.IOException;

public class ProgressRow extends ListCell<ObservableProgress> {

	@FXML
	private Text tTitle;
	@FXML
	private Text tPercentage;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private HBox container;

	private FXMLLoader mLLoader;

	@Override
	protected void updateItem(ObservableProgress progress, boolean empty) {
		super.updateItem(progress, empty);

		if (empty || progress == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (mLLoader == null) {
				mLLoader = new FXMLLoader(getClass().getResource("/fxml/dialogs/ProgressRow.fxml"));
				mLLoader.setController(this);

				try {
					mLLoader.load();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			tTitle.textProperty().bind(progress.titleProperty());

			NumberBinding percentProperty = progress.currentProperty()
					.multiply(1.0f)
					.divide(progress.totalProperty());

			percentProperty.addListener(titleListener);
			progressBar.progressProperty().bind(percentProperty);

			setText(null);
			setGraphic(container);
		}
	}

	private final ChangeListener<Number> titleListener = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newValue) {
			int percentage = (int) (newValue.doubleValue() * 100);
			tPercentage.setText(percentage + "%");
		}
	};
}