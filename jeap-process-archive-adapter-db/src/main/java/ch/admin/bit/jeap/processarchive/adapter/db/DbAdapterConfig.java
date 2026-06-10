package ch.admin.bit.jeap.processarchive.adapter.db;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan
@EntityScan(basePackageClasses = BackfillJobEntity.class)
@EnableJpaRepositories(basePackageClasses = BackfillJobRepository.class)
class DbAdapterConfig {
}
