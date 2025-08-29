package ch.admin.bit.jeap.test.processarchive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivetype.ArchiveTypeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@TestConfiguration
public class TestTypeLoaderConfig {
    @Bean
    public ArchiveTypeProvider archiveTypeProvider() {
        return () -> List.of(
                ch.admin.bit.jeap.processarchive.test.decree.v1.Decree.class,
                ch.admin.bit.jeap.processarchive.test.decree.v2.Decree.class);
    }
}
