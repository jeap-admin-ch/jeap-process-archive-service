package ch.admin.bit.jeap.processarchive.adapter.db;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BackfillJobRepository extends JpaRepository<BackfillJobEntity, UUID> {

    @EntityGraph(attributePaths = "tasks")
    Optional<BackfillJobEntity> findWithTasksByJobId(UUID jobId);
}
