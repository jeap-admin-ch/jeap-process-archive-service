package ch.admin.bit.jeap.processarchive.adapter.opensearch.config;

import ch.admin.bit.jeap.processarchive.adapter.opensearch.SearchItemsProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {SearchItemsProvider.class})
class OpenSearchAdapterConfig {
}
