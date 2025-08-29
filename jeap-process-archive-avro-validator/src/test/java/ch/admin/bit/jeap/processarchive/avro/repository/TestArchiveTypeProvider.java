package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.plugin.api.archivetype.ArchiveTypeProvider;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestArchiveTypeProvider implements ArchiveTypeProvider {

    @Override
    public List<Class<? extends SpecificRecordBase>> getArchiveTypeVersions() {
        return List.of(
                ch.admin.bit.jeap.processarchive.test.decree.v1.Decree.class,
                ch.admin.bit.jeap.processarchive.test.decree.v2.Decree.class);
    }
}
