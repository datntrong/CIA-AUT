package uet.fit.client.ui.controller.cia;

import javafx.application.Platform;
import javafx.fxml.FXML;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceCodeViewController {
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
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
	);

	@FXML private @NotNull CodeArea codeArea;
	private @NotNull String sourceCode = "";
	private @NotNull List<@NotNull String> lines = List.of();

	@FXML
	public void initialize() {
		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

		codeArea.multiPlainChanges()
				.successionEnds(Duration.ofMillis(500))
				.subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
	}

	/**
	 * Set source code content
	 */
	public void setSourceCode(@NotNull String sourceCode) {
		if (sourceCode.equals(this.sourceCode)) return;
		this.sourceCode = sourceCode;
		this.lines = List.of();
		codeArea.replaceText(sourceCode);
	}

	/**
	 * Highlight and focus a function (line count start at 1, not 0)
	 */
	public void highlight(int startLine, int endLine) {
		if (startLine < 1 || endLine < 1) return;
		if (lines.isEmpty()) {
			this.lines = List.of(sourceCode.split("\\R"));
		}
		final int start = startLine - 1;
		final int end = Math.min(endLine - 1, lines.size() - 1);
		final int startPos = codeArea.position(start, 0).toOffset();
		final int endPos = codeArea.position(end, lines.get(end).length()).toOffset();
		Platform.runLater(() -> {
			codeArea.selectRange(startPos, endPos);
			codeArea.showParagraphAtBottom(end);
			codeArea.showParagraphAtTop(start);
		});
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
			if (styleClass == null) throw new AssertionError();
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastPos);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastPos = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastPos);
		return spansBuilder.create();
	}
}
