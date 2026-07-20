package uet.fit.client.ui.controller.test;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;

import java.util.Arrays;

public class CoverageViewController {

	@FXML
	private ScrollPane spCoverage;

	public void loadContentToCoverageView(String content) {
		final WebView coverage = new WebView();
		final WebEngine webEngine = coverage.getEngine();

		final float scrollPercent = calculateYPosPercent(content);
		webEngine.documentProperty().addListener(new ChangeListener<Document>() {
			@Override
			public void changed(ObservableValue<? extends Document> observable, Document oldValue, Document newValue) {
				String heightText = webEngine.executeScript(
						"window.getComputedStyle(document.body, null).getPropertyValue('height')"
				).toString();
				double height = Double.parseDouble(heightText.replace("px", ""));
				double yPos = (height * scrollPercent) - 10f;
				if (yPos < 0) yPos = 0;

				webEngine.documentProperty().removeListener(this);

				String newContent = scrollWebView(yPos) + content;
				webEngine.loadContent(newContent);
			}
		});

		webEngine.loadContent(content);
		spCoverage.setContent(coverage);

		spCoverage.widthProperty().addListener((observable, oldValue, newValue) -> {
			Double width = (Double) newValue;
			coverage.setPrefWidth(width);
		});
		spCoverage.heightProperty().addListener((observable, oldValue, newValue) -> {
			Double height = (Double) newValue;
			coverage.setPrefHeight(height);
		});
	}

	private static StringBuilder scrollWebView(double yPos) {
		StringBuilder script = new StringBuilder().append("<html>");
		script.append("<head>");
		script.append("   <script language=\"javascript\" type=\"text/javascript\">");
		script.append("       function toBottom(){");
		script.append("           window.scrollTo(0, ").append(yPos).append(");");
		script.append("       }");
		script.append("   </script>");
		script.append("</head>");
		script.append("<body onload='toBottom()'>");
		return script;
	}

	private float calculateYPosPercent(String content) {
		if (content != null) {
			String[] lines = content.split("\\R");

			int lastLineInt = -1;
			for (int i = lines.length - 1; i >= 0; i--) {
				if (lines[i].contains(LINE_TAG)) {
					lastLineInt = getLine(lines[i]);
					break;
				}
			}

			String firstHighLightLine = Arrays.stream(lines)
					.filter(l -> l.contains("style=\"background-color:yellow"))
					.findFirst()
					.orElse(null);

			if (firstHighLightLine != null) {
				int line = getLine(firstHighLightLine);
				return ((float) line / ((float) lastLineInt));
			}
		}

		return 0;
	}
	private int getLine(String htmlLine) {
		String lineStr = htmlLine.replace(LINE_TAG, "")
				.replaceAll("</b>.*", "")
				.trim();

		return Integer.parseInt(lineStr);
	}

	private static final String LINE_TAG = "<b style=\"color: grey;\">";
}
