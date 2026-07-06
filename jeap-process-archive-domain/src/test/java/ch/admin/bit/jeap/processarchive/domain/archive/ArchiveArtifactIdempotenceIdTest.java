package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveArtifactIdempotenceIdTest {

    private static final String MESSAGE_TYPE = "TestEvent";
    private static final String MESSAGE_IDEMPOTENCE_ID = "msg-idempotence-id";

    @Test
    void create_withVersion_producesKnownSha256Hash() {
        String idempotenceId = ArchiveArtifactIdempotenceId.create(
                MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", 2);

        // sha256 of "msg-idempotence-id_JME_MySchema_ref-1_2"
        assertThat(idempotenceId)
                .isEqualTo("TestEvent_6323945aa21e854bc9b603c263e611d1025753ff7cb2b285c0ea44de161d31a4");
    }

    @Test
    void create_withoutVersion_producesKnownSha256Hash() {
        String idempotenceId = ArchiveArtifactIdempotenceId.create(
                MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", null);

        // sha256 of "msg-idempotence-id_JME_MySchema_ref-1"
        assertThat(idempotenceId)
                .isEqualTo("TestEvent_821a17c6752f5b632f90b27fb09cc0649f8d25524dc35399ca263d55b982714a");
    }

    @Test
    void create_isDeterministicAndFixedLength() {
        String idempotenceId = ArchiveArtifactIdempotenceId.create(
                MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "a-very-long-reference-id".repeat(20), 1);

        assertThat(idempotenceId)
                .isEqualTo(ArchiveArtifactIdempotenceId.create(
                        MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "a-very-long-reference-id".repeat(20), 1))
                .matches(MESSAGE_TYPE + "_[0-9a-f]{64}")
                .hasSize(MESSAGE_TYPE.length() + 65);
    }

    @Test
    void create_changesWhenAnyPartChanges() {
        String reference = ArchiveArtifactIdempotenceId.create(
                MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", 2);

        assertThat(List.of(
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, "other-id", "JME", "MySchema", "ref-1", 2),
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "OTHER", "MySchema", "ref-1", 2),
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "OtherSchema", "ref-1", 2),
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-2", 2),
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", 3),
                ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", null)))
                .doesNotContain(reference)
                .doesNotHaveDuplicates();
    }

    @Test
    void create_fromArchiveData_matchesStringBasedOverload() {
        ArchiveData archiveData = ArchiveData.builder()
                .system("JME")
                .schema("MySchema")
                .schemaVersion(1)
                .referenceId("ref-1")
                .version(2)
                .payload("payload".getBytes(StandardCharsets.UTF_8))
                .contentType("avro/binary")
                .metadata(List.of())
                .build();

        assertThat(ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, archiveData))
                .isEqualTo(ArchiveArtifactIdempotenceId.create(
                        MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", 2));
    }
}
