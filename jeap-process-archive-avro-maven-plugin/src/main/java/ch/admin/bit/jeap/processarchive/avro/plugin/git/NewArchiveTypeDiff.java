package ch.admin.bit.jeap.processarchive.avro.plugin.git;

import ch.admin.bit.jeap.processarchive.avro.plugin.helper.TypeDescriptorFactory;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

@UtilityClass
class NewArchiveTypeDiff {

    /**
     * @return The set of archive type versions added in a commit, compared to a base commit
     */
    static Set<NewArchiveTypeVersionDto> newNewArchiveTypeVersions(
            Path sourceDir, String descriptorPath, Git git, RevCommit newCommit, RevCommit baseCommit) throws MojoExecutionException {
        try {
            String newDescriptor = getContent(newCommit, descriptorPath, git);
            String baseDescriptor = getContent(baseCommit, descriptorPath, git);
            ArchiveTypeDescriptor newTypeDescriptor = TypeDescriptorFactory.readTypeDescriptor(newDescriptor, descriptorPath);
            ArchiveTypeDescriptor baseTypeDescriptor = TypeDescriptorFactory.readTypeDescriptor(baseDescriptor, descriptorPath);

            return diffArchiveTypes(sourceDir, Path.of(descriptorPath), newTypeDescriptor, baseTypeDescriptor);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read descriptor " + descriptorPath, ex);
        }
    }

    static Set<NewArchiveTypeVersionDto> diffArchiveTypes(Path sourceDir, Path descriptorPath,
                                                          ArchiveTypeDescriptor newTypeDescriptor,
                                                          ArchiveTypeDescriptor baseTypeDescriptor) {
        Set<Integer> newVersions = getVersionNumbers(newTypeDescriptor);
        Set<Integer> baseVersions = getVersionNumbers(baseTypeDescriptor);
        newVersions.removeAll(baseVersions);
        String systemName = systemName(descriptorPath);

        Path absoluteDescriptorPath = sourceDir.resolve(descriptorPath);

        return newVersions.stream()
                .map(v -> new NewArchiveTypeVersionDto(systemName, absoluteDescriptorPath, newTypeDescriptor, v))
                .collect(toSet());
    }

    /**
     * @return the system name part of a descriptor path (descriptor/sys/.. --> "sys")
     */
    private static String systemName(Path path) {
        return path.subpath(1, 2).toString().toLowerCase(Locale.ROOT);
    }

    private static Set<Integer> getVersionNumbers(ArchiveTypeDescriptor typeDescriptor) {
        if (typeDescriptor == null) {
            return Set.of();
        }
        return typeDescriptor.getVersions().stream()
                .map(ArchiveTypeVersion::getVersion)
                .collect(toCollection(HashSet::new));
    }

    /**
     * @return File content as String if found, null otherwise
     */
    private static String getContent(RevCommit commit, String descriptorPath, Git git) throws IOException {
        Repository repository = git.getRepository();
        RevTree tree = commit.getTree();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            if (!findFile(descriptorPath, treeWalk)) {
                return null;
            }

            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean findFile(String descriptorPath, TreeWalk treeWalk) throws IOException {
        while (treeWalk.next()) {
            if (treeWalk.getPathString().equals(descriptorPath)) {
                return true;
            }
        }
        return false;
    }
}
