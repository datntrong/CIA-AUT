package uet.fit.client.ui.controller.component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import uet.fit.dto.UserDTO.UserTypedefRow;

import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserCodeWindowController implements Initializable {
	@FXML
	private CodeArea codeArea;

	@FXML
	private Button cancelBtn;

	private static final String[] KEYWORDS = new String[]{
			"asm", "else", "new", "this", "auto", "enum", "operator", "throw",
			"explicit", "private", "true", "break", "export", "protected",
			"try", "case", "extern", "public", "typedef", "catch", "false", "register",
			"typeid", "reinterpret_cast", "typename", "class", "for",
			"return", "union", "const", "friend", "const_cast", "goto",
			"using", "continue", "if", "sizeof", "virtual", "default", "inline", "include",
			"static", "delete", "static_cast", "volatile", "do", "struct",
			"wchar_t", "mutable", "switch", "while", "dynamic_cast", "namespace", "template"
	};

	private static final String[] PRIMITIVES = new String[]{
			"bool", "char", "float", "double", "void", "unsigned", "long", "short", "signed", "int"
	};

	private static final String KEYWORD_PATTERN = "\\b(?:" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PRIMITIVE_PATTERN = "\\b(?:" + String.join("|", PRIMITIVES) + ")\\b";
	private static final String STRING_PATTERN = "(?<quote>[\"'])(?:\\\\.|(?:(?!\\k<quote>)[^\\r\\n\\\\])+)*\\k<quote>";
	private static final String COMMENT_PATTERN = "/\\*[^*]*\\*+(?:[^/][^*]*\\*+)*/|//.*";
	private static final String PAREN_PATTERN = "[()\\[\\]]+";
	private static final String BRACE_PATTERN = "[{}]+";
	private static final String SEMICOLON_PATTERN = "[-+/*;:.,!&<>^%~|=]+";
	private static final Pattern PATTERN = Pattern.compile(
			"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
					+ "|(?<PRIMITIVE>" + PRIMITIVE_PATTERN + ")"
					+ "|(?<STRING>" + STRING_PATTERN + ")"
					+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					+ "|(?<PAREN>" + PAREN_PATTERN + ")"
					+ "|(?<BRACE>" + BRACE_PATTERN + ")"
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" );


	private UserCodeTabController controller;
	private UserTypedefRow userTypedefRow;

	private boolean isEditing = false;
	private boolean isAdding = false;
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

		codeArea.multiPlainChanges()
				.successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
	}

	public UserCodeTabController getController() {
		return controller;
	}

	public void setController(UserCodeTabController controller) {
		this.controller = controller;
	}

	public void setCodeAreaText(String text) {
		codeArea.replaceText(text);
	}
	public void saveChangesButtonClicked() {

			String code = codeArea.getText();
		if(!isAdding) {
			controller.getDataList().remove(userTypedefRow);
			userTypedefRow.setValue(code);
			controller.getModifiedList().add(userTypedefRow);
			controller.getDataList().add(userTypedefRow);
			Stage stage = (Stage) cancelBtn.getScene().getWindow();
			stage.close();
		} else {
			controller.getValueField().replaceText(code);
			Stage stage = (Stage) cancelBtn.getScene().getWindow();
			stage.close();
		}
	}

	public void cancelBtnClicked() {
		Stage stage = (Stage) cancelBtn.getScene().getWindow();
		stage.close();
	}


	/**
	 * Computing highlighting code syntax after edit code
	 *
	 * @param text code need to be computed
	 * @return all style for code area
	 */
	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastPos = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword" :
					matcher.group("PRIMITIVE") != null ? "primitive" :
							matcher.group("STRING") != null ? "string" :
									matcher.group("COMMENT") != null ? "comment" :
											matcher.group("PAREN") != null ? "paren" :
													matcher.group("BRACE") != null ? "brace" :
															matcher.group("SEMICOLON") != null ? "semicolon" :
																	null; /* never happens */
			assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastPos);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastPos = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastPos);
		return spansBuilder.create();
	}

	public UserTypedefRow getUserTypedefRow() {
		return userTypedefRow;
	}

	public void setUserTypedefRow(UserTypedefRow userTypedefRow) {
		this.userTypedefRow = userTypedefRow;
	}

	public boolean isEditing() {
		return isEditing;
	}

	public void setEditing(boolean editing) {
		isEditing = editing;
	}

	public boolean isAdding() {
		return isAdding;
	}

	public void setAdding(boolean adding) {
		isAdding = adding;
	}
}
