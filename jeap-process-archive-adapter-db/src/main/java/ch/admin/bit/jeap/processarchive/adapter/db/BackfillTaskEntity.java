package ch.admin.bit.jeap.processarchive.adapter.db;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTaskState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "backfill_task",
        indexes = {
                @Index(name = "idx_backfill_task_job_state", columnList = "job_id, task_state"),
                @Index(name = "idx_backfill_task_job_reference", columnList = "job_id, reference_id, reference_version")
        })
@Getter
@Setter
public class BackfillTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private BackfillJobEntity job;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "reference_version", nullable = false)
    private Integer referenceVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_state", nullable = false)
    private BackfillTaskState taskState;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "error_trace_id")
    private String errorTraceId;
}
