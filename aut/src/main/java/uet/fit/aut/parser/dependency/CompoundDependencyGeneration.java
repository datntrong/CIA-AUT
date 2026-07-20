package uet.fit.aut.parser.dependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.SpecialCharacter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CompoundDependencyGeneration extends AbstractDependencyGeneration<NamespaceNode> {

    private final static Logger logger = LoggerFactory.getLogger(CompoundDependencyGeneration.class);

    private final Map<String, MergedNamespace> mergedNs = new HashMap<>();

    public CompoundDependencyGeneration(ProjectNode root, List<NamespaceNode> namespaces) {

        Map<String, List<NamespaceNode>> category = new HashMap<>();
        for (NamespaceNode ns : namespaces) {
            String relativePath = getRelativePath(ns);
            List<NamespaceNode> correspondingNamespaces = category.get(relativePath);
            if (correspondingNamespaces == null) {
                correspondingNamespaces = new ArrayList<>();
                category.put(relativePath, correspondingNamespaces);
            }
            correspondingNamespaces.add(ns);
        }

        List<String> allPaths = category.keySet().stream()
                .sorted(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int c1 = o1.split(File.separator).length;
                        int c2 = o2.split(File.separator).length;
                        return Integer.compare(c1, c2);
                    }
                })
                .collect(Collectors.toList());

        for (String relativePath : allPaths) {
            if (category.get(relativePath).size() > 1) {
                MergedNamespace mergedNamespace = mergeNamespace(root, relativePath);
                INode parent = findParentForMerge(root, relativePath, namespaces, mergedNs.values());
                mergedNamespace.setParent(parent);
                parent.getChildren().add(mergedNamespace);
                mergedNs.put(relativePath, mergedNamespace);
            }
        }
    }

    private INode findParentForMerge(ProjectNode root, String relativePath,
            List<NamespaceNode> singles, Collection<MergedNamespace> compounds) {

        String[] elements = relativePath.split(File.separator);

        if (elements.length > 1) {
            for (MergedNamespace compound : compounds) {
                File nsFile = new File(compound.getAbsolutePath());
                String curPath = PathUtils.relative(nsFile, root.getFile());
                String[] curPathElements = curPath.split(File.separator);
                if (isParent(elements, curPathElements)) {
                    return compound;
                }
            }

            for (NamespaceNode ns : singles) {
                String curPath = getRelativePath(ns);
                String[] curPathElements = curPath.split(File.separator);
                if (isParent(elements, curPathElements)) {
                    return ns;
                }
            }
        }

        return root;
    }

    private boolean isParent(String[] path, String[] parent) {
        if (parent.length == path.length - 1) {
            for (int i = 0; i < parent.length; i++) {
                if (!path[i].equals(parent[i]))
                    return false;
            }
            return true;
        }

        return false;
    }

    public void dependencyGeneration(NamespaceNode n) {
        String relativePath = getRelativePath(n);
        MergedNamespace mergedNamespace = mergedNs.get(relativePath);
        if (mergedNamespace != null) {
            CompoundDependency d = new CompoundDependency(mergedNamespace, n);
            if (!mergedNamespace.getDependencies().contains(d)
                    && !n.getDependencies().contains(d)) {
                mergedNamespace.getDependencies().add(d);
                n.getDependencies().add(d);
                logger.debug("Found a compound dependency: " + d);
            }
        }
    }

    private MergedNamespace mergeNamespace(ProjectNode root, String relativePath) {
        MergedNamespace mergedNamespace = new MergedNamespace();
        String absolutePath = root.getAbsolutePath() + File.separator + relativePath;
        mergedNamespace.setAbsolutePath(absolutePath);
        return mergedNamespace;
    }

    private String getRelativePath(NamespaceNode ns) {
        String path = SpecialCharacter.EMPTY;
        INode cur = ns;
        do {
            path = cur.getName() + path;
            if (!(cur.getParent() instanceof ISourcecodeFileNode))
                path = File.separator + path;
            cur = cur.getParent();
        } while (!(cur instanceof ISourcecodeFileNode));
        return path;
    }
}
