package ch.admin.bit.jeap.processarchive.domain.configuration;

import java.util.List;
import java.util.Optional;

public interface IndexTypeConfigurationRepository {

    List<IndexTypeConfiguration> getAll();

    Optional<IndexTypeConfiguration> findByName(String indexType);
}
