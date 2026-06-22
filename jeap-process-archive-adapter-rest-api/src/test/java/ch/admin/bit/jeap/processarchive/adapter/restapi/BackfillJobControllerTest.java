package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobException;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJob;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobService;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobSubmission;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTask;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTaskState;
import ch.admin.bit.jeap.security.resource.configuration.SemanticMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BackfillJobController.class, properties = {
      "jeap.processarchive.backfill.enabled=true",
      "jeap.processarchive.backfill.topic=jeap-process-archive-createartifact"
})
@ContextConfiguration(classes = {
        BackfillJobControllerTest.TestApplication.class,
        BackfillJobController.class,
        BackfillJobExceptionHandler.class
})
@Import(BackfillJobControllerTest.YamlTestConfig.class)
@AutoConfigureMockMvc
class BackfillJobControllerTest {

    private static final UUID JOB_ID = UUID.fromString("2ad48b66-472b-4a83-8efe-66a5f53ca111");
    private static final String REQUEST_YAML = """
            message: JmeDecreeDocumentCreatedEvent
            topic: jme-process-archive-decreedocumentcreated
            archiveDataReferences:
              - id: DOC-2024-001
                version: 1
              - id: DOC-2024-002
                version: 1
            """;
    private static final String REQUEST_YAML_WITHOUT_VERSION = """
            message: JmeDecreeDocumentCreatedEvent
            topic: jme-process-archive-decreedocumentcreated
            archiveDataReferences:
              - id: DOC-2024-001
            """;
    private static final Instant STARTED = Instant.parse("2026-05-08T07:26:37.123Z");
    private static final Instant REPORT_CREATED = Instant.parse("2026-05-08T07:30:15.456Z");

    private static final SemanticApplicationRole WRITE_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("backfilljob")
            .operation("write")
            .build();

    private static final SemanticApplicationRole READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("backfilljob")
            .operation("read")
            .build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackfillJobService backfillJobService;

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void createBackfillJob_validYaml_returnsOkAndSubmitsJob() throws Exception {
        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(backfillJobService).submitBackfillJob(any());
    }

    @Test
    void createBackfillJob_validXYaml_returnsOkAndSubmitsJob() throws Exception {
        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_X_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(backfillJobService).submitBackfillJob(any());
    }

    @Test
    void createBackfillJob_withoutReferenceVersion_returnsOkAndSubmitsJobWithoutVersion() throws Exception {
        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML_WITHOUT_VERSION)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isOk());

        ArgumentCaptor<BackfillJobSubmission> submissionCaptor = ArgumentCaptor.forClass(BackfillJobSubmission.class);
        verify(backfillJobService).submitBackfillJob(submissionCaptor.capture());
        assertEquals("DOC-2024-001", submissionCaptor.getValue().archiveDataReferences().getFirst().id());
        assertNull(submissionCaptor.getValue().archiveDataReferences().getFirst().version());
    }

    @Test
    void createBackfillJob_conflictingExistingJob_returnsConflict() throws Exception {
        doThrow(BackfillJobException.conflict("conflict")).when(backfillJobService).submitBackfillJob(any());

        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void createBackfillJob_withoutWriteRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(READ_ROLE))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(backfillJobService);
    }

    @Test
    void getReport_runningJob_returnsYamlReport() throws Exception {
        when(backfillJobService.getBackfillJob(JOB_ID)).thenReturn(Optional.of(runningJob()));

        mockMvc.perform(get("/api/jobs/{jobId}/report", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(BackfillJobController.APPLICATION_YAML_VALUE))
                .andExpect(content().string(containsString("message: \"JmeDecreeDocumentCreatedEvent\"")))
                .andExpect(content().string(containsString("topic: \"jme-process-archive-decreedocumentcreated\"")))
                .andExpect(content().string(containsString("job-state: \"open\"")))
                .andExpect(content().string(not(containsString("job-result:"))))
                .andExpect(content().string(containsString("job-id: \"2ad48b66-472b-4a83-8efe-66a5f53ca111\"")))
                .andExpect(content().string(containsString("started: \"2026-05-08T07:26:37.123Z\"")))
                .andExpect(content().string(not(containsString("report-created:"))))
                .andExpect(content().string(containsString("started-by-name: \"John Doe\"")))
                .andExpect(content().string(containsString("started-by-ext_id: \"287365\"")))
                .andExpect(content().string(containsString("archiveDataReferences:")))
                .andExpect(content().string(containsString("id: \"DOC-2024-001\"")))
                .andExpect(content().string(containsString("id: \"DOC-2024-002\"")))
                .andExpect(content().string(containsString("state: \"open\"")));
    }

    @Test
    void getReport_runningJobWithoutVersion_omitsVersion() throws Exception {
        when(backfillJobService.getBackfillJob(JOB_ID)).thenReturn(Optional.of(runningJobWithoutVersion()));

        mockMvc.perform(get("/api/jobs/{jobId}/report", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id: \"DOC-2024-001\"")))
                .andExpect(content().string(not(containsString("version:"))));
    }

    @Test
    void getReport_completedJob_returnsYamlReport() throws Exception {
        when(backfillJobService.getBackfillJob(JOB_ID)).thenReturn(Optional.of(completedJob()));

        mockMvc.perform(get("/api/jobs/{jobId}/report", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(BackfillJobController.APPLICATION_YAML_VALUE))
                .andExpect(content().string(containsString("message: \"JmeDecreeDocumentCreatedEvent\"")))
                .andExpect(content().string(containsString("topic: \"jme-process-archive-decreedocumentcreated\"")))
                .andExpect(content().string(containsString("job-state: \"completed\"")))
                .andExpect(content().string(containsString("job-result: \"partially-succeeded\"")))
                .andExpect(content().string(containsString("job-id: \"2ad48b66-472b-4a83-8efe-66a5f53ca111\"")))
                .andExpect(content().string(containsString("started: \"2026-05-08T07:26:37.123Z\"")))
                .andExpect(content().string(containsString("report-created: \"2026-05-08T07:30:15.456Z\"")))
                .andExpect(content().string(containsString("started-by-name: \"John Doe\"")))
                .andExpect(content().string(containsString("started-by-ext_id: \"287365\"")))
                .andExpect(content().string(containsString("id: \"DOC-2024-001\"")))
                .andExpect(content().string(containsString("state: \"succeeded\"")))
                .andExpect(content().string(containsString("id: \"DOC-2024-002\"")))
                .andExpect(content().string(containsString("state: \"failed\"")))
                .andExpect(content().string(containsString("error:")))
                .andExpect(content().string(containsString("message: \"Failed reading artifact from source service\"")))
                .andExpect(content().string(containsString("traceId: \"4bf92f3577b34da6a3ce929d0e0e4736\"")));
    }

    @Test
    void getReport_unknownJob_returnsNotFound() throws Exception {
        when(backfillJobService.getBackfillJob(JOB_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/{jobId}/report", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReport_withoutReadRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/jobs/{jobId}/report", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(WRITE_ROLE)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(backfillJobService);
    }

    private BackfillJob runningJob() {
        return new BackfillJob(
                JOB_ID,
                "JmeDecreeDocumentCreatedEvent",
                "jme-process-archive-decreedocumentcreated",
                BackfillJobState.OPEN,
                null,
                STARTED,
                null,
                "John Doe",
                "287365",
                List.of(
                        new BackfillTask(1L, "DOC-2024-001", 1, BackfillTaskState.OPEN, null, null),
                        new BackfillTask(2L, "DOC-2024-002", 1, BackfillTaskState.OPEN, null, null)));
    }

    private BackfillJob runningJobWithoutVersion() {
        return new BackfillJob(
                JOB_ID,
                "JmeDecreeDocumentCreatedEvent",
                "jme-process-archive-decreedocumentcreated",
                BackfillJobState.OPEN,
                null,
                STARTED,
                null,
                "John Doe",
                "287365",
                List.of(new BackfillTask(1L, "DOC-2024-001", null, BackfillTaskState.OPEN, null, null)));
    }

    private BackfillJob completedJob() {
        return new BackfillJob(
                JOB_ID,
                "JmeDecreeDocumentCreatedEvent",
                "jme-process-archive-decreedocumentcreated",
                BackfillJobState.COMPLETED,
                BackfillJobResult.PARTIALLY_SUCCEEDED,
                STARTED,
                REPORT_CREATED,
                "John Doe",
                "287365",
                List.of(
                        new BackfillTask(1L, "DOC-2024-001", 1, BackfillTaskState.SUCCEEDED, null, null),
                        new BackfillTask(2L, "DOC-2024-002", 1, BackfillTaskState.FAILED,
                                "Failed reading artifact from source service", "4bf92f3577b34da6a3ce929d0e0e4736")));
    }

    private RequestPostProcessor authenticationForUserRoles(SemanticApplicationRole... userroles) {
        JeapAuthenticationToken authentication = JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
        return request -> {
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            TestSecurityContextHolder.setContext(securityContext);
            return testSecurityContext().postProcessRequest(request);
        };
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    static class TestApplication {
        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext applicationContext) {
            SemanticMethodSecurityExpressionHandler expressionHandler = new SemanticMethodSecurityExpressionHandler("jme");
            expressionHandler.setApplicationContext(applicationContext);
            return expressionHandler;
        }
    }

    @TestConfiguration
    static class YamlTestConfig {
        @Bean
        JacksonYamlHttpMessageConverter yamlHttpMessageConverter() {
            JacksonYamlHttpMessageConverter converter = new JacksonYamlHttpMessageConverter();
            converter.setSupportedMediaTypes(java.util.List.of(
                    MediaType.parseMediaType(BackfillJobController.APPLICATION_YAML_VALUE),
                    MediaType.parseMediaType(BackfillJobController.APPLICATION_X_YAML_VALUE)
            ));
            return converter;
        }
    }
}
