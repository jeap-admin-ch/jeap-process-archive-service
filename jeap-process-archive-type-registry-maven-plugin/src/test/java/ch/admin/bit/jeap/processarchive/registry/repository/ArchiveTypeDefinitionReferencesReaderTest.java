package ch.admin.bit.jeap.processarchive.registry.repository;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveTypeDefinitionReferencesReaderTest {

    @Test
    void read_validFileWithCommit() {
        File validFile = new File("src/test/resources/valid-archive-type-definition-references.json");

        ArchiveTypeDefinitionReferences references = ArchiveTypeDefinitionReferencesReader.read(validFile);

        assertEquals("https://foo/bar/", references.getRepoUrl());
        assertEquals("023423fasdf2", references.getCommit());
        assertEquals("023423fasdf2", references.getGitReference().getCommit());
        assertEquals(List.of("foo", "bar"), references.getSystems());
    }

    @Test
    void read_validFileWithBranch() {
        File validFile = new File("src/test/resources/valid-archive-type-definition-references-branch.json");

        ArchiveTypeDefinitionReferences references = ArchiveTypeDefinitionReferencesReader.read(validFile);

        assertEquals("https://foo/bar/", references.getRepoUrl());
        assertEquals("master", references.getBranch());
        assertEquals("master", references.getGitReference().getBranch());
        assertEquals(List.of("foo", "bar"), references.getSystems());
    }

    @Test
    void read_invalidFile() {
        File validFile = new File("src/test/resources/invalid-archive-type-definition-references.json");

        RepositoryException ex = assertThrows(RepositoryException.class, () ->
                ArchiveTypeDefinitionReferencesReader.read(validFile));
        assertTrue(ex.getMessage().contains("does not conform to schema"), ex.getMessage());
    }
}
