package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillArchiveDataReference;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobService;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobSubmission;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobSubmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(value = "jeap.processarchive.backfill.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Backfill Jobs", description = "Submit PAS backfill jobs.")
@RequiredArgsConstructor
@Slf4j
public class BackfillJobController {

    static final String APPLICATION_YAML_VALUE = "application/yaml";
    static final String APPLICATION_X_YAML_VALUE = "application/x-yaml";

    private final BackfillJobService backfillJobService;

    @Operation(
            summary = "Create a process archive backfill job.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Job accepted or already submitted with same content"),
                    @ApiResponse(responseCode = "400", description = "Invalid request or unsupported archive configuration"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "409", description = "Job already exists with different content")
            },
            security = {@SecurityRequirement(name = "OIDC")}
    )
    @PreAuthorize("hasRole('backfilljob', 'write')")
    @PutMapping(value = "/{jobId}", consumes = {APPLICATION_YAML_VALUE, APPLICATION_X_YAML_VALUE})
    public ResponseEntity<Void> createBackfillJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody BackfillJobRequest request,
            Authentication authentication) {
        log.info("Received backfill job submit request for job '{}', message '{}' and topic '{}'.",
                jobId, request.message(), request.topic());

        backfillJobService.submitBackfillJob(toSubmission(jobId, request, jwt(authentication)));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get the process archive backfill job report.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Backfill job report", content = @Content(
                            mediaType = APPLICATION_YAML_VALUE,
                            examples = @ExampleObject(value = """
                                    message: JmeDecreeDocumentCreatedEvent
                                    topic: jme-process-archive-decreedocumentcreated
                                    job-state: completed
                                    job-result: partially-succeeded
                                    job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e
                                    started: 2026-05-08T07:26:37.123Z
                                    report-created: 2026-05-08T07:30:15.456Z
                                    started-by-name: John Doe
                                    started-by-ext_id: "287365"
                                    archiveDataReferences:
                                    - id: DOC-2024-001
                                      version: 1
                                      state: succeeded
                                    - id: DOC-2024-002
                                      version: 1
                                      state: failed
                                      error:
                                        message: Failed reading artifact from source service
                                        traceId: 4bf92f3577b34da6a3ce929d0e0e4736
                                    """))),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Backfill job not found")
            },
            security = {@SecurityRequirement(name = "OIDC")}
    )
    @PreAuthorize("hasRole('backfilljob', 'read')")
    @GetMapping(value = "/{jobId}/report", produces = APPLICATION_YAML_VALUE)
    public ResponseEntity<BackfillJobReport> getReport(@PathVariable UUID jobId) {
        return backfillJobService.getBackfillJob(jobId)
                .map(BackfillJobReport::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private BackfillJobSubmission toSubmission(UUID jobId, BackfillJobRequest request, Jwt jwt) {
        List<BackfillArchiveDataReference> references = request.archiveDataReferences().stream()
                .map(reference -> new BackfillArchiveDataReference(reference.id(), reference.version()))
                .toList();

        return new BackfillJobSubmission(
                jobId,
                request.message(),
                request.topic(),
                references,
                new BackfillJobSubmitter(claim(jwt, "name"), claim(jwt, "ext_id")));
    }

    private String claim(Jwt jwt, String claimName) {
        return jwt == null ? null : jwt.getClaimAsString(claimName);
    }

    private Jwt jwt(Authentication authentication) {
        return authentication instanceof JwtAuthenticationToken jwtAuthenticationToken ? jwtAuthenticationToken.getToken() : null;
    }
}
