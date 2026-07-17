package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.BackfillJobController;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(value = "jeap.processarchive.backfill.enabled", havingValue = "true")
class BackfillOpenApiConfig {

    @Bean
    GroupedOpenApi backfillJobsApi() {
        return GroupedOpenApi.builder()
                .group("Backfill Jobs API")
                .pathsToMatch("/api/jobs/**")
                .packagesToScan(BackfillJobController.class.getPackageName())
                .build();
    }
}
