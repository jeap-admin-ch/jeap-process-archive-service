create table backfill_job
(
    job_id            uuid primary key,
    message_name      varchar(255) not null,
    topic_name        varchar(255) not null,
    job_state         varchar(32)  not null,
    job_result        varchar(32),
    started_at        timestamp with time zone not null,
    report_created_at timestamp with time zone,
    started_by_name   varchar(255),
    started_by_ext_id varchar(255)
);

create table backfill_task
(
    id                bigserial primary key,
    job_id            uuid         not null references backfill_job (job_id) on delete cascade,
    reference_id      varchar(255) not null,
    reference_version integer      not null,
    task_state        varchar(32)  not null,
    error_message     text,
    error_trace_id    varchar(255)
);

create index idx_backfill_task_job_state on backfill_task (job_id, task_state);
create index idx_backfill_task_job_reference on backfill_task (job_id, reference_id, reference_version);
