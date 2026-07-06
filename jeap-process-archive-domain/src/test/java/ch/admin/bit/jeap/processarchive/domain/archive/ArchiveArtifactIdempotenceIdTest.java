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

        // sha256 of "18:msg-idempotence-id3:JME8:MySchema5:ref-11:2"
        assertThat(idempotenceId)
                .isEqualTo("TestEvent_d15ec0324853489294ae1fc4bd550ced9a7486017780db0301b29a7b2090cef2");
    }

    @Test
    void create_withoutVersion_producesKnownSha256Hash() {
        String idempotenceId = ArchiveArtifactIdempotenceId.create(
                MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref-1", null);

        // sha256 of "18:msg-idempotence-id3:JME8:MySchema5:ref-1"
        assertThat(idempotenceId)
                .isEqualTo("TestEvent_4361c4f785dec328d8f8deed9ece2e58379bf22c7c0c928bcd44b3e2293ed9d7");
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
    void create_fieldBoundariesAreUnambiguous() {
        // without length-prefixing, both would hash "..._ref_2"
        assertThat(ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref", 2))
                .isNotEqualTo(ArchiveArtifactIdempotenceId.create(MESSAGE_TYPE, MESSAGE_IDEMPOTENCE_ID, "JME", "MySchema", "ref_2", null));
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
