package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class ArchiveDataHttpResponseMapper {

    private static final String ARCHIVE_DATA_SYSTEM = "archive-data-system";
    private static final String ARCHIVE_DATA_SCHEMA = "archive-data-schema";
    private static final String ARCHIVE_DATA_SCHEMA_VERSION = "archive-data-schema-version";
    private static final String ARCHIVE_STORAGE_PREFIX = "archive-storage-prefix";
    private static final String ARCHIVE_STORAGE_BUCKET = "archive-storage-bucket";
    private static final String ARCHIVE_METADATA = "archive-metadata-";

    static ArchiveData mapResponseToArchiveData(ArchiveDataReference reference, ConvertibleClientHttpResponse clientResponse) throws IOException {
        if (clientResponse.getStatusCode().is2xxSuccessful()) {
            byte[] payload = clientResponse.bodyTo(byte[].class);
            return createArchiveData(reference, clientResponse.getHeaders(), payload);
        } else {
            throw RemoteDataProviderException.badHttpResponseStatus(reference, clientResponse.getStatusCode());
        }
    }

    private static ArchiveData createArchiveData(ArchiveDataReference reference, HttpHeaders headers, byte[] payload) {
        String contentType = requireHeader(reference, headers, HttpHeaders.CONTENT_TYPE);
        String system = requireHeader(reference, headers, ARCHIVE_DATA_SYSTEM);
        String schema = requireHeader(reference, headers, ARCHIVE_DATA_SCHEMA);
        int schemaVersion = requireVersionHeader(reference, headers);
        String storagePrefix = optionalHeader(headers, ARCHIVE_STORAGE_PREFIX);
        String storageBucket = optionalHeader(headers, ARCHIVE_STORAGE_BUCKET);
        List<Metadata> metadata = metadata(headers);

        return ArchiveData.builder()
                .contentType(contentType)
                .system(system)
                .schema(schema)
                .schemaVersion(schemaVersion)
                .referenceId(reference.getId())
                .version(reference.getVersion())
                .payload(payload)
                .storagePrefix(Optional.ofNullable(storagePrefix))
                .storageBucket(Optional.ofNullable(storageBucket))
                .metadata(metadata)
                .build();
    }

    private static List<Metadata> metadata(HttpHeaders headers) {

        return headers.entrySet().stream()
                .filter(metadataHeader -> metadataHeader.getKey().toLowerCase().startsWith(ARCHIVE_METADATA))
                .flatMap(ArchiveDataHttpResponseMapper::toMetadataStream)
                .collect(toList());
    }

    private static Stream<Metadata> toMetadataStream(Map.Entry<String, List<String>> metadataHeaders) {
        String nameWithoutPrefix = metadataHeaders.getKey().toLowerCase().substring(ARCHIVE_METADATA.length());
        return metadataHeaders.getValue().stream()
                .map(value -> Metadata.of(nameWithoutPrefix, value));
    }

    private static String requireHeader(ArchiveDataReference reference, HttpHeaders headers, String headerName) {
        String value = optionalHeader(headers, headerName);
        if (value == null) {
            throw RemoteDataProviderException.missingHeader(reference, headerName);
        }
        return value;
    }

    private static int requireVersionHeader(ArchiveDataReference reference, HttpHeaders headers) {
        String value = requireHeader(reference, headers, ARCHIVE_DATA_SCHEMA_VERSION);
        try {
            int version = Integer.parseInt(value);
            if (version < 1) {
                throw RemoteDataProviderException.invalidVersion(reference, ARCHIVE_DATA_SCHEMA_VERSION, "Version cannot be < 1: " + version);
            }
            return version;
        } catch (NumberFormatException e) {
            throw RemoteDataProviderException.invalidVersion(reference, ARCHIVE_DATA_SCHEMA_VERSION, e.getMessage());
        }
    }

    private static String optionalHeader(HttpHeaders headers, String headerName) {
        List<String> headerValues = headers.getValuesAsList(headerName);
        return headerValues.isEmpty() ? null : headerValues.get(0);
    }
}
