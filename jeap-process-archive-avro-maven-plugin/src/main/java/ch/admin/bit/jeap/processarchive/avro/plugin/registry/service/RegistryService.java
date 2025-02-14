package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.RegistryConnector;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.RegistryConnectorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.networknt.schema.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to consume types from the archive type registry. Uses {@link RegistryConnector} to get files
 */
public class RegistryService {
    static final String REFERENCES_SCHEMA_FILE = "classpath:/ArchiveTypeReferences.schema.json";
    private static final JsonSchema SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(SchemaLocation.of(REFERENCES_SCHEMA_FILE));

    private final ReferenceList referenceList;
    private final RegistryConnector registryConnector;
    private Map<String, File> globalFiles;

    public RegistryService(File referencesFile) throws RegistryException {
        this(referencesFile, e -> new RegistryConnector(e.getRepoUrl(), e.getGitReference()));
    }

    /**
     * Constructor to use another registryConnector, used for testing
     */
    RegistryService(File referencesFile, Function<ReferenceList, RegistryConnector> registryConnectorFactory) throws RegistryException {

        try {
            JsonNode referencesAsJson = JsonLoader.fromFile(referencesFile);
            Set<ValidationMessage> validationMessages = SCHEMA.validate(referencesAsJson);
            if (!validationMessages.isEmpty()) {
                String errorMessage = validationMessages.stream().map(ValidationMessage::toString).collect(Collectors.joining(", "));
                throw RegistryException.schemaValidationFailed(referencesFile, errorMessage);
            }
            referenceList = new ObjectMapper().treeToValue(referencesAsJson, ReferenceList.class);
        } catch (IOException e) {
            throw RegistryException.cannotReadFile(referencesFile, e);
        }
        this.registryConnector = registryConnectorFactory.apply(referenceList);
    }

    /**
     * Download all schemas for all types in the given references files.
     *
     * @return A list with the downloaded files
     * @throws RegistryException If the files cannot be downloaded
     */
    public DownloadResult download() throws RegistryException {
        // Sort types by system first and then handle each system separately
        return typesSortedBySystem().entrySet().stream()
                .map(e -> downloadSystem(e.getKey(), e.getValue()))
                .reduce(DownloadResult.empty(), DownloadResult::merge);
    }

    private Map<String, List<ArchiveTypeReference>> typesSortedBySystem() {
        Map<String, List<ArchiveTypeReference>> result = new HashMap<>();
        for (ArchiveTypeReference archiveTypeReference : referenceList.getArchiveTypes()) {
            String systemName = archiveTypeReference.getSystem().toLowerCase();
            if (!result.containsKey(systemName)) {
                result.put(systemName, new LinkedList<>());
            }
            result.get(systemName).add(archiveTypeReference);
        }
        return result;
    }

    private DownloadResult downloadSystem(String systemName, List<ArchiveTypeReference> archiveTypeReferences) {
        //Download common files, then handle each type separately
        Map<String, File> commonFiles = getCommonFiles(systemName);
        return archiveTypeReferences.stream()
                .map(r -> downloadType(r, commonFiles))
                .reduce(DownloadResult.empty(), DownloadResult::merge);
    }

    private Map<String, File> getCommonFiles(String systemName) {
        if (this.globalFiles == null) {
            this.globalFiles = registryConnector.downloadCommonFilesFromRegistry(null);
        }
        //Get the import files for this system. As the global files overwrite everything, add them last
        Map<String, File> result = new HashMap<>();
        result.putAll(registryConnector.downloadCommonFilesFromRegistry(systemName));
        result.putAll(globalFiles);
        return result;
    }

    private DownloadResult downloadType(ArchiveTypeReference archiveTypeReference, Map<String, File> commonFiles) {
        ArchiveTypeDescriptor typeDescriptor = getTypeDescriptor(archiveTypeReference);
        ArchiveTypeVersion archiveTypeVersion = getTypeVersion(typeDescriptor, archiveTypeReference);

        return DownloadResult.builder()
                .schema(getFileFromCommonOrArchiveType(archiveTypeReference, archiveTypeVersion.getSchema(), commonFiles))
                .archiveTypeDescriptor(typeDescriptor)
                .build();
    }

    private ArchiveTypeDescriptor getTypeDescriptor(ArchiveTypeReference archiveTypeReference) {
        try {
            return registryConnector.downloadDescriptorFromRegistry(archiveTypeReference);
        } catch (RegistryConnectorException e) {
            throw RegistryException.cannotDownloadDescriptor(archiveTypeReference, e);
        }
    }

    private ArchiveTypeVersion getTypeVersion(ArchiveTypeDescriptor typeDescriptor, ArchiveTypeReference archiveTypeReference) {
        return typeDescriptor.getVersions().stream()
                .filter(v -> v.getVersion().equals(archiveTypeReference.getVersion()))
                .findFirst()
                .orElseThrow(() -> RegistryException.invalidVersion(archiveTypeReference, typeDescriptor));
    }

    private DownloadedSchema getFileFromCommonOrArchiveType(ArchiveTypeReference archiveTypeReference, String filename,
                                                            Map<String, File> commonFiles) {
        // Common and precedence namespaces should be separated anyway... However, in case of problems, common is ignored
        try {
            return new DownloadedSchema(registryConnector.downloadSchemaFromRegistry(archiveTypeReference, filename), commonFiles);
        } catch (RegistryConnectorException e) {
            if (!commonFiles.containsKey(filename)) {
                throw RegistryException.cannotDownloadSchema(archiveTypeReference, filename, e);
            }
            return new DownloadedSchema(commonFiles.get(filename), commonFiles);
        }
    }
}
