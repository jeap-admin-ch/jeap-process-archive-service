package ch.admin.bit.jeap.processarchive.adapter.restapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BackfillJobRequest(
        @NotBlank String message,
        @JsonProperty("config-id") String configId,
        @NotEmpty List<@Valid ArchiveDataReferenceRequest> archiveDataReferences) {

    public record ArchiveDataReferenceRequest(@NotBlank String id, Integer version) {
    }
}
