package uet.fit.aut.parser.qt;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.parser.SourcecodeFileExpander;
import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.QtHeaderNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.ConstructorNodeCondition;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uet.fit.aut.parser.ProjectParser.EMPTY_FILE;
import static uet.fit.aut.parser.ProjectParser.LOG_SERVICE;

public class QTParser {

	public static final IncludeFileContentProvider CONTENT_PROVIDER = new QtContentProvider();

	private final Map<String, QtHeaderNode> sourceCache = new HashMap<>();

	private Map<String, String> realPredefinedMacros;

	private List<QtHeaderNode> qtHeaderNodes;

	private String[] includePathList;

	private String[] total;

	private File qtDir;

	public QTParser(Map<String, String> realPredefinedMacros, String[] includePathList, File qtDir, String[] total, List<QtHeaderNode> qtHeaderNodes) {
		this.realPredefinedMacros = realPredefinedMacros;
		this.includePathList = includePathList;
		this.total = total;
		this.qtHeaderNodes = qtHeaderNodes;
		this.qtDir = qtDir;
	}

	public List<ConstructorNode> parse() throws CoreException {

		//include lib headers
		List<String> includeQtHeaders = new ArrayList<>();

		includeQtHeaders.add(qtDir.getAbsolutePath());

		for (File child : Objects.requireNonNull(qtDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("Qt");
			}
		}))) {
			if (Utils.isMac()) {
				includeQtHeaders.add(child.getAbsolutePath() + File.separator + QTConst.MACOS_QTVERSION_POSTFIX);
			} else if (Utils.isUnix()) {
				includeQtHeaders.add(child.getAbsolutePath());
			}
		}

		String[] newIncludePathList = Stream.concat(Arrays.stream(includePathList), includeQtHeaders.stream()).toArray(String[]::new);

		final IScannerInfo scannerInfo = new ExtendedScannerInfo(realPredefinedMacros, newIncludePathList, null, total);

		IASTTranslationUnit translationUnit = GPPLanguage.getDefault()
				.getASTTranslationUnit(EMPTY_FILE, scannerInfo, CONTENT_PROVIDER, null,
						ILanguage.OPTION_IS_SOURCE_UNIT, LOG_SERVICE);

		IASTDeclaration[] declarations = translationUnit.getDeclarations();
		declarations = Arrays.stream(declarations)
				.filter(d -> !d.getFileLocation().getFileName().isEmpty())
				.toArray(IASTDeclaration[]::new);

		List<ConstructorNode> constructorNodes = new ArrayList<>();

		for (IASTDeclaration declaration : declarations) {
			String filename = declaration.getFileLocation().getFileName();
			QtHeaderNode searchedRoot = sourceCache.get(filename);
			if (searchedRoot == null) {
				searchedRoot = qtHeaderNodes.stream()
						.filter(f -> PathUtils.equals(PathUtils.getFilename(f.getAbsolutePath()), PathUtils.getFilename(filename)))
						.findFirst()
						.orElse(null);
				if (searchedRoot != null)
					sourceCache.put(filename, searchedRoot);
				else {
					continue;
				}
			} else {
				final INode expandRoot = searchedRoot;

				SourcecodeFileExpander srcParser = new SourcecodeFileExpander();
				srcParser.setRoot(expandRoot);
				srcParser.expand(declaration);

				constructorNodes.addAll(Search.searchNodes(searchedRoot, new ConstructorNodeCondition()));
			}
		}

		constructorNodes = constructorNodes.stream().distinct().collect(Collectors.toList());

		return constructorNodes;
	}

	static final class QtContentProvider extends InternalFileContentProvider {
		private @Nullable InternalFileContent fileContent(@NotNull String path) {
			if (Utils.isMac()) path = normalizePath(path);
			if (!getInclusionExists(path)) return null;
			return (InternalFileContent) FileContent.createForExternalFileLocation(path);
		}

		@Override
		public @Nullable InternalFileContent getContentForInclusion(@NotNull String path,
				@Nullable IMacroDictionary dictionary) {
			if (Utils.isMac()) path = normalizePath(path);
			return fileContent(path);
		}

		@Override
		public @Nullable InternalFileContent getContentForInclusion(@NotNull IIndexFileLocation location,
				@Nullable String astPath) {
			String path = location.getFullPath();
			if (Utils.isMac()) path = normalizePath(path);
			return path == null ? null : fileContent(path);
		}

		private String normalizePath(String path) {
			if (path != null && path.contains(QTConst.QT_VERSION) && !path.contains(QTConst.MACOS_QTLIB_POSTFIX)) {
				String[] items = path.split("/");
				for (int i = 0; i < items.length; i++) {
					if (i >= 2 && items[i-1].equals("lib") && items[i-2].equals("clang_64"))
						items[i] = items[i] + QTConst.MACOS_QTLIB_POSTFIX + QTConst.MACOS_QTVERSION_POSTFIX;
				}
				return String.join("/", items);
			} else {
				return path;
			}
		}
	}
}
