package uet.fit.aut.parser.finder;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import uet.fit.aut.parser.dependency.CompoundDependency;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.NamespaceNodeCondition;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Get searching space of a node in the structure tree.
 * <p>
 * If the node is function/attribute, the searching space consists of the
 * containing file, and all included file (represented in #include)
 *
 * @author DucAnh
 */
public class VariableSearchingSpace implements IVariableSearchingSpace {

//	private final static Logger logger = LoggerFactory.getLogger(VariableSearchingSpace.class);
	private final static Logger logger = new NOPLogger() {};

	public static int STRUCTURE_VS_NAMESPACE_INDEX = 0;

	public static int FILE_SCOPE_INDEX = 1;

	public static int INCLUDED_INDEX = 2;

	private final List<Level> spaces;

	private final Level extendLevel = new Level();

	public List<INode> includeNodes = new ArrayList<>();

	public VariableSearchingSpace(INode startNode) {
		spaces = generateSearchingSpace(startNode);
	}

	private Level getAllCurrentClassvsStructvsNamespace(INode n) {
		Level output = new Level();

		INode parent;
		do {
			parent = Utils.getClassvsStructvsNamesapceNodeParent(n);

			if (parent != null) {
				if (!output.contains(parent))
					output.add(parent);

				// add corresponding namespace in another file
				if (parent instanceof NamespaceNode) {
					for (Dependency d : parent.getDependencies()) {
						if (d instanceof CompoundDependency) {
							INode start = d.getStartArrow();
							if (start instanceof MergedNamespace) {
								for (INode item : start.getChildren()) {
									if (!output.contains(item))
										output.add(item);
								}
							}
						}
					}
				}

				n = parent.getParent();
			}
		} while (parent != null);

		return output;
	}

	public List<INode> getAllIncludedNodes(INode n) {
		List<INode> output = new ArrayList<>();

		if (n != null) {
			logger.debug("get all included nodes from " + n.getAbsolutePath());
			try {
				for (Dependency child : n.getDependencies()) {
					if (child instanceof IncludeHeaderDependency) {
						if (child.getStartArrow().equals(n)) {
							includeNodes.add(n);

							INode end = child.getEndArrow();
							if (!(end instanceof QtHeaderNode) && !includeNodes.contains(end)) {
								output.add(end);
								/*
								 * In case recursive include
								 */
								output.addAll(getAllIncludedNodes(end));
							}
						}
					}
				}
			} catch (StackOverflowError e) {
				e.printStackTrace();
			}
		}

		return output;
	}

	@Override
	public List<Level> getSpaces() {
		return spaces;
	}

	@Override
	public List<Level> generateExtendSpaces() {
		List<Level> extended = new ArrayList<>();

		logger.debug("Search level 4");

		if (!spaces.isEmpty()) {
			for (Level level : spaces) {
				String lastLevelName = level.getName();

				if (lastLevelName.equals(Level.INCLUDED_SCOPE) || lastLevelName.equals(Level.FILE_SCOPE)) {
					extendLevel.setName(Level.EXTENDED_INCLUDED_SCOPE);

					for (INode node : level)
						getAllNodesInclude(node);

					logger.debug("Search level 4. Done.");

					if (extendLevel.size() > 0)
						extended.add(extendLevel);
					for (INode item : extendLevel)
						logger.debug("space element at level 4: " + item.getAbsolutePath());
				}
			}
		}

		extended.addAll(0, spaces);
		return extended;
	}

	private List<INode> getAllNodesInclude(INode n) {

		if (n != null) {
			logger.debug("getAllNodesInclude " + n.getAbsolutePath());
			try {
				for (Dependency child : n.getDependencies()) {
					if (child instanceof IncludeHeaderDependency) {
						if (child.getEndArrow().equals(n)) {

							INode start = child.getStartArrow();
							if (!extendLevel.contains(start)) {
								extendLevel.add(start);
								/*
								 * In case recursive include
								 */
								for (INode node : getAllNodesInclude(start))
									if (!extendLevel.contains(node))
										extendLevel.add(node);
							}
						}
					}
				}
			} catch (StackOverflowError e) {
				e.printStackTrace();
			}
		}

		return extendLevel;
	}

	/**
	 * Get the searching space of a node. Notice that the order of class/struct/file
	 * nodes in the structure is very important!
	 *
	 * @param n node
	 * @return searching space
	 */
	private List<Level> generateSearchingSpace(INode n) {
		List<Level> outputNodes = new ArrayList<>();
		if (n == null)
			return outputNodes;
		/*
		 * Firstly, we must all its parents that belong to class, struct or namespace
		 * (highest priority)
		 */
		logger.debug("Creating searching space at level 1");
		INode parent = n.getParent();

		if (n instanceof AbstractFunctionNode)
			if (((AbstractFunctionNode) n).getRealParent() != null)
				parent = ((AbstractFunctionNode) n).getRealParent();

		Level level1 = null;
		logger.debug("Level 1: Get all current class/struct/namespace");
		if (parent instanceof ClassNode || parent instanceof StructNode || parent instanceof NamespaceNode) {
			level1 = getAllCurrentClassvsStructvsNamespace(n);
			level1.setName(Level.STRUCTURE_AND_NAMESPACE_SCOPE);
			outputNodes.add(level1);
			for (INode item : level1)
				logger.debug("space element at level 1: " + item.getAbsolutePath());
		}
		VariableSearchingSpace.STRUCTURE_VS_NAMESPACE_INDEX = 0;
		/*
		 * Secondly, we get the containing file of the given node
		 */
		logger.debug("Search level 2");
		logger.debug("Level 2: Get the source code file");
		INode sourceCodeFileNode = Utils.getSourcecodeFile(n);
		if (sourceCodeFileNode != null) {
			Level level2 = new Level();
			level2.add(sourceCodeFileNode);
			level2.setName(Level.FILE_SCOPE);
			outputNodes.add(level2);
			for (INode item : level2)
				logger.debug("space element at level 2: " + item.getAbsolutePath());
			VariableSearchingSpace.FILE_SCOPE_INDEX = 1;
		}
		/*
		 * Finally, get all included file (lowest priority)
		 */
		logger.debug("Search level 3");
		logger.debug("Level 3: Get all included files defined by users");
		List<INode> includedNodes = getAllIncludedNodes(sourceCodeFileNode);
		includeNodes = new ArrayList<>();
		if (includedNodes.size() > 0) {
			Level level3 = new Level(includedNodes);
			level3.setName(Level.INCLUDED_SCOPE);
			outputNodes.add(level3);
			for (INode item : level3)
				logger.debug("space element at level 3: " + item.getAbsolutePath());

			findAllNamespaceInIncluded(level3, level1);
		}
		VariableSearchingSpace.INCLUDED_INDEX = 2;
		logger.debug("Search level 3. Done.");

		for (Level l : outputNodes) {
			l.distinct();
		}

		return outputNodes;
	}

	private void findAllNamespaceInIncluded(Level level3, Level level1) {
		if (level1 == null)
			return;

		List<INode> namespaces = new ArrayList<>(level1);
		namespaces.removeIf(n -> !(n instanceof NamespaceNode));

		if (namespaces.isEmpty())
			return;

		INode sourceFileNode = Utils.getSourcecodeFile(namespaces.get(0));
		String sourceFilePath = sourceFileNode.getAbsolutePath();

		Map<String, INode> namespacePaths = new HashMap<>();
		for (INode namespace : namespaces) {
			String relativePath = namespace.getAbsolutePath().substring(sourceFilePath.length());
			namespacePaths.put(relativePath, namespace);
		}

		for (INode file : level3) {
			String filePath = file.getAbsolutePath();
			List<INode> namespacesInIncludedFile = Search.searchNodes(file, new NamespaceNodeCondition());

			for (INode namespace : namespacesInIncludedFile) {
				String relativePath = namespace.getAbsolutePath().substring(filePath.length());

				if (namespacePaths.containsKey(relativePath) && !level1.contains(namespace)) {
					INode correspondingNode = namespacePaths.get(relativePath);
					int index = level1.indexOf(correspondingNode);

					level1.add(index, namespace);
				}
			}
		}
	}

//	public List<INode> search(String name, SearchCondition condition) {
//		List<INode> result = new ArrayList<>();
//
//		for (Level level : spaces) {
//			for (INode node : level) {
//				result.addAll(Search.searchNodes(node, condition, name));
//			}
//		}
//
//		return result;
//	}
}
