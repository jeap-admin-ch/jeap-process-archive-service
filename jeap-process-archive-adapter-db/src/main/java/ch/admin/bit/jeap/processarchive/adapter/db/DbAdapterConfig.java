package ch.admin.bit.jeap.processarchive.adapter.db;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@PropertySource("classpath:process-archive-db-defaults.properties")
@ConditionalOnProperty(value = "jeap.processarchive.backfill.enabled", havingValue = "true")
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ConditionalOnBean(DataSource.class)
@EntityScan(basePackageClasses = BackfillJobEntity.class)
@EnableJpaRepositories(basePackageClasses = BackfillJobRepository.class)
class DbAdapterConfig {

    @Bean
    JpaBackfillJobAdapter jpaBackfillJobAdapter(BackfillJobRepository backfillJobRepository) {
        return new JpaBackfillJobAdapter(backfillJobRepository);
    }
}
