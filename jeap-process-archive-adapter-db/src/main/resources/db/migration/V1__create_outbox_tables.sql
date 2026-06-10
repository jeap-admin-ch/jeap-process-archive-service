create sequence deferred_message_sequence start with 1 increment by 1;

create table deferred_message
(
    id                     bigint primary key,
    message                bytea        not null,
    "key"                  bytea,
    cluster_name           varchar(255),
    topic                  varchar(255) not null,
    message_id             varchar(255) not null,
    message_idempotence_id varchar(255) not null,
    message_type_name      varchar(255) not null,
    message_type_version   varchar(255),
    created                timestamp with time zone not null,
    send_immediately       boolean      not null,
    schedule_after         timestamp with time zone,
    sent_immediately       timestamp with time zone,
    sent_scheduled         timestamp with time zone,
    failed                 timestamp with time zone,
    fail_reason            varchar(255),
    resend                 boolean      not null,
    trace_id_high          bigint,
    trace_id               bigint,
    span_id                bigint,
    parent_span_id         bigint,
    trace_id_string        varchar(255),
    sampled                boolean
);

create index idx_deferred_message_ready on deferred_message (send_immediately, sent_immediately, sent_scheduled, failed, schedule_after, resend);
create index idx_deferred_message_failed on deferred_message (failed, resend);

create table shedlock
(
    name       varchar(64) primary key,
    lock_until timestamp with time zone not null,
    locked_at  timestamp with time zone not null,
    locked_by  varchar(255) not null
);
