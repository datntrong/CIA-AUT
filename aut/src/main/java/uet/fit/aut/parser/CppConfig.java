package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.ter.OnFinishListener;
import uet.fit.aut.ter.ProcessHandler;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CppConfig {

	private static final Logger logger = LoggerFactory.getLogger(CppConfig.class);

	private static final String[] GPP_COMMAND = new String[]{"g++", "-x", "c++", "-std=c++14", "-v", "-E", "-dM", "/dev/null"};
	private static final String INCLUDE_START = "#include <...> search starts here:";
	private static final String INCLUDE_END = "End of search list.";
	private static final String MACOS_FRAMEWORK_DIR = " (framework directory)";

	private @NotNull String[] includePaths;
	private @NotNull Map<String, String> macros;

	public static void main(String[] args) throws IOException {
		CppConfig cppConfig = CppConfig.retrieve();
		System.out.println();
	}

	private CppConfig() {

	}

	public static CppConfig retrieve() throws IOException {
		CppConfig cppConfig = new CppConfig();

		Process gccProcess = Runtime.getRuntime().exec(GPP_COMMAND);
		ProcessHandler handler = new ProcessHandler()
				.setProcess(gccProcess)
				.setOnFinish(new OnFinishListener() {
					@Override
					public void onFinish(String out, String err) throws Exception {
						if (out != null)
							cppConfig.setMacros(parseMacro(out));
						if (err != null)
							cppConfig.setIncludePaths(searchIncludePaths(err));
					}
				});
		handler.run();

		return cppConfig;
	}

	private static Map<String, String> parseMacro(@NotNull String src) throws Exception {
		Map<String, String> macros = new HashMap<>();
		IASTTranslationUnit ast = Utils.getIASTTranslationUnitforCpp(src.toCharArray());
		IASTPreprocessorMacroDefinition[] macroDefinitions = ast.getMacroDefinitions();
		for (IASTPreprocessorMacroDefinition macro : macroDefinitions) {
			String name = macro.getName().toString();
			String value = macro.getExpansion();
			macros.put(name, value);
			logger.debug(String.format("Found a builtin macro %s=%s", name, value));
		}
		return macros;
	}

	private static String[] searchIncludePaths(String log) {
		List<String> includePaths = new ArrayList<>();

		final int startLength = INCLUDE_START.length();
		final int start = log.indexOf(INCLUDE_START) + startLength;
		if (start >= startLength) {
			final int end = log.indexOf(INCLUDE_END, start);
			if (end >= start) {
				final String[] includes = log.substring(start, end)
						.trim().split("[\r\n]+[ \t]+");
				for (final String include : includes) {
					String normalizedPath = PathUtils.normalize(include);
					if (normalizedPath.endsWith(MACOS_FRAMEWORK_DIR))
						normalizedPath = normalizedPath.substring(0, normalizedPath.length() - MACOS_FRAMEWORK_DIR.length());
					includePaths.add(normalizedPath);
					logger.debug(String.format("Found a include path %s", normalizedPath));
				}
			}
		}

		return includePaths.toArray(String[]::new);
	}

	public String[] getIncludePaths() {
		return includePaths;
	}

	public void setIncludePaths(String[] includePaths) {
		this.includePaths = includePaths;
	}

	public Map<String, String> getMacros() {
		return macros;
	}

	public void setMacros(Map<String, String> macros) {
		this.macros = macros;
	}
}
