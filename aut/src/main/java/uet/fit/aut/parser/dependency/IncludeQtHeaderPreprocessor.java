package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import uet.fit.aut.logger.IdMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.ProjectParser;
import uet.fit.aut.parser.obj.IFileNode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.QtHeaderNode;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class IncludeQtHeaderPreprocessor extends AbstractIncludeHeaderDependencyGeneration {

	private final static Logger logger = LoggerFactory.getLogger(IncludeQtHeaderPreprocessor.class);

	protected final File qtDir;

	protected final Map<String, QtHeaderNode> sCache;

	protected Map<String, String> defines;

	public IncludeQtHeaderPreprocessor(File qtDir, Map<String, QtHeaderNode> cache) {
		this.qtDir = qtDir;
		this.sCache = cache;
	}

	public void dependencyGeneration(IFileNode owner) {
		try {
			if (owner instanceof QtHeaderNode)
				owner.setIncludeHeaderDependencyState(true);

			List<QtHeaderNode> qtNodes = new ArrayList<>();

			IASTPreprocessorIncludeStatement[] includeDirectives = getIncludeDirectives(owner);
			for (IASTPreprocessorIncludeStatement includeStm : includeDirectives) {
				IFileNode referredNode = null;

				if (owner instanceof QtHeaderNode) {
					referredNode = findIncludeNode(includeStm, owner);
				} else if (owner instanceof ISourcecodeFileNode) {
					String includeName = includeStm.getName().getRawSignature();
					if (includeStm.isSystemInclude()
							&& includeName.startsWith(QTConst.QTLIB_FLAG)) {
						referredNode = handleIncludeQtLib(includeName);
					}
				}

				if (referredNode instanceof QtHeaderNode) {
					qtNodes.add((QtHeaderNode) referredNode);
				}
			}

			invokeQtTasks(qtDir, qtNodes);

		} catch (CoreException | InterruptedException e) {
			logger.error("Can't parse " + IdMapping.getInstance().getOrCreate(owner.getName()), e);
		}
	}

	protected IFileNode findIncludeNode(IASTPreprocessorIncludeStatement includeStm, IFileNode source) {
		IFileNode includedNode = null;

		/*
		 * Library function se khong tim duoc trong day
		 */
		String includeName = includeStm.getName().getRawSignature();

		if (!includeStm.isSystemInclude()) {
			if (sCache.get(includeName) != null) {
				includedNode = sCache.get(includeName);
			} else {
				String includeFilePath = PathUtils.normalize(includeName);
				includeFilePath = PathUtils.absolute(includeFilePath, source.getFile());
				if (new File(includeFilePath).exists()) {
					includedNode = findOrCreate(includeFilePath);
					sCache.put(includeName, (QtHeaderNode) includedNode);
				}
			}
		} else {
			// check if qt lib
			if (includeName.startsWith(QTConst.QTLIB_FLAG)) {
				includedNode = handleIncludeQtLib(includeName);
			}
		}

		return includedNode;
	}

	protected QtHeaderNode findOrCreate(final String absolutePath) {
		QtHeaderNode qtNode = sCache.values().stream()
				.filter(n -> n.getAbsolutePath().equals(absolutePath))
				.map(n -> (QtHeaderNode) n)
				.findFirst()
				.orElse(null);
		if (qtNode == null) {
			qtNode = new QtHeaderNode();
			qtNode.setAbsolutePath(absolutePath);
		}
		return qtNode;
	}

	public IFileNode handleIncludeQtLib(String includeName) {
		IFileNode includedNode;
		if (sCache.get(includeName) != null) {
			includedNode = sCache.get(includeName);
		} else {
			QtHeaderNode qtNode = searchQtHeader(includeName);
			includedNode = qtNode;
			if (includedNode != null) {
				sCache.put(includeName, qtNode);
			}
		}
		return includedNode;
	}

	protected QtHeaderNode searchQtHeader(String includeName) {
		QtHeaderNode includedNode = null;

		String[] items = includeName.split(QTConst.QTLIB_DELIMITER);
		File qtFile = null;
		long time = 0;
		// Ex: include <QObject>
		if (items.length == 1) {
			long start = System.currentTimeMillis();
			qtFile = Utils.searchFile(qtDir, includeName);
			time = System.currentTimeMillis() - start;
		}
		// Ex: include <QtCore/qbytearray.h>
		else if (items.length == 2) {
			String dir = items[0];
			if (Utils.isMac())
				dir += QTConst.MACOS_QTLIB_POSTFIX;
			long start = System.currentTimeMillis();
			File container = Utils.searchFolder(qtDir, dir);
			if (container != null) {
				qtFile = Utils.searchFile(container, items[1]);
				time = System.currentTimeMillis() - start;
			}
		}

		if (qtFile != null) {
			logger.debug("Finding QT header file " + IdMapping.getInstance().getOrCreate(includeName) + " costs " + time + "ms");
			final String absolutePath = qtFile.getAbsolutePath();
			includedNode = findOrCreate(absolutePath);
		} else {
			logger.error("Can't find QT header file " + includeName);
		}

		return includedNode;
	}

	protected IASTPreprocessorIncludeStatement[] getIncludeDirectives(IFileNode node) throws CoreException {
		String code = Utils.readFileContent(node.getAbsolutePath());
		FileContent fc = FileContent.create(node.getAbsolutePath(), code.toCharArray());

		IScannerInfo si = new ScannerInfo(defines, null);
		IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();

		IIndex idx = null;
		int options = ILanguage.OPTION_IS_SOURCE_UNIT;
		IParserLogService log = new DefaultLogService();

		IASTTranslationUnit translationUnit = GPPLanguage.getDefault()
				.getASTTranslationUnit(fc, si, ifcp, idx, options, log);

		return translationUnit.getIncludeDirectives();
	}

	public void setDefines(Map<String, String> defines) {
		this.defines = defines;
	}

	protected void invokeQtTasks(File qtDir, List<QtHeaderNode> qtNodes) throws InterruptedException {
		List<IncludeQtHeaderPreprocessor.Task> qtTasks = new ArrayList<>();
		for (QtHeaderNode qtNode : qtNodes) {
			if (!qtNode.isIncludeHeaderDependencyState())
				qtTasks.add(new IncludeQtHeaderPreprocessor.Task(qtDir, qtNode, sCache));
		}
		ProjectParser.es.invokeAll(qtTasks);
	}

	public static class Task implements Callable<Void> {

		private final File qtDir;
		private final IFileNode qtNode;

		private final Map<String, QtHeaderNode> cache;

		public Task(File qtDir, IFileNode qtNode, Map<String, QtHeaderNode> cache) {
			this.qtDir = qtDir;
			this.qtNode = qtNode;
			this.cache = cache;
		}

		@Override
		public Void call() throws Exception {
			IncludeQtHeaderPreprocessor gen = new IncludeQtHeaderPreprocessor(qtDir, cache);
			gen.dependencyGeneration(qtNode);
			return null;
		}
	}
}

