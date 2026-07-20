package uet.fit.aut.parser;


import uet.fit.aut.logger.IdMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.util.PathUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Construct structure tree corresponding to the given C/C++ project
 */
public class ProjectLoader implements IProjectLoader {

	private final static Logger logger = LoggerFactory.getLogger(ProjectLoader.class);

	private static final boolean IGNORE_HIDDEN_FOLDERS = true;
	private static final String HIDDEN_FLAG = ".";

	private List<File> ignoreFolders = new ArrayList<>();
	private List<File> considerFiles = new ArrayList<>();
	private List<File> considerFolders = new ArrayList<>();

	// True: Parse the current folder and all its sub-directories
	private boolean recursive = true;

	public ProjectLoader() {

	}

	/**
	 * Generate the structure tree of the given project down to file level. Each
	 * node in the structure has an unique id
	 */
	@Override
	public ProjectNode load(File projectPath) {
		if (projectPath.exists()) {
			ProjectNode projectNode = new ProjectNode();
			try {
				String path = PathUtils.normalize(projectPath.getAbsolutePath());
				projectNode.setAbsolutePath(path);
				parseSrcFolder(projectNode, new File(path));

				generateId(projectNode);
			} catch (IOException e) {
				 e.printStackTrace();
			}

			return projectNode;
		} else
			return null;
	}

	/**
	 * @param dir
	 *            Duong dan tuyet doi
	 * @return Cac duong dan tuyet doi cua cac thanh phan ben trong
	 */
	private ArrayList<String> getChildren(File dir) throws IOException {
		ArrayList<String> pathOfChildren = new ArrayList<>();
		String[] names = dir.list();

		if (names != null) {
			for (String name : names) {
				String path = PathUtils.normalize(dir.getCanonicalPath() + File.separator + name);
				pathOfChildren.add(path);
			}
		}

		return pathOfChildren;
	}

	/**
	 * @param pathItem
	 *            Duong dan tuyet doi cua doi tuong
	 * @return Kieu doi tuong
	 */
	private int getTypeOfPath(String pathItem) {
		int type = UNDEFINED_COMPONENT;

		// check whether is folder
		File file = new File(pathItem);
		if (file.isDirectory()) {
			type = FOLDER;
		} else {
			if (pathItem.endsWith(C_FILE_SYMBOL))
				type = C_FILE;
			else if (pathItem.endsWith(CPP_FILE_SYMBOL) || pathItem.endsWith(CC_FILE_SYMBOL))
				type = CPP_FILE;
			else if (pathItem.endsWith(HEADER_FILE_SYMBOL_TYPE_1)
					|| pathItem.endsWith(HEADER_FILE_SYMBOL_TYPE_2)
					|| pathItem.endsWith(HEADER_FILE_SYMBOL_TYPE_3))
				type = HEADER_FILE;
			else if (pathItem.endsWith(EXE_SYMBOL))
				type = EXE;
			else if (pathItem.endsWith(OBJECT_FILE_SYMBOL))
				type = OBJECT;

//			if (type == C_FILE || type == CPP_FILE || type == HEADER_FILE) {
//				if (!considerFiles.contains(file)) {
//					type = UNSPECIFIED_FILE;
//				}
//			}
		}

		return type;
	}

	/**
	 * Bo qua folder khong can thiet
	 *
	 */
	private boolean isIgnoredComponent(File file) {
		String absolutePath = file.getAbsolutePath();

		for (String ignoredName : IGNORED_FILE_SYMBOLS) {
			if (absolutePath.endsWith(ignoredName))
				return true;
		}

		for (File ignoreFolder : ignoreFolders) {
			if (file.equals(ignoreFolder))
				return true;
		}


		if (IGNORE_HIDDEN_FOLDERS) {
			if (file.getName().startsWith(HIDDEN_FLAG)) {
				return true;
			}
		}

		if (file.isFile()) {
			for (File considerFolder : considerFolders) {
				if (file.getAbsolutePath().startsWith(considerFolder.getAbsolutePath()))
					return false;
			}

			if (!considerFiles.isEmpty())
				return !considerFiles.contains(file);
		}

		return false;
	}

	private void parseSrcFolder(Node parent, File path) throws IOException {
		logger.debug("Loading "  + IdMapping.getInstance().getOrCreate(path.getAbsolutePath()));
		ArrayList<String> children = getChildren(path);

		for (String pathItem : children)
			if (!isIgnoredComponent(new File(pathItem))) {
				switch (getTypeOfPath(pathItem)) {
					case C_FILE:
						CFileNode cNode = new CFileNode();
						cNode.setAbsolutePath(pathItem);
						cNode.setParent(parent);
						parent.getChildren().add(cNode);
						break;

					case CPP_FILE:
						CppFileNode cppNode = new CppFileNode();
						cppNode.setAbsolutePath(pathItem);
						cppNode.setParent(parent);
						parent.getChildren().add(cppNode);
						break;

					case HEADER_FILE:
						HeaderNode headerNode = new HeaderNode();
						headerNode.setAbsolutePath(pathItem);
						headerNode.setParent(parent);
						parent.getChildren().add(headerNode);
						break;

					case UNSPECIFIED_FILE:
						UnspecifiedFileNode fileNode = new UnspecifiedFileNode();
						fileNode.setAbsolutePath(pathItem);
						fileNode.setParent(parent);
						parent.getChildren().add(fileNode);
						break;

					case FOLDER:
						FolderNode folderNode = new FolderNode();
						folderNode.setAbsolutePath(pathItem);
						folderNode.setParent(parent);
						parent.getChildren().add(folderNode);

						if (isRecursive())
							parseSrcFolder(folderNode, new File(pathItem));
						break;

					case EXE:
						ExeNode exeFile = new ExeNode();
						exeFile.setAbsolutePath(pathItem);
						exeFile.setParent(parent);
						parent.getChildren().add(exeFile);
						break;

					case OBJECT:
						ObjectNode objectNode = new ObjectNode();
						objectNode.setAbsolutePath(pathItem);
						objectNode.setParent(parent);
						parent.getChildren().add(objectNode);
						break;

					case UNDEFINED_COMPONENT:
						UnknowObjectNode undefinedComponentNode = new UnknowObjectNode();
						undefinedComponentNode.setAbsolutePath(pathItem);
						undefinedComponentNode.setParent(parent);
						parent.getChildren().add(undefinedComponentNode);
						break;
				}
			}
	}

	@Override
	public void generateId(INode root) {
		for (INode child : root.getChildren())
			generateId(child);
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public void setIgnoreFolders(List<File> ignoreFolders) {
		this.ignoreFolders = ignoreFolders;
	}

	public void setConsiderFiles(List<File> considerFiles) {
		this.considerFiles = considerFiles;
	}

	public void setConsiderFolders(List<File> considerFolders) {
		this.considerFolders = considerFolders;
	}
}
