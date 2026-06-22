package ch.admin.bit.jeap.processarchive.adapter.restapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BackfillJobRequest(
        @NotBlank String message,
        @NotBlank String topic,
        @NotEmpty List<@Valid ArchiveDataReferenceRequest> archiveDataReferences) {

    public record ArchiveDataReferenceRequest(@NotBlank String id, Integer version) {
    }
}
