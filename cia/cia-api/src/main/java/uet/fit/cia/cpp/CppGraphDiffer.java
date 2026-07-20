package uet.fit.cia.cpp;

import io.gitlab.multicia.cpp.CppConnection;
import io.gitlab.multicia.cpp.node.CppClassNode;
import io.gitlab.multicia.cpp.node.CppEnumerationNode;
import io.gitlab.multicia.cpp.node.CppFunctionNode;
import io.gitlab.multicia.cpp.node.CppGroup;
import io.gitlab.multicia.cpp.node.CppLocation;
import io.gitlab.multicia.cpp.node.CppNamespaceNode;
import io.gitlab.multicia.cpp.node.CppNode;
import io.gitlab.multicia.cpp.node.CppNodeHelper;
import io.gitlab.multicia.cpp.node.CppRootNode;
import io.gitlab.multicia.cpp.node.CppStructNode;
import io.gitlab.multicia.cpp.node.CppTypedefNode;
import io.gitlab.multicia.cpp.node.CppUnionNode;
import io.gitlab.multicia.cpp.node.CppVariableNode;
import io.gitlab.multicia.fs.DirectoryNode;
import io.gitlab.multicia.fs.FileNode;
import io.gitlab.multicia.fs.FileSystemGroup;
import io.gitlab.multicia.fs.FileSystemHelper;
import io.gitlab.multicia.fs.FileSystemNode;
import io.gitlab.multicia.fs.RootNode;
import io.gitlab.multicia.graph.Connection;
import io.gitlab.multicia.graph.Graph;
import io.gitlab.multicia.graph.GraphChange;
import io.gitlab.multicia.graph.GraphDiffer;
import io.gitlab.multicia.graph.GraphDifference;
import io.gitlab.multicia.graph.GraphException;
import io.gitlab.multicia.graph.Node;
import org.apache.commons.text.diff.CommandVisitor;
import org.apache.commons.text.diff.EditScript;
import org.apache.commons.text.diff.StringsComparator;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Triple;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.cpp.display.ModificationsResponse;
import uet.fit.cia.cpp.display.ProjectResponse;
import uet.fit.cia.cpp.display.ProjectResponse.Element;
import uet.fit.cia.cpp.display.ProjectResponse.Location;
import uet.fit.cia.cpp.display.ProjectResponse.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static uet.fit.cia.cpp.display.ProjectResponse.Type;

final class CppGraphDiffer {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CppGraphDiffer.class);

	private static final double SIMILAR_THRESHOLD = 0.25;

	private final @NotNull Graph graphA;
	private final @NotNull Graph graphB;
	private final @NotNull GraphDiffer differ;

	private final @NotNull MutableObjectIntMap<Element> map = new ObjectIntHashMap<>();

	CppGraphDiffer(@NotNull Graph graphA, @NotNull Graph graphB) {
		this.graphA = graphA;
		this.graphB = graphB;
		this.differ = new GraphDiffer(graphA, graphB);
	}


//	//region TEST ONLY
//
//	public static void main(@NotNull String[] strings) throws GraphException {
//		final Path jsonOld = Path.of("./local/cia/c7db898574815e43bcf9ab7b358a9d2f19877a3e");
//		final Path jsonNew = Path.of("./local/cia/274413d7e98068a0599d0a5e2edc30310ee0283b");
//		final Graph graphOld = CppApi.loadCppGraph(jsonOld);
//		final Graph graphNew = CppApi.loadCppGraph(jsonNew);
//		final DisplayJson json = compare(graphOld, graphNew);
//		System.out.println(json);
//	}
//
//	//endregion TEST ONLY


	static @NotNull CppGraphDiffer compare(@NotNull Graph graphA, @NotNull Graph graphB) {
		return new CppGraphDiffer(graphA, graphB);
	}


	@NotNull ProjectResponse buildProject() throws GraphException {
		LOGGER.info("Comparing FileSystemGroup...");
		final FileSystemGroup fileSystemGroupA = graphA.getGroup(FileSystemGroup.class);
		final FileSystemGroup fileSystemGroupB = graphB.getGroup(FileSystemGroup.class);
		if (fileSystemGroupA == null || fileSystemGroupB == null) {
			throw new GraphException("Cannot find FileSystemGroup in Graph!");
		}
		final GraphDifference<FileSystemNode> fileSystemDifference = differ.compare(fileSystemGroupA, fileSystemGroupB);
		final Map<FileSystemNode, Element> fileSystemMap = new LinkedHashMap<>();
		buildDifference(fileSystemDifference, this::createFromFileSystem, fileSystemMap);

		LOGGER.info("Comparing CppGroup...");
		final CppGroup cppGroupA = graphA.getGroup(CppGroup.class);
		final CppGroup cppGroupB = graphB.getGroup(CppGroup.class);
		if (cppGroupA == null || cppGroupB == null) {
			throw new GraphException("Cannot find CppGroup in Graph!");
		}
		final GraphDifference<CppNode> cppDifference = differ.compare(cppGroupA, cppGroupB);
		final Map<CppNode, Element> cppMap = new LinkedHashMap<>();
		final Map<Element, Twin<CppNode>> cppNodeMap
				= buildDifference(cppDifference, this::createFromCppNode, cppMap);

		LOGGER.info("Combining FileSystemGroup differences and CppGroup differences...");
		combineFileSystemWithCppNode(fileSystemMap, cppMap, cppNodeMap);

		LOGGER.info("Calculating the impact from differences...");
		final ObjectDoubleHashMap<Connection> connectionWeights = new ObjectDoubleHashMap<>();
		connectionWeights.put(CppConnection.USE, 1);
		connectionWeights.put(CppConnection.MEMBER, 1);
		connectionWeights.put(CppConnection.INHERIT, 1);
		connectionWeights.put(CppConnection.CALL, 1);
		connectionWeights.put(CppConnection.OVERRIDE, 1);
		final ObjectDoubleMap<CppNode> impactMap = cppDifference.calculateImpact(connectionWeights);
		for (final ObjectDoublePair<CppNode> pair : impactMap.keyValuesView()) {
			final CppNode node = pair.getOne();
			final Element element = cppMap.get(node);
			if (element != null) element.setImpact((float) pair.getTwo());
		}

		LOGGER.info("Creating ProjectResponse...");
		final ProjectResponse projectResponse = new ProjectResponse();

		for (final Map.Entry<FileSystemNode, Element> entry : fileSystemMap.entrySet()) {
			if (entry.getKey() instanceof RootNode) {
				projectResponse.setFileRoot(map.get(entry.getValue()));
				break;
			}
		}
		for (final Map.Entry<CppNode, Element> entry : cppMap.entrySet()) {
			if (entry.getKey() instanceof CppRootNode) {
				projectResponse.setLanguageRoot(map.get(entry.getValue()));
				break;
			}
		}

		projectResponse.setElements(
				Stream.concat(fileSystemMap.values().stream(), cppMap.values().stream())
						.distinct().toArray(Element[]::new)
		);

		return projectResponse;
	}

	@NotNull ModificationsResponse buildModifications() throws GraphException {
		final CppGroup cppGroupA = graphA.getGroup(CppGroup.class);
		final CppGroup cppGroupB = graphB.getGroup(CppGroup.class);
		if (cppGroupA == null || cppGroupB == null) {
			throw new GraphException("Cannot find CppGroup in Graph!");
		}
		final GraphDifference<CppNode> difference = differ.compare(cppGroupA, cppGroupB);
		final List<ModificationsResponse.Modification> modifications = new ArrayList<>();
		for (final Triple<CppNode, CppNode, GraphChange> triple : difference) {
			final GraphChange change = triple.getThree();
			if (change == GraphChange.UNCHANGED) continue;
			final CppNode node = change == GraphChange.REMOVED ? triple.getOne() : triple.getTwo();
			final String qualifiedName = CppNodeHelper.qualifiedName(node);
			final String withPrototype = CppNodeHelper.readableName(node);
			final String[] filePaths = node.getLocations().stream()
					.map(CppLocation::getFile)
					.filter(Objects::nonNull)
					.map(FileSystemHelper::absolutePath)
					.toArray(String[]::new);
			final ModificationsResponse.Modification modification = new ModificationsResponse.Modification();
			modification.setQualifiedName(qualifiedName);
			modification.setWithPrototype(withPrototype);
			modification.setFilePaths(filePaths);
			modification.setChange(ModificationsResponse.Change.valueOf(change.name()));
			modifications.add(modification);
		}
		final ModificationsResponse response = new ModificationsResponse();
		response.setModifications(modifications.toArray(ModificationsResponse.Modification[]::new));
		return response;
	}

	private @NotNull Position createPositionFromCppLocation(@NotNull Map<FileSystemNode, Element> fileSystemMap,
			@NotNull CppLocation location) {
		final Position position = new Position();
		final Element element = fileSystemMap.get(location.getFile());
		position.setFile(map.getOrThrow(element));
		position.setStartLine(location.getStartLine());
		position.setEndLine(location.getEndLine());
		return position;
	}

	private @NotNull List<Twin<CppLocation>> matchLocationsByContent(
			@NotNull List<CppLocation> locationsA, @NotNull List<CppLocation> locationsB) {
		final List<Twin<CppLocation>> locationList = new ArrayList<>(); // use as a set
		final Set<CppLocation> locationSet = new HashSet<>(); // use as a set
		// try matching locations with exactly same content
		{
			final Map<String, Set<CppLocation>> contentMap = new HashMap<>();
			for (final CppLocation locationA : locationsA) {
				contentMap.computeIfAbsent(locationA.getContent(), any -> new HashSet<>()).add(locationA);
			}
			for (final CppLocation locationB : locationsB) {
				final Set<CppLocation> matchedLocationsA = contentMap.get(locationB.getContent());
				if (matchedLocationsA == null) continue;
				if (matchedLocationsA.size() == 1) {
					final CppLocation locationA = matchedLocationsA.stream().findAny().get();
					locationSet.add(locationA);
					locationSet.add(locationB);
					locationList.add(Tuples.twin(locationA, locationB));
					matchedLocationsA.clear();
				} else {
					int distance = Integer.MAX_VALUE;
					CppLocation locationA = null;
					for (final CppLocation location : matchedLocationsA) {
						int current = Math.abs(location.getStartLine() + location.getEndLine()
								- locationB.getStartLine() - locationB.getEndLine());
						if (distance > current) {
							distance = current;
							locationA = location;
						}
					}
					locationSet.add(locationA);
					locationSet.add(locationB);
					locationList.add(Tuples.twin(locationA, locationB));
					matchedLocationsA.remove(locationA);
				}
			}
		}
		if (locationSet.size() < locationsA.size() + locationsB.size()) {
			// try matching with similar content
			final SortedSet<DoubleObjectPair<Twin<CppLocation>>> locationTwins
					= new TreeSet<>((pairA, pairB) -> Double.compare(pairB.getOne(), pairA.getOne()));
			for (final CppLocation locationA : locationsA) {
				final String contentA = Objects.requireNonNullElse(locationA.getContent(), "");
				for (final CppLocation locationB : locationsB) {
					final String contentB = Objects.requireNonNullElse(locationB.getContent(), "");
					final EditScript<Character> script = new StringsComparator(contentA, contentB).getScript();
					final CountingVisitor<Character> countingVisitor = new CountingVisitor<>();
					script.visit(countingVisitor);
					final double value = countingVisitor.getSimilarity();
					if (value >= SIMILAR_THRESHOLD) {
						locationTwins.add(PrimitiveTuples.pair(value, Tuples.twin(locationA, locationB)));
					}
				}
			}
			for (final DoubleObjectPair<Twin<CppLocation>> locationTwin : locationTwins) {
				final Twin<CppLocation> innerTwin = locationTwin.getTwo();
				final CppLocation locationA = innerTwin.getOne();
				final CppLocation locationB = innerTwin.getTwo();
				if (!locationSet.contains(locationA) && !locationSet.contains(locationB)) {
					locationSet.add(locationA);
					locationSet.add(locationB);
					locationList.add(Tuples.twin(locationA, locationB));
				}
			}
		}
		if (locationSet.size() < locationsA.size() + locationsB.size()) {
			// put everything else as ADDED or REMOVED
			for (final CppLocation locationA : locationsA) {
				if (locationSet.contains(locationA)) continue;
				locationList.add(Tuples.twin(locationA, null));
			}
			for (final CppLocation locationB : locationsB) {
				if (locationSet.contains(locationB)) continue;
				locationList.add(Tuples.twin(null, locationB));
			}
		}
		return locationList;
	}

	private static final class CountingVisitor<E> implements CommandVisitor<E> {
		private long union = 0;
		private long intersection = 0;

		@Override
		public void visitInsertCommand(E e) {
			this.union++;
		}

		@Override
		public void visitKeepCommand(E e) {
			this.union++;
			this.intersection++;
		}

		@Override
		public void visitDeleteCommand(E e) {
			this.union++;
		}

		public double getSimilarity() {
			return (double) intersection / union;
		}
	}

	private @NotNull List<Location> convertLocationAndCreateFileMap(@NotNull Map<FileSystemNode, Element> fileSystemMap,
			@NotNull Map<Element, Set<CppNode>> fileMap, @Nullable CppNode nodeA, @Nullable CppNode nodeB) {
		final List<Location> locations = new ArrayList<>();
		final Map<Element, Twin<List<CppLocation>>> objectMap = new LinkedHashMap<>();
		if (nodeA != null) {
			for (final CppLocation cppLocation : nodeA.getLocations()) {
				final FileNode fileNode = cppLocation.getFile();
				final Element fileElement = fileSystemMap.get(fileNode);
				objectMap.computeIfAbsent(fileElement, any -> Tuples.twin(new ArrayList<>(), new ArrayList<>()))
						.getOne().add(cppLocation);
				fileMap.computeIfAbsent(fileElement, any -> new LinkedHashSet<>()).add(nodeA);
			}
		}
		if (nodeB != null) {
			for (final CppLocation cppLocation : nodeB.getLocations()) {
				final FileNode fileNode = cppLocation.getFile();
				final Element fileElement = fileSystemMap.get(fileNode);
				objectMap.computeIfAbsent(fileElement, any -> Tuples.twin(new ArrayList<>(), new ArrayList<>()))
						.getTwo().add(cppLocation);
				fileMap.computeIfAbsent(fileElement, any -> new LinkedHashSet<>()).add(nodeB);
			}
		}
		for (final Twin<List<CppLocation>> twin : objectMap.values()) {
			final List<CppLocation> locationsA = twin.getOne();
			final List<CppLocation> locationsB = twin.getTwo();
			if (locationsA.isEmpty()) {
				for (final CppLocation locationB : locationsB) {
					final Location location = new Location();
					location.setNewPosition(createPositionFromCppLocation(fileSystemMap, locationB));
					location.setChange(ProjectResponse.Change.ADDED);
					locations.add(location);
				}
			} else if (locationsB.isEmpty()) {
				for (final CppLocation locationA : locationsA) {
					final Location location = new Location();
					location.setOldPosition(createPositionFromCppLocation(fileSystemMap, locationA));
					location.setChange(ProjectResponse.Change.REMOVED);
					locations.add(location);
				}
			} else {
				final List<Twin<CppLocation>> locationList = matchLocationsByContent(locationsA, locationsB);
				for (final Twin<CppLocation> locationTwin : locationList) {
					final CppLocation locationA = locationTwin.getOne();
					final CppLocation locationB = locationTwin.getTwo();
					final Location location = new Location();
					if (locationA != null && locationB != null) {
						location.setOldPosition(createPositionFromCppLocation(fileSystemMap, locationA));
						location.setNewPosition(createPositionFromCppLocation(fileSystemMap, locationB));
						location.setChange(Objects.equals(locationA.getContent(), locationB.getContent())
								? ProjectResponse.Change.UNCHANGED
								: ProjectResponse.Change.CHANGED);
					} else if (locationA != null) {
						location.setOldPosition(createPositionFromCppLocation(fileSystemMap, locationA));
						location.setChange(ProjectResponse.Change.REMOVED);
					} else if (locationB != null) {
						location.setNewPosition(createPositionFromCppLocation(fileSystemMap, locationB));
						location.setChange(ProjectResponse.Change.ADDED);
					} else {
						throw new AssertionError();
					}
					locations.add(location);
				}
			}
		}
		return locations;
	}

	private void combineFileSystemWithCppNode(@NotNull Map<FileSystemNode, Element> fileSystemMap,
			@NotNull Map<CppNode, Element> cppMap, @NotNull Map<Element, Twin<CppNode>> cppNodeMap) {

		final Map<Element, Set<CppNode>> fileMap = new HashMap<>();
		for (final Map.Entry<Element, Twin<CppNode>> entry : cppNodeMap.entrySet()) {
			final Element cppElement = entry.getKey();
			final Twin<CppNode> twin = entry.getValue();
			final List<Location> locations =
					convertLocationAndCreateFileMap(fileSystemMap, fileMap, twin.getOne(), twin.getTwo());
			cppElement.setLocations(locations.toArray(Location[]::new));
		}

		// get children for file element
		for (final Map.Entry<Element, Set<CppNode>> entry : fileMap.entrySet()) {
			final Element fileElement = entry.getKey();
			final Set<CppNode> cppNodes = entry.getValue();

			final Set<Node> processedNodes = new HashSet<>();
			final Set<Node> queuedNodes = new HashSet<>(cppNodes);
			final Queue<Node> queue = new ArrayDeque<>(cppNodes);
			while (!queue.isEmpty()) {
				final Node cppNode = queue.remove();
				processedNodes.add(cppNode);
				for (final Node cppChild : cppNode.getChildren()) {
					if (cppChild instanceof CppNode) {
						cppNodes.remove(cppChild);
						if (!processedNodes.contains(cppChild) && queuedNodes.add(cppChild)) queue.add(cppChild);
					}
				}
			}
			fileElement.setChildren(cppNodes.stream().map(cppMap::get).distinct().mapToInt(map::get).toArray());
		}
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	private <N extends Node> @NotNull Map<Element, Twin<N>> buildDifference(@NotNull GraphDifference<N> difference,
			@NotNull BiFunction<N, GraphChange, Element> createDisplayJson, @NotNull Map<N, Element> nodeMap) {
		final Map<Element, Twin<N>> jsonMap = new LinkedHashMap<>();

		for (final Triple<N, N, GraphChange> triple : difference) {
			final N nodeA = triple.getOne();
			final N nodeB = triple.getTwo();
			final Element displayJson = createDisplayJson.apply(nodeB != null ? nodeB : nodeA, triple.getThree());
			if (nodeA != null) nodeMap.put(nodeA, displayJson);
			if (nodeB != null) nodeMap.put(nodeB, displayJson);
			jsonMap.put(displayJson, Tuples.twin(nodeA, nodeB));
		}

		for (final Map.Entry<Element, Twin<N>> entry : jsonMap.entrySet()) {
			final Element element = entry.getKey();
			final Pair<N, N> pair = entry.getValue();
			final N nodeA = pair.getOne();
			final N nodeB = pair.getTwo();

			final Set<Element> children = new LinkedHashSet<>();
			if (nodeB != null) {
				for (final Node node : nodeB.getChildren()) {
					children.add(nodeMap.get(node));
				}
			}
			if (nodeA != null) {
				for (final Node node : nodeA.getChildren()) {
					children.add(nodeMap.get(node));
				}
			}
			final int[] childIds = new int[children.size()];
			int index = 0;
			for (final Element child : children) {
				childIds[index++] = map.get(child);
			}
			element.setChildren(childIds);
		}

		return jsonMap;
	}

	private @NotNull Element createFromFileSystem(@NotNull FileSystemNode node, @NotNull GraphChange change) {
		final Type type
				= node instanceof DirectoryNode
				? Type.DIRECTORY
				: node instanceof FileNode
				? Type.FILE
				: node instanceof RootNode
				? Type.ROOT
				: null;
		if (type == null) throw new IllegalArgumentException();
		final Element element = new Element();
		element.setName(node.getName());
		element.setType(type);
		element.setChange(ProjectResponse.Change.valueOf(change.toString()));
		map.put(element, map.size());
		return element;
	}

	private @NotNull Element createFromCppNode(@NotNull CppNode node, @NotNull GraphChange change) {
		final Type type
				= node instanceof CppClassNode
				? Type.CLASS
				: node instanceof CppEnumerationNode
				? Type.ENUM
				: node instanceof CppFunctionNode
				? Type.FUNCTION
				: node instanceof CppNamespaceNode
				? Type.NAMESPACE
				: node instanceof CppRootNode
				? Type.ROOT
				: node instanceof CppStructNode
				? Type.STRUCT
				: node instanceof CppTypedefNode
				? Type.TYPEDEF
				: node instanceof CppUnionNode
				? Type.UNION
				: node instanceof CppVariableNode
				? Type.VARIABLE
				: null;
		if (type == null) throw new IllegalArgumentException();
		final Element element = new Element();
		element.setName(CppNodeHelper.readableName(node));
		element.setType(type);
		element.setChange(ProjectResponse.Change.valueOf(change.toString()));
		map.put(element, map.size());
		return element;
	}
}
