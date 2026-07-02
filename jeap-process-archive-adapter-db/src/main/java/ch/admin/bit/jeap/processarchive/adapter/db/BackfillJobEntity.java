package ch.admin.bit.jeap.processarchive.adapter.db;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "backfill_job")
@Getter
@Setter
public class BackfillJobEntity {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "message_name", nullable = false)
    private String messageName;

    @Column(name = "config_id")
    private String configId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_state", nullable = false)
    private BackfillJobState jobState;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_result")
    private BackfillJobResult jobResult;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "report_created_at")
    private Instant reportCreatedAt;

    @Column(name = "started_by_name")
    private String startedByName;

    @Column(name = "started_by_ext_id")
    private String startedByExtId;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackfillTaskEntity> tasks = new ArrayList<>();

    void addTask(BackfillTaskEntity task) {
        tasks.add(task);
        task.setJob(this);
    }
}
