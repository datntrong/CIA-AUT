package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfo;
import uet.fit.aut.parser.dependency.IncludeQtHeaderPreprocessor;
import uet.fit.aut.parser.obj.IFileNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.PrimitiveTypedefDeclaration;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uet.fit.aut.parser.ProjectParser.*;

public class QtPrimitiveTypedefResolver {

	private static Map<String, String> cache = new HashMap<>();

	private static final String QT_GLOBAL = "QtGlobal";

	private final File qtDir;

	public QtPrimitiveTypedefResolver(File qtDir) {
		this.qtDir = qtDir;
	}

	public String run(String qtType) throws Exception {
		if (cache.containsKey(qtType))
			return cache.get(qtType);

		IncludeQtHeaderPreprocessor test = new IncludeQtHeaderPreprocessor(qtDir, new HashMap<>());
		IFileNode node =  test.handleIncludeQtLib(QT_GLOBAL);

		String[] projectFiles = new String[]{node.getAbsolutePath()};
		String[] includePaths = retrieveIncludePaths(qtDir);

		final IScannerInfo scannerInfo = new ExtendedScannerInfo(null, includePaths, null, projectFiles);
		IASTTranslationUnit translationUnit = GPPLanguage.getDefault()
				.getASTTranslationUnit(EMPTY_FILE, scannerInfo, CONTENT_PROVIDER, null,
						ILanguage.OPTION_IS_SOURCE_UNIT, LOG_SERVICE);

		SourcecodeFileExpander newExpander = new SourcecodeFileExpander();
		newExpander.setRoot(node);

		IASTDeclaration[] declarations = translationUnit.getDeclarations();
		for (IASTDeclaration declaration : declarations) {
			newExpander.expand(declaration);
		}

		for(INode child : node.getChildren()) {
			if (child.getNewType().equals(qtType)) {
				if (child instanceof PrimitiveTypedefDeclaration) {
					String type = ((PrimitiveTypedefDeclaration) child).getOldType();
					cache.put(qtType, type);
					return type;
				}
			}
		}

		throw new Exception("Type " + qtType + " not found");
	}

	private String[] retrieveIncludePaths(File qtDir) throws IOException {
		List<String> includePaths = new ArrayList<>();

		includePaths.add(qtDir.getAbsolutePath());

		try (final DirectoryStream<Path> qtInstallHeaders = Files.newDirectoryStream(qtDir.toPath())) {
			for (final Path qtInstallHeader : qtInstallHeaders) {
				if (Files.isDirectory(qtInstallHeader)) {
					includePaths.add(qtInstallHeader.toString());
				}

			}
		}

		return includePaths.toArray(String[]::new);
	}

}
