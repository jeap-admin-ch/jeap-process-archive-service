package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.SearchItemsController;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Process Archive Service API",
                description = "API of the Process Archive Service",
                contact = @Contact(
                        email = "jeap-community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://github.com/jeap-admin-ch/jeap"
                )
        ),
        externalDocs = @ExternalDocumentation(
                url = "https://github.com/jeap-admin-ch/jeap-process-archive-service/blob/main/README.md",
                description = "Documentation Process Archive Service"),
        security = {@SecurityRequirement(name = "OIDC")}
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("Index API")
                .pathsToMatch("/index-api/**")
                .packagesToScan(SearchItemsController.class.getPackageName())
                .build();
    }
}
