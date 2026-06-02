package ch.admin.bit.jeap.processarchive.domain.configuration;

import ch.admin.bit.jeap.processarchive.plugin.api.indextype.ArchiveTypeToSearchItemConverter;

public record IndexTypeConfiguration(String indexType,
                                     Class<Object> archiveType,
                                     ArchiveTypeToSearchItemConverter<Object> archiveTypeToSearchItemConverter) {

}
