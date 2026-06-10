package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobException;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobService;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobSubmission;
import ch.admin.bit.jeap.security.resource.configuration.SemanticMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BackfillJobController.class)
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
