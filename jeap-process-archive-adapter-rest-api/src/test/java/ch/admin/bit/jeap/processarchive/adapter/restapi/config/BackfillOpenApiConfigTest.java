package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.BackfillJobController;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BackfillOpenApiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BackfillOpenApiConfig.class));

    @Test
    void backfillEnabledExposesBackfillOpenApiGroup() {
        contextRunner
                .withPropertyValues("jeap.processarchive.backfill.enabled=true")
                .run(context -> {
                    GroupedOpenApi api = context.getBean(GroupedOpenApi.class);

                    assertThat(api.getGroup()).isEqualTo("Backfill Jobs API");
                    assertThat(api.getPathsToMatch()).containsExactly("/api/jobs/**");
                    assertThat(api.getPackagesToScan())
                            .containsExactly(BackfillJobController.class.getPackageName());
                });
    }

    @Test
    void backfillDisabledDoesNotExposeBackfillOpenApiGroup() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(GroupedOpenApi.class));
    }
}
