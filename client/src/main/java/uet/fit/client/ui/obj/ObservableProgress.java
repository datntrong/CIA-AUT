package uet.fit.client.ui.obj;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import uet.fit.dto.logger.ProgressDTO;

public class ObservableProgress {

	private final String id;

	private final StringProperty titleProperty;

	private final IntegerProperty currentProperty;
	private final IntegerProperty totalProperty;

	private final LongProperty timeProperty;

	public ObservableProgress(ProgressDTO dto) {
		this.id = dto.getId();

		titleProperty = new SimpleStringProperty(dto.getTitle());
		currentProperty = new SimpleIntegerProperty(dto.getCurrent());
		totalProperty = new SimpleIntegerProperty(dto.getTotal());

		timeProperty = new SimpleLongProperty(dto.getTime());
	}

	public StringProperty titleProperty() {
		return titleProperty;
	}

	public IntegerProperty currentProperty() {
		return currentProperty;
	}

	public IntegerProperty totalProperty() {
		return totalProperty;
	}

	public LongProperty timeProperty() {
		return timeProperty;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "ObservableProgress{" +
				"id='" + id + '\'' +
				", title=" + titleProperty.get() +
				", current=" + currentProperty.get() +
				", total=" + totalProperty.get() +
				", time=" + timeProperty.get() +
				'}';
	}
}
