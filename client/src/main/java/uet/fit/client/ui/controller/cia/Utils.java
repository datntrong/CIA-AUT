package uet.fit.client.ui.controller.cia;

import javafx.animation.PauseTransition;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

final class Utils {
	private Utils() {
	}

	static <T> void enableAutoComplete(@NotNull ComboBox<T> comboBox, @NotNull List<T> items) {
		final PauseTransition transition = new PauseTransition(Duration.millis(100));
		comboBox.setEditable(true);
		comboBox.getItems().setAll(items);
		comboBox.setOnKeyPressed(event -> comboBox.hide());
		comboBox.setOnKeyReleased(event -> {
			final TextField editor = comboBox.getEditor();
			final String text = editor.getText();

			final KeyCode keyCode = event.getCode();
			if (keyCode == KeyCode.UP || keyCode == KeyCode.DOWN) {
				comboBox.show();
				editor.positionCaret(text.length());
			} else if (keyCode != KeyCode.RIGHT && keyCode != KeyCode.LEFT
					&& keyCode != KeyCode.HOME && keyCode != KeyCode.END
					&& keyCode != KeyCode.TAB && !event.isShortcutDown()) {
				transition.playFromStart();
			}
		});
		transition.setOnFinished(event -> {
			final TextField editor = comboBox.getEditor();
			final String text = editor.getText();

			final HashSet<T> itemSet = new LinkedHashSet<>();
			for (final T item : items) if (item.toString().startsWith(text)) itemSet.add(item);
			for (final T item : items) if (item.toString().contains(text)) itemSet.add(item);

			final int position = editor.getCaretPosition();
			comboBox.getItems().setAll(itemSet);
			editor.setText(text);
			editor.positionCaret(position);
			if (itemSet.size() > 1
					|| itemSet.size() == 1 && !itemSet.stream().findAny().get().toString().equals(text)) {
				comboBox.show();
			}
		});
	}
}
