package ch.admin.bit.jeap.processarchive.avro.plugin.git;


import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;

import java.nio.file.Path;
import java.util.StringJoiner;

public record NewArchiveTypeVersionDto(
        String systemName,
        Path descriptorPath,
        ArchiveTypeDescriptor typeDescriptor,
        Integer version) {

    @Override
    public String toString() {
        return new StringJoiner(", ", NewArchiveTypeVersionDto.class.getSimpleName() + "[", "]")
                .add("systemName='" + systemName + "'")
                .add("version='" + version + "'")
                .add("descriptorPath=" + descriptorPath)
                .toString();
    }
}
