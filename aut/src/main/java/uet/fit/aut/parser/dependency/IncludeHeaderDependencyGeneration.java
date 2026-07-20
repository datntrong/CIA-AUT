package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import uet.fit.aut.parser.obj.IFileNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FileNodeCondition;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;

import java.io.File;
import java.util.List;

public class IncludeHeaderDependencyGeneration extends AbstractIncludeHeaderDependencyGeneration {

	// project root
	private final INode root;

	// include path
	private final List<File> includePaths;

	private final IASTPreprocessorIncludeStatement includeStm;

	public IncludeHeaderDependencyGeneration(INode root, List<File> includePaths, IASTPreprocessorIncludeStatement includeStm) {
		this.root = root;
		this.includePaths = includePaths;
		this.includeStm = includeStm;
	}

	public void dependencyGeneration(IFileNode owner) {
		INode referredNode = findIncludeNode(includeStm, owner);
		if (referredNode != null) {
			addDependency(owner, referredNode);
		}
	}

	protected IFileNode findIncludeNode(IASTPreprocessorIncludeStatement includeStm, IFileNode source) {
		IFileNode includedNode = null;

		/*
		 * Library function se khong tim duoc trong day
		 */
		String includeName = includeStm.getName().getRawSignature();

		// check if qt lib
		if (includeStm.isSystemInclude() && includeName.startsWith(QTConst.QTLIB_FLAG)) {
			// ignore qt header
		}

		String includeFilePath = PathUtils.normalize(includeName);
		includeFilePath = File.separator + includeFilePath;

		List<IFileNode> searchedNodes = Search
				.searchNodes(this.root, new FileNodeCondition(), includeFilePath);

		if (searchedNodes.size() == 1)
			includedNode = searchedNodes.get(0);
		else if (searchedNodes.size() > 1) {
			String absolutePath = PathUtils.absolute(includeName, source.getFile());
			includedNode = searchedNodes.stream()
					.filter(f -> PathUtils.equals(absolutePath, f.getAbsolutePath()))
					.findFirst()
					.orElse(null);
			if (includedNode == null) {
				for (File includePath : includePaths) {
					String currentPath = PathUtils.absolute(includeName, includePath);
					includedNode = searchedNodes.stream()
							.filter(f -> PathUtils.equals(currentPath, f.getAbsolutePath()))
							.findFirst()
							.orElse(null);
					if (includedNode != null)
						break;
				}
			}
		}

		return includedNode;
	}

//	private String toRelativePath(String path, IFileNode owner) {
//		String outPath = PathUtils.absolute(path, owner.getFile());
//
//		String rootPath = this.root.getAbsolutePath();
//		if (outPath.startsWith(rootPath)) {
//			outPath = outPath.replace(rootPath + File.separator, "");
//		}
//
//		return outPath;
//	}
}

