package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2EventPayload;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;

import java.nio.charset.StandardCharsets;

public class TestConfigModeDataProvider implements MessageArchiveDataProvider<TestDomain2Event> {

    @Override
    public ArchiveData getArchiveData(TestDomain2Event event) {
        String payload = event.getOptionalPayload()
                .map(TestDomain2EventPayload::getData)
                .orElse("no-data");
        return ArchiveData.builder()
                .system("Jme")
                .schema("MyType")
                .schemaVersion(1)
                .referenceId(event.getIdentity().getEventId())
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .contentType("application/octet-stream")
                .build();
    }
}
